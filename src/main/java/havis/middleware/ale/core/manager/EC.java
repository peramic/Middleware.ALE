package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.cycle.EventCycle;
import havis.middleware.ale.core.subscriber.Subscriber;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the ALE - Main API Class as specified in ALE 1.1.1 (8.1)
 */
public class EC extends CycleManager<EventCycle> {

	private havis.middleware.ale.core.depot.service.ec.EventCycle depot = havis.middleware.ale.core.depot.service.ec.EventCycle.getInstance();

	private static EC instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new EC();
	}

	/**
	 * Create a new instance.
	 */
	private EC() {
		super();
	}

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static EC getInstance() {
		return instance;
	}

	/**
	 * Defines a new event cycle with spec under name
	 * 
	 * @param name
	 *            The specification name
	 * @param spec
	 *            The event cycle specification
	 * @param persist
	 *            Persist changes
	 * @throws ImplementationException
	 * @throws ValidationException
	 *             If validation failed
	 * @throws DuplicateNameException
	 *             If name already defined
	 */
	public void define(String name, ECSpec spec, boolean persist)
			throws ImplementationException, ValidationException,
			DuplicateNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.Define, "Define {0} {1}", new Object[] { name, spec });
		try {
			if (Name.isValid(name)) {
				lock.lock();
				try {
					if (cycles.containsKey(name)) {
						throw new DuplicateNameException("Event cycle '" + name
								+ "' already defined");
					} else {
						if (spec == null) {
							throw new ValidationException(
									"No specification given for event cycle '"
											+ name + "'!");

						} else {
							EventCycle cycle = new EventCycle(name, spec);
							cycle.start();
							cycles.put(name, cycle);
							if (persist)
								depot.add(name, spec);
						}
					}
				} finally {
					lock.unlock();
				}
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.EC.Name, Exits.Service.EC.DefineFailed, "Define failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Un-defines the event cycle previously defined under name. Cleans up the
	 * event cycle instance.
	 *
	 * @param name
	 *            The specification name
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 *             if there is no specification with that name
	 */
	public void undefine(String name, boolean persist)
			throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.Undefine, "Undefine {0}", name);
		try {
			lock.lock();
			try {
				EventCycle cycle = cycles.get(name);
				if (cycle != null) {
					cycles.remove(name);
					cycle.dispose();
					if (persist)
						depot.remove(name);
				} else {
					throw new NoSuchNameException(
							"Could not undefine a unknown event cycle '" + name
									+ "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.EC.Name, Exits.Service.EC.UndefineFailed, "Undefine failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Returns the specification previously defines under <paramref
	 * name="name"/>.
	 *
	 * @param name
	 *            The specification name
	 * @return The event cycle specification
	 * @throws NoSuchNameException
	 *             if there is no specification with that name
	 */
	public ECSpec getSpec(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.GetECSpec, "Get EC spec {0}", name);
		try {
			lock.lock();
			try {
				EventCycle cycle = cycles.get(name);
				if (cycle != null) {
					return cycle.getSpec();
				} else {
					throw new NoSuchNameException(
							"Could not get specification for unknown event cycle '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.EC.Name, Exits.Service.EC.GetECSpecFailed, "Get EC spec failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Returns a list of all previously defined event cycle specifications.
	 *
	 * @return List of event cycle specification names
	 */
	public List<String> getNames() {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.GetECSpecNames, "Get EC spec names");
		List<String> list = new ArrayList<String>();
		lock.lock();
		try {
			list.addAll(cycles.keySet());
		} finally {
			lock.unlock();
		}
		return list;
	}

	/**
	 * Subscribes to the event cycle name with URI. Adds a new created
	 * {@link SubscriberController} to the event cycle.
	 *
	 * @param name
	 *            The name of the event cycle
	 * @param uri
	 *            The URI of the subscriber
	 * @param properties
	 *            The properties
	 * @param persist
	 *            Persist changes
	 * @throws ImplementationException
	 *             if instantiation of {@link SubscriberController} failed.
	 * @throws DuplicateNameException
	 *             if a subscription with URI already occurred.
	 * @throws InvalidURIException
	 * @throws ValidationException
	 * @throws NoSuchNameException
	 *             if no event cycle under name exists.
	 */
	private void subscribe(String name, URI uri, PropertiesType properties, boolean persist)
			throws ImplementationException, DuplicateSubscriptionException,
			InvalidURIException, NoSuchNameException {
		lock.lock();
		try {
			EventCycle cycle = cycles.get(name);
			if (cycle != null) {
				if (cycle.exists(uri)) {
					throw new DuplicateSubscriptionException("URI '" + uri
							+ "' already subscribed to event cycle '" + name
							+ "'");
				} else {
					cycle.add(Subscriber.getInstance().get(uri, properties, ECReports.class));
					if (persist)
						depot.add(name, uri);
				}
			} else {
				throw new NoSuchNameException(
						"Could not subscribe to unknown event cycle '" + name
								+ "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Subscribes to the event cycle.
	 *
	 * @param name
	 *            The name of the event cycle
	 * @param uri
	 *            The URI of the subscriber
	 * @param properties
	 *            The properties
	 * @param persist
	 *            Persist changes
	 * @throws InvalidURIException
	 * @throws NoSuchNameException
	 *             if no event cycle under name exists.
	 * @throws ValidationException
	 * @throws DuplicateNameException
	 *             if a subscription with URI already occurred
	 * @throws ImplementationException
	 *             if instantiation of {@link SubscriberController} failed.
	 */
	public void subscribe(String name, String uri, PropertiesType properties, boolean persist)
			throws InvalidURIException, ImplementationException,
			DuplicateSubscriptionException, NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.Subscribe, "Subscribe {0} {1}", new Object[] { name, uri });
		try {
			try {
				subscribe(name, new URI(uri), properties, persist);
			} catch (URISyntaxException e) {
				throw new InvalidURIException(e.getMessage());
			}
		} catch (InvalidURIException | DuplicateSubscriptionException
				| ImplementationException | NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.EC.Name, Exits.Service.EC.SubscribeFailed, "Subscribe failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Un-subscribes URI from event cycle name.
	 *
	 * @param name
	 *            The event cycle name
	 * @param uri
	 *            The subscriber URI
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchSubscriberException
	 * @throws NoSuchNameException
	 * @throws InvalidURIException 
	 */
	private void unsubscribe(String name, URI uri, boolean persist)
			throws NoSuchSubscriberException, NoSuchNameException, InvalidURIException {
		SubscriberController subscriber;
		EventCycle cycle;
		lock.lock();
		try {
			cycle = cycles.get(name);
			if (cycle != null) {
				subscriber = cycle.find(uri);
				if (subscriber == null) {
					throw new NoSuchSubscriberException("URI '"
							+ uri.toString()
							+ "' not subscribed on event cycle '" + name + "'");
				} else {
					if (persist)
						depot.remove(name, uri);
				}
			} else {
				throw new NoSuchNameException("Could not unsubscribe URI '"
						+ uri.toString() + "' from unknown event cycle '"
						+ name + "'");
			}
		} finally {
			lock.unlock();
		}
		cycle.remove(subscriber);
		subscriber.dispose();
	}

	/**
	 * Un-subscribes URI from event cycle <paramref name="name"/>.
	 *
	 * @param name
	 *            The event cycle name
	 * @param uri
	 *            The subscriber URI
	 * @param persist
	 *            Persist changes
	 * @throws InvalidURIException
	 * @throws NoSuchSubscriberException
	 * @throws NoSuchNameException
	 */
	public void unsubscribe(String name, String uri, boolean persist)
			throws InvalidURIException, NoSuchSubscriberException,
			NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.Unsubscribe, "Unsubscribe {0} {1}", new Object[] { name, uri });
		try {
			try {
				unsubscribe(name, new URI(uri), persist);
			} catch (URISyntaxException e) {
				throw new InvalidURIException(e.getMessage());
			}
		} catch (InvalidURIException | NoSuchSubscriberException
				| NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.EC.Name, Exits.Service.EC.UnsubscribeFailed, "Unsubscribe failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Polls to the event cycle <paramref name="name"/>
	 *
	 * @param name
	 *            The event cycle name
	 * @return The reports from one event cycle run
	 * @throws NoSuchNameException
	 *             if event cycle name does not exists.
	 */
	public ECReports poll(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.Poll, "Poll {0} {1}", name);
		try {
			SubscriberListener<ECReports> subscriber = new SubscriberListener<ECReports>(1);
			lock.lock();
			try {
				EventCycle cycle = cycles.get(name);
				if (cycle != null) {
					cycle.add(subscriber);
				} else {
					throw new NoSuchNameException(
							"Could not poll on unknown event cycle '" + name
									+ "'");
				}
			} finally {
				lock.unlock();
			}
			return subscriber.dequeue();
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.EC.Name, Exits.Service.EC.PollFailed, "Poll failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Creates a anonymously event cycle with <paramref name="spec"/>.
	 *
	 * @param spec
	 *            The event cycle specification
	 * @return The reports from event cycle run
	 * @throws ValidationException
	 * @throws ImplementationException
	 */
	public ECReports immediate(ECSpec spec) throws ValidationException, ImplementationException {
		// deliberately unsynchronized
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.Immediate, "Immediate {0}", spec);
		try {
			try {
				SubscriberListener<ECReports> subscriber = new SubscriberListener<ECReports>(1);
				EventCycle cycle = new EventCycle(null, spec);
				try {
					addVolatileCycle(cycle);
					cycle.start();
					cycle.add(subscriber);
					return subscriber.dequeue();
				} finally {
					removeVolatileCycle(cycle);
					cycle.dispose();
				}
			} catch (ValidationException | ImplementationException e) {
				throw e;
			} catch (Exception e) {
				throw new ImplementationException(e.getMessage());
			}
		} catch (ValidationException | ImplementationException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.EC.Name, Exits.Service.EC.ImmediateFailed, "Immediate failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets the list of subscribers from event cycle <paramref name="name"/>
	 *
	 * @param name
	 *            The name of the event cycle
	 * @return The list of subscribers
	 * @throws NoSuchNameException
	 */
	public List<String> getSubscribers(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.EC.Name, Exits.Service.EC.GetSubscribers, "Get subscribers {0}", name);
		try {
			lock.lock();
			try {
				EventCycle cycle = cycles.get(name);
				if (cycle != null) {
					return cycle.getSubscribers();
				} else {
					throw new NoSuchNameException(
							"Could not get subscribers for unknown event cycle '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.EC.Name, Exits.Service.EC.GetSubscribersFailed, "Get subscribers failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets the standard version
	 *
	 * @return The standard version
	 */
	public static String getStandardVersion() {
		return Config.getInstance().getService().getEc().getVersion()
				.getStandard();
	}
}