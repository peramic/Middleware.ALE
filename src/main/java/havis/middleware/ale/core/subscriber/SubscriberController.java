package havis.middleware.ale.core.subscriber;

import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.subscriber.SubscriberConnector;

import java.net.URI;

/**
 * This interface describe methods to provide reliable
 * {@link SubscriberConnector} access.
 * 
 * 
 * The interface defines a generic type which describe the kind of report a
 * specific subscriber implementation expected.
 * 
 * @param T
 *            The type of report which is expected by the subscriber
 *            implementation used with a concrete subscriber
 */
public interface SubscriberController {

	/**
	 * Retrieves the notification URI for the subscriber implementation.
	 * 
	 * @return The notification URI
	 */
	URI getURI();

	/**
	 * Gets the active state of the the subscriber implementation.
	 * 
	 * @return The active state
	 */
	boolean getActive();

	/**
	 * Sets the active state of the the subscriber implementation.
	 */
	void setActive(boolean state);

	/**
	 * Gets the current error state. The error state will be set initially and
	 * cleared on the first successful transmission.
	 * 
	 * @return true if the subscriber is in error state, false otherwise
	 */
	boolean isErrorState();

	/**
	 * Increases the outstanding report counter.
	 */
	void inc();

	/**
	 * Decreases the outstanding report counter.
	 */
	void dec();

	/**
	 * Enqueues a single reports instance to the pipe for Outstanding reports.
	 * 
	 * @param reports
	 *            The report instance to enqueue.
	 */
	void enqueue(ECReports reports);

	/**
	 * Enqueues a single reports instance to the pipe for Outstanding reports.
	 * 
	 * @param reports
	 *            The report instance to enqueue.
	 */
	void enqueue(CCReports reports);

	/**
	 * Enqueues a single reports instance to the pipe for Outstanding reports.
	 * 
	 * @param reports
	 *            The report instance to enqueue.
	 */
	void enqueue(PCReports reports);

	/**
	 * Disposes this instance and release all its resources.
	 */
	void dispose();
}
