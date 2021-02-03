package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ParameterForbiddenException;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.core.manager.CC;

import java.util.List;

/**
 * Implements the command cycle subscriber depot
 */
class Subscriber extends havis.middleware.ale.core.depot.service.Subscriber {

	/**
	 * Creates a new instance with given name and subscriber list
	 *
	 * @param name
	 *            The cycle name
	 * @param subscribers
	 *            The subscribers
	 */
	Subscriber(String name, List<SubscriberType> subscribers) {
		super(name, subscribers);
	}

	/**
	 * Subscribes the entry
	 *
	 * @param entry
	 *            The entry
	 * @throws NoSuchNameException
	 * @throws ImplementationException
	 * @throws DuplicateSubscriptionException
	 * @throws ParameterForbiddenException
	 * @throws InvalidURIException
	 */
	@Override
    public void subscribe(SubscriberType entry) throws InvalidURIException,
			ParameterForbiddenException, DuplicateSubscriptionException,
			ImplementationException, NoSuchNameException {
		CC.getInstance().subscribe(name, entry.getUri(), entry.getProperties(), false);
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
		CC.getInstance().unsubscribe(name, entry.getUri(), false);
	}
}