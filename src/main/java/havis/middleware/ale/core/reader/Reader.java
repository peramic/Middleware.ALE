package havis.middleware.ale.core.reader;

import havis.middleware.ale.Connector;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.ReaderConnectorType;
import havis.middleware.ale.config.ReaderConnectorsType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.reader.ReaderConnector;
import havis.util.monitor.Broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class that provides functionality to manage the available
 * {@link ReaderConnector} implementations.
 * 
 * This class is used to add, remove and update {@link ReaderConnector}
 * implementations to administrate a set of currently available connectors.
 * 
 * The class also provides operation to retrieve a {@link ReaderController}
 * instance for a specific reader connector instance.
 */
public class Reader {

	private static Reader instance = new Reader(Config.getInstance().getConnector().getReaders());

	/**
	 * Retrieves the singleton reader connector manager instance.
	 * 
	 * @return The singleton
	 */
	public static Reader getInstance() {
		return instance;
	}

	private Map<String, ReaderConnectorType> connectors;
	private Broker broker;

	/**
	 * Retrieves a set of currently registered and enabled reader connectors.
	 * 
	 * @return The connectors
	 */
	public Map<String, ReaderConnectorType> getConnectors() {
		return this.connectors;
	}

	/**
	 * Initializes a new instance
	 * 
	 * @param connectors
	 *            List of available connectors
	 */
	private Reader(ReaderConnectorsType connectors) {
		this.connectors = new HashMap<String, ReaderConnectorType>();
		if (connectors.getReader() != null) {
			for (ReaderConnectorType connector : connectors.getReader()) {
				if (connector.isEnable()) {
					// add connector to connector dictionary
					this.connectors.put(connector.getName(), connector);
				}
			}
		}
	}

	/**
	 * Operation to add a reader connector to the set of available connectors.
	 * 
	 * @param connector
	 *            The reader connector to add
	 * @throws DuplicateNameException
	 *             if a connector with the // same name already exists
	 */
	public void add(ReaderConnectorType connector) throws DuplicateNameException {
		if (connectors.containsKey(connector.getName())) {
			throw new DuplicateNameException("Reader connector '" + connector.getName() + "' already exists!");
		} else {
			this.connectors.put(connector.getName(), connector);
		}
	}

	/**
	 * Operation to remove a reader connector from the set of available
	 * connectors.
	 * 
	 * @param name
	 *            The name of the reader connector to remove
	 * @throws NoSuchNameException
	 *             if no connector with the // name exists.
	 */
	public void remove(String name) throws NoSuchNameException {
		if (connectors.remove(name) == null) {
			throw new NoSuchNameException("Reader connector '" + name + "'could not be found!");
		}
	}

	/**
	 * Operation to update a reader connector within the set of available
	 * connectors.
	 * 
	 * @param name
	 *            The name of the reader connector to update
	 * @param connector
	 *            The new connector information
	 * @throws DuplicateNameException
	 *             if connector defines a new name that already exists
	 * @throws NoSuchNameException
	 *             if no connector with the name exists.
	 */
	public void update(String name, ReaderConnectorType connector) throws DuplicateNameException, NoSuchNameException {
		if (connectors.containsKey(name)) {
			if (connector.getName().equals(name)) {
				this.connectors.put(name, connector);
			} else {
				if (connectors.containsKey(connector.getName())) {
					throw new DuplicateNameException("Reader connector '" + connector.getName() + "' already exist!");
				} else {
					connectors.remove(name);
					connectors.put(connector.getName(), connector);
				}
			}
		} else {
			throw new NoSuchNameException("Reader connector '" + name + "'could not be found!");
		}
	}

	/**
	 * Create {@link ReaderController} instance for a specific
	 * {@link ReaderConnector} implementation using the reader type the select
	 * the requested reader connector. This method takes the
	 * "Connector.ReaderType" property, finds the connector type and creates a
	 * instance.
	 * 
	 * @param name
	 *            The base reader name
	 * @param type
	 *            The name of the reader connector type to use.
	 * @param properties
	 *            A set of properties that should be forwarded to the reader
	 *            controller and connector.
	 * @return The requested {@link ReaderController} instance
	 * @throws ValidationException
	 *             if reader type could not be found
	 * @throws ImplementationException
	 *             if an implementation error occurs
	 */
	public ReaderController get(String name, String type, Map<String, String> properties) throws ImplementationException, ValidationException {
		ReaderConnector connector = Connector.getFactory().newInstance(ReaderConnector.class, type);
		if (connector != null) {
			try {
				return new ReaderController(name, connector, properties);
			} catch (ImplementationException | ValidationException e) {
				e.setReason("Failed to create a reader instance of type '" + type + "'! " + e.getReason());
				throw e;
			}
		} else {
			throw new ValidationException("Unknown reader type '" + type + "'!");
		}
	}

	/**
	 * Get all supported reader connector types
	 * 
	 * @return all supported reader connector types
	 * @throws ImplementationException
	 *             if an implementation error occurs
	 */
	public List<String> getTypes() throws ImplementationException {
		return Connector.getFactory().getTypes(ReaderConnector.class);
	}

	public Broker getBroker() {
		return broker;
	}

	public void setBroker(Broker broker) {
		this.broker = broker;
	}
}