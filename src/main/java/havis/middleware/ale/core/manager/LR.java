package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonBaseReaderException;
import havis.middleware.ale.base.exception.NonCompositeReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.LogicalReaderType;
import havis.middleware.ale.core.BaseReader;
import havis.middleware.ale.core.CompositeReader;
import havis.middleware.ale.core.ImmutableReader;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.reader.ImmutableReaderConnector;
import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.rc.RCConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton class that provides functionality to manage the logical reader
 * hierarchy.
 */
public class LR {

	private Lock lock = new ReentrantLock();
	private havis.middleware.ale.core.depot.service.lr.LogicalReader depot = havis.middleware.ale.core.depot.service.lr.LogicalReader
			.getInstance();

	private static LR instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new LR();
	}

	/**
	 * Gets the singleton manager instance.
	 * 
	 * @return The singleton
	 */
	public static LR getInstance() {
		return instance;
	}

	private Map<String, LogicalReader> readers;

	/**
	 * Initializes a new instance
	 */
	private LR() {
		readers = new LinkedHashMap<String, LogicalReader>();
	}

	/**
	 * Defines a new logical reader
	 * 
	 * @param name
	 *            The name of the logical reader
	 * @param spec
	 *            The specification to define the logical reader
	 * @param persist
	 *            Persist changes
	 * @throws ValidationException
	 *             if name is a empty string or null
	 * @throws DuplicateNameException
	 *             if a reader with name was already defined.
	 * @throws ReaderLoopException
	 * @throws ImplementationException
	 */
	public void define(String name, LRSpec spec, boolean persist)
			throws ValidationException, DuplicateNameException,
			ImplementationException, ImmutableReaderException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.Define, "Define {0} {1}", new Object[] { name, spec });
		try {
			// validate name
			if (Name.isValid(name)) {
				lock.lock();
				try {
					// check if reader name already exist
					if (readers.containsKey(name)) {
						throw new DuplicateNameException("The logical reader '"
								+ name + "' was already defined!");
					}
					if (spec == null) {
						throw new ValidationException(
								"No specification given for reader '" + name
										+ "'!");
					} else {
						// check if isComposite is true
						try {
							if (spec.isIsComposite() != null
									&& Boolean.TRUE
											.equals(spec.isIsComposite())) {
								readers.put(name, new CompositeReader(name,
										spec));
							} else {
								readers.put(name, new BaseReader(name, spec));
							}
							if (persist)
								depot.add(name, spec);
						} catch (ReaderLoopException e) {
							throw new ValidationException("Reader '" + name
									+ "': " + e.getReason());
						}
					}
				} finally {
					lock.unlock();
				}
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.DefineFailed, "Define failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Adds an immutable reader
	 * 
	 * @param name
	 *            The immutable reader name
	 * @param reader
	 *            The immutable reader instance
	 * @throws ValidationException
	 *             If name is invalid
	 * @throws DuplicateNameException
	 *             If reader with the same name already exists
	 * @throws ImmutableReaderException
	 * @throws ImplementationException
	 */
	public void add(String name, ImmutableReaderConnector reader)
			throws ValidationException, DuplicateNameException,
			ImplementationException, ImmutableReaderException {
		// validate name
		if (Name.isValid(name)) {
			try {
				lock.lock();
				// check if reader name already exist
				if (readers.containsKey(name)) {
					throw new DuplicateNameException("The logical reader '"
							+ name + "' was already defined!");
				}
				// make sure we don't duplicate the entry
				LogicalReaderType entry = depot.removeNonPersistent(name);
				LRSpec spec = entry != null ? entry.getSpec() : null;
				readers.put(name, new ImmutableReader(name, reader, spec));
				depot.addNonPersistent(name, spec);
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Removes immutable reader by name
	 * 
	 * @param name
	 *            The name of the immutable reader
	 * @throws ImplementationException
	 *             If reader does not exists or is not of immutable reader
	 *             instance or not free
	 */
	public void remove(String name) throws ImplementationException {
		try {
			lock.lock();
			LogicalReader reader = readers.get(name);
			if (reader instanceof ImmutableReader) {
				((ImmutableReader) reader).dispose();
				depot.removeNonPersistent(name);
				readers.remove(name);
			} else {
				throw new ImplementationException(
						"Failed to remove the logical reader");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Updates the definition of the logical reader
	 * 
	 * @param name
	 *            The name of the logical reader
	 * @param spec
	 *            The specification to update the logical reader
	 * @param persist
	 *            Persist changes
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws ValidationException
	 * @throws NoSuchNameException
	 * @throws ReaderLoopException
	 * @throws ImmutableReaderException
	 */
	public void update(String name, LRSpec spec, boolean persist)
			throws ImplementationException, InUseException,
			ValidationException, NoSuchNameException, ReaderLoopException,
			ImmutableReaderException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.Update, "Update {0} {1}", new Object[] { name, spec });
		try {
			lock.lock();
			try {
				LogicalReader reader = get(name);
				try {
					reader.update(spec, persist);
				} catch (ImplementationException | InUseException
						| ValidationException e) {
					e.setReason("Reader '" + name + "': " + e.getReason());
					throw e;
				}
			} finally {
				lock.unlock();
			}
		} catch (ImplementationException | InUseException | ValidationException
				| NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.UpdateFailed, "Update failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Removes the logical reader
	 * 
	 * @param name
	 *            The name of the logical reader to remove
	 * @param persist
	 *            Persist changes
	 * @throws ImplementationException
	 * @throws NoSuchNameException
	 * @throws InUseException
	 * @throws ImmutableReaderException
	 * @throws NoSuchIdException
	 */
	public void undefine(String name, boolean persist)
			throws ImplementationException, NoSuchNameException,
			InUseException, ImmutableReaderException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.Undefine, "Undefine {0}", name);
		try {
			lock.lock();
			try {
				LogicalReader reader = get(name);
				try {
					reader.undefine();
					readers.remove(name);
					if (persist)
						depot.remove(name);
				} catch (InUseException e) {
					e.setReason("Reader '" + name + "': " + e.getReason());
					throw e;
				} catch (ImplementationException e) {
					readers.remove(name);
					throw e;
				}
			} finally {
				lock.unlock();
			}
		} catch (ImplementationException | NoSuchNameException | InUseException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.UndefineFailed, "Undefine failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Requests an unordered list of the names of all logical reader.
	 * 
	 * @return The request list of all logical reader names
	 * @throws ImplementationException
	 */
	public List<String> getNames() throws ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.GetLRSpecNames, "Get LR spec names");
		lock.lock();
		try {
			return new ArrayList<String>(readers.keySet());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Requests a specification that describes the logical reader by name
	 * 
	 * @param name
	 *            The name of the logical reader to request from
	 * @return The requested LRSpec
	 * @throws NoSuchNameException
	 */
	public LRSpec getSpec(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.GetLRSpec, "Get LR spec {0}", name);
		try {
			lock.lock();
			try {
				return get(name).getSpec();
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.GetLRSpecFailed, "Get LR spec failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Adds the specified logical readers to the list of component readers for
	 * the composite logical reader
	 * 
	 * @param name
	 *            The name of the logical reader to add to
	 * @param readers
	 *            The list of logical reader names to add as component
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 * @throws NonCompositeReaderException
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws ValidationException
	 * @throws ReaderLoopException
	 * @throws ImmutableReaderException
	 */
	public void addReaders(String name, List<String> readers, boolean persist)
			throws NoSuchNameException, NonCompositeReaderException,
			ImplementationException, InUseException, ValidationException,
			ReaderLoopException, ImmutableReaderException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.AddReaders, "Add readers {0} {1}", new Object[] { name, readers });
		try {
			lock.lock();
			try {
				LogicalReader reader = get(name);
				try {
					reader.add(readers, persist);
				} catch (NonCompositeReaderException e) {
					e.setReason("Reader '" + name + "': " + e.getReason());
					throw e;
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException | NonCompositeReaderException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.AddReadersFailed, "Add readers failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Sets the list of component readers for the composite logical reader to
	 * the specified list.
	 * 
	 * @param name
	 *            The name of the logical reader to set readers
	 * @param readers
	 *            The list of logical reader names to set
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 * @throws NonCompositeReaderException
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws ValidationException
	 * @throws ReaderLoopException
	 * @throws ImmutableReaderException
	 */
	public void setReaders(String name, List<String> readers, boolean persist)
			throws NoSuchNameException, NonCompositeReaderException,
			ImplementationException, InUseException, ValidationException,
			ReaderLoopException, ImmutableReaderException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.SetReaders, "Set readers {0} {1}", new Object[] { name, readers });
		try {
			lock.lock();
			try {
				LogicalReader reader = get(name);
				try {
					reader.set(readers, persist);
				} catch (NonCompositeReaderException e) {
					e.setReason("Reader '" + name + "': " + e.getReason());
					throw e;
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException | NonCompositeReaderException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.SetReadersFailed, "Set readers failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Removes the specified logical readers from the list of component readers
	 * for the composite logical reader
	 * 
	 * @param name
	 *            The name of the logical reader to remove readers from
	 * @param readers
	 *            The list of logical reader names to be removed
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 * @throws NonCompositeReaderException
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws ImmutableReaderException
	 * @throws ValidationException
	 * @throws ReaderLoopException
	 */
	public void removeReaders(String name, List<String> readers, boolean persist)
			throws NoSuchNameException, NonCompositeReaderException,
			ImplementationException, InUseException, ImmutableReaderException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.RemoveReaders, "Remove readers {0} {1}", new Object[] { name, readers });
		try {
			lock.lock();
			try {
				LogicalReader reader = get(name);
				try {
					reader.remove(readers, persist);
				} catch (NonCompositeReaderException e) {
					e.setReason("Reader '" + name + "': " + e.getReason());
					throw e;
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException | NonCompositeReaderException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.RemoveReadersFailed, "Remove readers failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Sets the properties for the logical reader to the specified list.
	 * 
	 * @param name
	 *            The name of the logical reader to set properties to
	 * @param properties
	 *            The list of properties to set
	 * @param persist
	 *            Persist changes
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws ValidationException
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException
	 * @throws ReaderLoopException
	 */
	public void setProperties(String name, List<LRProperty> properties,
			boolean persist) throws ImplementationException, InUseException,
			ValidationException, NoSuchNameException, ImmutableReaderException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.SetProperties, "Set properties {0} {1}", new Object[] { name, properties });
		try {
			lock.lock();
			try {
				LogicalReader reader = get(name);
				try {
					reader.setProperties(properties, persist);
				} catch (ImplementationException | InUseException
						| ValidationException e) {
					e.setReason("Reader '" + name + "': " + e.getReason());
					throw e;
				}
			} finally {
				lock.unlock();
			}
		} catch (ImplementationException | InUseException | ValidationException
				| NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.SetPropertiesFailed, "Set properties failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets the current value of the specified property for the specified
	 * reader, or null if the specified reader does not have a property with the
	 * specified name.
	 * 
	 * @param name
	 *            The name of the logical reader to request a property from
	 * @param property
	 *            The name of the property to request
	 * @return Property value
	 * @throws NoSuchNameException
	 */
	public String getPropertyValue(String name, String property)
			throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.LR.Name, Exits.Service.LR.GetPropertyValue, "Get property value {0} {1}", new Object[] { name, property });
		try {
			lock.lock();
			try {
				return get(name).getPropertyValue(property);
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.LR.Name, Exits.Service.LR.GetPropertyValueFailed, "Get property value failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Locks the named logical reader and returns its instance
	 * 
	 * @param name
	 *            The logical reader name
	 * @return The instance of the logical reader
	 * @throws NoSuchNameException
	 */
	public LogicalReader lock(String name) throws NoSuchNameException {
		lock.lock();
		try {
			LogicalReader logicalReader = get(name);
			logicalReader.lock();
			return logicalReader;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns the reader configuration
	 * 
	 * @param name
	 *            The reader name
	 * @return The reader configuration
	 * @throws ImplementationException
	 * @throws NoSuchNameException
	 * @throws NonBaseReaderException
	 */
	public RCConfig getConfig(String name) throws ImplementationException,
			NoSuchNameException, NonBaseReaderException {
		lock.lock();
		try {
			return get(name).getConfig();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Get the logical reader named <paramref name="name"/>.
	 * 
	 * @param name
	 *            The name of the requested logical reader
	 * @return The requested logical reader instance
	 * @throws NoSuchNameException
	 *             if a reader does not exist
	 */
	LogicalReader get(String name) throws NoSuchNameException {
		LogicalReader logicalReader = readers.get(name);
		if (logicalReader != null) {
			return logicalReader;
		} else {
			throw new NoSuchNameException("The reader '" + name
					+ "' could not be found!");
		}
	}

	/**
	 * Gets the standard version
	 * 
	 * @return The standard version
	 */
	public static String getStandardVersion() {
		return Config.getInstance().getService().getLr().getVersion()
				.getStandard();
	}

	/**
	 * Disposes instance
	 * 
	 * @throws ImplementationException
	 */
	public void dispose() {
		lock.lock();
		try {
			ListIterator<Map.Entry<String, LogicalReader>> it = new ArrayList<>(
					readers.entrySet()).listIterator(readers.size());
			while (it.hasPrevious()) {
				Map.Entry<String, LogicalReader> entry = it.previous();
				try {
					entry.getValue().undefine();
				} catch (ALEException e) {
					Exits.Log.logp(Exits.Level.Error, Exits.Common.Name, Exits.Common.Error, "Failed to dispose logical reader: " + e.getMessage(), e);
				}
			}
		} finally {
			lock.unlock();
		}
	}
}