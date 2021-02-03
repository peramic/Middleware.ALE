package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.RoleValidationException;
import havis.middleware.ale.core.Useable;
import havis.middleware.ale.service.ac.ACRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the role as specified in ALE 1.1.1 (11.5)
 */
public class Role implements Useable {

	String name;
	ACRole spec;
	List<Permission> permissions;

	int count;

	/**
	 * Creates a new instance
	 * 
	 * @param name
	 *            The role name
	 * @param spec
	 *            The role specification
	 * @throws RoleValidationException
	 */
	public Role(String name, ACRole spec) throws RoleValidationException {
		this.name = name;
		this.spec = spec;
		try {
			permissions = Permissions.getInstance().lock(
					spec.getPermissionNames().getPermissionName());
		} catch (NoSuchPermissionException e) {
			throw new RoleValidationException(e.getReason());
		}
	}

	/**
	 * Gets the role specification
	 * 
	 * @return The role specification
	 */
	public ACRole getSpec() {
		spec.setPermissionNameList(getPermissionNames());
		return spec;
	}

	/**
	 * Sets the role specification
	 * 
	 * @param role
	 *            The role specification
	 * @throws RoleValidationException
	 */
	public void setSpec(ACRole role) throws RoleValidationException {
		try {
			List<Permission> permissions = Permissions.getInstance().lock(
					role.getPermissionNames().getPermissionName());
			Permissions.getInstance().unlock(this.permissions);
			this.permissions = permissions;
			spec = role;
		} catch (NoSuchPermissionException e) {
			throw new RoleValidationException(e.getReason());
		}
	}

	/**
	 * Adds permissions
	 * 
	 * @param list
	 *            The permission list
	 * @throws NoSuchPermissionException
	 */
	void add(List<String> list) throws NoSuchPermissionException {
		permissions.addAll(Permissions.getInstance().lock(list));
	}

	/**
	 * Removes permissions
	 * 
	 * @param list
	 *            The permission list
	 * @throws NoSuchPermissionException
	 */
	void remove(List<String> list) throws NoSuchPermissionException {
		for (Permission permission : Permissions.getInstance()
				.unlockNames(list)) {
			permissions.remove(permission);
		}
	}

	/**
	 * Sets permissions
	 * 
	 * @param list
	 *            The permission list
	 * @throws NoSuchPermissionException
	 */
	void set(List<String> list) throws NoSuchPermissionException {
		List<Permission> permissions = Permissions.getInstance().lock(list);
		Permissions.getInstance().unlock(this.permissions);
		this.permissions = permissions;
	}

	/**
	 * Returns the permissions as String list
	 * 
	 * @return The permission String list
	 */
	List<String> getPermissionNames() {
		List<String> list = new ArrayList<String>();
		for (Permission permission : permissions) {
			list.add(permission.getName());
		}
		return list;
	}

	/**
	 * Returns true if the role contains permission to any instance, instance or
	 * method instance
	 * 
	 * @param instance
	 *            The instance
	 * @param method
	 *            The method instance
	 * @return True if role contains permission, false otherwise
	 */
	boolean containsPermission(Method instance, Method method) {
		for (Permission permission : permissions) {
			if (permission.containsMethod(instance, method))
				return true;
		}
		return false;
	}

	/**
	 * Increases the use count
	 */
	@Override
	public void inc() {
		count++;
	}

	/**
	 * Decreases the use count
	 */
	@Override
	public void dec() {
		count--;
	}

	/**
	 * Returns the use state
	 * 
	 * @return True if use count greater then zero, false otherwise
	 */
	@Override
	public boolean isUsed() {
		return count > 0;
	}

	/**
	 * Compare given object with current
	 * 
	 * @param obj
	 *            The object to compare
	 * @return True if object is equal to current, false otherwise
	 */
	@Override
    public boolean equals(Object obj) {
		return (obj == this) || (obj.hashCode() == hashCode());
	}

	/**
	 * Returns the hash code
	 * 
	 * @return The hash code
	 */
	@Override
    public int hashCode() {
		return name.hashCode();
	}

	/**
	 * Disposes the instance
	 */
	void dispose() {
		Permissions.getInstance().unlock(permissions);
	}
}