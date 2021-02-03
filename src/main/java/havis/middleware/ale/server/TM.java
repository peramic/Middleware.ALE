package havis.middleware.ale.server;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.security.Method;
import havis.middleware.ale.service.tm.TMSpec;

import java.util.List;

public class TM {

    private static Security security = Security.getInstance();
    private static TM instance = new TM();

    private TM() {
    }

    public static TM getInstance() {
        return instance;
    }

    private static havis.middleware.ale.core.manager.TM getManager() {
        return havis.middleware.ale.core.manager.TM.getInstance();
    }

    public void define(String name, TMSpec spec) throws DuplicateNameException, ImplementationException, SecurityException, ValidationException {
        security.isAllowed(Method.ALETM, Method.ALETM_defineTMSpec);
        getManager().define(name, spec, true);
    }

    public void undefine(String name) throws ImplementationException, InUseException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALETM, Method.ALETM_undefineTMSpec);
        getManager().undefine(name, true);
    }

    public TMSpec getSpec(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALETM, Method.ALETM_getTMSpec);
        return getManager().getSpec(name);
    }

    public List<String> getNames() throws ImplementationException, SecurityException {
        security.isAllowed(Method.ALETM, Method.ALETM_getTMSpecNames);
        return getManager().getNames();
    }

    public String getStandardVersion() throws ImplementationException {
        return havis.middleware.ale.core.manager.TM.getStandardVersion();
    }

    public void dispose() {
        getManager().dispose();
    }
}