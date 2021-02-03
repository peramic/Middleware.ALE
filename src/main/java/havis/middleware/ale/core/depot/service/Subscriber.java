package havis.middleware.ale.core.depot.service;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ParameterForbiddenException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.config.PropertyType;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.core.depot.Depot;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.mc.MCProperty;
import havis.middleware.ale.service.mc.MCSubscriberSpec;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Implements the cycle subscriber depot
 */
abstract public class Subscriber extends
		Depot<SubscriberType, MCSubscriberSpec> {
	List<SubscriberType> subscribers;

	protected String name;

	/**
	 * Retrieves the subscriber entries
	 *
	 * @return The subscriber list
	 */
	@Override
    protected List<SubscriberType> getList() {
		return subscribers;
	}

	/**
	 * Returns the configuration entry by specification
	 *
	 * @param spec
	 *            The specification
	 */
	@Override
    protected SubscriberType get(final MCSubscriberSpec spec) {
		return new SubscriberType() {
			{
				setEnable(Boolean.valueOf(spec.isEnable()));
				setUri(spec.getUri());
				if (spec.getProperties() != null) {
					setProperties(new PropertiesType());
					for (MCProperty property : spec.getProperties().getProperty()) {
						getProperties().getProperty().add(new PropertyType(property.getName(), property.getValue()));
					}
				}
				setName(getUri());
			}
		};
	}

	/**
	 * Returns the specification by entry
	 *
	 * @param entry
	 *            The entry
	 */
	@Override
    protected MCSubscriberSpec get(final SubscriberType entry) {
		return new MCSubscriberSpec() {
			{
				setEnable(Boolean.valueOf(entry.isEnable()));
				setUri(entry.getUri());
				if (entry.getProperties() != null) {
					setProperties(new Properties());
					for (PropertyType property : entry.getProperties().getProperty()) {
						getProperties().getProperty().add(new MCProperty(property.getName(), property.getValue()));
					}
				}
				setName(entry.getUri());
			}
		};
	}

	/**
	 * Creates a new instance
	 *
	 * @param name
	 *            The cycle name
	 * @param subscribers
	 *            The subscriber list
	 */
	protected Subscriber(String name, List<SubscriberType> subscribers) {
		super(subscribers);
		this.name = name;
		this.subscribers = subscribers;
	}

	/**
	 * The cycle name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Creates a new URI instance by String
	 *
	 * @param uri
	 *            The String URI
	 * @return The {@link URI} instance
	 * @throws InvalidURIException
	 *             if String parsing fails
	 */
	static URI getUri(String uri) throws InvalidURIException {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new InvalidURIException("Invalid subscriber uri '" + uri
					+ "'. " + e.getMessage());
		}
	}

	/**
	 * Subscribes an entry
	 *
	 * @param entry
	 *            The entry
	 * @throws NoSuchNameException
	 * @throws ValidationException
	 * @throws DuplicateNameException
	 * @throws ImplementationException
	 * @throws InvalidURIException
	 * @throws DuplicateSubscriptionException
	 * @throws ParameterForbiddenException
	 */
	public abstract void subscribe(SubscriberType entry)
			throws InvalidURIException, ImplementationException,
			DuplicateNameException, ValidationException, NoSuchNameException,
			ParameterForbiddenException, DuplicateSubscriptionException;

	/**
	 * Un-subscribes an entry
	 *
	 * @param entry
	 *            The entry
	 * @throws NoSuchNameException
	 * @throws NoSuchSubscriberException
	 * @throws InvalidURIException
	 */
	public abstract void unsubscribe(SubscriberType entry)
			throws InvalidURIException, NoSuchSubscriberException,
			NoSuchNameException;

	/**
	 * Sets the enable state
	 *
	 * @param entry
	 *            The entry
	 * @param enable
	 *            The enable state
	 * @throws NoSuchNameException
	 * @throws ValidationException
	 * @throws DuplicateNameException
	 * @throws ImplementationException
	 * @throws InvalidURIException
	 * @throws DuplicateSubscriptionException
	 * @throws ParameterForbiddenException
	 */
	@Override
    protected void setEnable(SubscriberType entry, boolean enable)
			throws InvalidURIException, ImplementationException,
			DuplicateNameException, ValidationException, NoSuchNameException,
			ParameterForbiddenException, DuplicateSubscriptionException {
		if (enable) {
			subscribe(entry);
		} else {
			try {
				unsubscribe(entry);
			} catch (NoSuchSubscriberException e) {
				Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Subscriber not found: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Adds an entry by URI
	 *
	 * @param uri
	 *            The URI
	 */
	void add(URI uri) {
	    final URI subscriberUri = uri;
		super.add(new SubscriberType() {
			{
			    setUri(subscriberUri.toString());
			    setName(getUri());
				setEnable(Boolean.TRUE);
			}
		});
	}

	/**
	 * Clears the names list
	 */
	void clear() {
		names.clear();
	}
}