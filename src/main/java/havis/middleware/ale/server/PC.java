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
import havis.middleware.ale.service.pc.PCOpSpec;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.service.pc.PCSpec;

import java.util.List;

public class PC {

    private static Security security = Security.getInstance();
    private static PC instance = new PC();

    private PC() {
    }

    public static PC getInstance() {
        return instance;
    }

    private static havis.middleware.ale.core.manager.PC getManager() {
        return havis.middleware.ale.core.manager.PC.getInstance();
    }

    public void define(String name, PCSpec spec) throws DuplicateNameException, ImplementationException, ValidationException, SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_define);
        getManager().define(name, spec, true);
    }

    public void undefine(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_undefine);
        getManager().undefine(name, true);
    }

    public PCSpec getSpec(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_getPCSpec);
        return getManager().getSpec(name);
    }

    public List<String> getNames() throws ImplementationException, SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_getPCSpecNames);
        return getManager().getNames();
    }

    public void subscribe(String name, String uri) throws DuplicateSubscriptionException, ImplementationException, InvalidURIException, NoSuchNameException,
            SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_subscribe);
        getManager().subscribe(name, uri, null, true);
    }

    public void unsubscribe(String name, String uri) throws ImplementationException, InvalidURIException, NoSuchNameException, NoSuchSubscriberException,
            SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_unsubscribe);
        getManager().unsubscribe(name, uri, true);
    }

    public PCReports poll(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_poll);
        return getManager().poll(name);
    }

    public PCReports immediate(PCSpec spec) throws ImplementationException, ValidationException, SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_immediate);
        return getManager().immediate(spec);
    }

    public java.util.List<String> getSubscribers(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_getSubscribers);
        return getManager().getSubscribers(name);
    }

    public java.util.List<havis.middleware.ale.service.pc.PCOpReport> execute(List<PCOpSpec> list) throws ImplementationException, ValidationException,
            SecurityException {
        security.isAllowed(Method.ALEPC, Method.ALEPC_execute);
        return getManager().execute(list);
    }

    public String getStandardVersion() throws ImplementationException {
        return havis.middleware.ale.core.manager.PC.getStandardVersion();
    }

    public void dispose() {
        getManager().dispose();
    }
}