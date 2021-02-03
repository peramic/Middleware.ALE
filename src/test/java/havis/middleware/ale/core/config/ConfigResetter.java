package havis.middleware.ale.core.config;

import havis.middleware.misc.TdtResetter;

/**
 * Class to reset the config and TDT
 */
public class ConfigResetter {
    public static void reset() {
    	TdtResetter.reset();
        Config.reset();
        Config.getInstance();
    }

    public static void disablePersistence() {
        Config.setPersistMode(false);
    }
}
