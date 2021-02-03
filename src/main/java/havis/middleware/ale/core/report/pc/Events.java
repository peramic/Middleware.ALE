package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.operation.Event;
import havis.middleware.ale.core.report.Counter;
import havis.middleware.ale.core.report.IDatas;
import havis.middleware.utils.generic.LinkedHashSet;
import havis.middleware.utils.generic.Predicate;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements the port container
 */
public class Events implements IDatas {

	/**
	 * Event list
	 */
	private LinkedHashSet<Event> events;

	private Counter counter = new Counter();

	private AtomicBoolean disposed = new AtomicBoolean(false);

	/**
	 * Creates a new instance
	 */
	public Events() {
		init();
	}

	/**
	 * Creates a new instance with initial values
	 *
	 * @param events
	 *            The initial events
	 * @param counter
	 *            The initial counter
	 */
	private Events(LinkedHashSet<Event> events, Counter counter, AtomicBoolean disposed) {
		this.events = events;
		this.counter = counter;
		this.disposed = disposed;
	}

	void init() {
		events = new LinkedHashSet<Event>();
	}

	/**
	 * Adds new entry by key and value
	 *
	 * @param key
	 *            The entry key
	 * @param value
	 *            The entry value
	 */
	public void add(Event key, Event value) {
		events.add(key, value);
	}

	/**
	 * Removes the event
	 *
	 * @param event
	 *            The event
	 */
	public void remove(Event event) {
		events.remove(event);
	}

	/**
	 * Does nothing
	 */
	@Override
    public void clear() {
		init();
	}

	/**
	 * Rotates the container
	 */
	@Override
    public void rotate() {
	}

	/**
	 * Resets the container
	 */
	@Override
	public void reset() {
		if (!disposed.get()) {
			long counter = this.counter.await(null);
			while (events.find(new Predicate<Event>() {
				@Override
				public boolean invoke(Event e) {
					return e == null;
				}
			})) {
				counter = this.counter.await(Long.valueOf(counter));
				// TODO: add timeout
			}
		}
		init();
	}

	/**
	 * Retrieves the count of elements in container
	 *
	 * @return The current event size
	 */
	public int getCount() {
		return events.getCount();
	}

	/**
	 * Waits until new elements arrived
	 *
	 * @param counter
	 *            The last counter
	 * @return The next counter
	 */
	long await(Long counter) {
		if (disposed.get()) {
			return 0;
		}
		return this.counter.await(counter);
	}

	/**
	 * Pulses all waiting reports
	 */
	public void pulse() {
		counter.pulse();
	}

	/**
	 * Returns a shallow clone
	 *
	 * @return The data clone
	 */
	@Override
    public Events clone() {
		return new Events(events, counter, disposed);
	}

	/**
	 * Gets the event by event as key
	 *
	 * @param key
	 *            The key event
	 * @return The value event
	 */
	public Event get(Event key) {
		return events.get(key);
	}

	/**
	 * Returns whether the event is in the list
	 * @param key the event
	 * @return true if the event is in the list, false otherwise
	 */
	public boolean contains(Event key) {
		return events.containsKey(key);
	}

	/**
	 * Sets the event by event as key
	 *
	 * @param key
	 *            The key event
	 * @param value
	 *            The value event
	 */
	public void set(Event key, Event value) {
		events.set(key, value);
	}

	/**
	 * Returns the events as list
	 *
	 * @return The event list
	 */
	Set<Event> toList() {
		return events.toList();
	}

	@Override
	public String toString() {
		return "Events [events=" + events + ", counter=" + counter + "]";
	}

	public boolean isDisposed() {
		return disposed.get();
	}

	@Override
	public void dispose() {
		if (disposed.compareAndSet(false, true)) {
			if (events != null) {
				events.clear();
			}
			if (counter != null) {
				counter.pulse();
			}
		}
	}
}