package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Event;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.report.IReports;
import havis.middleware.ale.core.report.Initiation;
import havis.middleware.ale.core.report.ReportsInfo;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.pc.PCReport;
import havis.middleware.ale.service.pc.PCReportSpec;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.service.pc.PCSpec;
import havis.middleware.utils.threading.Task;
import havis.middleware.utils.threading.ThreadManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class implements the reports
 */
public class Reports implements Task, IReports<PCReports, Events> {

    private String name;
    private PCSpec spec;
    private BlockingQueue<ReportsInfo<PCReports, Events>> queue;
    private List<Report> reports;
    private TagOperation tagOperation;

    /**
     * Creates a new instance
     *
     * @param name
     *            The report name
     * @param spec
     *            The report specification
     * @param flat
     *            True if at least one logical reader is specified, false
     *            otherwise
     * @param callback
     *            The callback
     * @throws ImplementationException
     * @throws ValidationException
     */
    public Reports(String name, PCSpec spec, boolean flat, Callback callback) throws ImplementationException, ValidationException {
        if ((spec.getReportSpecs() == null) || spec.getReportSpecs().getReportSpec().size() == 0) {
            throw new ValidationException("No report specification given");
        } else {
            try {
                this.name = name == null ? "" : name;
                this.spec = spec;
                this.queue = new LinkedBlockingQueue<ReportsInfo<PCReports, Events>>();

                // reader operations
                int id = 0;
                List<havis.middleware.ale.base.operation.tag.Operation> operations = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                this.reports = new ArrayList<Report>();

                boolean tagged = false;

                List<String> names = new ArrayList<String>(); // report name
                // validation
                for (PCReportSpec reportSpec : spec.getReportSpecs().getReportSpec()) {
                    Report report = new Report(reportSpec, flat, callback);
                    this.reports.add(report);
                    if (names.contains(report.getName())) {
                        throw new ValidationException("Report specification already contains a report with name '" + report.getName() + "'");
                    }
                    names.add(report.getName());
                    if (report.getFilter() != null) {
                        tagged |= true;
                        for (havis.middleware.ale.base.operation.tag.Operation operation : report.getFilter().getOperations()) {
                            operation.setId(++id);
                            operations.add(operation);
                        }
                    }

                    for (Operation operation : report.getOperations()) {
                        operation.getPortOperation().setId(++id);
                    }
                }

                if (tagged)
                    this.tagOperation = new TagOperation(operations);
            } catch (ValidationException e) {
                dispose();
                throw e;
            }
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets reader operation to get additional data from tag
     */
    public TagOperation getTagOperation() {
        return this.tagOperation;
    }

    /**
     * Enqueues a report info instance for asynchronous report generation
     *
     * @param info
     *            The report info object
     */
    @Override
    public void enqueue(ReportsInfo<PCReports, Events> info) {
        this.queue.add(info);

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
     * Object contains the past and present tags, so that this this method
     * determines the additions, current and deletions tag list and decides for
     * each report depending on its type, for which set of tags it has to
     * generate a report. The data form {@link ReportsInfo} was also refer here.
     * Finally this method decides if a report set is set or not i.e if report
     * set is empty.
     */
    @Override
    public void run() {
        try {
            synchronized (this.queue) {
                final ReportsInfo<PCReports, Events> info;
                info = this.queue.take();
                if (this.queue.isEmpty())
                    this.queue.notify();

                // Initialize reports
                PCReports pcReports = new PCReports();
                pcReports.setSchemaVersion(new BigDecimal(1));
                pcReports.setCreationDate(info.getDate());
                pcReports.setSpecName(name);
                pcReports.setDate(info.getDate());
                pcReports.setALEID(Config.getInstance().getGlobal().getAleid());
                pcReports.setTotalMilliseconds(info.getTotalMilliseconds());
                pcReports.setInitiationCondition(Initiation.toString(info.getInitiation()));
                pcReports.setInitiationTrigger(info.getInitiator());
                pcReports.setTerminationCondition(Termination.toString(info.getTermination()));
                pcReports.setTerminationTrigger(info.getTerminator());

                // send empty report to each outstanding all poll call
                if ((info.getTermination() == Termination.UNDEFINE) && (info.getInitiation() == Initiation.UNDEFINE)) {
					Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Deliver,
							"Cycle {0} delivered {1}", new Object[] { this.name, pcReports });
                    for (SubscriberController subscriber : info.getSubscribers()) {
                        subscriber.enqueue(pcReports);
                    }
                } else {
                    // initialize report list
                    pcReports.setReports(new PCReports.Reports());

                    // include specification
                    if (this.spec.isIncludeSpecInReports()) {
                        pcReports.setPCSpec(this.spec);
                    }

                    for (Report report : reports) {
                        Set<Event> events = info.getDatas().toList();
                        if (events.size() > 0) {
                            long counter = info.getDatas().await(null);
                            // TODO: really completed?
                            boolean complete = false;
                            while (!complete && !info.getDatas().isDisposed()) {
                                for (Event event : events) {
                                    if (!isCompleted(event)) {
                                        counter = info.getDatas().await(Long.valueOf(counter));
                                        break;
                                    }
                                    complete = true;
                                }
                            }

                            // Removes all tags with empty result
                            List<Event> removable = new ArrayList<>();
                            for (Event event : events) {
                                if (event.getResult() == null) {
                                    removable.add(event);
                                }
                            }
                            events.removeAll(removable);
                        }

                        // Check if at least one reportSpec says reportIfEmpty
                        if (report.getSpec().isReportIfEmpty() || info.getDatas().getCount() > 0) {
                            PCReport pcReport = report.get(info.getDatas().toList());
                            if (report.getSpec().isReportIfEmpty() || pcReport.getEventReports().getEventReport().size() > 0) {
                                pcReports.getReports().getReport().add(pcReport);
                            }
                        }
                    }

					Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Deliver,
							"Cycle {0} delivered {1}", new Object[] { this.name, pcReports });
                    if (pcReports.getReports().getReport().size() > 0) {
                        // deliver report to each subscriber
                        for (SubscriberController subscriber : info.getSubscribers()) {
                            subscriber.enqueue(pcReports);
                        }
                    } else {
                        // deliver report only to poll or immediate
                        for (SubscriberController subscriber : info.getSubscribers()) {
                            if (subscriber instanceof SubscriberListener<?>) {
                                subscriber.enqueue(pcReports);
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
			Exits.Log.logp(Exits.Level.Error, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Error,
					"Run interrupted unexpectedly: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the operations to be execute on tag
     *
     * @param tag
     *            The tag
     * @return Operations to be execute
     */
    public List<Operation> getPortOperation(Tag tag) {
        boolean exists = false;
        List<Operation> list = new ArrayList<Operation>();
        for (Report report : this.reports) {
            if (report.getFilter() != null) {
                Boolean match = report.getFilter().match(tag);
                if (Boolean.TRUE.equals(match)) {
                    exists = true;
                    list.addAll(report.getOperations());
                }
            }
        }
        if (exists) {
            return list;
        }
        return null;
    }

    boolean isCompleted(Event event) {
        if (event.isCompleted()) {
            return true;
        } else {
            for (Report report : this.reports) {
                if (!report.isCompleted(event))
                    return false;
            }
            event.setCompleted(true);
            return true;
        }
    }

    /**
     * Calls dispose on each report : set
     */
    @Override
    public void dispose() {
        if (this.queue != null) {
            synchronized (this.queue) {
                while (!this.queue.isEmpty())
                    try {
                        this.queue.wait();
                    } catch (InterruptedException e) {
                        break; // stop waiting
                    }
            }
        }

        if (this.reports != null) {
            for (Report report : this.reports) {
                report.dispose();
            }
            this.reports = null;
        }
    }
}