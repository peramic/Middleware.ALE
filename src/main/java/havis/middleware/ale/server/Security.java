package havis.middleware.ale.server;

import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.core.security.Identities;
import havis.middleware.ale.core.security.Method;

public class Security {

	private static Security instance = new Security();

	private static String name;

	/**
	 * Creates a new instance. Initializes parameters.
	 */
	public Security() {
		// name = WindowsIdentity.GetCurrent().Name + "#";
	}

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static Security getInstance() {
		return instance;
	}

	public boolean isAllowed(Method instance, Method method)
			throws SecurityException {

		if (name != null) {
			return Identities.getInstance().containsPermission(name, instance,
					method);
		} else {
			// throw new SecurityException("Anonymous access forbidden");
			return true;
		}
	}
}
