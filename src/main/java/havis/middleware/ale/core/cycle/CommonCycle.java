package havis.middleware.ale.core.cycle;

import havis.middleware.ale.base.State;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.report.IDatas;
import havis.middleware.ale.core.report.IReports;
import havis.middleware.ale.core.report.Initiation;
import havis.middleware.ale.core.report.ReportsInfo;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.subscriber.Subscriber;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used for both event and command cycle. It provides basics that
 * are equal in both environments. You have to specified a specification type
 * like {@link ECSpec} or {@link CCSpec} and the reports type like
 * {@link ECReports} or {@link CCReports}. The constructor take only given
 * parameters an initializes data type. No validation occurs. It contains
 * methods to (un)define, enable and disable reader operations. To manage the
 * subscriber list and cycle state.
 *
 * @param <Spec>
 *            Type of specification
 * @param <Result>
 *            Type of Result
 * @param <Reports>
 *            Type of Reports
 * @param <Datas>
 *            Type of datas
 * @param <Data>
 *            Type of data
 */
public abstract class CommonCycle<Spec, Result extends havis.middleware.ale.service.IReports, Reports extends IReports<Result, Datas>, Datas extends IDatas, Data>
		implements Runnable {

	protected Lock lock = new ReentrantLock();
	protected Condition condition = lock.newCondition();

	protected Lock readersLock = new ReentrantLock();
	protected Lock subscribersLock = new ReentrantLock();

	/**
	 * Retrieves the unique ID
	 */
	protected String guid =  UUID.randomUUID().toString();

	/**
	 * Retrieves the cycle state
	 */
	protected State state;

	/**
	 * Retrieves the cycle starting point
	 */
	protected boolean immediate;

	/**
	 * Retrieves the duration
	 */
	protected long duration;

	/**
	 * Retrieves the repeat period
	 */
	protected long repeatPeriod;

	/**
	 * Retrieves the time interval without changes after that report generation
	 * starts
	 */
	protected long interval;

	/**
	 * Retrieves the interval timer
	 */
	protected Timer intervalTimer;

	/**
	 * Retrieves the timer for starting the cycle
	 */
	protected Timer startTimer;

	/**
	 * Retrieves the queue
	 */
	protected Reports reports;

	/**
	 * Retrieves the cycle name
	 */
	protected String name;

	/**
	 * Retrieves the specification
	 */
	protected Spec spec;

	/**
	 * Retrieves the current subscribers
	 */
	protected List<SubscriberController> subscribers;

	/**
	 * Retrieves trigger list
	 */
	protected List<Trigger> trigger;

	/**
	 * Retrieves the list of logical readers
	 */
	protected List<LogicalReader> logicalReaders;

	/**
	 * Retrieves the current cycle data
	 */
	protected Datas datas;

	/**
	 * Retrieves the initiation reason
	 */
	protected Initiation initiation;

	/**
	 * Retrieves the initiation object
	 */
	protected Trigger initiator;

	/**
	 * Retrieves termination reason
	 */
	protected Termination termination;

	/**
	 * Retrieves the termination object
	 */
	protected Trigger terminator;

	/**
	 * Retrieves the cycle thread
	 */
	protected Thread thread;

	private long lastCycleTriggeredTime = -1;

	private long nextCycleTriggeredTime = -1;

	private long lastCycleDurationDueTime = -1;

	private long nextCycleDurationDueTime = -1;

	/**
	 * Creates a new instance. Keep parameters and initializes class parameters
	 *
	 * @param name
	 *            The cycle name
	 * @param spec
	 *            The specification
	 */
	public CommonCycle(String name, Spec spec) {
		this.name = name;
		this.spec = spec;
		subscribers = new ArrayList<SubscriberController>();
		trigger = new ArrayList<Trigger>();
		logicalReaders = new ArrayList<LogicalReader>();
		datas = create();
		state = State.UNREQUESTED;
		onStateChanged(name, state);
		thread = new Thread(this, this.getClass().getSimpleName() + " " + (this.name != null ? this.name : "[no name]"));
	}

	protected abstract void onStateChanged(String name, State state);

	protected abstract void onStartTriggered(String name, String trigger);

	protected abstract void onStopTriggered(String name, String trigger);

	protected abstract void onCycleStarted(String name);

	protected abstract void onCycleFailed(String name, String message, Exception error);

	protected abstract void onReportStarted(String name);

	protected abstract void onEvaluateStarted(String name);

	private void resetCycleTimes() {
		lastCycleTriggeredTime = -1;
		nextCycleTriggeredTime = -1;
		lastCycleDurationDueTime = -1;
		nextCycleDurationDueTime = -1;
	}

	/**
	 * Reschedule the interval timer
	 * @param delay the delay
	 */
	protected void rescheduleIntervalTimer(long delay) {
		// TODO: don't use timer: http://stackoverflow.com/questions/32001/resettable-java-timer
		if (this.intervalTimer != null) {
			this.intervalTimer.cancel();
		}
		this.intervalTimer = new Timer(this.getClass().getSimpleName() + ".intervalTimer " + (this.name != null ? this.name : "[no name]"));
		this.intervalTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                interrupt();
            }
		}, delay);
	}

	private void rescheduleStartTimer(Date time) {
		if (this.startTimer != null) {
			this.startTimer.cancel();
		}
		this.startTimer = new Timer(this.getClass().getSimpleName() + ".startTimer " + (this.name != null ? this.name : "[no name]"));
		this.startTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				start(CommonCycle.this);
			}
		}, time);
	}

	/**
	 * Starts the cycle
	 */
	public void start() {
		if (thread != null) {
			thread.start();
		}
	}

	/**
	 * Gets the specification
	 */
	public Spec getSpec() {
		return spec;
	}

	/**
	 * Gets the list of logical reader names
	 */
	protected abstract List<String> getLogicalReaders();

	/**
	 * Creates a new instance of datas
	 *
	 * @return The new datas instance
	 */
	protected abstract Datas create();

	/**
	 * Retrieves a list of subscriber URIs
	 */
	public List<String> getSubscribers() {
		List<String> uri = new ArrayList<String>();
		subscribersLock.lock();
		try {
			for (SubscriberController subscriber : subscribers) {
				if (!(subscriber instanceof SubscriberListener)) {
					uri.add(subscriber.getURI().toASCIIString());
				}
			}
		} finally {
			subscribersLock.unlock();
		}
		return uri;
	}

	/**
	 * Locks logical readers
	 *
	 * @throws ValidationException
	 *             if reader list is empty or a logical reader does not exists.
	 */
	protected void lock() throws ValidationException {
		if (getLogicalReaders().isEmpty()) {
			throw new ValidationException("No readers specified");
		} else {
			readersLock.lock();
			try {
				for (String reader : getLogicalReaders()) {
					try {
						logicalReaders.add(LR.getInstance().lock(reader));
					} catch (NoSuchNameException e) {
						unlock();
						throw new ValidationException(e.getReason());
					}
				}
			} finally {
				readersLock.unlock();
			}
		}
	}

	/**
	 * Unlocks logical readers
	 */
	protected void unlock() {
		readersLock.lock();
		try {
			for (LogicalReader logicalReader : logicalReaders) {
				logicalReader.unlock();
			}
			logicalReaders.clear();
		} finally {
			readersLock.unlock();
		}
	}

	/**
	 * Defines reader operation on each reader in logical reader list
	 *
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	protected abstract void define() throws ImplementationException,
			ValidationException;

	/**
	 * Un-defines logical readers
	 */
	protected abstract void undefine();

	/**
	 * Disables logical readers
	 */
	protected abstract void disable();

	/**
	 * Enables logical readers
	 */
	protected abstract void enable();

	static void start(CommonCycle<?, ?, ?, ?, ?> cycle) {
		cycle.start((Trigger) null);
	}

	/**
	 * Starts the cycle
	 *
	 * @param trigger
	 *            Optional trigger
	 */
	protected boolean start(Trigger trigger) {
		lock.lock();
		try {
			if (state == State.REQUESTED) {
				state = State.ACTIVE;
				if (nextCycleTriggeredTime == -1) {
					// not yet calculated, use current time
					lastCycleTriggeredTime = System.currentTimeMillis();
				} else {
					lastCycleTriggeredTime = nextCycleTriggeredTime;						
				}
				onStateChanged(name, state);
				if (trigger instanceof Trigger) {
					initiation = Initiation.TRIGGER;
					onStartTriggered(name, trigger.getUri());
				}
				if (interval > 0) {
					// this makes sure that an old timer which didn't fire yet, is reset
					rescheduleIntervalTimer(interval);
				}
				initiator = trigger;
				terminator = null; // reset
				condition.signal();
				return true;
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	/**
	 * Retrieves the interval termination condition
	 */
	protected abstract Termination getInterval();

	/**
	 * Interrupts the cycle
	 */
	protected void interrupt() {
		lock.lock();
		try {
			if (state == State.ACTIVE) {
				termination = getInterval();
				condition.signal();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Stops the cycle
	 *
	 * @param trigger
	 *            Optional trigger instance if stop was called by a trigger
	 */
	protected boolean stop(Trigger trigger) {
		lock.lock();
		try {
			if (state == State.ACTIVE) {
				state = State.REQUESTED;
				resetCycleTimes();
				onStateChanged(name, state);
				if (trigger instanceof Trigger) {
					termination = Termination.TRIGGER;
					onStopTriggered(name, trigger.getUri());
				}
				terminator = trigger;
				condition.signal();
				return true;
			} else if (state == State.REQUESTED && initiation == Initiation.REPEAT_PERIOD) {
				// not active but repeat is scheduled
				if (startTimer != null) {
					startTimer.cancel();
				}
				resetCycleTimes();
				return true;
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	/**
	 * Executes the main cycle run. <h1>The thread sleeps as long as</h1>
	 * <ul>
	 * <li>duration time expires</li>
	 * <li>or interval expires</li>
	 * <li>or a stop trigger occurs</li>
	 * <li>cycle has be undefined</li>
	 * </ul>
	 * After thread awakes, enqueue a new generates element to thread manager.
	 */
	private void exec() {
		synchronized (datas) {
			datas.clear();
		}

		onCycleStarted(name);

		if (interval > 0) {
			rescheduleIntervalTimer(interval);
		}

		long startTime = System.currentTimeMillis();

		try {
			termination = Termination.DURATION;
			if (duration <= 0) {
				condition.await(); // wait infinitely
			} else {
				lastCycleDurationDueTime = nextCycleDurationDueTime > -1 ? nextCycleDurationDueTime : startTime + duration;
				condition.awaitUntil(new Date(lastCycleDurationDueTime));
			}
		} catch (InterruptedException e) {
			lock.lock();
			onCycleFailed(name, "Cycle interrupted unacceptably [Duration]", e);
		}

		onEvaluateStarted(name);

		synchronized (datas) {

			long creationTime = System.currentTimeMillis();
			long totalMilliseconds = creationTime - startTime;

			List<SubscriberController> subscribers = new ArrayList<>();
			subscribersLock.lock();
			try {
				for (SubscriberController subscriber : this.subscribers) {
					if (subscriber.getActive()) {
						subscribers.add(subscriber);
					}
				}
			} finally {
				subscribersLock.unlock();
			}
			@SuppressWarnings("unchecked")
			ReportsInfo<Result, Datas> info = new ReportsInfo<Result, Datas>(
					subscribers.toArray(new SubscriberController[] {}),
					(Datas) datas.clone(), new Date(creationTime), totalMilliseconds,
					initiation, initiator != null ? initiator.getUri() : null,
					termination, terminator != null ? terminator.getUri()
							: null);

			onReportStarted(name);

			reports.enqueue(info);

			datas.rotate();

			nextCycleDurationDueTime = -1;

			// set cycle inactive if repeat period greater then duration
			// or cycle aborted i.e. by stop trigger
			if (state == State.ACTIVE) {
				if (repeatPeriod > -1) {
					repeatCycle();
				} else {
					state = State.REQUESTED;
					onStateChanged(name, state);
				}
			}
		}
		remove();
	}

	private void repeatCycle() {
		initiation = Initiation.REPEAT_PERIOD;
		if (repeatPeriod > duration) {
			nextCycleTriggeredTime = lastCycleTriggeredTime + repeatPeriod;
			if (nextCycleTriggeredTime > System.currentTimeMillis()) {
				state = State.REQUESTED;
				onStateChanged(name, state);
				rescheduleStartTimer(new Date(nextCycleTriggeredTime));
			} else {
				// next repeat is in the past
				// don't change the state,
				// therefore repeat immediately
				// and set current time
				lastCycleTriggeredTime = System.currentTimeMillis();
				nextCycleTriggeredTime = -1;

				if (duration > 0) {
					// compensate for any delays by specifying a
					// due time for the next duration
					nextCycleDurationDueTime = lastCycleDurationDueTime + duration;
				}
			}
		} else if (repeatPeriod == duration) {
			// compensate for any delays by specifying a
			// due time for the next duration
			nextCycleDurationDueTime = lastCycleDurationDueTime + duration;

			long timeToNextCycleEnd = nextCycleDurationDueTime - System.currentTimeMillis();
			if (timeToNextCycleEnd > repeatPeriod) {
				// we were signaled earlier than expected and
				// since we don't want to repeat earlier than
				// the repeat period, we schedule the start time
				state = State.REQUESTED;
				onStateChanged(name, state);

				long triggerTime = System.currentTimeMillis() + (timeToNextCycleEnd - repeatPeriod);
				long calculatedTriggerTime = lastCycleTriggeredTime + repeatPeriod;
				if (calculatedTriggerTime > System.currentTimeMillis()) {
					// same thing happened in the last cycle,
					// so we can use the pre calculated time
					triggerTime = calculatedTriggerTime;

					// also set the next time
					nextCycleTriggeredTime = calculatedTriggerTime;
				}

				rescheduleStartTimer(new Date(triggerTime));
			}
		} else if (duration > 0) {
			if (lastCycleDurationDueTime <= System.currentTimeMillis()) {
				// if we haven't been triggered earlier
				// compensate for any delays by specifying a
				// due time for the next duration
				nextCycleDurationDueTime = lastCycleDurationDueTime + duration;
			}
		}
	}

	/**
	 * Controls the cycle state change events.
	 */
	@Override
	public void run() {
		lock.lock();
		try {
			while (State.UNREQUESTED.compareTo(state) <= 0) {
				while (State.REQUESTED.compareTo(state) <= 0) {
					if (State.ACTIVE.equals(state)) {
						enable();
						try {
							while (State.ACTIVE.equals(state)) {
								exec();
							}
						} finally {
							disable();
						}
					}
					if (State.REQUESTED.compareTo(state) <= 0) {
						condition.awaitUninterruptibly();
					} else {
						break;
					}
				}
				if (State.UNREQUESTED.compareTo(state) <= 0) {
					condition.awaitUninterruptibly();
				} else {
					break;
				}
			}
			// event cycle already undefined
			if ((initiation == Initiation.UNDEFINE)
					&& (termination == Termination.UNDEFINE)) {
				List<SubscriberController> list = new ArrayList<SubscriberController>();
				subscribersLock.lock();
				try {
					for (SubscriberController subscriber : subscribers) {
						if (subscriber instanceof SubscriberListener) {
							list.add(subscriber);
						}
					}
				} finally {
					subscribersLock.unlock();
				}

				if (list.size() > 0) {
					// send empty report to each outstanding all poll call
					ReportsInfo<Result, Datas> info = new ReportsInfo<Result, Datas>(
							list.toArray(new SubscriberController[] {}),
							initiation, termination);
					onReportStarted(name);
					reports.enqueue(info);
				} else {
					reports.dispose();
				}
			}
			condition.signal();
			clear();
		} catch (Exception e) {
			e.printStackTrace();
			thread = null;
			onCycleFailed(name, "Cycle aborted unexpectedly", e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Adds a subscriber. Starts the cycle if there are no start triggers.
	 *
	 * @param subscriber
	 *            The subscriber
	 */
	public void add(SubscriberController subscriber) {
		lock.lock();
		try {
			subscribersLock.lock();
			try {
				subscribers.add(subscriber);
				if (subscribers.size() == 1) {
					if (State.UNREQUESTED.compareTo(state) >= 0) {
						state = State.REQUESTED;
						onStateChanged(name, state);
						if (immediate) {
							initiation = Initiation.REQUESTED;
							start((Trigger) null);
						}
					}
				}
			} finally {
				subscribersLock.unlock();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Removes a subscriber
	 *
	 * @param subscriber
	 *            The subscriber
	 */
	public void remove(SubscriberController subscriber) {
		lock.lock();
		try {
			if (State.UNREQUESTED.compareTo(state) <= 0) {
				subscribersLock.lock();
				try {
					subscriber.setActive(false);
					subscribers.remove(subscriber);
					if (subscribers.size() == 0) {
						if (State.ACTIVE.equals(state)) {
							termination = Termination.UNREQUESTED;
						}
						state = State.UNREQUESTED;
						// Cycle state must be inactive, otherwise tags reported by the
						// reader will block the processing of the execution results.
						datas.reset();
						resetCycleTimes();
						onStateChanged(name, state);
						condition.signal();
					}
				} finally {
					subscribersLock.unlock();
				}
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Removes all subscriber of type {@link SubscriberListener} where cycle
	 * count is zero.
	 */
	private void remove() {
		subscribersLock.lock();
		try {
			List<SubscriberController> subscribers = new ArrayList<SubscriberController>();
			for (SubscriberController subscriber : this.subscribers) {
				if ((subscriber instanceof SubscriberListener)
						&& ((SubscriberListener<?>) subscriber).getStale()) {
					subscribers.add(subscriber);
				}
			}
			this.subscribers.removeAll(subscribers);
			if ((this.subscribers.size() == 0) && (State.REQUESTED.compareTo(state) <= 0)) {
				state = State.UNREQUESTED;
				// Cycle state must be inactive, otherwise tags reported by the
				// reader will block the processing of the execution results.
				datas.reset();
				resetCycleTimes();
				onStateChanged(name, state);
			}
		} finally {
			subscribersLock.unlock();
		}
	}

	/**
	 * Finds a subscriber where URI is equal to given URI.
	 *
	 * @param uri
	 *            The URI of the subscriber
	 * @return The subscriber controller
	 * @throws InvalidURIException 
	 */
	public SubscriberController find(URI uri) throws InvalidURIException {
		Subscriber.getInstance().validateUri(uri);

		subscribersLock.lock();
		try {
			for (SubscriberController subscriber : subscribers) {
				if (subscriber.getURI().compareTo(uri) == 0) {
					return subscriber;
				}
			}
		} finally {
			subscribersLock.unlock();
		}
		return null;
	}

	/**
	 * Retrieves if a subscriber with URI exists.
	 *
	 * @param uri
	 *            The URI of subscriber looking for
	 * @return true is subscriber exists, false otherwise
	 */
	public boolean exists(URI uri) {
		subscribersLock.lock();
		try {
			for (SubscriberController subscriber : subscribers) {
				if (!(subscriber instanceof SubscriberListener)) {
					if (subscriber.getURI().compareTo(uri) == 0) {
						return true;
					}
				}
			}
		} finally {
			subscribersLock.unlock();
		}
		return false;
	}

	/**
	 * Returns if cycle is busy
	 *
	 * @return True if cycle is busy, false otherwise
	 */
	public boolean isBusy() {
		return State.UNREQUESTED.compareTo(state) < 0; // state > UNREQUESTED
	}

	/**
	 * Returns if cycle is active. Callers should take care about locks to
	 * prevent state changes.
	 *
	 * @return True if cycle is active, false otherwise
	 */
	protected boolean isActive() {
		return state == State.ACTIVE;
	}

	/**
	 * Disposes this instance. Disposes all triggers and un-defines all reader
	 * operations.
	 */
	public void dispose() {
		boolean locked = false;
		try {
			locked = lock.tryLock(100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}

		// stop waiting for any missing data
		if (datas != null) {
			datas.dispose();
		}

		if (!locked) {
			lock.lock();
		}

		try {
			if (intervalTimer != null) {
				intervalTimer.cancel();
			}
			if (startTimer != null) {
				startTimer.cancel();
			}
			termination = Termination.UNDEFINE;
			if (State.UNDEFINED.compareTo(state) < 0 /* state > UNDEFINED */) {
				switch (state) {
				case UNREQUESTED:
					if (reports != null)
						reports.dispose();
					break;
				case REQUESTED:
					initiation = Initiation.UNDEFINE;
					break;
				default:
					break;
				}
				state = State.UNDEFINED;
				onStateChanged(name, state);
				if (thread != null && thread.isAlive()) {
					condition.signal();
					condition.awaitUninterruptibly();
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				disposeSubscribers();
				undefine();
			}
		} finally {
			lock.unlock();
		}

		for (Trigger trigger : this.trigger) {
			trigger.dispose();
		}
		this.trigger.clear();
		unlock();
	}

	private void disposeSubscribers() {
		subscribersLock.lock();
		try {
			int pass = 0;
			while (!subscribers.isEmpty()) {
				pass++;
				Iterator<SubscriberController> it = subscribers.iterator();
				while (it.hasNext()) {
					SubscriberController subscriber = it.next();
					if (subscriber instanceof SubscriberListener<?>) {
						// only dispose real subscribers, listeners will be
						// disposed elsewhere
						it.remove();
					} else if (pass == 1 && subscriber.isErrorState() || pass > 1) {
						// first dispose all subscribers in error state, then
						// all others
						subscriber.dispose();
						it.remove();
					}
				}
			}
		} finally {
			subscribersLock.unlock();
		}
	}

	/**
	 * Notifies the cycle about a new data set.
	 * 
	 * @param reader
	 *            Name of logical reader who has seen the tag
	 * @param data
	 *            The data
	 * @param controller
	 *            The reader controller
	 */
	protected abstract void notify(String reader, Data data, ReaderController controller);

	/**
	 * Clears cycle run data
	 */
	protected void clear() {
	}
}
