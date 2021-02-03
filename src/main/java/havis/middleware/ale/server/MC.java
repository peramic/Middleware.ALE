package havis.middleware.ale.server;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchPathException;
import havis.middleware.ale.base.exception.NoSuchPropertyException;
import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ParameterForbiddenException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.security.Method;
import havis.middleware.ale.service.IReports;
import havis.middleware.ale.service.cc.CCParameterListEntry;
import havis.middleware.ale.service.mc.MCPCOpSpecs;
import havis.middleware.ale.service.mc.MCProperty;
import havis.middleware.ale.service.mc.MCSpec;
import havis.middleware.ale.service.pc.PCOpReport;

import java.util.List;

public class MC implements havis.middleware.ale.service.mc.MC {

    private static Security security = Security.getInstance();
    private static MC instance = new MC();

    private MC() {
    }

    public static MC getInstance() {
        return instance;
    }

    private static havis.middleware.ale.core.manager.MC getManager() {
        return havis.middleware.ale.core.manager.MC.getInstance();
    }

    @Override
    public String add(String path, MCSpec spec, String parent) throws ImplementationException, ValidationException, NoSuchPathException, SecurityException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_add);
        return getManager().add(path, spec, parent);
    }

    @Override
    public String add(String path, MCSpec spec) throws ImplementationException, ValidationException, NoSuchPathException, SecurityException {
        return add(path, spec, null);
    }

    @Override
    public void remove(String path, String id, String parent) throws ImplementationException, ValidationException, NoSuchIdException, NoSuchPathException,
            SecurityException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_remove);
        getManager().remove(path, id, parent);
    }

    @Override
    public void remove(String path, String id) throws ImplementationException, ValidationException, NoSuchIdException, NoSuchPathException, SecurityException {
        remove(path, id, null);
    }

    @Override
    public void update(String path, String id, MCSpec spec, String parent) throws ImplementationException, ValidationException, NoSuchIdException,
            NoSuchPathException, SecurityException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_update);
        getManager().update(path, id, spec, parent);
    }

    @Override
    public void update(String path, String id, MCSpec spec) throws ImplementationException, ValidationException, NoSuchIdException, NoSuchPathException,
            SecurityException {
        update(path, id, spec, null);
    }

    @Override
    public MCSpec get(String path, String id, String parent) throws ImplementationException, NoSuchIdException, NoSuchPathException, SecurityException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_get);
        return getManager().get(path, id, parent);
    }

    @Override
    public MCSpec get(String path, String id) throws ImplementationException, NoSuchIdException, NoSuchPathException, SecurityException {
        return get(path, id, null);
    }

    @Override
    public MCSpec get(String path) throws ImplementationException, NoSuchIdException, NoSuchPathException, SecurityException {
        return get(path, null, null);
    }

    @Override
    public List<String> list(String path, String parent) throws ImplementationException, NoSuchIdException, NoSuchPathException, SecurityException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_list);
        return getManager().list(path, parent);
    }

    @Override
    public List<String> list(String path) throws ImplementationException, NoSuchIdException, NoSuchPathException, SecurityException {
        return list(path, null);
    }

    @Override
    public String getProperty(String name) throws ImplementationException, NoSuchPropertyException, SecurityException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_getProperty);
        return getManager().getProperty(name);
    }

	@Override
	public List<MCProperty> getProperties(List<String> properties) throws ImplementationException, NoSuchPropertyException, SecurityException {
		security.isAllowed(Method.HAVISMC, Method.HAVISMC_getProperty);
		return getManager().getProperties(properties);
	}

    @Override
    public void setProperty(String name, String value) throws ImplementationException, NoSuchPropertyException, SecurityException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_setProperty);
        getManager().setProperty(name, value);
    }

    @Override
    public String getStandardVersion() {
        return getManager().getStandardVersion();
    }

    @Override
    public String getVendorVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IReports execute(String path, String id) throws ImplementationException, NoSuchIdException, NoSuchPathException, SecurityException,
            NoSuchNameException, ValidationException, ParameterException, ParameterForbiddenException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_execute);
        return getManager().execute(path, id);
    }

    @Override
    public IReports execute(String path, String id, List<CCParameterListEntry> parameters) throws ImplementationException, NoSuchIdException, NoSuchPathException, SecurityException,
            NoSuchNameException, ValidationException, ParameterException, ParameterForbiddenException {
        security.isAllowed(Method.HAVISMC, Method.HAVISMC_execute);
        return getManager().execute(id, parameters);
    }

	@Override
	public List<PCOpReport> execute(MCPCOpSpecs specs) throws ImplementationException, SecurityException, ValidationException {
		security.isAllowed(Method.HAVISMC, Method.HAVISMC_execute);
		return getManager().execute(specs);
	}
}