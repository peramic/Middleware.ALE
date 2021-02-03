package havis.middleware.ale.core.depot.service.tm;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.FieldType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.Depot;
import havis.middleware.ale.core.manager.TM;
import havis.middleware.ale.service.mc.MCTagMemorySpec;
import havis.middleware.ale.service.tm.TMSpec;

import java.util.List;

/**
 * Implements the tag memory depot
 */
public class TagMemory extends Depot<FieldType, MCTagMemorySpec> {

	private static TagMemory instance;
	static {
		reset();
	}
	
	public static void reset() {
		instance = new TagMemory();
	}

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static TagMemory getInstance() {
		return instance;
	}

	/**
	 * Retrieves the entries
	 * 
	 * @return The entry list
	 */
	@Override
    protected List<FieldType> getList() {
		return Config.getInstance().getService().getTm().getFields().getField();
	}

	/**
	 * Returns the entry by specification
	 * 
	 * @param spec
	 *            The specification
	 * @return The entry
	 */
	@Override
    protected FieldType get(final MCTagMemorySpec spec) {
		return new FieldType(spec.getName(), spec.isEnable(), spec.getSpec());
	}

	/**
	 * Returns the specification by entry
	 * 
	 * @param entry
	 *            The entry
	 * @return The specification
	 */
	@Override
    protected MCTagMemorySpec get(final FieldType entry) {
		return new MCTagMemorySpec(entry.getName(), entry.isEnable(),
				entry.getSpec());
	}

	/**
	 * Defines an entry
	 * 
	 * @param entry
	 *            The entry
	 * @throws ImplementationException
	 * @throws ValidationException
	 * @throws DuplicateNameException
	 */
	void define(FieldType entry) throws DuplicateNameException,
			ValidationException, ImplementationException {
		TM.getInstance().define(entry.getName(), entry.getSpec(), false);
	}

	/**
	 * Un-defines an entry
	 * 
	 * @param entry
	 *            The entry
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws NoSuchNameException
	 */
	void undefine(FieldType entry) throws NoSuchNameException, InUseException,
			ImplementationException {
		TM.getInstance().undefine(entry.getName(), false);
	}

	/**
	 * Sets the enable state
	 * 
	 * @param entry
	 *            The entry
	 * @param enable
	 *            The enable state
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws NoSuchNameException
	 * @throws ValidationException
	 * @throws DuplicateNameException
	 */
	@Override
    protected void setEnable(FieldType entry, boolean enable)
			throws NoSuchNameException, InUseException,
			ImplementationException, DuplicateNameException,
			ValidationException {
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
	public void add(final String name, TMSpec spec) {
		super.add(new FieldType(name, true, spec));
	}
}