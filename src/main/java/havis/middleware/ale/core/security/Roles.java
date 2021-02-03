package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.DuplicateRoleException;
import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.NoSuchRoleException;
import havis.middleware.ale.base.exception.RoleValidationException;
import havis.middleware.ale.service.ac.ACRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements the role management
 */
public class Roles {

	static Roles instance = new Roles();
	Map<String, Role> roles;

	/**
	 * Creates a new instance. Initializes parameters.
	 */
	Roles() {
		roles = new HashMap<String, Role>();
	}

	/**
	 * Retrieves the static instance
	 *
	 * @return The static instance
	 */
	public static Roles getInstance() {
		return instance;
	}

	/**
	 * Retrieves the list of all role names.
	 *
	 * @return The list of role names
	 */
	public List<String> getNames() {
		List<String> list = new ArrayList<String>();
		synchronized (roles) {
			list.addAll(roles.keySet());
		}
		return list;
	}

	/**
	 * Defines a new role
	 *
	 * @param name
	 *            The role name
	 * @param spec
	 *            The role specification
	 * @throws DuplicateRoleException
	 * @throws RoleValidationException
	 */
	public void define(String name, ACRole spec) throws DuplicateRoleException,
			RoleValidationException {
		synchronized (roles) {
			if (roles.containsKey(name)) {
				throw new DuplicateRoleException("Role '" + name
						+ "' already defined");
			} else {
				roles.put(name, new Role(name, spec));
			}
		}
	}

	/**
	 * Updates a role
	 *
	 * @param name
	 *            The role name
	 * @param spec
	 *            The role specification
	 * @throws NoSuchRoleException
	 * @throws RoleValidationException
	 */
	public void update(String name, ACRole spec) throws NoSuchRoleException,
			RoleValidationException {
		synchronized (roles) {
			Role role = roles.get(name);
			if (role != null) {
				role.setSpec(spec);
			} else {
				throw new NoSuchRoleException(
						"Could not update a unknown role '" + name + "'");
			}
		}
	}

	/**
	 * Returns the role specification
	 *
	 * @param name
	 *            The role name
	 * @return The role specification
	 * @throws NoSuchRoleException
	 */
	public ACRole get(String name) throws NoSuchRoleException {
		synchronized (roles) {
			Role role = roles.get(name);
			if (role != null) {
				return role.getSpec();
			} else {
				throw new NoSuchRoleException("Could not get a unknown role '"
						+ name + "'");
			}
		}
	}

	/**
	 * Un-defines a role
	 *
	 * @param name
	 *            The role name
	 * @throws NoSuchRoleException
	 */
	public void undefine(String name) throws NoSuchRoleException {
		synchronized (roles) {
			Role role = roles.get(name);
			if (role != null) {
				role.dispose();
				roles.remove(name);
			} else {
				throw new NoSuchRoleException(
						"Could not undefine a unknown role '" + name + "'");
			}
		}
	}

	/**
	 * Adds permissions to role
	 *
	 * @param name
	 *            The role name
	 * @param list
	 *            The permission list
	 * @throws NoSuchRoleException
	 * @throws NoSuchPermissionException
	 */
	public void add(String name, List<String> list) throws NoSuchRoleException,
			NoSuchPermissionException {
		synchronized (roles) {
			Role role = roles.get(name);
			if (role != null) {
				role.add(list);
			} else {
				throw new NoSuchRoleException(
						"Could not add permissions to a unknown role '" + name
								+ "'");
			}
		}
	}

	/**
	 * Sets permissions on role
	 *
	 * @param name
	 *            The role name
	 * @param list
	 *            The permission list
	 * @throws NoSuchRoleException
	 * @throws NoSuchPermissionException
	 */
	public void set(String name, List<String> list) throws NoSuchRoleException,
			NoSuchPermissionException {
		synchronized (roles) {
			Role role = roles.get(name);
			if (role != null) {
				role.set(list);
			} else {
				throw new NoSuchRoleException(
						"Could not set permissions on a unknown role '" + name
								+ "'");
			}
		}
	}

	/**
	 * Removes permissions from role
	 *
	 * @param name
	 *            The role name
	 * @param list
	 *            The permission list
	 * @throws NoSuchRoleException
	 * @throws NoSuchPermissionException
	 */
	public void remove(String name, List<String> list)
			throws NoSuchRoleException, NoSuchPermissionException {
		synchronized (roles) {
			Role role = roles.get(name);
			if (role != null) {
				role.remove(list);
			} else {
				throw new NoSuchRoleException(
						"Could not remove permissions from a unknown role '"
								+ name + "'");
			}
		}
	}

	/**
	 * Retrieves a role list by names
	 *
	 * @param list
	 *            The role names
	 * @return The role list
	 * @throws NoSuchRoleException
	 */
	List<Role> get(List<String> list) throws NoSuchRoleException {
		synchronized (this.roles) {
			List<Role> roles = new ArrayList<Role>();
			for (String entry : list) {
				Role role = this.roles.get(entry);
				if (role != null) {
					roles.add(role);
				} else {
					throw new NoSuchRoleException("Unknown role '" + entry
							+ "'");
				}
			}
			return roles;
		}
	}
}