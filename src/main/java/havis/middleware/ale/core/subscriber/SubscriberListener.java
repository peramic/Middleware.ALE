package havis.middleware.ale.core.subscriber;

import havis.middleware.ale.service.IReports;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.subscriber.SubscriberConnector;
import havis.middleware.utils.threading.Pipeline;

import java.net.URI;

/**
 * Class that represents a subscriber listener which is used for poll or
 * immediate calls from a client application.
 * 
 * This class holds a cycle count to indicate for how many count of cycles this
 * subscriber needs to get reports. In most cases the cycle count is set to one,
 * so that the subscriber listener unsubscribes immediately by the common cycle
 * after the first cycle run is done. This class inherits from {@link Pipeline}
 * so that <code>Dequeue()</code> can be invoke directly to wait for the arrival
 * of reports.
 * 
 * @param <T>
 *            The type of {@link IReports} which is expected by the poll or
 *            immediate call from a client application.
 */
public class SubscriberListener<T extends IReports> implements SubscriberController {

	Pipeline<T> queue = new Pipeline<T>();

	/**
	 * Retrieves the count of cycle listener will be involved
	 */
	private int cycles;

	/**
	 * Creates new instance
	 * 
	 * @param cycles
	 *            The cycle count
	 */
	public SubscriberListener(int cycles) {
		this.cycles = cycles;
	}

	/**
	 * Sets the count of cycle listener will be involved
	 */
	void setCycles(int cycles) {
		this.cycles = cycles;
	}

	/**
	 * Retrieves the stale state. Each call decreases the cycle count.
	 * 
	 * @return The stale state
	 */
	public boolean getStale() {
		return --cycles < 1;
	}

	/**
	 * Retrieves the notification uri for the {@link SubscriberConnector}
	 * implementation.
	 */
	@Override
	public URI getURI() {
		return null;
	}

	/**
	 * Retrieves the active state of the the {@link SubscriberConnector}
	 * implementation.
	 */
	@Override
	public boolean getActive() {
		return true;
	}

	/**
	 * Sets the active state
	 * 
	 * @param state
	 *            The active state
	 */
	@Override
	public void setActive(boolean state) {
	}

	@Override
	public boolean isErrorState() {
		return false;
	}

	/**
	 * Operation to increase the outstanding report counter
	 */
	@Override
	public void inc() {
	}

	/**
	 * Operation to decrease the outstanding report counter.
	 */
	@Override
	public void dec() {
	}

	@Override
	public void enqueue(ECReports reports) {
		@SuppressWarnings("unchecked")
		T r = (T) reports;
		queue.enqueue(r);
	}

	@Override
	public void enqueue(CCReports reports) {
		@SuppressWarnings("unchecked")
		T r = (T) reports;
		queue.enqueue(r);
	}

	@Override
	public void enqueue(PCReports reports) {
		@SuppressWarnings("unchecked")
		T r = (T) reports;
		queue.enqueue(r);
	}

	public T dequeue() {
		return queue.dequeue();
	}

	@Override
	public void dispose() {
		queue.dispose();
	}
}
