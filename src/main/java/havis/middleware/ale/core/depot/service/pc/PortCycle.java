package havis.middleware.ale.core.depot.service.pc;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.PortCycleType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.service.Cycle;
import havis.middleware.ale.core.manager.PC;
import havis.middleware.ale.service.mc.MCPortCycleSpec;
import havis.middleware.ale.service.pc.PCSpec;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Implements the port cycle depot
 */
public class PortCycle extends Cycle<PortCycleType, MCPortCycleSpec, PCSpec> {

	private static PortCycle instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new PortCycle();
	}

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static PortCycle getInstance() {
		return instance;
	}

	/**
	 * Provides the entries
	 * 
	 * @return The entry list
	 */
	@Override
    protected List<PortCycleType> getList() {
		return Config.getInstance().getService().getPc().getPortCycles()
				.getPortCycle();
	}

	/**
	 * Returns the entry by specification
	 * 
	 * @param spec
	 *            The specification
	 * @return The entry
	 */
	@Override
    protected PortCycleType get(final MCPortCycleSpec spec) {
		return new PortCycleType(spec.getName(), spec.isEnable(),
				spec.getSpec());
	}

	/**
	 * Return the specification by entry
	 * 
	 * @param entry
	 *            The entry
	 * @return The specification
	 */
	@Override
    protected MCPortCycleSpec get(final PortCycleType entry) {
		return new MCPortCycleSpec(entry.getName(), entry.isEnable(),
				entry.getSpec());
	}

	/**
	 * Creates a new instance and adds a subscriber to each entry
	 */
	protected PortCycle() {
		super();
		for (Entry<UUID, PortCycleType> pair : this.dict.entrySet()) {
			subscribers.put(pair.getKey(), new Subscriber(pair.getValue()
					.getName(), pair.getValue().getSubscribers()
					.getSubscriber()));
		}
	}

	/**
	 * Defines the entry
	 * 
	 * @param entry
	 *            The entry
	 * @throws ImplementationException
	 * @throws DuplicateNameException
	 * @throws ValidationException
	 */
	@Override
    protected void define(PortCycleType entry) throws ValidationException,
			DuplicateNameException, ImplementationException {
		PC.getInstance().define(entry.getName(), entry.getSpec(), false);
	}

	/**
	 * Un-defines the entry
	 * 
	 * @param entry
	 *            The entry
	 * @throws NoSuchNameException
	 */
	@Override
    protected void undefine(PortCycleType entry) throws NoSuchNameException {
		PC.getInstance().undefine(entry.getName(), false);
	}

	/**
	 * Adds a new entry with empty subscriber list and returns the new created
	 * id. Serializes the configuration
	 * 
	 * @param entry
	 *            The entry
	 * @return The unique id
	 */
	@Override
    protected UUID add(PortCycleType entry) {
		UUID guid = super.add(entry);
		subscribers.put(guid, new Subscriber(entry.getName(), entry
				.getSubscribers().getSubscriber()));
		return guid;
	}

	/**
	 * Adds a new entry
	 * 
	 * @param name
	 *            The name
	 * @param spec
	 *            The specification
	 */
	public void add(final String name, final PCSpec spec) {
		add(new PortCycleType(name, true, spec));
	}
}