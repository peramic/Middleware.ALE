package havis.middleware.ale.server;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonCompositeReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.security.Method;
import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;

import java.util.List;

public class LR {

    private static Security security = Security.getInstance();
    private static LR instance = new LR();

    private LR() {
    }

    public static LR getInstance() {
        return instance;
    }

    private static havis.middleware.ale.core.manager.LR getManager() {
        return havis.middleware.ale.core.manager.LR.getInstance();
    }

    public void define(String name, LRSpec spec) throws DuplicateNameException, ImplementationException, SecurityException, ValidationException, ImmutableReaderException {
        security.isAllowed(Method.ALELR, Method.ALELR_define);
        getManager().define(name, spec, true);
    }

    public void update(String name, LRSpec spec) throws ImmutableReaderException, ImplementationException, InUseException, NoSuchNameException,
            ReaderLoopException, SecurityException, ValidationException {
        security.isAllowed(Method.ALELR, Method.ALELR_update);
        getManager().update(name, spec, true);
    }

    public void undefine(String name) throws ImmutableReaderException, ImplementationException, InUseException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALELR, Method.ALELR_undefine);
        getManager().undefine(name, true);
    }

    public java.util.List<String> getNames() throws ImplementationException, SecurityException {
        security.isAllowed(Method.ALELR, Method.ALELR_getLogicalReaderNames);
        return getManager().getNames();
    }

    public LRSpec getSpec(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALELR, Method.ALELR_getLRSpec);
        return getManager().getSpec(name);
    }

    public void addReaders(String name, List<String> readers) throws ImmutableReaderException, ImplementationException, InUseException, NoSuchNameException,
            NonCompositeReaderException, ReaderLoopException, SecurityException, ValidationException {
        security.isAllowed(Method.ALELR, Method.ALELR_addReaders);
        getManager().addReaders(name, readers, true);
    }

    public void setReaders(String name, List<String> readers) throws ImmutableReaderException, ImplementationException, InUseException, NoSuchNameException,
            NonCompositeReaderException, ReaderLoopException, SecurityException, ValidationException {
        security.isAllowed(Method.ALELR, Method.ALELR_setReaders);
        getManager().setReaders(name, readers, true);
    }

    public void removeReaders(String name, List<String> readers) throws ImmutableReaderException, ImplementationException, InUseException, NoSuchNameException,
            NonCompositeReaderException, SecurityException {
        security.isAllowed(Method.ALELR, Method.ALELR_removeReaders);
        getManager().removeReaders(name, readers, true);
    }

    public void setProperties(String name, List<LRProperty> properties) throws ImmutableReaderException, ImplementationException, InUseException,
            NoSuchNameException, SecurityException, ValidationException {
        security.isAllowed(Method.ALELR, Method.ALELR_setProperties);
        getManager().setProperties(name, properties, true);
    }

    public String getPropertyValue(String name, String property) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALELR, Method.ALELR_getPropertyValue);
        return getManager().getPropertyValue(name, property);
    }

    public String getStandardVersion() {
        return havis.middleware.ale.core.manager.LR.getStandardVersion();
    }

    public void dispose() {
        getManager().dispose();
    }
}