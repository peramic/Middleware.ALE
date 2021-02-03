package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.CommandCycleType;
import havis.middleware.ale.config.SubscribersType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.service.Cycle;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.ale.service.mc.MCCommandCycleSpec;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Implements the command cycle depot
 */
public class CommandCycle extends
		Cycle<CommandCycleType, MCCommandCycleSpec, CCSpec> {

	private static CommandCycle instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new CommandCycle();
	}

	/**
	 * Creates a new instance and adds a subscriber to each entry
	 */
	protected CommandCycle() {
		super();
		for (Entry<UUID, CommandCycleType> pair : this.dict.entrySet()) {
			SubscribersType subscribers = pair.getValue().getSubscribers();
			this.subscribers.put(pair.getKey(), new Subscriber(pair.getValue()
					.getName(), subscribers.getSubscriber()));
		}
	}

	/**
	 * Retrieves the static instance
	 *
	 * @return The static instance
	 */
	public static CommandCycle getInstance() {
		return instance;
	}

	/**
	 * Retrieves the entries
	 *
	 * @return The entry list
	 */
	@Override
    protected List<CommandCycleType> getList() {
		return Config.getInstance().getService().getCc().getCommandCycles()
				.getCommandCycle();
	}

	/**
	 * Returns the entry by specification
	 *
	 * @param spec
	 *            The specification
	 * @return The entry
	 */
	@Override
    protected CommandCycleType get(final MCCommandCycleSpec spec) {
		return new CommandCycleType(spec.getName(), spec.isEnable(),
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
    protected MCCommandCycleSpec get(final CommandCycleType entry) {
		return new MCCommandCycleSpec(entry.getName(), entry.isEnable(),
				entry.getSpec());
	}

	/**
	 * Define the entry
	 *
	 * @param entry
	 *            The entry
	 * @throws ImplementationException
	 * @throws DuplicateNameException
	 * @throws ValidationException
	 */
	@Override
    protected void define(CommandCycleType entry) throws ValidationException,
			DuplicateNameException, ImplementationException {
		havis.middleware.ale.core.manager.CC.getInstance().define(
				entry.getName(), entry.getSpec(), false);
	}

	/**
	 * Un-define the entry
	 *
	 * @param entry
	 *            The entry
	 * @throws NoSuchNameException
	 */
	@Override
    protected void undefine(CommandCycleType entry) throws NoSuchNameException {
		havis.middleware.ale.core.manager.CC.getInstance().undefine(
				entry.getName(), false);
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
    protected UUID add(CommandCycleType entry) {
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
	public void add(final String name, final CCSpec spec) {
		add(new CommandCycleType(name, true, spec));
	}
}