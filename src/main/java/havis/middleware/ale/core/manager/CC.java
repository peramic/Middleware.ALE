package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ParameterForbiddenException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.cycle.CommandCycle;
import havis.middleware.ale.core.subscriber.Subscriber;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.CCParameterListEntry;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.cc.CCSpec;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the ALE Writing API as specified : ALE 1.1.1 (9.1)
 */
public class CC extends CycleManager<CommandCycle> {

    private havis.middleware.ale.core.depot.service.cc.CommandCycle depot = havis.middleware.ale.core.depot.service.cc.CommandCycle.getInstance();

	private static CC instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new CC();
	}

	/**
	 * Creates a new instance. Initializes parameters.
	 */
	private CC() {
		super();
	}

	/**
	 * Retrieves the static instance
	 *
	 * @return The static instance
	 */
	public static CC getInstance() {
		return instance;
	}

	/**
	 * Defines a new command cycle by name specified by specification
	 *
	 * @param name
	 *            The name of the command cycle
	 * @param spec
	 *            The specification of the command cycle
	 * @param persist
	 *            Persist changes
	 * @throws ValidationException
	 *             if validation failed
	 * @throws DuplicateNameException
	 *             if name already exists
	 * @throws ImplementationException
	 */
	public void define(String name, CCSpec spec, boolean persist)
			throws ValidationException, DuplicateNameException,
			ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.Define, "Define {0} {1}", new Object[] { name, spec });
		try {
			if (Name.isValid(name)) {
				lock.lock();
				try {
					if (cycles.containsKey(name)) {
						throw new DuplicateNameException("Command cycle '"
								+ name + "' already defined");
					} else {
						if (spec == null) {
							throw new ValidationException(
									"No specification given for command cycle '"
											+ name + "'!");
						} else {
						    CommandCycle cycle = new CommandCycle(name, spec);
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
		} catch (ValidationException | DuplicateNameException
				| ImplementationException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Name, Exits.Service.CC.DefineFailed, "Define failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Un-defines an existing command cycle
	 *
	 * @param name
	 *            The name of the command cycle
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 *             if named command cycle does not exists
	 */
	public void undefine(String name, boolean persist)
			throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.Undefine, "Undefine {0}", name);
		try {
			lock.lock();
			try {
				CommandCycle cycle = cycles.get(name);
				if (cycle != null) {
					cycles.remove(name);
					cycle.dispose();
					if (persist)
						depot.remove(name);
				} else {
					throw new NoSuchNameException(
							"Could not undefine a unknown command cycle '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Name, Exits.Service.CC.UndefineFailed, "Undefine failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets the specification of a command cycle
	 *
	 * @param name
	 *            The name of the command cycle
	 * @return The command cycle specification
	 * @throws NoSuchNameException
	 *             If name doesn't exists
	 */
	public CCSpec getSpec(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.GetCCSpec, "Get CC spec {0}", name);
		try {
			lock.lock();
			try {
				CommandCycle cycle = cycles.get(name);
				if (cycle != null) {
					return cycle.getSpec();
				} else {
					throw new NoSuchNameException(
							"Could not get specification for unknown command cycle '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Name, Exits.Service.CC.GetCCSpecFailed, "Get CC spec failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets a list of names of all existing command cycles definitions
	 *
	 * @return List of command cycle names
	 */
	public List<String> getNames() {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.GetCCSpecNames, "Get CC spec names");
		lock.lock();
		try {
			return new ArrayList<String>(cycles.keySet());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Subscribes to a command cycle
	 *
	 * @param name
	 *            Name of the command cycle
	 * @param uri
	 *            The URI to subscribe
	 * @param properties
	 *            The properties
	 * @param persist
	 *            Persist changes
	 * @throws ParameterForbiddenException
	 * @throws DuplicateSubscriptionException
	 *             If previous subscription with the same uri exists
	 * @throws ImplementationException
	 *             If subscriber connector instantiation failed
	 * @throws NoSuchNameException
	 *             if no command cycle with that name exists
	 * @throws InvalidURIException
	 */
	void subscribe(String name, URI uri, PropertiesType properties, boolean persist)
			throws ParameterForbiddenException, DuplicateSubscriptionException,
			ImplementationException, NoSuchNameException, InvalidURIException {
		lock.lock();
		try {
			CommandCycle cycle = cycles.get(name);
			if (cycle != null) {
				if (cycle.isParameterized()) {
					throw new ParameterForbiddenException(
							"Command cycle '" + name + "' is parameterized");
				} else {
					if (cycle.exists(uri)) {
						throw new DuplicateSubscriptionException("URI '" + uri
								+ "' already subscribed to command cycle '"
								+ name + "'");
					} else {
						cycle.add(Subscriber.getInstance().get(uri, properties, CCReports.class));
						if (persist)
							depot.add(name, uri);
					}
				}
			} else {
				throw new NoSuchNameException(
						"Could not subscribe to unknown command cycle '" + name
								+ "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Subscribes to a command cycle
	 *
	 * @param name
	 *            Name of the command cycle
	 * @param uri
	 *            The URI to subscribe
	 * @param properties
	 *            The properties
	 * @param persist
	 *            Persist changes
	 * @throws InvalidURIException
	 * @throws ParameterForbiddenException
	 * @throws DuplicateSubscriptionException
	 *             if previous subscription with the same URI exists
	 * @throws ImplementationException
	 *             if subscriber connector instantiation failed
	 * @throws NoSuchNameException
	 *             if no command cycle with that name exists
	 */
	public void subscribe(String name, String uri, PropertiesType properties, boolean persist)
			throws InvalidURIException, ParameterForbiddenException,
			DuplicateSubscriptionException, ImplementationException,
			NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.Subscribe, "Subscribe {0} {1}", new Object[] { name, uri });
		try {
			try {
				subscribe(name, new URI(uri), properties, persist);
			} catch (URISyntaxException e) {
				throw new InvalidURIException(e.getMessage());
			}
		} catch (InvalidURIException | ParameterForbiddenException
				| DuplicateSubscriptionException | ImplementationException
				| NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Name, Exits.Service.CC.SubscribeFailed, "Subscribe failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Un-subscribes URI from named command cycle.
	 *
	 * @param name
	 *            The name of the command cycle
	 * @param uri
	 *            The URI to un-subscribe
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchSubscriberException
	 *             if no subscription with this url exists
	 * @throws NoSuchNameException
	 *             if no command cycle with that name exists
	 * @throws InvalidURIException 
	 *             if the URI is invalid
	 */
	void unsubscribe(String name, URI uri, boolean persist)
			throws NoSuchSubscriberException, NoSuchNameException, InvalidURIException {
		SubscriberController subscriber;
		CommandCycle cycle;
		lock.lock();
		try {
			cycle = cycles.get(name);
			if (cycle != null) {
				subscriber = cycle.find(uri);
				if (subscriber == null) {
					throw new NoSuchSubscriberException("URI '"
							+ uri.toString()
							+ "' not subscribed on command cycle '" + name
							+ "'");
				} else {
					if (persist)
						depot.remove(name, uri);
				}
			} else {
				throw new NoSuchNameException("Could not unsubscribe URI '"
						+ uri.toString() + "' from unknown command cycle '"
						+ name + "'");
			}
		} finally {
			lock.unlock();
		}
		cycle.remove(subscriber);
		subscriber.dispose();
	}

	/**
	 * Un-subscribes URI from named command cycle.
	 *
	 * @param name
	 *            The name of the command cycle
	 * @param uri
	 *            The URI to un-subscribe
	 * @param persist
	 *            Persist changes
	 * @throws InvalidURIException
	 * @throws NoSuchSubscriberException
	 *             if no subscription with this url exists
	 * @throws NoSuchNameException
	 *             if no command cycle with that name exists
	 */
	public void unsubscribe(String name, String uri, boolean persist)
			throws InvalidURIException, NoSuchSubscriberException,
			NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.Unsubscribe, "Unsubscribe {0} {1}", new Object[] { name, uri });
		try {
			try {
				unsubscribe(name, new URI(uri), persist);
			} catch (URISyntaxException e) {
				throw new InvalidURIException(e.getMessage());
			}
		} catch (InvalidURIException | NoSuchSubscriberException
				| NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Name, Exits.Service.CC.UnsubscribeFailed, "Unsubscribe failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Polls to a named command cycle
	 *
	 * @param name
	 *            The command cycle name
	 * @param entries
	 *            List of parameterized entries
	 * @return The reports object
	 * @throws NoSuchNameException
	 *             if no command cycle with that name exists
	 * @throws ImplementationException
	 * @throws ValidationException
	 * @throws ParameterException
	 */
	public CCReports poll(String name, List<CCParameterListEntry> entries)
			throws NoSuchNameException, ImplementationException,
			ParameterException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.Poll, "Poll {0} {1}", new Object[] { name, entries });
		try {
			SubscriberListener<CCReports> subscriber = new SubscriberListener<CCReports>(
					1);
			lock.lock();
			try {
				CommandCycle cycle = cycles.get(name);
				if (cycle != null) {
					if (cycle.isParameterized()
							|| (entries != null && entries.size() > 0)) {
						if (entries == null)
							entries = new ArrayList<CCParameterListEntry>();
						if (cycle.isBusy()) {
							try {
								cycle = new CommandCycle(name, cycle.getSpec());
								cycle.start();
								cycle.add(subscriber, entries);
								CCReports reports = subscriber.dequeue();
								cycle.dispose();
								return reports;
							} catch (ValidationException e) {
								throw new ImplementationException(e.getReason());
							}
						} else {
							cycle.add(subscriber, entries);
						}
					} else {
						cycle.add(subscriber);
					}
				} else {
					throw new NoSuchNameException(
							"Could not poll on unknown command cycle '" + name
									+ "'");
				}
			} finally {
				lock.unlock();
			}
			return subscriber.dequeue();
		} catch (NoSuchNameException | ImplementationException
				| ParameterException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Name, Exits.Service.CC.PollFailed, "Poll failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Takes a immediate command cycle call with given specification.
	 * 
	 * @param spec
	 *            The command cycle specification
	 * @return The reports object
	 * @throws ImplementationException
	 * @throws ParameterForbiddenException
	 * @throws ValidationException
	 *             If validation failed
	 */
	public CCReports immediate(CCSpec spec) throws ImplementationException, ParameterForbiddenException, ValidationException {
		// deliberately unsynchronized
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.Immediate, "Immediate {0}", spec);
		try {
			try {
				SubscriberListener<CCReports> subscriber = new SubscriberListener<CCReports>(1);
				CommandCycle cycle = new CommandCycle(null, spec);
				if (cycle.isParameterized()) {
					cycle.dispose();
					throw new ParameterForbiddenException("Command cycle is parameterized");
				} else {
					try {
						addVolatileCycle(cycle);
						cycle.start();
						cycle.add(subscriber);
						return subscriber.dequeue();
					} finally {
						removeVolatileCycle(cycle);
						cycle.dispose();
					}
				}
			} catch (ParameterForbiddenException | ValidationException e) {
				throw e;
			} catch (Exception e) {
				throw new ImplementationException(e.getMessage());
			}
		} catch (ImplementationException | ParameterForbiddenException | ValidationException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Name, Exits.Service.CC.ImmediateFailed, "Immediate failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets a list of subscribers of the named command cycle.
	 *
	 * @param name
	 *            The name of the command cycle
	 * @return List subscriber URIs
	 * @throws NoSuchNameException
	 *             if no command cycle with that name exists
	 */
	public List<String> getSubscribers(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Name, Exits.Service.CC.GetSubscribers, "Get subscribers {0}", name);
		try {
			lock.lock();
			try {
				CommandCycle cycle = cycles.get(name);
				if (cycle != null) {
					return cycle.getSubscribers();
				} else {
					throw new NoSuchNameException(
							"Could not get subscribers for unknown command cycle '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Name, Exits.Service.CC.GetSubscribersFailed, "Get subscribers failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets the standard version
	 *
	 * @return The standard version
	 */
	public static String getStandardVersion() {
		return Config.getInstance().getService().getCc().getVersion()
				.getStandard();
	}
}