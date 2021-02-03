package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.DuplicatePermissionException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.service.ac.ACPermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class implements the permission management
 */
public class Permissions {

	Lock lock = new ReentrantLock();

	static Permissions instance = new Permissions();
	Map<String, Permission> permissions;

	/**
	 * Creates a new instance. Initializes parameters.
	 */
	Permissions() {
		permissions = new HashMap<String, Permission>();
	}

	/**
	 * Retrieves the static instance
	 *
	 * @return The static instance
	 */
	public static Permissions getInstance() {
		return instance;
	}

	/**
	 * Returns all permission names
	 *
	 * @return The permission names
	 */
	public List<String> getNames() {
		List<String> list = new ArrayList<String>();
		lock.lock();
		try {
			list.addAll(permissions.keySet());
		} finally {
			lock.unlock();
		}
		return list;
	}

	/**
	 * Define a new permission
	 *
	 * @param name
	 *            The permission name
	 * @param spec
	 *            The permission specification
	 * @throws DuplicatePermissionException
	 * @throws PermissionValidationException
	 */
	public void define(String name, ACPermission spec)
			throws DuplicatePermissionException, PermissionValidationException {
		lock.lock();
		try {
			if (permissions.containsKey(name)) {
				throw new DuplicatePermissionException("Permission '" + name
						+ "' already defined");
			} else {
				permissions.put(name, new Permission(name, spec));
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Updates a permission
	 *
	 * @param name
	 *            The permission name
	 * @param spec
	 *            The permission specification
	 * @throws NoSuchPermissionException
	 * @throws PermissionValidationException
	 */
	public void update(String name, ACPermission spec)
			throws NoSuchPermissionException, PermissionValidationException {
		lock.lock();
		try {
			if (permissions.containsKey(name)) {
				permissions.put(name, new Permission(name, spec));
			} else {
				throw new NoSuchPermissionException(
						"Could not update a unknown permission '" + name + "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns a permission specification
	 *
	 * @param name
	 *            The permission name
	 * @return The permission specification
	 * @throws NoSuchPermissionException
	 */
	public ACPermission get(String name) throws NoSuchPermissionException {
		lock.lock();
		try {
			Permission permission = permissions.get(name);
			if (permission != null) {
				return permission.getSpec();
			} else {
				throw new NoSuchPermissionException(
						"Could not get a unknown permission '" + name + "'");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Undefines a permission
	 *
	 * @param name
	 *            The permission name
	 * @throws InUseException
	 * @throws NoSuchPermissionException
	 */
	public void undefine(String name) throws InUseException,
			NoSuchPermissionException {
		lock.lock();
		try {
			Permission permission = permissions.get(name);
			if (permission != null) {
				if (permission.isUsed()) {
					throw new InUseException(
							"Could not undefine a in use permission '" + name
									+ "'");
				} else {
					permissions.remove(name);
					permission.dispose();
				}
			} else {
				throw new NoSuchPermissionException(
						"Could not undefine a unknown permission '" + name
								+ "'");
			}
		} finally {
			lock.unlock();
		}
	}

	List<Permission> get(List<String> list) throws NoSuchPermissionException {
		List<Permission> permissions = new ArrayList<Permission>();
		for (String name : list) {
			Permission permission = this.permissions.get(name);
			if (permission != null) {
				permissions.add(permission);
			} else {
				throw new NoSuchPermissionException("Unknown permission '"
						+ name + "'");
			}
		}
		return permissions;
	}

	/**
	 * Locks a list of permission stings and returns it as a list of permission
	 * objects
	 *
	 * @param list
	 *            The permission String list
	 * @return The list of permission objects
	 * @throws NoSuchPermissionException
	 */
	List<Permission> lock(List<String> list) throws NoSuchPermissionException {
		lock.lock();
		try {
			List<Permission> permissions = get(list);
			for (Permission permission : permissions) {
				permission.inc();
			}
			return permissions;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Unlocks a list of permission stings and returns it as a list of
	 * permission objects
	 *
	 * @param list
	 *            The permission String list
	 * @return The list of permission objects
	 * @throws NoSuchPermissionException
	 */
	List<Permission> unlockNames(List<String> list)
			throws NoSuchPermissionException {
		lock.lock();
		try {
			List<Permission> permissions = get(list);
			for (Permission permission : permissions) {
				permission.dec();
			}
			return permissions;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Unlocks a list of permission objects
	 *
	 * @param permissions
	 *            The permission list
	 */
	void unlock(List<Permission> permissions) {
		lock.lock();
		try {
			for (Permission permission : permissions) {
				permission.dec();
			}
		} finally {
			lock.unlock();
		}
	}
}