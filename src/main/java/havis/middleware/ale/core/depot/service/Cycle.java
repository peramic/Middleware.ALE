package havis.middleware.ale.core.depot.service;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.CycleType;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.config.SubscribersType;
import havis.middleware.ale.core.depot.Depot;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.Document;
import havis.middleware.ale.service.mc.MCSpec;
import havis.middleware.ale.service.mc.MCSubscriberSpec;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Implements the cycle depot
 *
 * @param <T>
 *            The cycle type
 * @param <S>
 *            The specification type
 * @param <D>
 *            The document type
 */
public abstract class Cycle<T extends CycleType, S extends MCSpec, D extends Document>
		extends Depot<T, S> {
	/**
	 * Retrieves the subscribers
	 */
	protected Map<UUID, Subscriber> subscribers;

	/**
	 * Creates a new instance and initializes the subscribers
	 */
	protected Cycle() {
		super();
		subscribers = new LinkedHashMap<UUID, Subscriber>();
	}

	/**
	 * Defines the entry
	 *
	 * @param entry
	 *            The entry
	 * @throws DuplicateNameException
	 * @throws ValidationException
	 * @throws ImplementationException
	 */
	abstract protected void define(T entry) throws ImplementationException,
			ValidationException, DuplicateNameException;

	/**
	 * Un-defines the entry
	 *
	 * @param entry
	 *            The entry
	 * @throws NoSuchNameException
	 */
	abstract protected void undefine(T entry) throws NoSuchNameException;

	/**
	 * Sets the enable state of the entry
	 *
	 * @param entry
	 *            The entry
	 * @param enable
	 *            The enable state
	 * @throws DuplicateNameException
	 * @throws ValidationException
	 * @throws ImplementationException
	 */
	@Override
	protected void setEnable(T entry, boolean enable)
			throws ImplementationException, ValidationException,
			DuplicateNameException {
		if (enable) {
			define(entry);
		} else {
			try {
				undefine(entry);
			} catch (NoSuchNameException e) {
				Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Cycle not found: " + e.getMessage(), e);
			}
			for (SubscriberType subscriber : entry.getSubscribers()
					.getSubscriber()) {
				subscriber.setEnable(Boolean.FALSE);
			}
		}
	}

	/**
	 * Adds a new entry with empty subscriber list and returns the new created
	 * id.
	 *
	 * @param entry
	 *            The entry
	 * @return The id
	 */
	@Override
	protected UUID add(T entry) {
		entry.setSubscribers(new SubscribersType());
		return super.add(entry);
	}

	/**
	 * Updates an entry. Adopt subscriber
	 *
	 * @param id
	 *            The id
	 * @param o
	 *            The old entry
	 * @param n
	 *            The new entry
	 */
	@Override
	protected void update(UUID id, T o, T n) {
		n.setSubscribers(o.getSubscribers());
		super.update(id, o, n);
	}

	/**
	 * Updates an existing entry by id
	 *
	 * @param id
	 *            The id
	 * @param spec
	 *            The specification
	 * @throws ALEException
	 */
	@Override
	public void update(UUID id, MCSpec spec) throws ALEException {
		super.update(id, spec);
		Subscriber subscriber = getSubscriber(id);
		if (subscriber != null) {
			if (spec.getName() instanceof String) {
				subscriber.setName(spec.getName());
			} else {
				subscriber.clear();
			}
		}
	}

	/**
	 * Removes a entry by name
	 *
	 * @param name
	 *            The name
	 */
	@Override
	public UUID remove(String name) {
		UUID id = super.remove(name);
		subscribers.remove(id);
		return id;
	}

	/**
	 * Removes the entry by id
	 *
	 * @param id
	 *            The id
	 * @throws ALEException
	 */
	@Override
	public void remove(UUID id) throws ALEException {
		super.remove(id);
		subscribers.remove(id);
	}

	/**
	 * Try to get the subscriber by parent id
	 *
	 * @param parent
	 *            The parent id
	 * @return The subscriber, if subscriber exists
	 * @throws NoSuchIdException
	 *             If parent id doesn't exists
	 */
	final Subscriber getSubscriber(UUID parent) throws NoSuchIdException {
		Subscriber subscriber = subscribers.get(parent);
		if (subscriber != null) {
			return subscriber;
		} else {
			throw new NoSuchIdException("Parent id '" + parent
					+ "' does not exists");
		}
	}

	/**
	 * Returns the subscriber id list by parent id
	 *
	 * @param parent
	 *            The parent id
	 * @return The subscriber id list
	 * @throws NoSuchIdException
	 *             If parent id doesn't exists
	 */
	public List<String> toList(UUID parent) throws NoSuchIdException {
	    return getSubscriber(parent).toList();
	}

	/**
	 * Adds a new entry by specification with parent id and returns the new
	 * created id as String
	 *
	 * @param spec
	 *            The specification
	 * @param parent
	 *            The parent id
	 * @return The id as String
	 * @throws ALEException
	 */
	public String add(MCSpec spec, UUID parent) throws ALEException {
		if (spec instanceof MCSubscriberSpec) {
			return add((MCSubscriberSpec) spec, parent);
		} else {
			throw new ValidationException("Invalid specification type '"
					+ spec.getClass().getName() + "' for parent '" + parent
					+ "'");
		}
	}

	/**
	 * Adds a new by subscriber specification with parent id and returns the new
	 * created id as String
	 *
	 * @param spec
	 *            The subscriber specification
	 * @param parent
	 *            The parent id
	 * @return The id as String
	 * @throws ALEException
	 */
	String add(MCSubscriberSpec spec, UUID parent) throws ALEException,
			DuplicateNameException {
		return getSubscriber(parent).add(spec).toString();
	}

	/**
	 * Adds a new subscriber entry by cycle name and URI
	 *
	 * @param name
	 *            The cycle name
	 * @param uri
	 *            The subscriber URI
	 */
	public void add(String name, URI uri) {
		try {
			UUID id = getId(name);
			if (id != null) {
				Subscriber subscriber = getSubscriber(id);
				if (subscriber != null) {
					subscriber.add(uri);
				}
			}
		} catch (NoSuchIdException e) {
			Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Failed to add subscriber to unknown cycle: " + e.getMessage(), e);
		}
	}

	/**
	 * Removes a subscriber entry by cycle name and URI
	 *
	 * @param name
	 *            The cycle name
	 * @param uri
	 *            The subscriber URI
	 * @throws NoSuchIdException
	 *             If parent id doesn't exists
	 */
	public void remove(String name, URI uri) {
		try {
			UUID id = getId(name);
			if (id != null) {
				Subscriber subscriber = getSubscriber(id);
				if (subscriber != null) {
					subscriber.remove(uri.toString());
				}
			}
		} catch (NoSuchIdException e) {
			Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Failed to remove unknown subscriber: " + e.getMessage(), e);
		}
	}

	/**
	 * Removes the subscriber entry by id with parent id
	 *
	 * @param id
	 *            The subscriber id
	 * @param parent
	 *            The parent id
	 * @throws ALEException
	 */
	public void remove(UUID id, UUID parent) throws ALEException,
			DuplicateNameException {
		Subscriber subscriber = getSubscriber(parent);
		if (subscriber != null) {
			subscriber.remove(id);
		}
	}

	/**
	 * Updates the subscriber entry by id with specification and parent cycle id
	 *
	 * @param id
	 *            The id
	 * @param spec
	 *            The specification
	 * @param parent
	 *            The parent cycle id
	 * @throws ALEException
	 */
	public void update(UUID id, MCSpec spec, UUID parent) throws ALEException {
		Subscriber subscriber = getSubscriber(parent);
		if (subscriber != null) {
			subscriber.update(id, spec);
		}
	}

	/**
	 * Returns the subscriber specification by id with parent id
	 *
	 * @param id
	 *            The subscriber id
	 * @param parent
	 *            The parent cycle id
	 * @return The subscriber specification
	 * @throws NoSuchIdException
	 *             If id or parent id doesn't exists
	 */
	public MCSubscriberSpec get(UUID id, UUID parent) throws NoSuchIdException {
		return getSubscriber(parent).get(id);
	}

	/**
	 * Initializes the entries
	 *
	 * @throws DuplicateNameException
	 * @throws ValidationException
	 */
	@Override
	public void init() {
		for (Entry<UUID, T> pair : dict.entrySet()) {
			if (pair.getValue().isEnable()) {
				try {
					setEnable(pair.getValue(), true);
					names.put(pair.getValue().getName(), pair.getKey());

					Subscriber subscriber = getSubscriber(pair.getKey());
					if (subscriber != null) {
						subscriber.init();
					}
				} catch (ALEException e) {
					pair.getValue().setEnable(Boolean.FALSE);

					for (SubscriberType subscriber : pair.getValue()
							.getSubscribers().getSubscriber()) {
						subscriber.setEnable(Boolean.FALSE);
					}
					Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Failed to enable cycle: " + e.getMessage(), e);
				}
			}
		}
	}
}