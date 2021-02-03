package havis.middleware.ale.core.depot.service.ec;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.SubscriberType;

import java.util.List;

/**
 * Implements the event cycle subscriber depot
 */
public class Subscriber extends
		havis.middleware.ale.core.depot.service.Subscriber {

	/**
	 * Creates a new instance
	 *
	 * @param name
	 *            The cycle name
	 * @param subscribers
	 *            The subscriber list
	 */
	public Subscriber(String name, List<SubscriberType> subscribers) {
		super(name, subscribers);
	}

	/**
	 * Subscribes the entry
	 *
	 * @param entry
	 *            The entry
	 * @throws NoSuchNameException
	 * @throws ValidationException
	 * @throws DuplicateNameException
	 * @throws ImplementationException
	 * @throws InvalidURIException
	 */
	@Override
    public void subscribe(SubscriberType entry) throws InvalidURIException,
			ImplementationException, DuplicateSubscriptionException,
			ValidationException, NoSuchNameException {
		havis.middleware.ale.core.manager.EC.getInstance().subscribe(name, entry.getUri(), entry.getProperties(), false);
	}

	/**
	 * Un-subscribes the entry
	 *
	 * @param entry
	 *            The entry
	 * @throws NoSuchNameException
	 * @throws NoSuchSubscriberException
	 * @throws InvalidURIException
	 */
	@Override
    public void unsubscribe(SubscriberType entry) throws InvalidURIException,
			NoSuchSubscriberException, NoSuchNameException {
		havis.middleware.ale.core.manager.EC.getInstance().unsubscribe(name,
				entry.getUri(), false);
	}
}