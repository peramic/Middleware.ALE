package havis.middleware.ale.core.manager;

import havis.middleware.ale.core.config.Config;

public class AC {

	/**
	 * Gets the standard version
	 * 
	 * @return The standard version
	 */
	public static String getStandardVersion() {
		return Config.getInstance().getService().getAc().getVersion()
				.getStandard();
	}
}