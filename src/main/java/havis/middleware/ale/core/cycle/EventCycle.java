package havis.middleware.ale.core.cycle;

import havis.middleware.ale.base.State;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.Time;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.report.ec.PrimaryKey;
import havis.middleware.ale.core.report.ec.Reports;
import havis.middleware.ale.core.report.ec.Tags;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.ec.ECBoundarySpec;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * This is a implementation of the event cycle specified in ALE 1.1.1 (5.2)
 */
public class EventCycle extends TagCycle<ECSpec, ECReports, Reports, Tags> {

    boolean whenDataAvailable; // report immediately if one filter match

    private Timer dataAvailableTimer = null;

    /**
     * Creates new instance. Validates boundary specification.
     *
     * @param name
     *            The cycle name
     * @param spec
     *            The specification
     * @throws ImplementationException
     *             If implementation cause a failure
     * @throws ValidationException
     *             If validation of specification failed.
     */
    public EventCycle(String name, ECSpec spec) throws ImplementationException, ValidationException {
        super(name, spec);
        try {
            setBoundary(spec.getBoundarySpec());
            reports = new Reports(name, spec);
            lock();
            try {
                define();
            } catch (ALEException e) {
                unlock();
                throw e;
            }
        } catch (ALEException e) {
            dispose();
            e.setReason("Failed to define event cycle" + (name == null ? "" : " '" + name + "'") + ": " + e.getReason());
            throw e;
        }
	}

	@Override
	protected void onNotifyStarted(String name, String reader, Tag tag) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Notify, "Cycle {0} received {2} on {1}",
					new Object[] { name, reader, tag.tag() });
		}
	}

	@Override
	protected void onFiltered(String name, String reader, Tag tag) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Filter, "Cycle {0} filtered {2} on {1}",
					new Object[] { name, reader, tag.tag() });
		}
	}

	@Override
	protected void onStateChanged(String name, State state) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.State,
				"Cycle {0} changed state to {1}", new Object[] { name, state });
	}

	@Override
	protected void onStartTriggered(String name, String trigger) {
		Exits.Log.logp(Exits.Level.Information, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Trigger.Start,
				"Cycle {0} start was triggered by {1}", new Object[] { name, trigger });
	}

	@Override
	protected void onStopTriggered(String name, String trigger) {
		Exits.Log.logp(Exits.Level.Information, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Trigger.Stop,
				"Cycle {0} stop was triggered by {1}", new Object[] { name, trigger });
	}

	@Override
	protected void onCycleStarted(String name) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Begin,
				"Cycle {0} was started", new Object[] { name });
	}

	@Override
	protected void onCycleFailed(String name, String message, Exception error) {
		Exits.Log.logp(Exits.Level.Error, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Error,
				"Cycle {0} failed: {1}: {2}", new Object[] { name, message, error.getMessage() });
	}

	@Override
	protected void onReportStarted(String name) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Report,
				"Cycle {0} sent report", new Object[] { name });
	}

	@Override
	protected void onEvaluateStarted(String name) {
		if (whenDataAvailable) {
			cancelDataAvailableTimer();	
		}

		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.EventCycle.Name, Exits.Core.Cycle.EventCycle.Evaluate,
				"Cycle {0} starts evaluate", new Object[] { name });
	}

    private void setBoundary(ECBoundarySpec spec) throws ValidationException, ImplementationException {
        if (spec != null) {
            boolean hasStartTrigger = false;
            boolean hasStopTrigger = false;
            // Deprecated: for back-compatibility with ALE 1.0
            if (spec.getStartTrigger() != null) {
                hasStartTrigger = true;
                this.trigger.add(Trigger.getInstance(this.guid, spec.getStartTrigger(), new Trigger.Callback() {
                    @Override
                    public boolean invoke(Trigger trigger) {
                        return start(trigger);
                    }
                }));
            }
            if (spec.getExtension() != null) {
                if (spec.getExtension().getStartTriggerList() != null) {
                    for (String triggerUri : spec.getExtension().getStartTriggerList().getStartTrigger()) {
                        hasStartTrigger = true;
                        this.trigger.add(Trigger.getInstance(this.guid, triggerUri, new Trigger.Callback() {
                            @Override
                            public boolean invoke(Trigger trigger) {
                                return start(trigger);
                            }
                        }));
                    }
                }
                if (spec.getExtension().getStopTriggerList() != null) {
                    for (String triggerUri : spec.getExtension().getStopTriggerList().getStopTrigger()) {
                        hasStopTrigger = true;
                        this.trigger.add(Trigger.getInstance(this.guid, triggerUri, new Trigger.Callback() {
                            @Override
                            public boolean invoke(Trigger trigger) {
                                return stop(trigger);
                            }
                        }));
                    }
                }
                whenDataAvailable = Boolean.TRUE.equals(spec.getExtension().isWhenDataAvailable());
            }
            // Deprecated: for back-compatibility with ALE 1.0
            if (spec.getStopTrigger() != null) {
                hasStopTrigger = true;
                this.trigger.add(Trigger.getInstance(this.guid, spec.getStopTrigger(), new Trigger.Callback() {
                    @Override
                    public boolean invoke(Trigger trigger) {
                        return stop(trigger);
                    }
                }));
            }
            try {
                repeatPeriod = Time.getValue(spec.getRepeatPeriod());
            } catch (ValidationException e) {
                e.setReason("Repeat period of event cycle '" + name + "' is invalid. " + e.getReason());
                throw e;
            }
            try {
                duration = Time.getValue(spec.getDuration());
            } catch (ValidationException e) {
                e.setReason("Duration of event cycle '" + name + "' is invalid. " + e.getReason());
                throw e;
            }
            try {
                interval = Time.getValue(spec.getStableSetInterval());
            } catch (ValidationException e) {
                e.setReason("Stable set interval of event cycle '" + name + "' is invalid. " + e.getReason());
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
     * Retrieves the logical reader list
     *
     * @return The logical reader string list
     */
    @Override
    protected List<String> getLogicalReaders() {
        return spec.getLogicalReaders() != null ? spec.getLogicalReaders().getLogicalReader() : Collections.<String> emptyList();
    }

    /**
     * Retrieves a STABLE_SET termination condition
     */
    @Override
    protected Termination getInterval() {
        return Termination.STABLE_SET;
    }

    /**
     * Notifies the cycle about a tag. This method is called each time one of
     * the registers Readers sees one tag before any filtering or collection.
     *
     * @param reader
     *            Name of logical reader who has seen the tag
     * @param tag
     *            The tag
     * @param controller
     *            The operable
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
                                    PrimaryKey key = new PrimaryKey(tag, reports.getFields());

                                    if (key.match()) {
                                        // is this tag seen the first time
                                        if (datas.contains(key)) {
                                            Tag t = datas.get(key);
                                            t.stat(reader, tag);
                                            if (!t.isCompleted()) {
                                                t.apply(tag);
                                                t.setCompleted(reports.isCompleted(t));
                                            }
                                        } else {
                                            onFiltered(name, reader, tag);
                                            Boolean match = reports.match(tag);
                                            if (match != null) {
                                                tag.stat(reader);
                                                datas.add(key, tag);
                                                tag.setCompleted(reports.isCompleted(tag));
                                                if (Boolean.TRUE.equals(match)) {
                                                    if (whenDataAvailable && !isDataAvailableTimerScheduled()) {
                                                        scheduleDataAvailableTimer();
                                                    } else if (interval > 0) {
                                                    	rescheduleIntervalTimer(interval);
                                                    }
                                                }
                                            }
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
                e.printStackTrace();
            }
        }
	}

	private boolean isDataAvailableTimerScheduled() {
		return this.dataAvailableTimer != null;
	}

	private void scheduleDataAvailableTimer() {
		int readerCycleDuration = Math.max(0, Config.getInstance().getGlobal().getReaderCycle().getDuration());
		this.dataAvailableTimer = new Timer(this.getClass().getSimpleName() + ".dataAvailableTimer " + (this.name != null ? this.name : "[no name]"));
		this.dataAvailableTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				lock.lock();
				try {
					termination = Termination.DATA_AVAILABLE;
					condition.signal();
				} finally {
					lock.unlock();
				}
			}
		}, readerCycleDuration);
	}

	private void cancelDataAvailableTimer() {
		if (this.dataAvailableTimer != null) {
			this.dataAvailableTimer.cancel();
			dataAvailableTimer = null;
		}
	}

	/**
     * Retrieves the reader operation
     *
     * @return The reader operation
     */
    @Override
    protected TagOperation getTagOperation() {
        return reports != null ? reports.getReaderOperation() : null;
    }

    @Override
    protected Tags create() {
        return new Tags();
    }
}