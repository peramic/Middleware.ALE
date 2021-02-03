package havis.middleware.ale.core.reader;

import havis.middleware.ale.Connector;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.message.Message;
import havis.middleware.ale.base.message.MessageHandler;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.base.operation.tag.Filter;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.reader.Capability;
import havis.middleware.ale.reader.Prefix;
import havis.middleware.ale.reader.Property;
import havis.middleware.ale.reader.ReaderConnector;
import havis.middleware.ale.service.rc.RCConfig;
import havis.middleware.utils.threading.NamedThreadFactory;
import havis.util.monitor.ReconnectError;
import havis.util.monitor.VisibilityChanged;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that provides reliable access to a {@link ReaderConnector}
 * implementation. This class is used to manage the communication between
 * middleware and a {@link ReaderConnector} implementation through a
 * interprocess communication using WCF services.
 *
 * It initializes the {@link ReaderConnector} in a new process for interprocess
 * communication.
 *
 */
public class ReaderController implements MessageHandler {
	
	private static final int DEFAULT_EXECUTE_TIMEOUT = 3000;
	private static final int DEFAULT_RECONNECT_DELAY = 2000;
	private static final boolean DEFAULT_OPTIMIZE_WRITE_OPERATIONS = true;

	private long id;
	private String name;

	private ReaderConnector connector;

	private Map<String, String> properties;
	private boolean connected = false;
	private AtomicBoolean reconnecting = new AtomicBoolean(false);

	private boolean lostEpc;

	private CallbackHandler callbackHandler;

	private Map<Integer, TagOperator> operators;
	private Map<Integer, PortObservator> observators;

	private ExecutorService portExecutor, tagExecutor, reconnectExecutor;
	private Runnable reconnectCommand = new Runnable() {
		@Override
		public void run() {
			try {
				reconnect();
			} catch (ImplementationException e) {
				// ignore
			}
		}
	};

	private int executeTimeout = DEFAULT_EXECUTE_TIMEOUT;
	private int reconnectDelay = DEFAULT_RECONNECT_DELAY;
	private boolean optimizeWriteOperations = DEFAULT_OPTIMIZE_WRITE_OPERATIONS;
	private Lock lock = new ReentrantLock();

	/**
	 * Initializes a new instance of the ReaderController class, using a set
	 * of parameters that specify which {@link ReaderConnector} implementation
	 * this reader controller should communicate to.
	 *
	 * @param name
	 *            The name of the base reader.
	 * @param connector
	 *            The reader connector.
	 * @param properties
	 *            A set of properties to initialize the reader connector.
	 * @throws ALEException
	 *             if the constructor was unable to create the instance.
	 */
	public ReaderController(String name, ReaderConnector connector,
			Map<String, String> properties) throws ImplementationException,
			ValidationException {
		this.name = name;

		portExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory(this.getClass().getSimpleName() + " "
				+ (this.name != null ? this.name : "[no name]") + " port execute()"));
		tagExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory(this.getClass().getSimpleName() + " "
				+ (this.name != null ? this.name : "[no name]") + " tag execute()"));
		reconnectExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory(this.getClass().getSimpleName() + " "
				+ (this.name != null ? this.name : "[no name]") + " reconnect()"));

		this.connector = connector;
		this.properties = properties;

		callbackHandler = new QueuedCallbackHandler(name, this, this.connector);

		operators = new HashMap<>();
		observators = new HashMap<>();

		try {
			init();
		} catch (ALEException e) {
			dispose();
			throw e;
		} catch (Throwable e) {
			dispose();
			throw new ImplementationException(e.getMessage());
		}
	}

	private long next() {
		return ++id > 0 ? id : ++id;
	}

	/**
	 * Initializes the controller and connector.
	 *
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	private void init() throws ImplementationException, ValidationException {
		connector.setCallback(callbackHandler);
		getCapability();
		update(properties);
		Connector.getFactory().getBroker().notify(this.connector, new VisibilityChanged(new Date(), true));
	}

	/**
	 * Gets capabilities from the {@link ReaderConnector} implementation.
	 *
	 * @throws ImplementationException
	 */
	private void getCapability() throws ImplementationException {
		try {
			lostEpc = Boolean.parseBoolean(connector
					.getCapability(Capability.LostEPCOnWrite));
		} catch (Exception e) {
			error("Failed to parse capability 'LostEPCOnWrite'", e);
		}
	}

    /**
     * @return the name of the connector
     */
	public String getName() {
		return name;
	}

	/**
	 * @return true if the connector is connected, false otherwise
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Establishes the connection between connector and physical reader.
	 * 
	 * @throws ValidationException
	 * @throws ImplementationException
	 */
	private void connect() throws ValidationException, ImplementationException {
		if (!connected) {
			try {
				connector.connect();
				connected = true;
			} catch (Exception e) {
				Exits.Log.logp(Exits.Level.Error, Exits.Reader.Controller.Name, Exits.Reader.Controller.ConnectFailed, "Failed to connect to reader: " + e.getMessage(), e);
				throw e;
			}
		}
	}

	private void disconnect() {
		if (connected) {
			if ((operators.size() == 0) && (observators.size() == 0)) {
				try {
					connector.disconnect();
				} catch (Exception e) {
					error("An error occurred while disconnecting", e);
				}
				connected = false;
			}
		}
	}

	/**
	 * Restores the connection between connector and physical reader.
	 *
	 * @throws ImplementationException
	 */
	private void reconnect() throws ImplementationException {
		reconnecting.set(true);
		try {
			lock.lock();
			try {
				if (connected) {
					connector.disconnect();
					connected = false;
				}
			} finally {
				lock.unlock();
			}
			boolean first = true;
			while (!connected) {
				Thread.sleep(reconnectDelay);
				lock.lock();
				try {
					connector.connect();
					connected = true;
					Exits.Log.logp(Exits.Level.Information, Exits.Reader.Controller.Name, Exits.Reader.Controller.Reconnect,
							"Reader {0} reconnected successfully", name);
					if (!first) {
						Connector.getFactory().getBroker().notify(this.connector, new ReconnectError(new Date(), false, "Reconnected successfully to reader " + name));
					}
					break;
				} catch (Exception e) {
					Exits.Log.logp(Exits.Level.Detail, Exits.Reader.Controller.Name, Exits.Reader.Controller.ReconnectFailed, "Failed to reconnect to reader: "
							+ e.getMessage(), e);
					if (first) {
						Connector.getFactory().getBroker()
								.notify(this.connector, new ReconnectError(new Date(), true, "Failed to reconnect to reader " + name));
					}
				} finally {
					lock.unlock();
				}
				first = false;
			}
		} catch (InterruptedException e) {
			// ignore
		} finally {
			reconnecting.set(false);
		}
	}

	/**
	 * Try a reconnect
	 * 
	 * @return true if previous reconnect failed and another attempt is started,
	 *         false otherwise
	 */
	public boolean tryReconnect() {
		if (!connected && reconnecting.compareAndSet(false, true)) {
			connected = true;
			reconnectExecutor.execute(reconnectCommand);
			return true;
		}
		return false;
	}

	/**
	 * Sets controller properties based on a set of properties. Every property
	 * with prefix "controller" processed.
	 *
	 * @param properties
	 *            The set of properties to set
	 * @throws ValidationException
	 *             if property value has wrong format.
	 */
	private void set(Map<String, String> properties) throws ValidationException {
		String timeout = properties.get(Property.Controller.Timeout);
		if (timeout != null) {
			try {
				this.executeTimeout = Integer.parseInt(timeout);
				if (this.executeTimeout < 0) {
					throw new ValidationException("'Timeout' property value should be a positive number");
				}
			} catch (NumberFormatException e) {
				throw new ValidationException("Could not parse 'Timeout' property value. " + e.getMessage());
			}
		} else {
			this.executeTimeout = DEFAULT_EXECUTE_TIMEOUT;
		}
		String reconnectDelay = properties.get(Property.Controller.ReconnectDelay);
		if (reconnectDelay != null) {
			try {
				this.reconnectDelay = Integer.parseInt(reconnectDelay);
				if (this.reconnectDelay < 0) {
					throw new ValidationException("'ReconnectDelay' property value should be a positive number");
				}
			} catch (NumberFormatException e) {
				throw new ValidationException("Could not parse 'ReconnectDelay' property value. " + e.getMessage());
			}
		} else {
			this.reconnectDelay = DEFAULT_RECONNECT_DELAY;
		}
		String optimizeWriteOperations = properties.get(Property.Controller.OptimizeWriteOperations);
		if (optimizeWriteOperations != null) {
			if (optimizeWriteOperations.equalsIgnoreCase(Boolean.TRUE.toString())) {
				this.optimizeWriteOperations = true;
			} else if (optimizeWriteOperations.equalsIgnoreCase(Boolean.FALSE.toString())) {
				this.optimizeWriteOperations = false;
			} else {
				throw new ValidationException("Could not parse 'OptimizeWriteOperations' property value.");
			}
		} else {
			this.optimizeWriteOperations = DEFAULT_OPTIMIZE_WRITE_OPERATIONS;
		}
	}

	/**
	 * Validates a set of properties. Every property with prefix "Controller"
	 * will be processed.
	 *
	 * @param properties
	 *            The set of properties
	 * @throws ValidationException
	 *             if a property with prefix "Controller" is not recognized
	 */
	private void validate(Map<String, String> properties)
			throws ValidationException {
		for (Entry<String, String> property : properties.entrySet()) {
			switch (property.getKey()) {
			case Property.Controller.OptimizeWriteOperations:
			case Property.Controller.ReconnectDelay:
			case Property.Controller.Timeout:
				break;
			default:
				if (property.getKey().startsWith(Prefix.Controller)) {
					throw new ValidationException("Controller property '"
							+ property.getKey() + "' is not recognized!");
				}
				break;
			}
		}
	}

	/**
	 * Updates the reader controller and connector properties based on a set of
	 * properties.
	 *
	 * @param properties
	 *            The set of properties
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	public void update(final Map<String, String> properties)
			throws ImplementationException, ValidationException {
		validate(properties);
		try {
			lock.lock();
			connector.setProperties(properties);
			set(properties);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Method to define a new tag operation on the reader connector.
	 *
	 * @param operation
	 *            The tag operation to define
	 * @param caller
	 *            A delegate which will be called to report the operation result
	 * @param name
	 *            The logical reader name
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	public void define(final TagOperation operation, Caller<Tag> caller,
			final String name) throws ValidationException,
			ImplementationException {
		try {
			lock.lock();
			connect();
			TagOperator operator = operators.get(Integer.valueOf(operation.getId()));
			if (operator == null) {
				operator = new TagOperator(this.callbackHandler, next(), this, connector, operation);
				operators.put(Integer.valueOf(operation.getId()), operator);
			}
			operator.put(name, caller);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Method to undefine a tag operation on the reader connector.
	 *
	 * @param operation
	 *            The tag operation to undefine
	 * @param name
	 *            The logical reader name
	 * @throws ImplementationException
	 */
	public void undefine(TagOperation operation, String name)
			throws ImplementationException {
		try {
			lock.lock();
			TagOperator operator = operators.get(Integer.valueOf(operation.getId()));
			if (operator != null) {
				operator.remove(name); // remove caller
				if (!operator.hasCallers()) {
					// no callers left, remove operation
					operators.remove(Integer.valueOf(operation.getId()));
					operator.dispose();
					disconnect();
				}
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Method to enable a tag operation on the reader connector.After this
	 * method the reader connector will report result using the given callback
	 * method.
	 *
	 * @param operation
	 *            The tag operation to enable
	 * @throws ImplementationException
	 */
	public void enable(TagOperation operation) throws ImplementationException {
		try {
			lock.lock();
			TagOperator operator = operators.get(Integer.valueOf(operation.getId()));
			if (operator != null) {
				operator.enable();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Disables a tag operation on the reader.After this method the reader
	 * connector will no longer report results.
	 *
	 * @param operation
	 *            The tag operation to disable
	 * @throws ImplementationException
	 */
	public void disable(TagOperation operation) throws ImplementationException {
		try {
			lock.lock();
			TagOperator operator = operators.get(Integer.valueOf(operation.getId()));
			if (operator != null) {
				operator.disable();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Method to define a tag operation and execute it once. This method will
	 * enqueue the tag operation which will be later processed by the run method
	 * of this class.
	 *
	 * @param name
	 *            The logical reader name
	 * @param operation
	 *            The reader operation to execute
	 * @param caller
	 *            A delegate which will be called to report the operation result
	 */
	public void execute(final String name, final TagOperation operation, final Caller<Tag> caller) {
		try {
			lock.lock();
			tagExecutor.execute(new Runnable() {
				@Override
				public void run() {
					AtomicBoolean errorState = new AtomicBoolean(false);
					if (lostEpc) {
						// split operations for connectors which loose the tag when writing the EPC
						List<Operation> current = new ArrayList<>();
						List<TagOperation> operations = new ArrayList<>();
						Tag tag = new Tag(operation.getFilter());
						List<Filter> filter = tag.getFilter();
						for (Operation op : operation.getOperations()) {
							current.add(op);
							if (tag.apply(op)) {
								operations.add(new TagOperation(current, filter));
								filter = tag.getFilter();
								current = new ArrayList<>();
							}
						}
						if (current.size() > 0) {
							operations.add(new TagOperation(current, filter));
						}

						for (TagOperation tagOp : operations) {
							TagExecutor executor = new TagExecutor(callbackHandler, next(), ReaderController.this, connector, name, caller, tagOp,
									executeTimeout, optimizeWriteOperations, errorState);
							executor.execute();
						}
					} else {
						TagExecutor executor = new TagExecutor(callbackHandler, next(), ReaderController.this, connector, name, caller, operation,
								executeTimeout, optimizeWriteOperations, errorState);
						executor.execute();
					}
				}
			});
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Defines a port observation on a reader connector.
	 *
	 * @param observation
	 *            The port observation
	 * @param caller
	 *            A delegate which will be called to report the observation
	 *            result
	 * @param name
	 *            The logical reader name
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	public void define(PortObservation observation, Caller<Port> caller,
			String name) throws ImplementationException, ValidationException {
		try {
			lock.lock();
			connect();
			PortObservator observator = observators.get(Integer.valueOf(observation.getId()));
			if (observator == null) {
				observator = new PortObservator(this.callbackHandler, next(), this, connector, observation);
				observators.put(Integer.valueOf(observation.getId()), observator);
			}
			observator.put(name, caller);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Undefines a port observation on the reader connector.
	 *
	 * @param observation
	 *            The port observation
	 * @param name
	 *            The logical reader name
	 * @throws ImplementationException
	 */
	public void undefine(PortObservation observation, String name)
			throws ImplementationException {
		try {
			lock.lock();
			PortObservator observator = observators.get(Integer.valueOf(observation.getId()));
			if (observator != null) {
				observator.remove(name); // remove caller
				if (!observator.hasCallers()) {
					// no callers left, remove operation
					observators.remove(Integer.valueOf(observation.getId()));
					observator.dispose();
					disconnect();
				}
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Enables a port observation on the reader connector. After this method the
	 * reader connector will report result using the given callback method.
	 *
	 * @param observation
	 *            The port observation
	 * @throws ImplementationException
	 */
	public void enable(PortObservation observation)
			throws ImplementationException {
		try {
			lock.lock();
			PortObservator observator = observators.get(Integer.valueOf(observation.getId()));
			if (observator != null) {
				observator.enable();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Disables a port observation on the reader connector. After this method
	 * the reader connector will no longer report results.
	 *
	 * @param observation
	 *            The port observation
	 * @throws ImplementationException
	 */
	public void disable(PortObservation observation)
			throws ImplementationException {
		try {
			lock.lock();
			PortObservator observator = observators.get(Integer.valueOf(observation.getId()));
			if (observator != null) {
				observator.disable();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Defines a port operation and execute it once.
	 *
	 * @param operation
	 *            The port operation to execute
	 * @param caller
	 *            A delegate which will be called to report the operation result
	 * @throws ValidationException
	 * @throws ImplementationException
	 */
	public void execute(final PortOperation operation, final Caller<Port> caller) throws ValidationException, ImplementationException {
		try {
			lock.lock();
			try {
				connect();
			} catch (ImplementationException | ValidationException e) {
				Port port = new Port();
				port.setCompleted(true);
				caller.invoke(port, this);
				throw e;
			}
			portExecutor.execute(new Runnable() {
				@Override
				public void run() {
					PortExecutor executor = new PortExecutor(ReaderController.this.callbackHandler, next(), ReaderController.this, connector, caller, operation);
					executor.execute();
					disconnect();
				}
			});
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Disposes the instance and release all its resources.
	 *
	 * @throws ImplementationException
	 */
	public void dispose() throws ImplementationException {
		connected = false;
        lock.lock();
        try {
            for (TagOperator operator : operators.values()) {
                try {
                	operator.dispose();
                } catch (Exception e) {
                    error("An error occurred while disposing tag operator", e);
                }
            }
            operators.clear();
            for (PortObservator observator : observators.values()) {
                try {
                	observator.dispose();
                } catch (Exception e) {
                    error("An error occurred while disposing port observator", e);
                }
            }
            observators.clear();
        } finally {
            lock.unlock();
        }
        tagExecutor.shutdownNow();
        portExecutor.shutdownNow();
        reconnectExecutor.shutdownNow();
        Connector.getFactory().getBroker().notify(this.connector, new VisibilityChanged(new Date(), false));
        lock.lock();
        try {
            if (connector != null) {
                try {
                    connector.dispose();
                } catch (Exception e) {
                    error("An error occurred while disposing reader connector", e);
                }
                connector = null;
            }
            if (callbackHandler != null) {
                callbackHandler.dispose();
                callbackHandler = null;
            }
        } finally {
            lock.unlock();
        }
	}

	/**
	 * Gets the reader configuration from the reader connector.
	 *
	 * @return The requested reader configuration
	 * @throws ImplementationException
	 */
	public RCConfig getConfig() throws ImplementationException {
		try {
			lock.lock();
			connect();
			RCConfig config = connector.getConfig();
			if ((operators.size() == 0) && (observators.size() == 0)) {
				try {
					connector.disconnect();
				} catch (Exception e) {
					error("An error occurred while disconnecting", e);
				}
				connected = false;
			}
			return config;
		} catch (Exception e) {
			throw exception("An error occurred while getting configuration", e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Creates a log entry and an error event with message and exception.
	 *
	 * @param message
	 *            The message text
	 * @param e
	 *            The exception
	 */
	void error(String message, Exception e) {
		Exits.Log.logp(Exits.Level.Error, Exits.Reader.Controller.Name, Exits.Reader.Controller.Error, message + ": " + e.getMessage(), e);
	}

	/**
	 * Creates a log entry, an error event with message and exception and .
	 *
	 * @param message
	 *            The message text
	 * @param e
	 *            The exception
	 */
	private ImplementationException exception(String message, Exception e) {
		if (e instanceof ImplementationException) {
			return (ImplementationException) e;
		} else {
			error(message, e);
			return new ImplementationException(String.format("%s:%s\n%s\n%s",
					e.getClass(), message, e.getMessage(),
					Arrays.asList(e.getStackTrace())));
		}
	}

	/**
	 * Handles message notifications send by the {@link ReaderConnector}
	 * implementation.
	 * 
	 * @param message
	 *            the message
	 */
	@Override
	public void notify(Message message) {
		switch (message.getType()) {
		case Exits.Reader.Controller.Error:
			Exits.Log.logp(Exits.Level.Error, Exits.Reader.Controller.Name, Exits.Reader.Controller.Error, "Reader {0}: {1}",
					new Object[] { name, message.getText(), message.getException() });
			break;
		case Exits.Reader.Controller.Warning:
			Exits.Log.logp(Exits.Level.Warning, Exits.Reader.Controller.Name, Exits.Reader.Controller.Warning, "Reader {0}: {1}",
					new Object[] { name, message.getText(), message.getException() });
			break;
		case Exits.Reader.Controller.Information:
			Exits.Log.logp(Exits.Level.Information, Exits.Reader.Controller.Name, Exits.Reader.Controller.Information, "Reader {0}: {1}", new Object[] { name,
					message.getText() });
			break;
		case Exits.Reader.Controller.ConnectionLost:
			Exits.Log.logp(Exits.Level.Information, Exits.Reader.Controller.Name, Exits.Reader.Controller.ConnectionLost, "Reader {0}: {1}", new Object[] {
					name, message.getText() });
			if (reconnecting.compareAndSet(false, true)) {
				reconnectExecutor.execute(reconnectCommand);
			}
			break;
		}
	}
}
