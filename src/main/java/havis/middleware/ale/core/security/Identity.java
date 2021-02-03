package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.ClientIdentityValidationException;
import havis.middleware.ale.base.exception.NoSuchRoleException;
import havis.middleware.ale.service.ac.ACClientIdentity;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the ACClientIdentity as specified in ALE 1.1.1 (11.3)
 */
public class Identity {
	String name;
	List<Role> roles;
	ACClientIdentity spec;

	/**
	 * Gets the specification
	 *
	 * @return The specification
	 */
	public ACClientIdentity getSpec() {
		spec.setRoleNameList(getPermissionNames());
		return spec;
	}

	/**
	 * Set the specification
	 *
	 * @param spec
	 *            The specification
	 */
	public void setSpec(ACClientIdentity spec) {
		this.spec = spec;
	}

	/**
	 * Creates a new instance
	 *
	 * @param name
	 *            The client identity name
	 * @param spec
	 *            The client identity specification
	 * @throws ClientIdentityValidationException
	 */
	public Identity(String name, ACClientIdentity spec)
			throws ClientIdentityValidationException {
		this.name = name;
		this.spec = spec;
		try {
			roles = Roles.getInstance().get(spec.getRoleNames().getRoleName());
		} catch (NoSuchRoleException e) {
			throw new ClientIdentityValidationException(e.getReason());
		}
	}

	/**
	 * Adds roles
	 *
	 * @param list
	 *            The role list
	 * @throws NoSuchRoleException
	 */
	void add(List<String> list) throws NoSuchRoleException {
		for (Role role : Roles.getInstance().get(list)) {
			roles.add(role);
			role.inc();
		}
	}

	/**
	 * Removes roles
	 *
	 * @param list
	 *            The role list
	 * @throws NoSuchRoleException
	 */
	void remove(List<String> list) throws NoSuchRoleException {
		for (Role role : Roles.getInstance().get(list)) {
			if (roles.remove(role))
				role.dec();
		}
	}

	/**
	 * Sets roles
	 *
	 * @param list
	 *            The role list
	 * @throws NoSuchRoleException
	 */
	void set(List<String> list) throws NoSuchRoleException {
		for (Role role : roles) {
			role.dec();
		}
		roles.clear();
		for (Role role : Roles.getInstance().get(list)) {
			roles.add(role);
			role.inc();
		}
	}

	/**
	 * Returns the permissions
	 *
	 * @return The permission list
	 */
	List<String> getPermissionNames() {
		List<String> list = new ArrayList<String>();
		for (Role role : roles) {
			list.addAll(role.getPermissionNames());
		}
		return list;
	}

	/**
	 * Returns if client identity contains a permission
	 *
	 * @param instance
	 *            The instance
	 * @param method
	 *            The instance method
	 * @return True if client identity contains a permission, false otherwise
	 */
	public boolean containsPermission(Method instance, Method method) {
		for (Role role : roles) {
			if (role.containsPermission(instance, method))
				return true;
		}
		return false;
	}

	/**
	 * Disposes the instance
	 */
	void dispose() {
	}
}