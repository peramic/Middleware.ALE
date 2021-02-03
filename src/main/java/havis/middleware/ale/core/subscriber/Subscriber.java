package havis.middleware.ale.core.subscriber;

import havis.middleware.ale.Connector;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.config.SubscriberConnectorType;
import havis.middleware.ale.config.SubscriberConnectorsType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.service.IReports;
import havis.middleware.ale.subscriber.SubscriberConnector;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class that provides functionality to manage the available
 * {@link SubscriberConnectorType} implementations. This class is used to add,
 * remove and update {@link SubscriberConnectorType} implementations to
 * administered a set of currently available connectors.
 *
 * The class also provides operation to retrieve a {@link SubscriberController}
 * instance for a specific reader connector instance.
 */
public class Subscriber {

	private static Subscriber instance = new Subscriber(Config.getInstance()
			.getConnector().getSubscribers());

	/**
	 * Gets the singleton subscriber connector manager instance.
	 *
	 * @return The static instance
	 */
	public static Subscriber getInstance() {
		return instance;
	}

	private Map<String, SubscriberConnectorType> connectors;

	/**
	 * Gets a set of currently registered and enabled reader connectors.
	 *
	 * @return The available connectors
	 */
	public Map<String, SubscriberConnectorType> getConnectorTypes() {
		return this.connectors;
	}

	/**
	 * Initializes a new instance.
	 *
	 * @param connectors
	 *            List of available connectors.
	 */
	public Subscriber(SubscriberConnectorsType connectors) {
		this.connectors = new HashMap<String, SubscriberConnectorType>();
		if (connectors.getSubscriber() != null) {
			for (SubscriberConnectorType connector : connectors.getSubscriber()) {
				// validate enable state
				if (connector.isEnable()) {
					// add connector to connector dictionary
					this.connectors.put(connector.getName().toLowerCase(),
							connector);
				}
			}
		}
	}

	/**
	 * Operation to add a subscriber connector to the set of available
	 * connectors.
	 *
	 * @param connector
	 *            The subscriber connector to add
	 * @throws DuplicateNameException
	 *             If a connector with the same name already exists.
	 */
	public void add(SubscriberConnectorType connector)
			throws DuplicateNameException {
		if (!this.connectors.containsKey(connector.getName())) {
			this.connectors.put(connector.getName(), connector);
		} else {
			throw new DuplicateNameException("Subscriber connector '"
					+ connector.getName() + "' already exists!");
		}
	}

	/**
	 * Operation to remove a subscriber connector from the set of available
	 * connectors.
	 *
	 * @param name
	 *            The name of the subscriber connector to remove
	 * @throws NoSuchNameException
	 *             if no connector with given name exists
	 */
	public void remove(String name) throws NoSuchNameException {
		if (connectors.remove(name) == null) {
			throw new NoSuchNameException("Subscriber connector '" + name
					+ "'could not be found!");
		}
	}

	/**
	 * Operation to update a subscriber connector within the set of available
	 * connectors.
	 *
	 * @param name
	 *            The name of the subscriber connector to update
	 * @param connector
	 *            The new connector information
	 * @throws DuplicateNameException
	 *             if connector defines a new name that already exists
	 * @throws NoSuchNameException
	 *             if no connector with given name exists
	 */
	public void update(String name, SubscriberConnectorType connector)
			throws DuplicateNameException, NoSuchNameException {
		if (connectors.containsKey(name)) {
			if (connector.getName().equals(name)) {
				this.connectors.put(name, connector);
			} else {
				if (connectors.containsKey(connector.getName())) {
					throw new DuplicateNameException("Subscriber connector '"
							+ connector.getName() + "' already exist!");
				} else {
					connectors.remove(name);
					connectors.put(connector.getName(), connector);
				}
			}
		} else {
			throw new NoSuchNameException("Subscriber connector '" + name
					+ "' could not be found!");
		}
	}

	/**
	 * Validate the subscriber URI
	 * 
	 * @param uri
	 *            the URI to validate
	 * @return the URI
	 * @throws InvalidURIException
	 */
	public URI validateUri(URI uri) throws InvalidURIException {
		if (uri.getScheme() == null) {
			throw new InvalidURIException("Invalid URI, no scheme specified '" + uri.toString() + "'");
		}
		return uri;
	}

	/**
	 * Create a instance for a specific {@link SubscriberConnectorType}
	 * implementation using the subscriber connector that provides the scheme
	 * from URI. This method takes the URI scheme, finds the connector type and
	 * creates a instance.
	 * 
	 * @param uri
	 *            The subscriber URI to select the requested type
	 * @param properties
	 *            The properties
	 * @param reportClass
	 *            The report type
	 * @return The requested controller instance
	 * @throws InvalidURIException
	 *             If no subscriber connector for given scheme exists
	 * @throws ValidationException
	 *             If subscriber type could not be found
	 * @throws ImplementationException
	 *             If the operation was unable to create a instance.
	 */
	public SubscriberController get(URI uri, PropertiesType properties, Class<? extends IReports> reportClass) throws ImplementationException,
			InvalidURIException {
		String type = validateUri(uri).getScheme().toLowerCase();
		SubscriberConnector connector = Connector.getFactory().newInstance(SubscriberConnector.class, type);
		// look for connector by URI scheme
		if (connector != null) {
			try {
				return new DefaultSubscriberController(uri, properties, connector, reportClass);
			} catch (ImplementationException e) {
				e.setReason("Failed to create instance of '" + type + "'. " + e.getReason());
				throw e;
			}
		}
		throw new InvalidURIException("No subscriber connector found for uri scheme '" + type + "'");
	}

	/**
	 * Get all supported subscriber connector types
	 * 
	 * @return all supported subscriber connector types
	 * @throws ImplementationException
	 *             if an implementation error occurs
	 */
	public List<String> getTypes() throws ImplementationException {
		return Connector.getFactory().getTypes(SubscriberConnector.class);
	}
}