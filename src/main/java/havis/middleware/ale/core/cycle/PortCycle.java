package havis.middleware.ale.core.cycle;

import havis.middleware.ale.base.State;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Event;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.Time;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.report.pc.Callback;
import havis.middleware.ale.core.report.pc.Events;
import havis.middleware.ale.core.report.pc.Operation;
import havis.middleware.ale.core.report.pc.Report;
import havis.middleware.ale.core.report.pc.Reports;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.pc.PCBoundarySpec;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.service.pc.PCSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the GPIO port cycle
 */
public class PortCycle extends TagCycle<PCSpec, PCReports, Reports, Events> {

    protected boolean whenDataAvailable; // report immediately if one filter
                                         // match

    /**
     * Creates new instance. Validates boundary specification.
     *
     * @param name
     *            The cycle name
     * @param spec
     *            The specification
     * @throws ValidationException
     *             if validation of specification failed.
     * @throws ImplementationException
     */
    public PortCycle(String name, PCSpec spec) throws ValidationException, ImplementationException {
        super(name, spec);
        try {
            setBoundary(spec.getBoundarySpec());
            boolean flat = getLogicalReaders().isEmpty();
            reports = new Reports(name, spec, flat, new Callback() {
                @Override
                public void invoke(Report report, Trigger trigger) {
                    PortCycle.this.triggerReport(report, trigger);
                }
            });

            if (!getLogicalReaders().isEmpty()) {
                lock();
                try {
                    define();
                } catch (ImplementationException e) {
                    unlock();
                    throw e;
                }
            }
        } catch (ValidationException | ImplementationException e) {
            dispose();
            e.setReason("Failed to define port cycle" + " '" + name + "': " + e.getReason());
            throw e;
        }
    }

	protected void onExecuteStarted(String name, String reader, Event event) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Execute, "Cycle {0} started execute for {2} on {1}",
					new Object[] { name, reader != null ? reader : "trigger", event.getUri() });
		}
	}

	@Override
	protected void onNotifyStarted(String name, String reader, Tag tag) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Notify, "Cycle {0} received {2} on {1}",
					new Object[] { name, reader, tag.tag() });
		}
	}

	@Override
	protected void onFiltered(String name, String reader, Tag tag) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Filter, "Cycle {0} filtered {2} on {1}",
					new Object[] { name, reader, tag.tag() });
		}
	}

	@Override
	protected void onStateChanged(String name, State state) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.State, "Cycle {0} changed state to {1}", new Object[] {
				name, state });
	}

	@Override
	protected void onStartTriggered(String name, String trigger) {
		Exits.Log.logp(Exits.Level.Information, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Trigger.Start,
				"Cycle {0} start was triggered by {1}", new Object[] { name, trigger });
	}

	@Override
	protected void onStopTriggered(String name, String trigger) {
		Exits.Log.logp(Exits.Level.Information, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Trigger.Stop,
				"Cycle {0} stop was triggered by {1}", new Object[] { name, trigger });
	}

	@Override
	protected void onCycleStarted(String name) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Begin,
				"Cycle {0} was started", new Object[] { name });
	}

	@Override
	protected void onCycleFailed(String name, String message, Exception error) {
		Exits.Log.logp(Exits.Level.Error, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Error,
				"Cycle {0} failed: {1}: {2}", new Object[] { name, message, error.getMessage() });
	}

	@Override
	protected void onReportStarted(String name) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Report,
				"Cycle {0} sent report", new Object[] { name });
	}

	@Override
	protected void onEvaluateStarted(String name) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.PortCycle.Name, Exits.Core.Cycle.PortCycle.Evaluate,
				"Cycle {0} starts evaluate", new Object[] { name });
	}

    private void setBoundary(PCBoundarySpec spec) throws ImplementationException, ValidationException {
        if (spec != null) {
            boolean hasStartTrigger = spec.getStartTriggerList() != null && !spec.getStartTriggerList().getStartTrigger().isEmpty();
            boolean hasStopTrigger = spec.getStopTriggerList() != null && !spec.getStopTriggerList().getStopTrigger().isEmpty();
            if (hasStartTrigger) {
                for (String triggerUri : spec.getStartTriggerList().getStartTrigger()) {
                    this.trigger.add(Trigger.getInstance(this.guid, triggerUri, new Trigger.Callback() {
                        @Override
                        public boolean invoke(Trigger trigger) {
                            return start(trigger);
                        }
                    }));
                }
            }
            if (hasStopTrigger) {
                for (String triggerUri : spec.getStopTriggerList().getStopTrigger()) {
                    this.trigger.add(Trigger.getInstance(this.guid, triggerUri, new Trigger.Callback() {
                        @Override
                        public boolean invoke(Trigger trigger) {
                            return stop(trigger);
                        }
                    }));
                }
            }
            whenDataAvailable = Boolean.TRUE.equals(spec.isWhenDataAvailable());
            try {
                repeatPeriod = Time.getValue(spec.getRepeatPeriod());
            } catch (ValidationException e) {
                e.setReason("Repeat period of port cycle '" + name + "' is invalid. " + e.getReason());
                throw e;
            }
            try {
                duration = Time.getValue(spec.getDuration());
            } catch (ValidationException e) {
                e.setReason("Duration of port cycle '" + name + "' is invalid. " + e.getReason());
                throw e;
            }
            try {
                interval = Time.getValue(spec.getNoNewEventsInterval());
            } catch (ValidationException e) {
                e.setReason("No new events interval of port cycle '" + name + "' is invalid. " + e.getReason());
                throw e;
            }
            immediate = !hasStartTrigger;

            if (duration <= 0 && interval <= 0 && !hasStopTrigger && !whenDataAvailable)
                throw new ValidationException("No stop condition given");
        } else {
            throw new ValidationException("No boundary specification given");
        }
    }

	/**
	 * @return the logical reader list
	 */
    @Override
    protected List<String> getLogicalReaders() {
        return spec.getLogicalReaders() != null ? spec.getLogicalReaders().getLogicalReader() : Collections.<String> emptyList();
    }

    /**
     * Gets a NO_NEW_PORTS termination condition
     */
    @Override
    protected Termination getInterval() {
        return Termination.NO_NEW_EVENTS;
    }

    private static void execute(LogicalReader reader, List<havis.middleware.ale.base.operation.port.Operation> operations, Caller<Port> callback) {
        try {
            reader.execute(new PortOperation(operations), callback);
        } catch (ImplementationException | ValidationException e) {
            // TODO: ignore all exceptions?
        }
    }

    /**
     * Execute the specified operations
     *
     * @param operations
     *            the operations to execute
     * @param callback
     *            the callback to use
     */
    public static void execute(List<Operation> operations, Caller<Port> callback) {
        LogicalReader reader = null;
        List<havis.middleware.ale.base.operation.port.Operation> list = new ArrayList<havis.middleware.ale.base.operation.port.Operation>();
        for (Operation operation : operations) {
            if (operation.getLogicalReader() != reader) {
                if ((reader != null) && (list.size() > 0)) {
                    execute(reader, list, callback);
                    list.clear();
                }
                reader = operation.getLogicalReader();
            }
            list.add(operation.getPortOperation());
        }
        if ((reader != null) && (list.size() > 0))
            execute(reader, list, callback);
    }

    private void triggerReport(Report report, Trigger trigger) {
        while (isActive()) {
            try {
                if (lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                    try {
                        if (isActive()) {
                            synchronized (datas) {
                                List<Operation> operations = report.getOperations();
                                Event event = new Event(trigger.getUri());
                                if (datas.contains(event)) {
									Event e = datas.get(event);
									if (e != null) {
										e.stat(event);
									}
                                } else {
                                    event.stat(event);
                                    notify(null, event, operations);
                                }
                            }
                        }
                        break;
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private void notify(String reader, final Event event, List<Operation> operations) {
        datas.add(event, null);

        if (whenDataAvailable && (termination != Termination.DATA_AVAILABLE)) {
            termination = Termination.DATA_AVAILABLE;
            condition.signal();
        } else if (interval > 0) {
        	rescheduleIntervalTimer(interval);
        }

        if (operations.size() > 0) {
            final Events events = datas.clone();
        	onExecuteStarted(name, reader, event);
            execute(operations, new Caller<Port>() {
                @Override
                public void invoke(Port port, ReaderController controller) {
                    synchronized (datas) {
                        if (port.isCompleted()) {
                            event.setResult(null);
                            event.setCompleted(true);
                            events.remove(event);
                        } else {
                            Event currentEvent = event;
                            if (events.get(event) == null) { // initial state
                                events.set(event, event);
                            }
                            else {
                                currentEvent = events.get(event);
                            }

                            for (Entry<Integer, Result> pair : port.getResult().entrySet()) {
                                currentEvent.getResult().put(pair.getKey(), pair.getValue());
                            }
                        }

                        events.pulse();
                    }
                }
            });
        } else {
            event.setCompleted(true);
            datas.set(event, event);

            datas.pulse();
        }
    }

    /**
     * Notifies the cycle about a tag.
     *
     * @param reader
     *            Name of logical reader who has send the result
     * @param tag
     *            The tag
     * @param controller
     *            The controller
     */
    @Override
    protected void notify(String reader, Tag tag, ReaderController controller) {
        while (isActive()) {
            try {
                if (lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                    try {
                        if (isActive()) {
                            synchronized (datas) {
                                super.notify(reader, tag, controller);

                                if((tag.getEpc() != null && tag.getEpc().length > 0) && (!Tag.isExtended() || (tag.getTid() != null && tag.getTid().length > 0))) {
                                	// TODO: TDT decode happens here, this should be optimized
                                	// maybe make event not rely on URI but on Tag.equals/hashCode
                                    Event event = new Event(tag);
    
                                    // is this tag seen the first time
                                    if (datas.contains(event)) {
    									Event e = datas.get(event);
    									if (e != null) {
    										e.stat(event, reader);
    									}
                                    } else {
                                        onFiltered(name, reader, tag);
                                        List<Operation> operations = reports.getPortOperation(tag);
                                        if (operations != null) {
                                            event.stat(event, reader);
                                            notify(reader, event, operations);
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Gets the reader operation
     *
     * @return The reader operation
     */
    @Override
    protected TagOperation getTagOperation() {
        return reports != null ? reports.getTagOperation() : null;
    }

    @Override
    protected Events create() {
        return new Events();
    }
}
