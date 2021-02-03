package havis.middleware.ale.core.depot.service.lr;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.LogicalReaderType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.Depot;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.mc.MCLogicalReaderSpec;
import havis.middleware.ale.service.mc.MCSpec;

import java.util.List;
import java.util.UUID;

/**
 * Implements the logical reader depot
 */
public class LogicalReader extends
		Depot<LogicalReaderType, MCLogicalReaderSpec> {

	private static LogicalReader instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new LogicalReader();
	}

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static LogicalReader getInstance() {
		return instance;
	}

	/**
	 * Provides the entries
	 * 
	 * @return The entries
	 */
	@Override
	protected List<LogicalReaderType> getList() {
		return Config.getInstance().getService().getLr().getLogicalReaders()
				.getLogicalReader();
	}

	/**
	 * Adds a new entry to depot
	 * 
	 * @return The unique identifier of the added entry
	 */
	@Override
	protected UUID add(LogicalReaderType entry) {
		return super.add(entry);
	}

	/**
	 * Returns the entry by specification
	 * 
	 * @param spec
	 *            The specification
	 * @return The entry
	 */
	@Override
	protected LogicalReaderType get(MCLogicalReaderSpec spec) {
		return new LogicalReaderType(spec.getName(), spec.isEnable(),
				spec.getSpec());
	}

	/**
	 * Returns the specification by entry
	 * 
	 * @param entry
	 *            The entry
	 * @return The specification
	 */
	@Override
	protected MCLogicalReaderSpec get(LogicalReaderType entry) {
		return new MCLogicalReaderSpec(entry.getName(), entry.isEnable(),
				entry.getSpec());
	}

	/**
	 * Defines the entry
	 * 
	 * @param entry
	 *            The entry
	 * @throws ValidationException
	 * @throws DuplicateNameException
	 * @throws ReaderLoopException
	 * @throws ImplementationException
	 * @throws ImmutableReaderException
	 */
	void define(LogicalReaderType entry) throws ValidationException,
			DuplicateNameException, ReaderLoopException,
			ImplementationException, ImmutableReaderException {
		LR.getInstance().define(entry.getName(), entry.getSpec(), false);
	}

	/**
	 * Un-defines the entry
	 * 
	 * @param entry
	 *            The entry
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws NoSuchIdException
	 * @throws ImmutableReaderException
	 */
	void undefine(LogicalReaderType entry) throws ImplementationException,
			InUseException, NoSuchIdException, ImmutableReaderException {
		try {
			LR.getInstance().undefine(entry.getName(), false);
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Logical reader not found: " + e.getMessage(), e);
		}
	}

	/**
	 * Sets the enable state
	 * 
	 * @param entry
	 *            The entry
	 * @param enable
	 *            The enable state
	 * @throws NoSuchIdException
	 * @throws ImmutableReaderException
	 */
	@Override
	protected void setEnable(LogicalReaderType entry, boolean enable)
			throws ImplementationException, ValidationException,
			DuplicateNameException, ReaderLoopException, InUseException,
			NoSuchIdException, ImmutableReaderException {
		if (enable) {
			define(entry);
		} else {
			undefine(entry);
		}
	}

	/**
	 * Adds a new entry
	 * 
	 * @param name
	 *            The name
	 * @param spec
	 *            The specification
	 */
	public void add(final String name, final LRSpec spec) {
		super.add(new LogicalReaderType(name, true, spec));
	}

	/**
	 * Adds a new entry and doesn't persist the configuration
	 * 
	 * @param name
	 *            The name
	 * @param spec
	 *            The specification
	 */
	public void addNonPersistent(final String name, final LRSpec spec) {
		super.add(new LogicalReaderType(name, true, spec), false);
	}

	/**
	 * Updates an existing entry
	 * 
	 * @param name
	 *            The name
	 * @param spec
	 *            The specification
	 * @throws NoSuchIdException
	 */
	public void update(String name, LRSpec spec) throws NoSuchIdException {
		UUID id = getId(name);
		if (id != null) {
			LogicalReaderType reader = getType(id);
			if (reader != null) {
				reader.setSpec(spec);
			}
		}
	}

	/**
	 * Updates the logical reader specification
	 * 
	 * @param spec
	 *            The specification
	 * @param entry
	 *            The entry
	 * @return True if specification is valid, false otherwise
	 * @throws ALEException
	 */
	boolean update(MCLogicalReaderSpec spec, LogicalReaderType entry)
			throws ALEException {
		if ((spec.getSpec() != null)) {
			LR.getInstance().update(entry.getName(), spec.getSpec(), false);
			entry.setSpec(spec.getSpec());
			Config.serialize();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Updates the given specification
	 * 
	 * @param id
	 *            The unique id
	 * @param spec
	 *            The specification
	 * @throws ALEException
	 */
	@Override
	public void update(UUID id, MCSpec spec) throws ALEException {
		LogicalReaderType entry;
		if (!(spec.isEnable()
				&& (entry = getType(id)) != null
				&& entry.isEnable()
				&& ((spec.getName() == null) || (spec.getName().equals(entry
						.getName()))) && (spec instanceof MCLogicalReaderSpec)
		/* all checks passed, call internal method */
		&& update((MCLogicalReaderSpec) spec, entry)
		/* will call super method below if false is returned */)) {
			super.update(id, spec);
		}
	}
}