package havis.middleware.ale.core.depot.service.ec;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.EventCycleType;
import havis.middleware.ale.config.SubscribersType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.service.Cycle;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.ale.service.mc.MCEventCycleSpec;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Implements the event cycle depot
 */
public class EventCycle extends Cycle<EventCycleType, MCEventCycleSpec, ECSpec> {

	private static EventCycle instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new EventCycle();
	}

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static EventCycle getInstance() {
		return instance;
	}

	/**
	 * Retrieves the configuration entries
	 * 
	 * @return The entry list
	 */
	@Override
    protected List<EventCycleType> getList() {
		return Config.getInstance().getService().getEc().getEventCycles()
				.getEventCycle();
	}

	/**
	 * Returns the configuration by specification
	 * 
	 * @param spec
	 *            The specification
	 * @return The configuration entry
	 */
	@Override
    protected EventCycleType get(final MCEventCycleSpec spec) {
		return new EventCycleType(spec.getName(), spec.isEnable(),
				spec.getSpec());
	}

	/**
	 * Returns the specification by entry
	 * 
	 * @param entry
	 *            The configuration entry
	 * @return The specification
	 */
	@Override
    protected MCEventCycleSpec get(final EventCycleType entry) {
		return new MCEventCycleSpec(entry.getName(), entry.isEnable(),
				entry.getSpec());
	}

	/**
	 * Creates a new instance and adds a subscriber to each entry
	 */
	protected EventCycle() {
		super();
		for (Entry<UUID, EventCycleType> pair : this.dict.entrySet()) {
			SubscribersType subscribers = pair.getValue().getSubscribers();
			this.subscribers.put(pair.getKey(), new Subscriber(pair.getValue()
					.getName(), subscribers.getSubscriber()));
		}
	}

	/**
	 * Defines the entry
	 * 
	 * @param entry
	 *            The entry
	 */
	@Override
    protected void define(EventCycleType entry) throws ImplementationException,
			ValidationException, DuplicateNameException {
		havis.middleware.ale.core.manager.EC.getInstance().define(
				entry.getName(), entry.getSpec(), false);
	}

	/**
	 * Un-defines the entry
	 * 
	 * @param entry
	 *            The entry
	 */
	@Override
    protected void undefine(EventCycleType entry) throws NoSuchNameException {
		havis.middleware.ale.core.manager.EC.getInstance().undefine(
				entry.getName(), false);
	}

	/**
	 * Adds a new entry with empty subscriber list and returns the new created
	 * id. Serializes the configuration
	 * 
	 * @param entry
	 *            The entry
	 * @return The id
	 */
	@Override
    protected UUID add(EventCycleType entry) {
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
	public void add(final String name, final ECSpec spec) {
		add(new EventCycleType(name, true, spec));
	}
}