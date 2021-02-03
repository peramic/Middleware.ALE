package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.report.IReports;
import havis.middleware.ale.core.report.Initiation;
import havis.middleware.ale.core.report.ReportsInfo;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.CCCmdReport;
import havis.middleware.ale.service.cc.CCCmdSpec;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.cc.CCReports.CmdReports;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.utils.threading.Task;
import havis.middleware.utils.threading.ThreadManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class will be used to generate a set of reports as specified : ALE 1.1.1
 * (9.4). Currently it only validates basically any {@link CCCmdSpec} within
 * this specification. It also keeps the {@link Report} and {@link Filter}
 * lists.
 */
public class Reports implements Task, IReports<CCReports, Tags> {

	private String name;
	private CCSpec spec;
	private BlockingQueue<ReportsInfo<CCReports, Tags>> queue;
	private List<Report> reports;
	private TagOperation readerOperation;
	private Parameters parameters;

	/**
	 * Creates a new instance. Keeps a {@link Report} and {@link Filter}
	 * instance for each {@link CCCmdSpec} within the given specification.
	 * 
	 * @param name
	 *            Report output name
	 * @param spec
	 *            The command cycle specification
	 * @throws ValidationException
	 *             if the specification contains no {@link CCCmdSpec}, the
	 *             command specifications or filters are invalid.
	 */
	public Reports(String name, CCSpec spec) throws ValidationException {
		if ((spec.getCmdSpecs() == null) || spec.getCmdSpecs().getCmdSpec().size() == 0)
			throw new ValidationException("No command specification given");

		try {
			this.name = name == null ? "" : name;
			this.spec = spec;
			queue = new LinkedBlockingQueue<ReportsInfo<CCReports, Tags>>();
			parameters = new Parameters();

			// reader operations
			int id = 0;
			List<havis.middleware.ale.base.operation.tag.Operation> operations = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

			reports = new ArrayList<Report>();

			List<String> names = new ArrayList<String>(); // report name
			// validation
			for (CCCmdSpec cmdSpec : spec.getCmdSpecs().getCmdSpec()) {
				Report report = new Report(cmdSpec, parameters);
				reports.add(report);
				if (names.contains(report.getName())) {
					throw new ValidationException("Command specification already contains the name '" + report.getName() + "'");
				}
				names.add(report.getName());
				for (havis.middleware.ale.base.operation.tag.Operation operation : report.getFilter().getOperations()) {
					operation.setId(++id);
					operations.add(operation);
				}
				for (CCOperation operation : report.getOperations()) {
					operation.setId(++id);
					if (operation.isAdvanced())
						operations.add(operation.getBase());
				}
			}

			readerOperation = new TagOperation(operations);
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the available parameters for poll
	 */
	public Parameters getParameters() {
		return parameters;
	}

	/**
	 * Retrieve reader operation to get additional data from tag
	 */
	public TagOperation getReaderOperation() {
		return readerOperation;
	}

	/**
	 * Enqueues a report info instance for asynchronous report generation
	 * 
	 * @param info
	 *            The report info
	 */
	@Override
	public void enqueue(ReportsInfo<CCReports, Tags> info) {
		queue.add(info);
		for (SubscriberController subscriber : info.getSubscribers()) {
			subscriber.inc();
		}
		ThreadManager.enqueue(this);
	}

	@Override
	public int getGroupId() {
		return hashCode();
	}

	/**
	 * Dequeues the first {@link ReportsInfo} instance, generates the command
	 * cycle report set and enqueues it to all subscribers. The report info
	 * object contains the past and present tags, so that this this method
	 * determines the additions, current and deletions tag list and decides for
	 * each report depending on its type, for which set of tags it has to
	 * generate a report. The data form {@link ReportsInfo} was also refer here.
	 * Finally this method decides if a report set instance of set or not i.e if
	 * report set instance of empty.
	 */
	@Override
	public void run() {
		try {
			synchronized (queue) {
				final ReportsInfo<CCReports, Tags> info;
				info = queue.take();
				if (queue.isEmpty())
					queue.notify();

				// Initialize reports
				CCReports ccReports = new CCReports(new BigDecimal(1), info.getDate(), name, info.getDate(), Config.getInstance().getGlobal().getAleid(),
						info.getTotalMilliseconds(), Initiation.toString(info.getInitiation()), info.getInitiator(),
						Termination.toString(info.getTermination()), info.getTerminator());

				// send empty report to each outstanding all poll call
				if ((info.getTermination() == Termination.UNDEFINE) && (info.getInitiation() == Initiation.UNDEFINE)) {
					Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Deliver, "Cycle {0} delivered {1}",
							new Object[] { this.name, ccReports });
					for (SubscriberController subscriber : info.getSubscribers()) {
						subscriber.enqueue(ccReports);
					}
				} else {
					// Initialize report list
					ccReports.setCmdReports(new CmdReports());

					// include specification
					if (spec.isIncludeSpecInReports()) {
						ccReports.setCCSpec(spec);
					}

					for (Report report : reports) {
						List<Tag> tags = info.getDatas().toList();
						if (tags.size() > 0) {
							long counter = info.getDatas().await(null);
							// TODO: really completed?
							for (Tag tag : tags) {
								if (!isCompleted(tag)) {
									counter = info.getDatas().await(Long.valueOf(counter));
								}
							}
							// Removes all tags with empty result
							List<Tag> removable = new ArrayList<>();
							for (Tag tag : tags) {
								if (tag.getResult() == null) {
									removable.add(tag);
								} else {
									for (Entry<Integer, Result> entry : tag.getResult().entrySet()) {
										Result result = entry.getValue();
										if (result == null) {
											removable.add(tag);
										}
									}
								}
							}
							tags.removeAll(removable);
						}
						// Check if at least one reportSpec says reportIfEmpty
						if (report.getSpec().isReportIfEmpty() || (tags.size() > 0)) {
							CCCmdReport cmdReport = report.get(tags);
							if ((report.getSpec().isReportIfEmpty()) || (cmdReport.getTagReports().getTagReport().size() > 0)) {
								ccReports.getCmdReports().getCmdReport().add(cmdReport);
							}
						}
					}

					Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Deliver, "Cycle {0} delivered {1}",
							new Object[] { this.name, ccReports });
					if (ccReports.getCmdReports().getCmdReport().size() > 0) {
						// deliver report to each subscriber
						for (SubscriberController subscriber : info.getSubscribers()) {
							subscriber.enqueue(ccReports);
						}
					} else {
						// deliver report only to poll or immediate
						for (SubscriberController subscriber : info.getSubscribers()) {
							if (subscriber instanceof SubscriberListener<?>) {
								subscriber.enqueue(ccReports);
							} else {
								subscriber.dec();
							}
						}
					}
				}
				if (info.getTermination() == Termination.UNDEFINE) {
					dispose();
				}
			}
		} catch (Exception e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Error,
					"Run interrupted unexpectedly: " + e.getMessage(), e);
		}
	}

	/**
	 * Returns the operation to be execute on tag
	 * 
	 * @param tag
	 *            The tag
	 * @return Operation to be execute
	 * @throws ValidationException
	 */
	public TagOperation getTagOperation(Tag tag) {
		boolean exists = false;
		List<havis.middleware.ale.base.operation.tag.Operation> operations = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();
		for (Report report : reports) {
			Boolean match = report.getFilter().match(tag);
			if (Boolean.TRUE.equals(match)) {
				exists = true;
				report.getOperations(tag, operations);
			}
		}
		if (exists) {
			return new TagOperation(operations, tag.getFilter());
		}
		return null;
	}

	boolean isCompleted(havis.middleware.ale.base.operation.tag.Tag tag) {
		if (tag.isCompleted()) {
			return true;
		} else {
			for (Report report : reports) {
				if (Boolean.TRUE.equals(report.getFilter().match(tag))) {
					if (!report.isCompleted(tag))
						return false;
				}
			}
			tag.setCompleted(true);
			return true;
		}
	}

	/**
	 * Disposes each report
	 */
	@Override
	public void dispose() {
		if (queue != null) {
			synchronized (queue) {
				while (!queue.isEmpty())
					try {
						queue.wait();
					} catch (InterruptedException e) {
						break; // stop waiting
					}
			}
		}

		if (reports != null) {
			for (Report report : reports) {
				report.dispose();
			}
			reports = null;
		}
	}
}