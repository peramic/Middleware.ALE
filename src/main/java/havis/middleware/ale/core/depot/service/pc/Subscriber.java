package havis.middleware.ale.core.depot.service.pc;

import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.core.manager.PC;

import java.util.List;

/**
 * Implements the port cycle subscriber depot
 */
public class Subscriber extends havis.middleware.ale.core.depot.service.Subscriber {

    /**
     * Creates a new instance with given name and subscriber list
     *
     * @param name
     *            The name
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
     * @throws ImplementationException
     * @throws ValidationException
     * @throws DuplicateSubscriptionException
     */
    @Override
    public void subscribe(SubscriberType entry) throws InvalidURIException, DuplicateSubscriptionException, ValidationException, ImplementationException,
            NoSuchNameException {
        PC.getInstance().subscribe(name, entry.getUri(), entry.getProperties(), false);
    }

    /**
     * Un-subscribes the entry
     *
     * @param entry
     *            The entry
     * @throws NoSuchNameException
     * @throws NoSuchSubscriberException
     */
    @Override
    public void unsubscribe(SubscriberType entry) throws InvalidURIException, NoSuchSubscriberException, NoSuchNameException {
        PC.getInstance().unsubscribe(name, entry.getUri(), false);
    }
}