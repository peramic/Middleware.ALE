package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.port.result.Result.State;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.cycle.PortCycle;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.report.Counter;
import havis.middleware.ale.core.report.pc.Operation;
import havis.middleware.ale.core.subscriber.Subscriber;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.pc.PCOpReport;
import havis.middleware.ale.service.pc.PCOpSpec;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.service.pc.PCSpec;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class provides the ALE - Port Cycle API
 */
public class PC extends CycleManager<PortCycle> {

	private havis.middleware.ale.core.depot.service.pc.PortCycle depot = havis.middleware.ale.core.depot.service.pc.PortCycle.getInstance();

	private static PC instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new PC();
	}

	/**
	 * Create a new instance.
	 */
	private PC() {
		super();
	}

	/**
	 * Retrieves the static instance
	 *
	 * @return The static instance
	 */
	public static PC getInstance() {
		return instance;
	}

	/**
	 * Defines a new port cycle with specification under name.
	 *
	 * @param name
	 *            The specification name
	 * @param spec
	 *            The port cycle specification
	 * @param persist
	 *            Persist changes
	 * @throws DuplicateNameException
	 *             if name already defined
	 * @throws ValidationException
	 *             if validation failed
	 */
	public void define(String name, PCSpec spec, boolean persist)
			throws ValidationException, DuplicateNameException,
			ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.Define, "Define {0} {1}", new Object[] { name, spec });
		try {
			if (Name.isValid(name)) {
				lock.lock();
				try {
					if (cycles.containsKey(name)) {
						throw new DuplicateNameException("Port cycle '" + name
								+ "' already defined");
					} else {
						if (spec == null) {
							throw new ValidationException(
									"No specification given for port cycle '"
											+ name + "'!");
						} else {
							PortCycle cycle = new PortCycle(name, spec);
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
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.DefineFailed, "Define failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Un-defines the port cycle previously defined under name. Cleans up the
	 * port cycle instance.
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
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.Define, "Undefine {0}", name);
		try {
			lock.lock();
			try {
				PortCycle cycle = cycles.get(name);
				if (cycle != null) {
					cycles.remove(name);
					cycle.dispose();
					if (persist)
						depot.remove(name);
				} else {
					throw new NoSuchNameException(
							"Could not undefine a unknown port cycle '" + name
									+ "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.UndefineFailed, "Undefine failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Returns the specification previously defines under name.
	 *
	 * @param name
	 *            The specification name
	 * @return The port cycle specification
	 * @throws NoSuchNameException
	 *             if there is no specification with that name
	 */
	public PCSpec getSpec(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.GetPCSpec, "Get PC spec {0}", name);
		try {
			lock.lock();
			try {
				PortCycle cycle = cycles.get(name);
				if (cycle != null) {
					return cycle.getSpec();
				} else {
					throw new NoSuchNameException(
							"Could not get specification for unknown port cycle '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.GetPCSpecFailed, "Get PC spec failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Returns a list of all previously defined port cycle specifications.
	 *
	 * @return List of port cycle specification names
	 */
	public List<String> getNames() {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.GetPCSpecNames, "Get PC spec names");
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
	 * Subscribes to the port cycle name with URI. Adds a new created
	 * {@link SubscriberController} to the port cycle.
	 *
	 * @param name
	 *            The name of the port cycle
	 * @param uri
	 *            The URI of the subscriber
	 * @param properties
	 *            The properties
	 * @param persist
	 *            Persist changes
	 * @throws DuplicateSubscriptionException
	 *             if a subscription with URI already occurred
	 * @throws InvalidURIException
	 * @throws ValidationException
	 * @throws ImplementationException
	 *             if instantiation of {@link SubscriberController} failed
	 * @throws NoSuchNameException
	 *             if no port cycle under name exists.
	 */
	private void subscribe(String name, URI uri, PropertiesType properties, boolean persist)
			throws DuplicateSubscriptionException, InvalidURIException,
			ImplementationException, NoSuchNameException {
		lock.lock();
		try {
			PortCycle cycle = cycles.get(name);
			if (cycle != null) {
				if (cycle.exists(uri)) {
					throw new DuplicateSubscriptionException("URI '" + uri
							+ "' already subscribed to port cycle '" + name
							+ "'");
				} else {
					cycle.add(Subscriber.getInstance().get(uri, properties, PCReports.class));
					if (persist)
						depot.add(name, uri);
				}
			} else {
				throw new NoSuchNameException(
						"Could not subscribe to unknown port cycle '" + name
								+ "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Subscribes to the port cycle name with URI. Adds a new created
	 * {@link SubscriberController} to the port cycle.
	 *
	 * @param name
	 *            The name of the port cycle
	 * @param uri
	 *            The URI of the subscriber
	 * @param properties
	 *            The properties
	 * @param persist
	 *            Persist changes
	 * @throws InvalidURIException
	 * @throws DuplicateSubscriptionException
	 *             if a subscription with URI already occurred
	 * @throws ImplementationException
	 *             If instantiation of {@link SubscriberController} failed
	 * @throws NoSuchNameException
	 *             if no port cycle under name exists
	 */
	public void subscribe(String name, String uri, PropertiesType properties, boolean persist)
			throws InvalidURIException, DuplicateSubscriptionException,
			ImplementationException, NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.Subscribe, "Subscribe {0} {1}", new Object[] { name, uri });
		try {
			try {
				subscribe(name, new URI(uri), properties, persist);
			} catch (URISyntaxException e) {
				throw new InvalidURIException(e.getMessage());
			}
		} catch (InvalidURIException | DuplicateSubscriptionException
				| ImplementationException | NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.SubscribeFailed, "Subscribe failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Un-subscribes URI from port cycle name.
	 *
	 * @param name
	 *            The port cycle name
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
		PortCycle cycle;
		lock.lock();
		try {
			cycle = cycles.get(name);
			if (cycle != null) {
				subscriber = cycle.find(uri);
				if (subscriber == null) {
					throw new NoSuchSubscriberException("URI '"
							+ uri.toString()
							+ "' not subscribed on port cycle '" + name + "'");
				} else {
					if (persist)
						depot.remove(name, uri);
				}
			} else {
				throw new NoSuchNameException("Could not unsubscribe URI '"
						+ uri.toString() + "' from unknown port cycle '" + name
						+ "'");
			}
		} finally {
			lock.unlock();
		}
		cycle.remove(subscriber);
		subscriber.dispose();
	}

	/**
	 * Un-subscribes URI from port cycle name.
	 *
	 * @param name
	 *            The port cycle name
	 * @param uri
	 *            The subscriber URI
	 * @param persist
	 *            Persist changes
	 * @throws InvalidURIException
	 */
	public void unsubscribe(String name, String uri, boolean persist)
			throws InvalidURIException, NoSuchSubscriberException,
			NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.Unsubscribe, "Unsubscribe {0} {1}", new Object[] { name, uri });
		try {
			try {
				unsubscribe(name, new URI(uri), persist);
			} catch (URISyntaxException e) {
				throw new InvalidURIException(e.getMessage());
			}
		} catch (InvalidURIException | NoSuchSubscriberException
				| NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.UnsubscribeFailed, "Unsubscribe failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Polls to the port cycle name
	 *
	 * @param name
	 *            The port cycle name
	 * @return The reports from one port cycle run
	 * @throws NoSuchNameException
	 *             If port cycle name does not exists
	 */
	public PCReports poll(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.Poll, "Poll {0}", name);
		try {
			SubscriberListener<PCReports> subscriber = new SubscriberListener<PCReports>(
					1);
			lock.lock();
			try {
				PortCycle cycle = cycles.get(name);
				if (cycle != null) {
					cycle.add(subscriber);
				} else {
					throw new NoSuchNameException(
							"Could not poll on unknown port cycle '" + name
									+ "'");
				}
			} finally {
				lock.unlock();
			}
			return subscriber.dequeue();
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.PollFailed, "Poll failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Creates a anonymously port cycle with specification.
	 *
	 * @param spec
	 *            The port cycle specification
	 * @return The reports from port cycle run
	 * @throws ValidationException
	 * @throws ImplementationException
	 */
	public PCReports immediate(PCSpec spec) throws ValidationException, ImplementationException {
		// deliberately unsynchronized
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.Immediate, "Immediate {0}", spec);
		try {
			try {
				SubscriberListener<PCReports> subscriber = new SubscriberListener<PCReports>(1);
				PortCycle cycle = new PortCycle(null, spec);
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
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.ImmediateFailed, "Immediate failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets the list of subscribers from port cycle <paramref name="name"/>
	 *
	 * @param name
	 *            The name of the port cycle
	 * @return The list of subscribers
	 * @throws NoSuchNameException
	 */
	public List<String> getSubscribers(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.GetSubscribers, "Get subscribers {0}", name);
		try {
			lock.lock();
			try {
				PortCycle cycle = cycles.get(name);
				if (cycle != null) {
					return cycle.getSubscribers();
				} else {
					throw new NoSuchNameException(
							"Could not get subscribers for unknown port cycle '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.GetSubscribersFailed, "Get subscribers failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Executes a port operation with specifications.
	 *
	 * @param specs
	 *            The port operation specification list
	 * @return The operation report list
	 * @throws ValidationException
	 */
	public List<PCOpReport> execute(List<PCOpSpec> specs)
			throws ValidationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.PC.Name, Exits.Service.PC.Execute, "Execute {0}", new Object[] { specs });
		try {
			if ((specs == null) || (specs.size() == 0)) {
				throw new ValidationException("Could not execute, no operation");
			} else {
				List<PCOpReport> reports = new ArrayList<PCOpReport>();
				final List<Operation> operations = Operation.get(specs);
				try {
					int id = 0;
					for (Operation operation : operations) {
						operation.getPortOperation().setId(++id);
					}
					final Counter counter = new Counter();
					long count = counter.await(null);
					final Map<Integer, Result> results = new HashMap<Integer, Result>();
					PortCycle.execute(operations, new Caller<Port>() {
						@Override
						public void invoke(Port port, ReaderController controller) {
							if (port.isCompleted()) {
								for (Operation operation : operations) {
									results.put(Integer.valueOf(operation.getPortOperation().getId()), new Result(State.MISC_ERROR_TOTAL));
									counter.pulse();
								}
							} else {
								for (Entry<Integer, Result> pair : port.getResult().entrySet()) {
									results.put(pair.getKey(), pair.getValue());
									counter.pulse();
								}
							}
						}
					});
					while (count < operations.size()) {
						count = counter.await(Long.valueOf(count));
					}
					for (Operation operation : operations) {
						Result result = results.get(Integer.valueOf(operation
								.getPortOperation().getId()));
						if (result != null) {
							reports.add(operation.getReport(result));
						}
					}
				} finally {
					for (Operation operation : operations) {
						operation.dispose();
					}
				}
				return reports;
			}
		} catch (ValidationException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.PC.Name, Exits.Service.PC.ExecuteFailed, "Execute failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets the standard version
	 *
	 * @return The standard version
	 */
	public static String getStandardVersion() {
		return Config.getInstance().getService().getPc().getVersion()
				.getStandard();
	}
}