package havis.middleware.ale.core.trigger;

import havis.middleware.utils.threading.NamedThreadFactory;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implement the RTC trigger service
 */
public class RtcTriggerService {

	private static TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");

	private static RtcTriggerService instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new RtcTriggerService();
	}

	protected static class TriggerState {
		private RtcTrigger trigger;
		private long next = 0;

		public TriggerState(RtcTrigger trigger) {
			this.trigger = trigger;
		}

		public boolean isNew() {
			return this.next == 0;
		}

		public long reset(long currentTime, long passedMsOfDay) {
			this.next = currentTime + this.trigger.getNext(passedMsOfDay);
			return remaining(currentTime);
		}

		public long remaining(long currentTime) {
			return isNew() ? Long.MAX_VALUE : Math.max(0, this.next - currentTime);
		}

		public RtcTrigger getTrigger() {
			return this.trigger;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((trigger == null) ? 0 : trigger.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof TriggerState))
				return false;
			TriggerState other = (TriggerState) obj;
			if (trigger == null) {
				if (other.trigger != null)
					return false;
			} else if (trigger != other.trigger)
				// using reference equality for exact removal
				return false;
			return true;
		}
	}

	private List<TriggerState> triggers = new CopyOnWriteArrayList<>();
	private ExecutorService executor;
	private Lock lock = new ReentrantLock();
	private Object monitor = new Object();

	private Runnable processTriggers = new Runnable() {
		@Override
		public void run() {
			Calendar c = Calendar.getInstance(utcTimeZone);
			long delay;
			Set<RtcTrigger> invoked = new HashSet<>();
			while (!Thread.interrupted()) {
				c.setTimeInMillis(System.currentTimeMillis());
				delay = Long.MAX_VALUE;
				invoked.clear();
				for (TriggerState state : triggers) {
					long remaining;
					if (state.isNew()) {
						// newly added trigger, has to be set first
						remaining = state.reset(c.getTimeInMillis(), getPassedMsOfDay(c));
					} else if ((remaining = state.remaining(c.getTimeInMillis())) == 0) {
						// trigger must be invoked now
						handle(state.getTrigger(), invoked);
						remaining = state.reset(c.getTimeInMillis(), getPassedMsOfDay(c));
					}
					delay = Math.min(delay, remaining);
				}
				synchronized (monitor) {
					try {
						// will be notified before the delay is passed
						// when a new trigger is added
						monitor.wait(delay);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}
	};

	public static RtcTriggerService getInstance() {
		return instance;
	}

	/**
	 * Creates a new instance
	 */
	RtcTriggerService() {
	}

	private long getPassedMsOfDay(Calendar calendar) {
		long now = calendar.getTimeInMillis();
		try {
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			return now - calendar.getTimeInMillis();
		} finally {
			calendar.setTimeInMillis(now);
		}
	}

	/**
	 * Adds a new RTC trigger
	 * 
	 * @param trigger
	 *            The trigger
	 */
	void add(RtcTrigger trigger) {
		this.triggers.add(new TriggerState(trigger));
		synchronized (monitor) {
			// notify the worker to calculate the new trigger
			monitor.notify();
		}
		startExecutor();
	}

	private void startExecutor() {
		if (this.executor == null) {
			lock.lock();
			try {
				if (this.executor == null) {
					this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory(RtcTriggerService.class.getSimpleName()));
					this.executor.execute(processTriggers);
				}
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Removes the RTC trigger
	 * 
	 * @param trigger
	 *            The trigger
	 */
	void remove(RtcTrigger trigger) {
		// remove the exact instance only, see TriggerState.equals()
		this.triggers.remove(new TriggerState(trigger));
		stopExecutor();
	}

	private void stopExecutor() {
		if (this.triggers.size() == 0 && this.executor != null) {
			lock.lock();
			try {
				if (this.triggers.size() == 0 && this.executor != null) {
					this.executor.shutdownNow();
					this.executor = null;
				}
			} finally {
				lock.unlock();
			}
		}
	}

	private void handle(RtcTrigger trigger, Set<RtcTrigger> invoked) {
		// Make sure we trigger equal instances only once, we try
		// triggering all triggers with the same RTC that are
		// not equal (with a different creator ID).
		if (!invoked.contains(trigger)) {
			if (trigger.invoke()) {
				invoked.add(trigger);
			}
		}
	}
}