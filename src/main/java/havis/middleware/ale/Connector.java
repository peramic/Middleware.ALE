package havis.middleware.ale;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.util.monitor.Broker;

import java.util.List;

/**
 * Abstract connector factory
 */
public abstract class Connector {

	private static Connector instance;

	/**
	 * @return the current factory
	 */
	public static Connector getFactory() {
		if (instance == null)
			throw new IllegalStateException("Connector factory has not been initialized");
		return instance;
	}

	/**
	 * @param connector
	 *            the factory to set
	 */
	public static void createFactory(Connector connector) {
		if (connector == null)
			throw new NullPointerException("connector must not be null");
		instance = connector;
	}

	/**
	 * Clear the current factory, connector instantiation will not be possible
	 */
	public static void clearFactory() {
		instance = null;
	}

	/**
	 * Creates a new connector instance
	 * 
	 * @param clazz
	 *            the connector interface
	 * @param type
	 *            the type of the connector
	 * @return the connector instance
	 * @throws ImplementationException
	 *             if creation failed
	 */
	public abstract <S> S newInstance(Class<S> clazz, String type) throws ImplementationException;

	/**
	 * Get all types for the specified connector interface
	 * 
	 * @param clazz
	 *            the connector interface
	 * @return all types for the specified connector interface
	 * @throws ImplementationException
	 *             if retrieval failed
	 */
	public abstract <S> List<String> getTypes(Class<S> clazz) throws ImplementationException;

	/**
	 * @return the broker for monitoring
	 */
	public abstract Broker getBroker();
}