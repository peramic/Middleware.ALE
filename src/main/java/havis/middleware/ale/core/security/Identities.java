package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.ClientIdentityValidationException;
import havis.middleware.ale.base.exception.DuplicateClientIdentityException;
import havis.middleware.ale.base.exception.NoSuchClientIdentityException;
import havis.middleware.ale.base.exception.NoSuchRoleException;
import havis.middleware.ale.service.ac.ACClientIdentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class implements the client identity management
 */
public class Identities {

	Lock lock = new ReentrantLock();

	static Identities instance = new Identities();
	Map<String, Identity> identities;

	/**
	 * Creates a new instance. Initializes parameters.
	 */
	Identities() {
		identities = new HashMap<String, Identity>();
	}

	/**
	 * Gets the static instance
	 *
	 * @return The static instance
	 */
	public static Identities getInstance() {
		return instance;
	}

	/**
	 * Returns all client identity names
	 *
	 * @return The client identity names
	 */
	public List<String> getNames() {
		List<String> list = new ArrayList<String>();
		lock.lock();
		try {
			list.addAll(identities.keySet());
		} finally {
			lock.unlock();
		}
		return list;
	}

	/**
	 * Define a new client identity
	 *
	 * @param name
	 *            The client identity name
	 * @param spec
	 *            The client identity specification
	 * @throws ClientIdentityValidationException
	 * @throws DuplicateClientIdentityException
	 */
	public void define(String name, ACClientIdentity spec)
			throws ClientIdentityValidationException,
			DuplicateClientIdentityException {
		lock.lock();
		try {
			if (identities.containsKey(name)) {
				throw new DuplicateClientIdentityException("Client identity '"
						+ name + "' already defined");
			} else {
				identities.put(name, new Identity(name, spec));
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Updates a client identity
	 *
	 * @param name
	 *            The client identity name
	 * @param spec
	 *            The client identity specification
	 * @throws NoSuchClientIdentityException
	 */
	public void update(String name, ACClientIdentity spec)
			throws NoSuchClientIdentityException {
		lock.lock();
		try {
			Identity identity = identities.get(name);
			if (identity != null) {
				identity.setSpec(spec);
			} else {
				throw new NoSuchClientIdentityException(
						"Could not update a unknown client identity '" + name
								+ "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns a client identity specification
	 *
	 * @param name
	 *            The client identity name
	 * @return The client identity specification
	 * @throws NoSuchClientIdentityException
	 */
	public ACClientIdentity get(String name)
			throws NoSuchClientIdentityException {
		lock.lock();
		try {
			Identity identity = identities.get(name);
			if (identity != null) {
				return identity.getSpec();
			} else {
				throw new NoSuchClientIdentityException(
						"Could not get a unknown client identity '" + name
								+ "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Retrieves the client identity permissions
	 *
	 * @param name
	 *            The client identity name
	 * @return The client identity permissions
	 * @throws NoSuchClientIdentityException
	 */
	public List<String> getPermissionNames(String name)
			throws NoSuchClientIdentityException {
		lock.lock();
		try {
			Identity identity = identities.get(name);
			if (identity != null) {
				return identity.getPermissionNames();
			} else {
				throw new NoSuchClientIdentityException(
						"Could not get permission names for a unknown client identity '"
								+ name + "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Un-defines a client identity
	 *
	 * @param name
	 *            The client identity name
	 * @throws NoSuchClientIdentityException
	 */
	public void undefine(String name) throws NoSuchClientIdentityException {
		lock.lock();
		try {
			Identity identity = identities.get(name);
			if (identity != null) {
				identity.dispose();
				identities.remove(name);
			} else {
				throw new NoSuchClientIdentityException(
						"Could not undefine a unknown client identity '" + name
								+ "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Adds roles to a client identity
	 *
	 * @param name
	 *            The client identity name
	 * @param list
	 *            The role list
	 * @throws NoSuchRoleException
	 * @throws NoSuchClientIdentityException
	 */
	public void add(String name, List<String> list) throws NoSuchRoleException,
			NoSuchClientIdentityException {
		lock.lock();
		try {
			Identity identity = identities.get(name);
			if (identity != null) {
				identity.add(list);
			} else {
				throw new NoSuchClientIdentityException(
						"Could not add roles to a unknown client identity '"
								+ name + "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Removes roles from client identity
	 *
	 * @param name
	 *            The client identity name
	 * @param list
	 *            The role lisz
	 * @throws NoSuchClientIdentityException
	 * @throws NoSuchRoleException
	 */
	public void remove(String name, List<String> list)
			throws NoSuchClientIdentityException, NoSuchRoleException {
		lock.lock();
		try {
			Identity identity = identities.get(name);
			if (identity != null) {
				identity.remove(list);
			} else {
				throw new NoSuchClientIdentityException(
						"Could not remove roles from a unknown client identity '"
								+ name + "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Sets roles on client identity
	 *
	 * @param name
	 *            The client identity name
	 * @param list
	 *            The role list
	 * @throws NoSuchClientIdentityException
	 * @throws NoSuchRoleException
	 */
	public void set(String name, List<String> list)
			throws NoSuchClientIdentityException, NoSuchRoleException {
		lock.lock();
		try {
			Identity identity = identities.get(name);
			if (identity != null) {
				identity.set(list);
			} else {
				throw new NoSuchClientIdentityException(
						"Could not set roles on a unknown client identity '"
								+ name + "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns if client identity contains a method instance
	 *
	 * @param name
	 *            The client identity name
	 * @param instance
	 *            The instance name
	 * @param method
	 *            The instance method name
	 * @return True if client identity contains the instance, false otherwise
	 */
	public boolean containsPermission(String name, Method instance,
			Method method) {
		lock.lock();
		try {
			Identity identity = identities.get(name);
			if (identity != null) {
				if (identity.containsPermission(instance, method)) {
					return true;
				} else {
					throw new SecurityException(
							"Access denied for client identity '" + name + "'");
				}
			} else {
				throw new SecurityException("Unknown client identity '" + name
						+ "'");
			}
		} finally {
			lock.unlock();
		}
	}
}