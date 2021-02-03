package havis.middleware.ale.core.subscriber;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.config.PropertyType;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.IReports;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.subscriber.SubscriberConnector;
import havis.middleware.utils.threading.Task;
import havis.middleware.utils.threading.ThreadManager;
import havis.util.monitor.TransportSource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that provides reliable access to a {@link SubscriberConnector}
 * implementation.
 * 
 * This class is used to manage the communication between middleware and a
 * {@link SubscriberConnector} implementation.
 * 
 * It initializes the {@link SubscriberConnector} in a new process for
 * interprocess communication.
 */
public class DefaultSubscriberController implements SubscriberController, TransportSource {

	public interface Call {
		void invoke() throws ValidationException, ImplementationException;
	}

	private URI uri;
	private Map<String, String> properties;
	private SubscriberConnector connector;
	private Class<? extends IReports> reportClass;
	private int count = 0;

	private Lock lock = new ReentrantLock();
	private Lock counter = new ReentrantLock();
	private Condition condition = counter.newCondition();
	private AtomicBoolean first = new AtomicBoolean(true);
	private AtomicBoolean errorState = new AtomicBoolean(true);
	private AtomicBoolean disposed = new AtomicBoolean(false);

	/**
	 * Retrieves the active state of the the {@link SubscriberConnector T}"/>
	 * implementation.
	 */
	public boolean active = true;

	/**
	 * Initializes a new instance of the {@link DefaultSubscriberController}
	 * class.
	 * 
	 * @param uri
	 *            The notification URI to initialize the subscriber connector
	 * @param properties
	 *            The properties
	 * @param connector
	 *            The subscriber connector
	 * @param reportClass
	 *            The report type
	 * @throws ALEException
	 *             if the constructor was unable to create the instance.
	 */
	public DefaultSubscriberController(URI uri, PropertiesType properties, SubscriberConnector connector, Class<? extends IReports> reportClass)
			throws ImplementationException, InvalidURIException {
		this.uri = uri;
		this.reportClass = reportClass;
		this.properties = new HashMap<>();
		if (properties != null) {
			for (PropertyType p : properties.getProperty()) {
				this.properties.put(p.getName(), p.getValue());
			}
		}
		this.connector = connector;

		lock.lock();
		try {
			init();
		} catch (ImplementationException | InvalidURIException e) {
			dispose();
			throw e;
		} catch (Throwable e) {
			dispose();
			throw new ImplementationException(e.getMessage());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Method to initializes the controller and create the connector instance.
	 * 
	 * @throws ImplementationException
	 * @throws InvalidURIException
	 * @throws ValidationException
	 */
	void init() throws ImplementationException, InvalidURIException {
		if (this.connector instanceof MessengerSubscriberConnector) {
			((MessengerSubscriberConnector) this.connector).setReportClass(this.reportClass);
		}
		this.connector.init(this.uri, this.properties);
	}

	@Override
	public String getUri() {
		return this.uri.toString();
	}

	@Override
	public boolean getActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Enqueues a single reports instance to the pipe for outstanding reports.
	 * This method uses the {@link ThreadManager} to deliver the report
	 * asynchronously.
	 * 
	 * @param reports
	 *            The reports instance to enqueue.
	 */
	@Override
	public void enqueue(final ECReports reports) {
		enqueueReport(reports);
	}

	/**
	 * Enqueues a single reports instance to the pipe for outstanding reports.
	 * This method uses the {@link ThreadManager} to deliver the report
	 * asynchronously.
	 * 
	 * @param reports
	 *            The reports instance to enqueue.
	 */
	@Override
	public void enqueue(final CCReports reports) {
		enqueueReport(reports);
	}

	/**
	 * Enqueues a single reports instance to the pipe for outstanding reports.
	 * This method uses the {@link ThreadManager} to deliver the report
	 * asynchronously.
	 * 
	 * @param reports
	 *            The reports instance to enqueue.
	 */
	@Override
	public void enqueue(final PCReports reports) {
		enqueueReport(reports);
	}

	private void enqueueReport(final IReports reports) {
		if (!disposed.get() && reports != null) {
			ThreadManager.enqueue(new Task() {
				@Override
				public void run() {
					// was in queue before, now running because of interrupt
					// from dispose, end immediately
					if (disposed.get()) {
						dec();
						return;
					}
					try {
						while (!lock.tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
						}
					} catch (InterruptedException e) {
						// abort sending
						dec();
						Thread.currentThread().interrupt();
						return;
					}
					try {
						if (!disposed.get()) {
							if (reports instanceof ECReports) {
								connector.send((ECReports) reports);
							} else if (reports instanceof CCReports) {
								connector.send((CCReports) reports);
							} else if (reports instanceof PCReports) {
								connector.send((PCReports) reports);
							}
							if (errorState.compareAndSet(true, false) && !first.compareAndSet(true, false)) {
								Exits.Log.logp(Exits.Level.Information, Exits.Subscriber.Controller.Name, Exits.Subscriber.Controller.DeliverFailed,
										"Subscriber {0} delivery error state was resolved", new Object[] { uri.toString(), reports });
							}
						}
					} catch (Exception e) {
						if (errorState.compareAndSet(false, true) || first.compareAndSet(true, false)) {
							Exits.Log.logp(Exits.Level.Error, Exits.Subscriber.Controller.Name, Exits.Subscriber.Controller.DeliverFailed,
									"Subscriber {0} delivery entered error state: {1}", new Object[] { uri.toString(), e.getMessage(), reports });
						}
					} finally {
						lock.unlock();
						dec();
					}
				}

				@Override
				public int getGroupId() {
					return DefaultSubscriberController.this.hashCode();
				}
			});
		}
	}

	/**
	 * Retrieves the notification URI for the {@link SubscriberConnector}
	 * implementation.
	 */
	@Override
	public URI getURI() {
		return uri;
	}

	/**
	 * Method to increase the outstanding report counter.
	 */
	@Override
	public void inc() {
		counter.lock();
		count++;
		counter.unlock();
	}

	/**
	 * Method to decrease the outstanding report counter.
	 */
	@Override
	public void dec() {
		counter.lock();
		try {
			count--;
			condition.signal();
		} finally {
			counter.unlock();
		}
	}

	@Override
	public boolean isErrorState() {
		return this.errorState.get();
	}

	private void await() {
		if (!errorState.get()) {
			// only wait in error state
			counter.lock();
			try {
				while (count > 0) {
					condition.awaitUninterruptibly();
				}
			} finally {
				counter.unlock();
			}
		}
	}

	/**
	 * Disposes the instance and release all its resources.
	 */
	@Override
	public void dispose() {
		await();
		disposed.set(true);
		if (connector != null) {
			// interrupt all running threads
			ThreadManager.interrupt(hashCode());
			lock.lock();
			try {
				connector.dispose();
			} catch (Exception e) {
				// ignoring errors on dispose
			} finally {
				lock.unlock();
			}
			connector = null;
		}
	}
}
