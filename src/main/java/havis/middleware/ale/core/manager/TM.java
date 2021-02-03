package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.tm.TMSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class provides the ALE - Tag Memory API as specified in ALE 1.1.1 (7)
 */
public class TM {

    private Lock lock = new ReentrantLock();
    private havis.middleware.ale.core.depot.service.tm.TagMemory depot = havis.middleware.ale.core.depot.service.tm.TagMemory.getInstance();
    private Map<String, TMSpec> specs;

	private static TM instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new TM();
	}

	/**
	 * Creates a new instance.
	 */
	private TM() {
		specs = new LinkedHashMap<String, TMSpec>();
	}

	/**
	 * Retrieves the static instance
	 *
	 * @return The static instance
	 */
	public static TM getInstance() {
		return instance;
	}

	/**
	 * Defines a tag memory fields. Checks if field specification names are
	 * distinct and no previous definition of a field with one of the names
	 * exists.
	 *
	 * @param name
	 *            The name of the tag memory
	 * @param spec
	 *            The specification of the tag memory
	 * @param persist
	 *            Persist changes
	 * @throws DuplicateNameException
	 *             If tag memory with that name already exists
	 * @throws ValidationException
	 *             Is specification validation failed
	 */
	public void define(String name, TMSpec spec, boolean persist)
			throws DuplicateNameException, ValidationException,
			ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.TM.Name, Exits.Service.TM.Define, "Define {0} {1}", new Object[] { name, spec });
        try {
            lock.lock();
            try {
                if (specs.containsKey(name)) {
                    throw new DuplicateNameException("Tag memory '" + name + "' already defined");
                } else {
                    if (spec == null) {
                        throw new ValidationException("No specification given for tag memory '" + name + "'!");
                    } else {
                        try {
                            Fields.lock();
                            Map<String, CommonField> list = new HashMap<String, CommonField>();
                            for (CommonField field : Fields.getInstance().get(spec)) {
                                if (Fields.getInstance().containsKey(field.getName())) {
                                    throw new ValidationException("Field '" + field.getName() + "' in tag memory '" + name + "' already defined anywhere else");
                                } else if (list.containsKey(field.getName())) {
                                    throw new ValidationException("Field '" + field.getName() + "' in tag memory '" + name + "' already defined here");
                                } else {
                                    list.put(field.getName(), field);
                                }
                            }
                            for (Entry<String, CommonField> entry : list.entrySet()) {
                                Fields.getInstance().set(entry.getKey(), entry.getValue());
                            }
                        } finally {
                            Fields.unlock();
                        }
                        specs.put(name, spec);
                        if (persist)
                            depot.add(name, spec);
                    }
                }
            } finally {
                lock.unlock();
            }
        } catch (DuplicateNameException | ValidationException | ImplementationException e) {
        	Exits.Log.logp(Exits.Level.Error, Exits.Service.TM.Name, Exits.Service.TM.DefineFailed, "Define failed: " + e.getMessage(), e);
            throw e;
        }
	}

	/**
	 * Un-defines tag memory fields.
	 *
	 * @param name
	 *            The name of the tag memory specification
	 * @param persist
	 *            Persist changes
	 * @throws ImplementationException
	 *             If a predefined field does not exists anymore
	 * @throws InUseException
	 *             If a field of the tag memory specification is in used
	 * @throws NoSuchNameException
	 *             If no tag memory specification with that name exists
	 */
	public void undefine(String name, boolean persist)
			throws NoSuchNameException, InUseException, ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.TM.Name, Exits.Service.TM.Undefine, "Undefine {0}", name);
		try {
            lock.lock();
            try {
                TMSpec spec = specs.get(name);
                if (spec != null) {
                    try {
                        Fields.lock();
                        List<CommonField> list = new ArrayList<CommonField>();
                        for (String fieldname : Fields.getNames(spec)) {
                            CommonField field;
                            if ((field = Fields.getInstance().get(fieldname)) == null) {
                                throw new ImplementationException("");
                            } else {
                                if (field.isUsed()) {
                                    // roll back
                                    for (CommonField f : list) {
                                        Fields.getInstance().set(f.getName(), f);
                                    }
                                    throw new InUseException("Could not undefine tag memory '" + name + "' while field '" + fieldname + "' is in use");
                                } else {
                                    Fields.getInstance().set(fieldname, null);
                                }
                                list.add(field);
                            }
                        }
                    } finally {
                        Fields.unlock();
                    }
                    specs.remove(name);
                    if (persist)
                        depot.remove(name);
                } else {
                    throw new NoSuchNameException("Could not undefine nonexisting tag memory '" + name + "'");
                }
            } finally {
                lock.unlock();
            }
		} catch (NoSuchNameException | InUseException | ImplementationException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.TM.Name, Exits.Service.TM.UndefineFailed, "Undefine failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves tag memory specification.
	 *
	 * @param name
	 *            The name of the specification
	 * @return The specification
	 * @throws NoSuchNameException
	 *             if no tag memory specification with that name exists
	 */
	public TMSpec getSpec(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.TM.Name, Exits.Service.TM.GetTMSpec, "Get TM spec {0}", name);
		try {
            lock.lock();
            try {
                TMSpec spec = specs.get(name);
                if (spec != null) {
                    return spec;
                } else {
                    throw new NoSuchNameException("Could not get nonexisting tag memory '" + name + "'");
                }
            } finally {
                lock.unlock();
            }
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.TM.Name, Exits.Service.TM.GetTMSpecFailed, "Get TM spec failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves names of all known tag memory specifications.
	 *
	 * @return List of tag memory specification names
	 */
	public List<String> getNames() {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.TM.Name, Exits.Service.TM.GetTMSpecNames, "Get TM spec names");
		List<String> list = new ArrayList<String>();
        lock.lock();
        try {
            list.addAll(specs.keySet());
        } finally {
            lock.unlock();
        }
		return list;
	}

	/**
	 * Gets the standard version
	 *
	 * @return The standard version
	 */
	public static String getStandardVersion() {
		return Config.getInstance().getService().getTm().getVersion()
				.getStandard();
	}

	/**
	 * Disposes instance
	 */
    public void dispose() {
        lock.lock();
        try {
            Fields.getInstance().dispose();
            specs.clear();
        } finally {
            lock.unlock();
        }
	}
}