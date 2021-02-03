package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchPathException;
import havis.middleware.ale.base.exception.NoSuchPropertyException;
import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ParameterForbiddenException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.ConfigType;
import havis.middleware.ale.config.service.mc.Path;
import havis.middleware.ale.config.service.mc.Property;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.service.cc.Association;
import havis.middleware.ale.core.depot.service.cc.Cache;
import havis.middleware.ale.core.depot.service.cc.CommandCycle;
import havis.middleware.ale.core.depot.service.cc.Random;
import havis.middleware.ale.core.depot.service.ec.EventCycle;
import havis.middleware.ale.core.depot.service.lr.LogicalReader;
import havis.middleware.ale.core.depot.service.pc.PortCycle;
import havis.middleware.ale.core.depot.service.tm.TagMemory;
import havis.middleware.ale.core.reader.Reader;
import havis.middleware.ale.core.subscriber.Subscriber;
import havis.middleware.ale.service.IReports;
import havis.middleware.ale.service.cc.CCParameterListEntry;
import havis.middleware.ale.service.mc.MCCommandCycleSpec;
import havis.middleware.ale.service.mc.MCConnectorSpec;
import havis.middleware.ale.service.mc.MCEventCycleSpec;
import havis.middleware.ale.service.mc.MCPCOpSpecs;
import havis.middleware.ale.service.mc.MCPortCycleSpec;
import havis.middleware.ale.service.mc.MCProperty;
import havis.middleware.ale.service.mc.MCSpec;
import havis.middleware.ale.service.mc.MCVersionSpec;
import havis.middleware.ale.service.pc.PCOpReport;
import havis.middleware.utils.threading.ThreadManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.w3c.dom.Document;

/**
 * Implements the Ha-VIS MC management interface
 */
public class MC {

	private ConfigType config = Config.getInstance();
	private LogicalReader logicalReader = LogicalReader.getInstance();
	private TagMemory tagMemory = TagMemory.getInstance();
	private EventCycle eventCycle = EventCycle.getInstance();
	private CommandCycle commandCycle = CommandCycle.getInstance();
	private Cache cache = Cache.getInstance();
	private Association association = Association.getInstance();
	private Random random = Random.getInstance();
	private PortCycle portCycle = PortCycle.getInstance();
	private Map<String, Document> files = new LinkedHashMap<String, Document>();

	private static MC instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new MC();
	}

	/**
	 * Create a new instance.
	 */
	private MC() {
	}

	/**
	 * @return the static instance
	 */
	public static MC getInstance() {
		return instance;
	}

	/**
	 * Gets the file
	 * 
	 * @param name
	 *            The name
	 * @return The file
	 */
	public Document get(String name) {
		return files.get(name);
	}

	/**
	 * Sets the file
	 * 
	 * @param name
	 *            The name
	 * @param file
	 *            The file
	 */
	public void set(String name, Document file) {
		files.put(name, file);
	}

	UUID uuid(String id) throws ImplementationException {
		try {
			return UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			throw new ImplementationException("Invalid identifier '" + id + "'");
		}
	}

	public String add(String path, MCSpec spec) throws ALEException {
		return add(path, spec, null);
	}

	/**
	 * Adds a new spec
	 * 
	 * @param path
	 *            The spec path
	 * @param spec
	 *            The spec
	 * @param parent
	 *            The optional parent i.e. for subscriber
	 * @return A UUID
	 * @throws ALEException
	 */
	public String add(String path, MCSpec spec, String parent)
			throws ImplementationException, ValidationException, NoSuchPathException, SecurityException {
		try {
			switch (path) {
			case Path.Service.LR.LogicalReader:
				return logicalReader.add(spec);
			case Path.Service.TM.TagMemory:
				return tagMemory.add(spec);
			case Path.Service.EC.EventCycle:
				return eventCycle.add(spec);
			case Path.Service.EC.Subscriber:
				return eventCycle.add(spec, uuid(parent));
			case Path.Service.CC.CommandCycle:
				return commandCycle.add(spec);
			case Path.Service.CC.Subscriber:
				return commandCycle.add(spec, uuid(parent));
			case Path.Service.CC.Cache:
				return cache.add(spec);
			case Path.Service.CC.Association:
				return association.add(spec);
			case Path.Service.CC.Random:
				return random.add(spec);
			case Path.Service.PC.PortCycle:
				return portCycle.add(spec);
			case Path.Service.PC.Subscriber:
				return portCycle.add(spec, uuid(parent));
			default:
				throw new NoSuchPathException("Unknown path '" + path + "'");
			}
		} catch (ImplementationException | ValidationException | NoSuchPathException | SecurityException e) {
			throw e;
		} catch (ALEException e) {
			throw new ImplementationException(e.getReason());
		}
	}

	/**
	 * Removes a spec
	 * 
	 * @param path
	 *            The spec path
	 * @param id
	 *            The id
	 * @param parent
	 *            The parent i.e. for subscriber
	 * @throws ALEException
	 */
	public void remove(String path, String id, String parent) throws ImplementationException, ValidationException, NoSuchIdException, NoSuchPathException,
			SecurityException {
		try {
			switch (path) {
			case Path.Service.LR.LogicalReader:
				logicalReader.remove(uuid(id));
				break;
			case Path.Service.TM.TagMemory:
				tagMemory.remove(uuid(id));
				break;
			case Path.Service.EC.EventCycle:
				eventCycle.remove(uuid(id));
				break;
			case Path.Service.EC.Subscriber:
				eventCycle.remove(uuid(id), uuid(parent));
				break;
			case Path.Service.CC.CommandCycle:
				commandCycle.remove(uuid(id));
				break;
			case Path.Service.CC.Subscriber:
				commandCycle.remove(uuid(id), uuid(parent));
				break;
			case Path.Service.CC.Cache:
				cache.remove(uuid(id));
				break;
			case Path.Service.CC.Association:
				association.remove(uuid(id));
				break;
			case Path.Service.CC.Random:
				random.remove(uuid(id));
				break;
			case Path.Service.PC.PortCycle:
				portCycle.remove(uuid(id));
				break;
			case Path.Service.PC.Subscriber:
				portCycle.remove(uuid(id), uuid(parent));
				break;
			default:
				throw new NoSuchPathException("Unknown path '" + path + "'");
			}
		} catch (ImplementationException | ValidationException | NoSuchIdException | NoSuchPathException | SecurityException e) {
			throw e;
		} catch (ALEException e) {
			throw new ImplementationException(e.getReason());
		}
	}

	/**
	 * Removes a spec
	 * 
	 * @param path
	 *            The spec path
	 * @param id
	 *            The id
	 * @throws ALEException
	 */
	public void remove(String path, String id) throws ALEException {
		remove(path, id, null);
	}

	/**
	 * Updates a spec
	 * 
	 * @param path
	 *            The spec path
	 * @param id
	 *            The id
	 * @param spec
	 *            The spec
	 * @param parent
	 *            The parent i.e. for subscriber
	 * @throws ALEException
	 * @throws ValidationException
	 */
	public void update(String path, String id, MCSpec spec, String parent) throws ImplementationException, ValidationException, NoSuchIdException,
			NoSuchPathException, SecurityException {
		try {
			switch (path) {
			case Path.Service.LR.LogicalReader:
				logicalReader.update(uuid(id), spec);
				break;
			case Path.Service.TM.TagMemory:
				tagMemory.update(uuid(id), spec);
				break;
			case Path.Service.EC.EventCycle:
				eventCycle.update(uuid(id), spec);
				break;
			case Path.Service.EC.Subscriber:
				eventCycle.update(uuid(id), spec, uuid(parent));
				break;
			case Path.Service.CC.CommandCycle:
				commandCycle.update(uuid(id), spec);
				break;
			case Path.Service.CC.Subscriber:
				commandCycle.update(uuid(id), spec, uuid(parent));
				break;
			case Path.Service.CC.Cache:
				cache.update(uuid(id), spec);
				break;
			case Path.Service.CC.Association:
				association.update(uuid(id), spec);
				break;
			case Path.Service.CC.Random:
				random.update(uuid(id), spec);
				break;
			case Path.Service.PC.PortCycle:
				portCycle.update(uuid(id), spec);
				break;
			case Path.Service.PC.Subscriber:
				portCycle.update(uuid(id), spec, uuid(parent));
				break;
			default:
				throw new NoSuchPathException("Unknown path '" + path + "'");
			}
		} catch (ImplementationException | ValidationException | NoSuchIdException | NoSuchPathException | SecurityException e) {
			throw e;
		} catch (ALEException e) {
			throw new ImplementationException(e.getReason());
		}
	}

	/**
	 * Updates a spec
	 * 
	 * @param path
	 *            The spec path
	 * @param id
	 *            The id
	 * @param spec
	 *            The spec
	 * @throws ALEException
	 * @throws ValidationException
	 */
	public void update(String path, String id, MCSpec spec) throws ValidationException, ALEException {
		update(path, id, spec, null);
	}

	/**
	 * Returns the spec
	 * 
	 * @param path
	 *            The spec path
	 * @param id
	 *            The id
	 * @param parent
	 *            The optional parent i.e. for subscriber
	 * @return The spec
	 * @throws ValidationException
	 * @throws NoSuchIdException
	 * @throws ImplementationException
	 * @throws NoSuchPathException
	 */
	public MCSpec get(String path, String id, String parent) throws NoSuchIdException, ImplementationException, NoSuchPathException {
		try {
			switch (path) {
			case Path.Service.LR.LogicalReader:
				return logicalReader.get(uuid(id));
			case Path.Service.TM.TagMemory:
				return tagMemory.get(uuid(id));
			case Path.Service.EC.EventCycle:
				return eventCycle.get(uuid(id));
			case Path.Service.EC.Subscriber:
				return eventCycle.get(uuid(id), uuid(parent));
			case Path.Service.CC.CommandCycle:
				return commandCycle.get(uuid(id));
			case Path.Service.CC.Subscriber:
				return commandCycle.get(uuid(id), uuid(parent));
			case Path.Service.CC.Cache:
				return cache.get(uuid(id));
			case Path.Service.CC.Association:
				return association.get(uuid(id));
			case Path.Service.CC.Random:
				return random.get(uuid(id));
			case Path.Service.PC.PortCycle:
				return portCycle.get(uuid(id));
			case Path.Service.PC.Subscriber:
				return portCycle.get(uuid(id), uuid(parent));
			case Path.Service.LR.Version:
				return new MCVersionSpec(LR.getStandardVersion(), null);
			case Path.Service.TM.Version:
				return new MCVersionSpec(TM.getStandardVersion(), null);
			case Path.Service.EC.Version:
				return new MCVersionSpec(EC.getStandardVersion(), null);
			case Path.Service.CC.Version:
				return new MCVersionSpec(CC.getStandardVersion(), null);
			case Path.Service.PC.Version:
				return new MCVersionSpec(PC.getStandardVersion(), null);
			case Path.Connector.Reader:
				return new MCConnectorSpec(id);
			case Path.Connector.Subscriber:
				return new MCConnectorSpec(id);
			default:
				throw new NoSuchPathException("Unknown path '" + path + "'");
			}
		} catch (NoSuchIdException | NoSuchPathException e) {
			throw e;
		} catch (ALEException e) {
			throw new ImplementationException(e.getReason());
		}
	}

	/**
	 * Returns the spec
	 * 
	 * @param path
	 *            The spec path
	 * @param id
	 *            The id
	 * @return The spec
	 * @throws NoSuchPathException
	 * @throws ImplementationException
	 * @throws ValidationException
	 * @throws NoSuchIdException
	 */
	public MCSpec get(String path, String id) throws NoSuchIdException, ValidationException, ImplementationException, NoSuchPathException {
		return get(path, id, null);
	}

	/**
	 * Lists all id
	 * 
	 * @param path
	 *            The path
	 * @param parent
	 *            The parent i.e. for subscriber
	 * @return The is list
	 * @throws ValidationException
	 * @throws NoSuchIdException
	 * @throws NoSuchPathException
	 * @throws ImplementationException
	 */
	public List<String> list(String path, String parent) throws NoSuchIdException, NoSuchPathException, ImplementationException {
		try {
			switch (path) {
			case Path.Service.LR.LogicalReader:
				return logicalReader.toList();
			case Path.Service.TM.TagMemory:
				return tagMemory.toList();
			case Path.Service.EC.EventCycle:
				return eventCycle.toList();
			case Path.Service.EC.Subscriber:
				return eventCycle.toList(uuid(parent));
			case Path.Service.CC.CommandCycle:
				return commandCycle.toList();
			case Path.Service.CC.Subscriber:
				return commandCycle.toList(uuid(parent));
			case Path.Service.CC.Cache:
				return cache.toList();
			case Path.Service.CC.Association:
				return association.toList();
			case Path.Service.CC.Random:
				return random.toList();
			case Path.Service.PC.PortCycle:
				return portCycle.toList();
			case Path.Service.PC.Subscriber:
				return portCycle.toList(uuid(parent));
			case Path.Connector.Reader:
				return Reader.getInstance().getTypes();
			case Path.Connector.Subscriber:
				return Subscriber.getInstance().getTypes();
			default:
				throw new NoSuchPathException("Unknown path '" + path + "'");
			}
		} catch (NoSuchIdException | NoSuchPathException e) {
			throw e;
		} catch (ALEException e) {
			throw new ImplementationException(e.getReason());
		}
	}

	/**
	 * Lists all id
	 * 
	 * @param path
	 *            The path
	 * @return The is list
	 * @throws NoSuchPathException
	 * @throws ValidationException
	 * @throws NoSuchIdException
	 * @throws ImplementationException
	 */
	public List<String> list(String path) throws NoSuchIdException, NoSuchPathException, ImplementationException {
		return list(path, null);
	}

	/**
	 * Gets property value
	 * 
	 * @param name
	 *            The property name
	 * @return The property value
	 * @throws NoSuchPropertyException
	 */
	public String getProperty(String name) throws NoSuchPropertyException {
		switch (name) {
		case Property.Global.Name:
			return config.getGlobal().getName();
		case Property.Global.ALEID:
			return config.getGlobal().getAleid();
		case Property.Global.SOAPService:
			return Boolean.toString(Config.isSoapService());
		case Property.Global.MaxThreads:
			return Long.toString(config.getGlobal().getMaxThreads());
		case Property.Global.PersistMode:
			return Boolean.toString(Config.isPersistMode());
		case Property.Global.ReaderCycle.Duration:
			return Integer.toString(config.getGlobal().getReaderCycle().getDuration());
		case Property.Global.ReaderCycle.Count:
			return Integer.toString(config.getGlobal().getReaderCycle().getCount());
		case Property.Global.ReaderCycle.Lifetime:
			return Long.toString(config.getGlobal().getReaderCycle().getLifetime());
		case Property.Global.ReaderCycle.ExtendedMode:
			return Boolean.toString(Config.isExtendedMode());
		case Property.Global.Subscriber.ConnectTimeout:
			return Integer.toString(config.getGlobal().getSubscriber().getConnectTimeout());
		case Property.Global.Subscriber.HttpsSecurity:
			return Boolean.toString(config.getGlobal().getSubscriber().isHttpsSecurity());
		default:
			throw new NoSuchPropertyException("Unknown property '" + name + "'");
		}
	}

	/**
	 * Gets the properties
	 * 
	 * @param names
	 *            The property names
	 * @return The properties
	 * @throws NoSuchPropertyException
	 */
	public List<MCProperty> getProperties(List<String> names) throws NoSuchPropertyException {
		List<MCProperty> properties = new ArrayList<MCProperty>();
		for (final String propertyName : names) {
			properties.add(new MCProperty() {
				{
					setName(propertyName);
					setValue(getProperty(propertyName));
				}
			});
		}
		return properties;
	}

	/**
	 * Sets property value
	 * 
	 * @param name
	 *            The property name
	 * @param value
	 *            The property value
	 * @throws ImplementationException
	 * @throws NoSuchPropertyException
	 */
	public void setProperty(String name, String value) throws ImplementationException, NoSuchPropertyException {
		switch (name) {
		case Property.Global.ALEID:
			config.getGlobal().setAleid(value);
			break;
		case Property.Global.SOAPService:
			Config.setSoapService(Boolean.parseBoolean(value));
			break;
		case Property.Global.MaxThreads:
			try {
				int n = Integer.parseInt(value);
				if ((n >= 1)) {
					config.getGlobal().setMaxThreads(n);
					ThreadManager.setMaxThreads(n);
				} else {
					throw new Exception("Maximum thread count shall be greater then 0.");
				}
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.ThreadTimeout:
			try {
				int n = Integer.parseInt(value);
				if ((n >= 1)) {
					config.getGlobal().setThreadTimeout(n);
					ThreadManager.setThreadTimeout(n);
				} else {
					throw new Exception("Thread timeout shall be greater then 0.");
				}
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.QueueWarningTimeout:
			try {
				int n = Integer.parseInt(value);
				if ((n >= 1)) {
					config.getGlobal().setQueueWarningTimeout(n);
					ThreadManager.setQueueWarningTimeout(n);
				} else {
					throw new Exception("Queue warning timeout shall be greater then 0.");
				}
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.PersistMode:
			try {
				boolean b = Boolean.parseBoolean(value);
				Config.setPersistMode(b);
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.ReaderCycle.Duration:
			try {
				int n = Integer.parseInt(value);
				if ((n >= 0) && (n <= 1000)) {
					config.getGlobal().getReaderCycle().setDuration(n);
				} else {
					throw new Exception("Duration of reader cycle shall be between 0 and 1000 milliseconds.");
				}
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.ReaderCycle.Count:
			try {
				int n = Integer.parseInt(value);
				if ((n >= 0) && (n <= 65535)) {
					config.getGlobal().getReaderCycle().setCount(n);
				} else {
					throw new Exception("Count of tags shall be between 0 and 65535.");
				}
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.ReaderCycle.Lifetime:
			try {
				long n = Long.parseLong(value);
				if ((n >= 0) && (n <= 86400000)) {
					config.getGlobal().getReaderCycle().setLifetime(n);
				} else {
					throw new Exception("Liftime of tags shall be greater then zero and lesser then seconds of a day.");
				}
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.ReaderCycle.ExtendedMode:
			try {
				boolean b = Boolean.parseBoolean(value);
				Config.setExtendedMode(b);
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.Subscriber.ConnectTimeout:
			try {
				int n = Integer.parseInt(value);
				if (n >= 0) {
					config.getGlobal().getSubscriber().setConnectTimeout(n);
				} else {
					throw new Exception("Subscriber connect timeout must not be negative");
				}
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		case Property.Global.Subscriber.HttpsSecurity:
			try {
				boolean b = Boolean.parseBoolean(value);
				config.getGlobal().getSubscriber().setHttpsSecurity(b);
			} catch (Exception e) {
				throw new ImplementationException("Invalid value for property '" + name + "'. " + e.getMessage());
			}
			break;
		default:
			throw new NoSuchPropertyException("Unknown property '" + name + "'");
		}
		Config.serialize();
	}

	private IReports execute(MCEventCycleSpec spec, EC ec) throws NoSuchNameException, ValidationException, ImplementationException {
		return spec.isEnable() ? ec.poll(spec.getName()) : ec.immediate(spec.getSpec());
	}

	private IReports execute(MCCommandCycleSpec spec, CC cc, List<CCParameterListEntry> parameters) throws NoSuchNameException, ValidationException, ImplementationException, ParameterException,
			ParameterForbiddenException {
		return spec.isEnable() ? cc.poll(spec.getName(), parameters) : cc.immediate(spec.getSpec());
	}

	private IReports execute(MCPortCycleSpec spec, PC pc) throws NoSuchNameException, ValidationException, ImplementationException {
		return spec.isEnable() ? pc.poll(spec.getName()) : pc.immediate(spec.getSpec());
	}

	public IReports execute(String path, String id) throws NoSuchIdException, ValidationException, ImplementationException, NoSuchPathException,
			NoSuchNameException, SecurityException, ParameterException, ParameterForbiddenException {
		switch (path) {
		case Path.Service.EC.EventCycle:
			return execute(eventCycle.get(uuid(id)), EC.getInstance());
		case Path.Service.CC.CommandCycle:
			return execute(commandCycle.get(uuid(id)), CC.getInstance(), null);
		case Path.Service.PC.PortCycle:
			return execute(portCycle.get(uuid(id)), PC.getInstance());
		}
		return null;
	}


	public IReports execute(String id, List<CCParameterListEntry> parameters) throws NoSuchIdException, ValidationException, ImplementationException, NoSuchPathException,
			NoSuchNameException, SecurityException, ParameterException, ParameterForbiddenException {
		return execute(commandCycle.get(uuid(id)), CC.getInstance(), parameters);
	}

	public List<PCOpReport> execute(MCPCOpSpecs specs) throws ImplementationException, ValidationException, SecurityException {
		return PC.getInstance().execute(specs.getSpecs().getOpSpec());
	}

	/**
	 * Gets the standard version
	 * 
	 * @return The standard version
	 */
	public String getStandardVersion() {
		return Config.getInstance().getService().getMc().getVersion().getStandard();
	}
}
