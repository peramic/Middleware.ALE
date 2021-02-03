package havis.middleware.ale.core.cycle;

import havis.middleware.ale.base.State;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.Time;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.report.cc.Reports;
import havis.middleware.ale.core.report.cc.Tags;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.CCBoundarySpec;
import havis.middleware.ale.service.cc.CCParameterListEntry;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.cc.CCSpec;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * This is a implementation of the command cycle specified in ALE 1.1.1 (5.3)
 */
public class CommandCycle extends TagCycle<CCSpec, CCReports, Reports, Tags> {

    private int tagsProcessedCount;

    private boolean afterError;

    /**
     * Creates new instance. Validates boundary specification.
     *
     * @param name
     *            The cycle name
     * @param spec
     *            The specification
     * @throws ImplementationException
     * @throws ValidationException
     */
    public CommandCycle(String name, CCSpec spec) throws ImplementationException, ValidationException {
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
            e.setReason("Failed to define command cycle" + (name == null ? "" : " '" + name + "'") + ": " + e.getReason());
            throw e;
        }
	}

	protected void onExecuteStarted(String name, String reader, Tag tag) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Execute,
					"Cycle {0} started execute of {2} on {1}", new Object[] { name, reader, tag.tag() });
		}
	}

	protected void onExecuteFinished(String name, String reader, Tag tag) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Executed,
					"Cycle {0} finished execute of {2} on {1}", new Object[] { name, reader, tag.tag() });
		}
	}

	@Override
	protected void onNotifyStarted(String name, String reader, Tag tag) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Notify, "Cycle {0} received {2} on {1}",
					new Object[] { name, reader, tag.tag() });
		}
	}

	@Override
	protected void onFiltered(String name, String reader, Tag tag) {
		if (Exits.Log.isLoggable(Exits.Level.Detail)) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Filter, "Cycle {0} filtered {2} on {1}",
					new Object[] { name, reader, tag.tag() });
		}
	}

	@Override
	protected void onStateChanged(String name, State state) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.State,
				"Cycle {0} changed state to {1}", new Object[] { name, state });
	}

	@Override
	protected void onStartTriggered(String name, String trigger) {
		Exits.Log.logp(Exits.Level.Information, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Trigger.Start,
				"Cycle {0} start was triggered by {1}", new Object[] { name, trigger });
	}

	@Override
	protected void onStopTriggered(String name, String trigger) {
		Exits.Log.logp(Exits.Level.Information, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Trigger.Stop,
				"Cycle {0} stop was triggered by {1}", new Object[] { name, trigger });
	}

	@Override
	protected void onCycleStarted(String name) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Begin,
				"Cycle {0} was started", new Object[] { name });
	}

	@Override
	protected void onCycleFailed(String name, String message, Exception error) {
		Exits.Log.logp(Exits.Level.Error, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Error,
				"Cycle {0} failed: {1}: {2}", new Object[] { name, message, error.getMessage() });
	}

	@Override
	protected void onReportStarted(String name) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Report,
				"Cycle {0} sent report", new Object[] { name });
	}

	@Override
	protected void onEvaluateStarted(String name) {
		Exits.Log.logp(Exits.Level.Detail, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Evaluate,
				"Cycle {0} starts evaluate", new Object[] { name });

	}

	private void setBoundary(CCBoundarySpec spec) throws ImplementationException, ValidationException {
        if (spec != null) {
            if (spec.getStartTriggerList() != null) {
                for (String triggerUri : spec.getStartTriggerList().getStartTrigger()) {
                    this.trigger.add(Trigger.getInstance(this.guid, triggerUri, new Trigger.Callback() {
                        @Override
                        public boolean invoke(Trigger trigger) {
                            return start(trigger);
                        }
                    }));
                }
            }
            if (spec.getStopTriggerList() != null) {
                for (String triggerUri : spec.getStopTriggerList().getStopTrigger()) {
                    this.trigger.add(Trigger.getInstance(this.guid, triggerUri, new Trigger.Callback() {
                        @Override
                        public boolean invoke(Trigger trigger) {
                            return stop(trigger);
                        }
                    }));
                }
            }

            repeatPeriod = Time.getValue(spec.getRepeatPeriod());
            duration = Time.getValue(spec.getDuration());
            interval = Time.getValue(spec.getNoNewTagsInterval());
            immediate = (spec.getStartTriggerList() == null || spec.getStartTriggerList().getStartTrigger().size() == 0);
            tagsProcessedCount = spec.getTagsProcessedCount() != null ? spec.getTagsProcessedCount().intValue() : 0;
            if (tagsProcessedCount < 0) {
                throw new ValidationException("Value of tagsProcessedCount could not be negative");
            }
            afterError = Boolean.TRUE.equals(spec.isAfterError());

            if ((spec.getStopTriggerList() == null || spec.getStopTriggerList().getStopTrigger().size() == 0) && (duration < 1) && (interval < 1)
                    && (tagsProcessedCount == 0))
                throw new ValidationException("No stop condition given");
        } else {
            throw new ValidationException("No boundary specification given");
        }
    }

    /**
     * Retrieves the logical reader list
     *
     * @return The logical reader list
     */
    @Override
    protected List<String> getLogicalReaders() {
        return spec.getLogicalReaders() != null ? spec.getLogicalReaders().getLogicalReader() : Collections.<String> emptyList();
    }

    /**
     * Gets NO_NEW_TAGS termination condition
     */
    @Override
    protected Termination getInterval() {
        return Termination.NO_NEW_TAGS;
    }

    /**
     * Executes reader operations on tag
     *
     * @param tags
     *            The tag dictionary
     * @param reader
     *            The reader name
     * @param tag
     *            The tag
     * @param controller
     *            The controller
     * @param operation
     *            The operation to be executed
     */
    private void execute(final Tags tags, final String reader, final Tag tag, ReaderController controller, TagOperation operation) {
        onExecuteStarted(name, reader, tag);
        controller.execute(reader, operation, new Caller<Tag>() {
            @Override
            public void invoke(Tag executedTag, ReaderController controller) {
                onExecuteFinished(name, reader, tag);
                boolean error = false;
                synchronized (datas) {
                    Tag seenTag = tag;
                    if (executedTag.isCompleted()) {
                        seenTag.setResult(null);
                        seenTag.setCompleted(true);
                        tags.remove(seenTag);
                    } else {
                        if (tags.get(seenTag) == null) {
                            tags.put(seenTag, seenTag);
                        }
                        else {
                            seenTag = tags.get(seenTag);
                        }
                        if ((executedTag.getEpc() != null) && !seenTag.equals(executedTag)) {
                            tags.remove(seenTag);
                            seenTag.apply(executedTag.getEpc());
                            tags.add(seenTag);
                        }
                        try {
                            if (executedTag.getResult().size() > 0) {
                                for (Entry<Integer, Result> pair : executedTag.getResult().entrySet()) {
                                    seenTag.getResult().put(pair.getKey(), pair.getValue());
                                    error = error || (afterError && (pair.getValue() != null) && (pair.getValue().getState() != ResultState.SUCCESS));
                                }
                                tags.pulse();
                            }
                        } catch (Exception e) {
                        	onCycleFailed(name, "Failed to execute operation for command cycle", e);
                        }
                    }
                }
                if (error) {
                	while (isActive()) {
						try {
							if (lock.tryLock(50, TimeUnit.MILLISECONDS)) {
								try {
									if (tags.hasSameData(datas)) {
										termination = Termination.ERROR;
										condition.signal();
									}
								} finally {
									lock.unlock();
								}
								break;
							}
						} catch (InterruptedException e) {
							// we have been interrupted while waiting for the lock, give up
							break;
						}
                	}
                }
            }
        });
    }

    /**
     * Notifies cycle about a tag.
     *
     * @param reader
     *            Name of the logical reader who has seen the tag
     * @param tag
     *            The tag
     * @param controller
     *            The reader controller
     */
    @Override
    protected void notify(String reader, Tag tag, ReaderController controller) {
        while (isActive()) {
            try {
                if (lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                    try {
                        if (termination != Termination.COUNT) {
                            if (isActive()) {
                                synchronized (datas) {
                                    super.notify(reader, tag, controller);

                                    if((tag.getEpc() != null && tag.getEpc().length > 0) && (!Tag.isExtended() || (tag.getTid() != null && tag.getTid().length > 0))) {
                                        // is this tag seen the first time
                                        if (datas.removeLifetimeExceededAndCheckWhetherSeen(tag)) {
                                        	Tag t = datas.get(tag);
    										if (t != null) {
    											t.stat(reader, tag);
    										}
                                        } else {
                                            onFiltered(name, reader, tag);
                                            TagOperation operation = reports.getTagOperation(tag);
                                            if (operation != null) {
                                                datas.add(tag, null);
                                                tag.stat(reader);
                                                if ((tagsProcessedCount > 0) && (datas.getCount() >= tagsProcessedCount)) {
                                                    termination = Termination.COUNT;
                                                    condition.signal();
                                                } else if (interval > 0) {
                                                	rescheduleIntervalTimer(interval);
                                                }
                                                if (operation.getOperations().size() > 0) {
                                                    execute((Tags) datas.clone(), reader, tag, controller, operation);
                                                } else {
                                                    tag.setCompleted(true);
    
                                                    if (datas.get(tag) == null)
                                                        datas.put(tag, tag);
                                                    else
                                                        tag = datas.get(tag);
    
                                                    datas.pulse();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves the reader operations
     *
     * @return The tag operation
     */
    @Override
    protected TagOperation getTagOperation() {
        return reports == null ? null : reports.getReaderOperation();
    }

    /**
     * Retrieves the parameterization
     *
     * @return True if cycle is parameterized, false otherwise
     */
    public boolean isParameterized() {
        return reports.getParameters().hasParameters();
    }

    /**
	 * Adds a subscriber listener and the entries to the command cycle
	 * 
	 * @param subscriber
	 *            The subscriber listener
	 * @param entries
	 *            The entries
	 * @throws ParameterException
	 */
	public void add(SubscriberListener<CCReports> subscriber, List<CCParameterListEntry> entries) throws ParameterException {
		reports.getParameters().updateParameterValues(entries);
		lock.lock();
		try {
			subscribersLock.lock();
			try {
				super.add(subscriber);
			} finally {
				subscribersLock.unlock();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Clears the command cycle run data. Frees bytes : parameters
	 */
    @Override
    protected void clear() {
        if (reports != null) {
            if (reports.getParameters() != null) {
                reports.getParameters().clearParameterValues();
            }
        }
    }

    @Override
    protected Tags create() {
        return new Tags();
    }
}