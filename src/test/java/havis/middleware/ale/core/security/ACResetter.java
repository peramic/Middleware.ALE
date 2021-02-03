package havis.middleware.ale.core.security;

/**
 * generated class to reset AC object instances to fit the security level of package
 */
public class ACResetter {
	public static void reset() {
		Roles.instance = new Roles();
		Identities.instance = new Identities();
		Permissions.instance = new Permissions();
	}
}
