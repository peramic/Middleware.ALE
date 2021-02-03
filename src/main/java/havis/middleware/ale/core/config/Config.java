package havis.middleware.ale.core.config;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.config.ConfigType;
import havis.middleware.ale.config.GlobalType;
import havis.middleware.ale.config.ReaderCycleType;
import havis.middleware.ale.config.SubscriberConfigType;
import havis.middleware.ale.exit.Exits;
import havis.middleware.utils.threading.ThreadManager;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is used to persistent hold and provide configuration parameters
 */
public class Config {

	private final static Logger log = Logger.getLogger(Config.class.getName());

	private static final String DEFAULT_CONFIG = "havis/middleware/ale/core/config/default.json";
	private static final String CONFIG_FILE_NAME = "havis.middleware.ale.config";

	private static String fileName = "conf/ale.json";

	private static File file;

	private static Lock lock = new ReentrantLock();

	private static ConfigType instance;

	private static ObjectMapper mapper;

	private volatile static boolean persist = true;

	private static void init() {
        String fileNameFromProperty = System.getProperty(CONFIG_FILE_NAME);
        if (fileNameFromProperty != null) {
            log.log(Level.FINE, "Changing default config of ALE service to {0}.", fileNameFromProperty);
            fileName = fileNameFromProperty;
        }

        mapper = new ObjectMapper();

	// load config
        file = new File(Config.fileName);
        try {
            if (file.exists()) {
                instance = mapper.readValue(file, ConfigType.class);
            } else {
                instance = mapper.readValue(Config.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG), ConfigType.class);
            }
        } catch (Exception e) {
        	Exits.Log.logp(Exits.Level.Error, Exits.Common.Name, Exits.Common.Error, "Failed to load config: " + e.getMessage(), e);
        }
        
        setDefaults(instance);
        setSettings(instance);
	}

	private static void setDefaults(ConfigType config) {
		if (config.getGlobal() == null) {
			config.setGlobal(new GlobalType());
			config.getGlobal().setMaxThreads(10);
			config.getGlobal().setThreadTimeout(60000);
			config.getGlobal().setQueueWarningTimeout(5);
		}
		if (config.getGlobal().getReaderCycle() == null) {
			config.getGlobal().setReaderCycle(new ReaderCycleType());
		}
		if (config.getGlobal().getSubscriber() == null) {
			config.getGlobal().setSubscriber(new SubscriberConfigType());
			config.getGlobal().getSubscriber().setConnectTimeout(1000);
			config.getGlobal().getSubscriber().setHttpsSecurity(true);
		}
	}

    private static void setSettings(ConfigType config) {
        persist = config.isPersist();

        // set extended mode
        Tag.setExtended(config.getGlobal().getReaderCycle().isExtended());
        
        // configure ThreadManager
        ThreadManager.init();
        ThreadManager.setMaxThreads((int) config.getGlobal().getMaxThreads());
        ThreadManager.setThreadTimeout((int) config.getGlobal().getThreadTimeout());
        ThreadManager.setQueueWarningTimeout((int) config.getGlobal().getQueueWarningTimeout());
	}

	public static ConfigType getInstance() {
        if (instance == null) {
            lock.lock();
            try {
                if (instance == null) {
                    init();
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }

	public static void reset() {
		instance = null;
	}

	public static boolean isPersistMode() {
		return persist;
	}

	public static void setPersistMode(boolean persist) {
		Config.persist = persist;
	}

	public static boolean isSoapService() {
		return instance.isSoapService();
	}

	public static void setSoapService(boolean soapService) {
		instance.setSoapService(soapService);
		// TODO: register SOAP service if enabled?
	}

	public static boolean isExtendedMode() {
		return instance.getGlobal().getReaderCycle().isExtended();
	}

	public static void setExtendedMode(boolean extended) {
		instance.getGlobal().getReaderCycle().setExtended(extended);
		Tag.setExtended(extended);
	}

    public static void serialize() {
        if (persist) {
            lock.lock();
            try {
                ConfigType config = getInstance();
                File tmpFile = null;
                try {
                    tmpFile = File.createTempFile(Config.file.getName(), ".bak", Config.file.getParentFile());
                    mapper.writerWithDefaultPrettyPrinter().writeValue(tmpFile, config);
                    tmpFile.renameTo(Config.file);
                } catch (Exception e) {
                	Exits.Log.logp(Exits.Level.Error, Exits.Common.Name, Exits.Common.Error, "Failed to save config: " + e.getMessage(), e);
					if (tmpFile != null && tmpFile.exists()) {
						// clean up temp file
						tmpFile.delete();
					}
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
