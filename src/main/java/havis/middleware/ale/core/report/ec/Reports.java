package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.report.IReports;
import havis.middleware.ale.core.report.Initiation;
import havis.middleware.ale.core.report.ReportsInfo;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ec.ECReport;
import havis.middleware.ale.service.ec.ECReportSpec;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.utils.threading.Task;
import havis.middleware.utils.threading.ThreadManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is used to generate a report set as specified in ALE 1.1.1 (8.3).
 * It validates the name, filters and each report. It allows to enqueue a
 * {@link ReportsInfo} and implements the {@link Runnable} interface to generate
 * asynchrony event cycle reports
 */
public class Reports implements Task, IReports<ECReports, Tags> {

	private String name;
	private ECSpec spec;
	private BlockingQueue<ReportsInfo<ECReports, Tags>> queue;
	private List<Report> reports;

	/**
	 * Retrieves the primary key fields
	 */
	private List<CommonField> fields;

	/**
	 * Retrieve reader operation to get additional data from tag
	 */
	private TagOperation readerOperation;

	/**
	 * creates a new instance. validate name, reports specification and filters.
	 * Keeps spec, name, reports, filters and reader operations.
	 *
	 * @param name
	 *            The report output name
	 * @param spec
	 *            The event cycle specification
	 * @throws ValidationException
	 *             if specification contains no or invalid report
	 *             specifications, report names not unique, filters or reader
	 *             operations are invalid.
	 */
	public Reports(String name, ECSpec spec) throws ValidationException {
		if ((spec.getReportSpecs() == null)
				|| spec.getReportSpecs().getReportSpec() == null
				|| (spec.getReportSpecs().getReportSpec().size() == 0)) {
			throw new ValidationException("No report specification given");
		} else {
			try {
				this.name = name == null ? "" : name;
				this.spec = spec;
				queue = new LinkedBlockingQueue<ReportsInfo<ECReports, Tags>>();

				// reader operations
				int id = 0;
				List<Operation> operations = new ArrayList<Operation>();

				if (spec.getExtension() != null) {
					if (spec.getExtension().getPrimaryKeyFields() != null) {
						fields = new ArrayList<CommonField>();
						try {
							Fields.lock();
							for (final String fieldname : spec.getExtension()
									.getPrimaryKeyFields().getPrimaryKeyField()) {
								CommonField field;
								if ((field = Fields.getInstance().get(new ECFieldSpec(fieldname))) == null) {
									throw new ValidationException("Primary key field '" + fieldname + "' does not exist!");
								} else if (field.getFieldDatatype() == FieldDatatype.ISO) {
									throw new ValidationException("Variable field '" + fieldname + "' is not supported as primary key field!");									
								} else {
									// TODO: locking isn't atomic
									field.inc();
									fields.add(field);
									if (!"epc".equals(fieldname)) {
										if (!Tag.isExtended()
												|| !"tidBank".equals(fieldname)) {
											operations.add(new Operation(++id,
													OperationType.READ, field
															.getField()));
										}
									}
								}
							}
						} finally {
							Fields.unlock();
						}
					}
				}

				reports = new ArrayList<Report>();

				List<String> names = new ArrayList<String>(); // report name
				// validation
				for (ECReportSpec reportSpec : spec.getReportSpecs()
						.getReportSpec()) {
					Report report = new Report(reportSpec);
					reports.add(report);
					if (names.contains(report.getName())) {
						throw new ValidationException(
								"Report specification already contains a report with name '"
										+ report.getName() + "'");
					}
					names.add(report.getName());
					for (Operation operation : report.getOperations()) {
						operation.setId(++id);
						operations.add(operation);
					}
				}

				readerOperation = new TagOperation(operations, null);
			} catch (ValidationException e) {
				dispose();
				throw e;
			}
		}
	}

	/**
	 * Gets the fields
	 *
	 * @return The fields
	 */
	public Iterable<CommonField> getFields() {
		return fields;
	}

	/**
	 * Gets the reader operation
	 *
	 * @return The reader operation
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
	public void enqueue(ReportsInfo<ECReports, Tags> info) {
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
	 * Dequeues the first {@link ReportsInfo} instance, generates the event
	 * cycle report set and enqueues it to all subscribers. The report info
	 * object contains the past and present tags, so that this this method
	 * determines the additions, current and deletions tag list and decides for
	 * each report depending on its type, for which set of tags it has to
	 * generate a report. The data form {@link ReportsInfo} was also refer
	 * here.Finally this method decides if a report set is set or not i.e if
	 * report set is empty.
	 */
	@Override
	public void run() {
		try {
            synchronized (this.queue) {
    			final ReportsInfo<ECReports, Tags> info;

    			info = queue.take();
                if (this.queue.isEmpty())
                    this.queue.notify();

    			// initialize reports
    			ECReports ecReports = new ECReports(new BigDecimal(1),
    					info.getDate(), name, info.getDate(), Config.getInstance()
    							.getGlobal().getAleid(),
    					info.getTotalMilliseconds(), Initiation.toString(info
    							.getInitiation()), info.getInitiator(),
    					Termination.toString(info.getTermination()),
    					info.getTerminator());

    			// send empty report to each outstanding all poll call
    			if ((info.getTermination() == Termination.UNDEFINE)
    					&& (info.getInitiation() == Initiation.UNDEFINE)) {
    				Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Deliver,
    						"Cycle {0} delivered {1}", new Object[] { this.name, ecReports });
    				for (SubscriberController subscriber : info.getSubscribers()) {
    					subscriber.enqueue(ecReports);
    				}
    			} else {
    				Map<PrimaryKey, Tag> past = info.getDatas().getPast();
    				Map<PrimaryKey, Tag> present = info.getDatas().getPresent();

    				List<Tag> additions = new ArrayList<Tag>();
    				List<Tag> current = new ArrayList<Tag>();
    				List<Tag> deletions = new ArrayList<Tag>();

    				// separate additions, current and deletions
    				for (Entry<PrimaryKey, Tag> pair : present.entrySet()) {
    					Tag t = past.get(pair.getKey());
    					if (t != null) {
    						pair.getValue().merge(t);
    					} else {
    						additions.add(pair.getValue());
    					}
    					current.add(pair.getValue());
    				}
    				for (Entry<PrimaryKey, Tag> pair : past.entrySet()) {
    					if (!present.containsKey(pair.getKey())) {
    						pair.getValue().clear();
    						deletions.add(pair.getValue());
    					}
    				}

    				// initialize report list
    				ecReports.setReports(new ECReports.Reports());

    				// include specification
    				if (spec.isIncludeSpecInReports()) {
    					ecReports.setECSpec(spec);
    				}

    				if (reports != null) {
    					for (Report report : reports) {
    						List<Tag> tags;
    						switch (report.getSet()) {
    						case ADDITIONS:
    							tags = additions;
    							break;
    						case CURRENT:
    							tags = current;
    							break;
    						case DELETIONS:
    							tags = deletions;
    							break;
    						default:
    							tags = new ArrayList<Tag>();
    							break;
    						}

    						boolean changed = false;
    						if (report.getSpec().isReportOnlyOnChange())
        						// see ALE spec 2401-2403, change must be calculated in any case
    							changed = report.processChanged(current /* see ALE spec 2401-2402 */);

    						// check if at least one reportSpec says reportIfEmpty
    						if (report.getSpec().isReportIfEmpty()
    								|| (tags.size() > 0)) {
    							ECReport ecReport = report.get(tags);
    							if ((report.getSpec().isReportIfEmpty() || ecReport.getGroup().size() > 0)
    									&& (!report.getSpec().isReportOnlyOnChange() || changed)) {
    								ecReports.getReports().getReport().add(ecReport);
    							}
    						}
    					}
    				}

    				Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Deliver,
    						"Cycle {0} delivered {1}", new Object[] { this.name, ecReports });
    				if (ecReports.getReports().getReport().size() > 0) {
    					// deliver report to each subscriber
    					for (SubscriberController subscriber : info
    							.getSubscribers()) {
    						subscriber.enqueue(ecReports);
    					}
    				} else {
    					// deliver report only to poll or immediate
    					for (SubscriberController subscriber : info
    							.getSubscribers()) {
    						if (subscriber instanceof SubscriberListener) {
    							subscriber.enqueue(ecReports);
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
			Exits.Log.logp(Exits.Level.Error, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Error,
					"Run interrupted unexpectedly: " + e.getMessage(), e);
		}
	}

	/**
	 * Returns if a tag match to one filter of any contains reports
	 *
	 * @param tag
	 *            The tag
	 * @return True, if a filter matches, false otherwise
	 */
	public Boolean match(Tag tag) {
		Boolean match = Boolean.FALSE;
		for (Report report : reports) {
			Boolean m = report.getFilter().match(tag);
			if (Boolean.TRUE.equals(m))
				return Boolean.TRUE;
			if (m == null)
				match = null;
		}
		return match;
	}

	public boolean isCompleted(Tag tag) {
		for (Report report : reports) {
			if (!report.isCompleted(tag))
				return false;
		}
		return true;
	}

	/**
	 * Disposes each report and field
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
		if (fields != null) {
			for (CommonField field : fields) {
				field.dec();
			}
			fields = null;
		}
	}
}
