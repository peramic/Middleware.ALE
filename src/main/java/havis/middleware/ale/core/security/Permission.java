package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.core.Useable;
import havis.middleware.ale.service.ac.ACPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the ACPermission as specified in ALE 1.1.1 (11.6)
 */
public class Permission implements Useable {

	private enum Type {
		Method
	}

	private int count;

	private Type type;
	private String name;
	private ACPermission spec;
	private List<Method> methods;

	/**
	 * Creates a new instance
	 * 
	 * @param name
	 *            The permission name
	 * @param spec
	 *            The permission specification
	 * @throws PermissionValidationException
	 */
	public Permission(String name, ACPermission spec)
			throws PermissionValidationException {
		this.name = name;
		this.spec = spec;
		type = Type.valueOf(spec.getPermissionClass());
		if (type == null) {
			throw new PermissionValidationException(
					"Unknown permission class '" + spec.getPermissionClass()
							+ "'");
		}
		methods = new ArrayList<Method>();
		for (String method : spec.getInstances().getInstance()) {
			switch (method) {
			case "*":
				methods.add(Method.Any);
				break;
			default:
				Method _instance = Method.valueOf(method.replace('.', '_'));
				if (_instance != null) {
					methods.add(_instance);
				} else {
					throw new PermissionValidationException(
							"Unknown instance '" + method
									+ "' for permission class '"
									+ spec.getPermissionClass() + "'");
				}
				break;
			}
		}
	}

	/**
	 * Gets the specification
	 * 
	 * @return the specification
	 */
	public ACPermission getSpec() {
		return spec;
	}

	/**
	 * Retrieves the specification
	 * 
	 * @param spec
	 *            The specification
	 */
	public void setSpec(ACPermission spec) {
		this.spec = spec;
	}

	/**
	 * Retrieves the permission name
	 * 
	 * @return The permission name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns if permission is of type method and contains instance any,
	 * instance or operation instance
	 * 
	 * @param instance
	 *            The instance
	 * @param operation
	 *            The operation instance
	 * @return True if permission is of type method and contains instance, false
	 *         otherwise
	 */
	boolean containsMethod(Method instance, Method operation) {
		return (type == Type.Method)
				&& (methods.contains(Method.Any) || methods.contains(instance) || methods
						.contains(operation));
	}

	/**
	 * Disposes the instance
	 */
	void dispose() {
	}

	/**
	 * Increase the use count
	 */
	@Override
	public void inc() {
		count++;
	}

	/**
	 * Decrease the use count
	 */
	@Override
	public void dec() {
		count--;
	}

	/**
	 * Returns the use state
	 * 
	 * @return True if is in use, false otherwise
	 */
	@Override
	public boolean isUsed() {
		return count > 0;
	}
}