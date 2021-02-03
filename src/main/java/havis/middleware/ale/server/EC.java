package havis.middleware.ale.server;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.security.Method;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;

import java.util.List;

public class EC {

    private static Security security = Security.getInstance();
    private static EC instance = new EC();

    private EC() {
    }

    public static EC getInstance() {
        return instance;
    }

    private static havis.middleware.ale.core.manager.EC getManager() {
        return havis.middleware.ale.core.manager.EC.getInstance();
    }

    public void define(String name, ECSpec spec) throws DuplicateNameException, ValidationException, ImplementationException, SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_define);
        getManager().define(name, spec, true);
    }

    public void undefine(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_undefine);
        getManager().undefine(name, true);
    }

    public ECSpec getSpec(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_getECSpec);
        return getManager().getSpec(name);
    }

    public List<String> getNames() throws ImplementationException, SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_getECSpecNames);
        return getManager().getNames();
    }

    public void subscribe(String name, String uri) throws DuplicateSubscriptionException, ImplementationException, InvalidURIException, NoSuchNameException,
            SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_subscribe);
        getManager().subscribe(name, uri, null, true);
    }

    public void unsubscribe(String name, String uri) throws ImplementationException, InvalidURIException, NoSuchNameException, NoSuchSubscriberException,
            SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_subscribe);
        getManager().unsubscribe(name, uri, true);
    }

    public ECReports poll(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_poll);
        return getManager().poll(name);
    }

    public ECReports immediate(ECSpec spec) throws ImplementationException, ValidationException, SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_immediate);
        return getManager().immediate(spec);
    }

    public java.util.List<String> getSubscribers(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALE, Method.ALE_getSubscribers);
        return getManager().getSubscribers(name);
    }

    public String getStandardVersion() {
        return havis.middleware.ale.core.manager.EC.getStandardVersion();
    }

    public void dispose() {
        getManager().dispose();
    }
}