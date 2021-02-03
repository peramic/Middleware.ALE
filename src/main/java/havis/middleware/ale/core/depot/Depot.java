package havis.middleware.ale.core.depot;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.DepotType;
import havis.middleware.ale.config.Property;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.mc.MCProperty;
import havis.middleware.ale.service.mc.MCSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements the depot
 *
 * @param <E>
 *            The configuration type
 * @param <S>
 *            the specification type
 */
public abstract class Depot<E extends DepotType, S extends MCSpec> {

	private Lock updateLock = new ReentrantLock();

	/**
	 * Retrieves the entries by id
	 */
	protected Map<UUID, E> dict;

	/**
	 * Retrieves the id's by name
	 */
	protected Map<String, UUID> names;

	/**
	 * Retrieves the entry list
	 */
	private List<E> list;

	/**
	 * Retrieves the entry list
	 */
	protected abstract List<E> getList();

	/**
	 * Creates a new instance and initializes the entries in list
	 *
	 * @param list
	 *            The list
	 */
	protected Depot(List<E> list) {
		this.list = list == null ? getList() : list;
		dict = new LinkedHashMap<UUID, E>();
		for (E t : this.list) {
			dict.put(UUID.randomUUID(), t);
		}
		names = new LinkedHashMap<String, UUID>();
	}

	/**
	 * Creates a new instance with empty list
	 */
	protected Depot() {
		this(null);
	}

	/**
	 * Retrieves entry by specification
	 *
	 * @param spec
	 *            The specification
	 * @return The entry
	 */
	protected abstract E get(S spec);

	/**
	 * Retrieves specification by entry
	 *
	 * @param entry
	 *            The entry
	 * @return The specification
	 */
	protected abstract S get(E entry);

	/**
	 * Sets the enable state of a entry
	 *
	 * @param entry
	 *            The entry
	 * @param enable
	 *            The enable state
	 * @throws ALEException
	 */
	protected abstract void setEnable(E entry, boolean enable)
			throws ALEException;

	/**
	 * Tries to get the entry by id
	 *
	 * @param id
	 *            The unique id
	 * @return The entry
	 * @throws NoSuchIdException
	 *             if id does not exists
	 */
	protected final E getType(UUID id) throws NoSuchIdException {
		E entry = dict.get(id);
		if (entry != null) {
			return entry;
		} else {
			throw new NoSuchIdException("Unknown specification id '"
					+ id.toString() + "'");
		}
	}

	/**
	 * Tries to get the entry by name
	 *
	 * @param name
	 *            The name
	 * @return The output id
	 */
	protected UUID getId(String name) {
		return names.get(name);
	}

	/**
	 * Returns the string representation of the id's as list
	 *
	 * @param ids
	 *            The id's
	 * @return The id list
	 */
	List<String> toList(Iterable<UUID> ids) {
		List<String> list = new ArrayList<String>();
		for (UUID guid : ids) {
			list.add(guid.toString());
		}
		return list;
	}

	/**
	 * Returns the id's a list of strings
	 *
	 * @return The id list
	 */
	public List<String> toList() {
		return toList(dict.keySet());
	}

	/**
	 * Adds a new entry and returns the new created id. Serializes the
	 * configuration
	 *
	 * @param entry
	 *            The entry
	 * @return The id
	 */
	protected UUID add(E entry) {
		return add(entry, true);
	}

	/**
	 * Adds a new entry and returns the new created id. Serializes the
	 * configuration only when specified.
	 * 
	 * @param entry
	 *            The entry
	 * @param persist
	 *            whether to serialize the configuration
	 * @return The id
	 */
	protected UUID add(E entry, boolean persist) {
		list.add(entry);
		UUID guid = UUID.randomUUID();
		dict.put(guid, entry);
		if (entry.isEnable()) {
			names.put(entry.getName(), guid);
		}
		if (persist) {
			Config.serialize();
		}
		return guid;
	}

	/**
	 * Adds a new entry by specification and returns the new created id as
	 * string
	 * 
	 * @param spec
	 *            The specification
	 * @return The id
	 * @throws ALEException
	 *             if the specification has the wrong type
	 */
	@SuppressWarnings("unchecked")
	public String add(MCSpec spec) throws ALEException {
		try {
			E t = get((S) spec);
			if (spec.isEnable()) {
				setEnable(t, true);
			}
			return add(t).toString();
		} catch (ClassCastException e) {
			throw new ValidationException("Invalid specification:"
					+ e.getMessage());
		}
	}

	/**
	 * Gets the specific type of the specification
	 *
	 * @param spec
	 *            The specification
	 * @return The specific specification
	 * @throws ValidationException
	 *             if the specification has the wrong type
	 */
	// @SuppressWarnings("unchecked")
	// S getSpec(MCSpec spec) throws ValidationException {
	// try {
	// return (S) spec;
	// } catch (ClassCastException e) {
	// throw new ValidationException("Invalid specification: "
	// + e.getMessage());
	// }
	// }

	/**
	 * Gets specification by id
	 *
	 * @param id
	 *            The id
	 * @return The specification
	 * @throws NoSuchIdException
	 */
	public S get(UUID id) throws NoSuchIdException {
	    return get(getType(id));
	}

	/**
	 * Replace the old entry in list with new entry
	 *
	 * @param list
	 *            The list
	 * @param o
	 *            The old entry
	 * @param n
	 *            The new entry
	 */
	void update(List<E> list, E o, E n) {
		int index = list.indexOf(o);
		list.set(index, n);
	}

	/**
	 * Updates an entry
	 *
	 * @param id
	 *            The id
	 * @param o
	 *            The old entry
	 * @param n
	 *            The new entry
	 */
	protected void update(UUID id, E o, E n) {
	    dict.put(id, n); // returns the previous entry
		update(list, o, n);
	}

	/**
	 * Updates the specification of an entry by id. If specification is of
	 * common type, try to update the name or the enable state. Tries to disable
	 * specification before updating or updating specification before enabling.
	 * Serializes the configuration
	 *
	 * @param id
	 *            The id
	 * @param spec
	 *            The specification
	 * @throws ALEException
	 *             if the specification is enabled and stays enabled
	 */
	@SuppressWarnings("unchecked")
	public void update(UUID id, MCSpec spec) throws ALEException {
		updateLock.lock();
		try {
			E t = getType(id);
			if (t != null) {
				if (t.isEnable()) {
					if (spec.isEnable()) {
						throw new ValidationException("Could not update the active specification '" + t.getName() + "'!");
					} else {
						setEnable(t, false);
						names.remove(t.getName());
						try {
							update(id, t, get((S) spec));
						} catch (ClassCastException e) {
							t.setEnable(Boolean.FALSE);
							if (spec.getName() != null) {
								t.setName(spec.getName());
							}
						}
					}
				} else {
					if (spec.isEnable()) {
						try {
							E n = get((S) spec);
							setEnable(n, true);
							names.put(n.getName(), id);
							update(id, t, n);
						} catch (ClassCastException ex) {
							String name = t.getName();
							if (spec.getName() != null) {
								t.setName(spec.getName());
							}
							try {
								setEnable(t, true);
								names.put(t.getName(), id);
								t.setEnable(Boolean.TRUE);
							} catch (ALEException e) {
								t.setName(name);
								throw e;
							}
						}
					} else {
						try {
							update(id, t, get((S) spec));
						} catch (ClassCastException e) {
							t.setName(spec.getName());
						}
					}
				}
				Config.serialize();
			}
		} finally {
			updateLock.unlock();
		}
	}

	/**
	 * Removes an entry by name, even when it's disabled and doesn't persist the
	 * configuration
	 * 
	 * @param name The name
	 * @return the depot entry
	 */
	public E removeNonPersistent(String name) {
		UUID id = getId(name);
		E entry = null;
		if (id != null) {
			// remove enabled entry
			names.remove(name);
			entry = dict.get(id);
			if (entry != null) {
				list.remove(entry);
			}
			dict.remove(id);
		} else {
			// find disabled entry and remove it
			for (Entry<UUID, E> pair : dict.entrySet()) {
				if (name.equals(pair.getValue().getName())) {
					id = pair.getKey();
					entry = pair.getValue();
					if (entry != null) {
						list.remove(entry);
					}
					break;
				}
			}
			if (id != null) {
				dict.remove(id);
			}
		}
		return entry;
	}

	/**
	 * Removes entry by id. Serializes the configuration
	 * 
	 * @param id
	 *            The unique id
	 * @throws ALEException
	 */
	public void remove(UUID id) throws ALEException {
		E t = getType(id);
		if (t != null) {
			if (t.isEnable()) {
				setEnable(t, false);
				names.remove(t.getName());
			}
			list.remove(t);
			dict.remove(id);
			Config.serialize();
		}
	}

	/**
	 * Removes entry by name and provides the id of the removed entry.
	 * Serializes the configuration
	 *
	 * @param name
	 *            The name
	 * @throws NoSuchIdException
	 */
	public UUID remove(String name) {
		UUID id = getId(name);
		if (id != null) {
			names.remove(name);
			E t = dict.get(id);
			if (t != null) {
				list.remove(t);
			} else {
				Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Failed to remove unknown id {0}", id);
			}
			dict.remove(id);
			Config.serialize();
		}
		return id;
	}

	/**
	 * Gets the property list
	 *
	 * @param properties
	 *            The input properties
	 * @return The output properties
	 */
	static List<Property> getMCProperty(List<MCProperty> properties) {

		if (properties == null) {
			return null;
		} else {
			List<Property> list = new ArrayList<Property>(properties.size());
			for (MCProperty property : properties) {
				list.add(new Property(property.getName(), property.getValue()));
			}
			return list;
		}
	}

	/**
	 * Gets the property list
	 *
	 * @param properties
	 *            The input properties
	 * @return The output properties
	 */
	static List<MCProperty> getProperty(List<Property> properties) {

		if (properties == null) {
			return null;
		} else {
			List<MCProperty> list = new ArrayList<MCProperty>(properties.size());
			for (Property property : properties) {
				MCProperty p = new MCProperty();
				p.setName(property.getName());
				p.setValue(property.getValue());
				list.add(p);
			}
			return list;
		}
	}

	/**
	 * Initializes the entries
	 *
	 * @throws ImplementationException
	 * @throws NoSuchIdException
	 * @throws DuplicateNameException
	 * @throws ValidationException
	 */
	public void init() throws ImplementationException, NoSuchIdException,
			ValidationException, DuplicateNameException {
		for (Entry<UUID, E> pair : dict.entrySet()) {
			if (pair.getValue().isEnable() && !names.containsKey(pair.getValue().getName())) {
				try {
					setEnable(pair.getValue(), true);
					names.put(pair.getValue().getName(), pair.getKey());
				} catch (ALEException e) {
					pair.getValue().setEnable(Boolean.FALSE);
					Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Failed to enable depot entry: " + e.getMessage(), e);
				}
			}
		}
	}
}
