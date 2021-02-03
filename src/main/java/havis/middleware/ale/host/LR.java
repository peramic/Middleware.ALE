package havis.middleware.ale.host;

import havis.middleware.ale.base.annotation.EndpointMetadata;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonCompositeReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.lr.AddReaders;
import havis.middleware.ale.service.lr.AddReadersResult;
import havis.middleware.ale.service.lr.ArrayOfString;
import havis.middleware.ale.service.lr.Define;
import havis.middleware.ale.service.lr.DefineResult;
import havis.middleware.ale.service.lr.DuplicateNameExceptionResponse;
import havis.middleware.ale.service.lr.EmptyParms;
import havis.middleware.ale.service.lr.GetLRSpec;
import havis.middleware.ale.service.lr.GetPropertyValue;
import havis.middleware.ale.service.lr.ImmutableReaderExceptionResponse;
import havis.middleware.ale.service.lr.ImplementationExceptionResponse;
import havis.middleware.ale.service.lr.InUseExceptionResponse;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.lr.NoSuchNameExceptionResponse;
import havis.middleware.ale.service.lr.NonCompositeReaderExceptionResponse;
import havis.middleware.ale.service.lr.ReaderLoopExceptionResponse;
import havis.middleware.ale.service.lr.RemoveReaders;
import havis.middleware.ale.service.lr.RemoveReadersResult;
import havis.middleware.ale.service.lr.SecurityExceptionResponse;
import havis.middleware.ale.service.lr.SetProperties;
import havis.middleware.ale.service.lr.SetPropertiesResult;
import havis.middleware.ale.service.lr.SetReaders;
import havis.middleware.ale.service.lr.SetReadersResult;
import havis.middleware.ale.service.lr.Undefine;
import havis.middleware.ale.service.lr.UndefineResult;
import havis.middleware.ale.service.lr.Update;
import havis.middleware.ale.service.lr.UpdateResult;
import havis.middleware.ale.service.lr.ValidationExceptionResponse;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

@WebService(targetNamespace = "urn:epcglobal:alelr:wsdl:1", portName = "ALELRServicePort", serviceName = "ALELRService", endpointInterface = "havis.middleware.ale.service.lr.ALELRServicePortType", wsdlLocation = "havis/middleware/wsdl/EPCglobal-ale-1_1-alelr.wsdl")
@EndpointMetadata(xsdLocations = { "havis/middleware/wsdl/EPCglobal-ale-1_1-alelr.xsd", "havis/middleware/wsdl/EPCglobal.xsd" })
public class LR {

	@Resource
	WebServiceContext context;

	havis.middleware.ale.server.LR server = havis.middleware.ale.server.LR
			.getInstance();

	public DefineResult define(Define parms) throws Exception {
		try {
			server.define(parms.getName(), parms.getSpec());
			return new DefineResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UpdateResult update(Update parms) throws Exception {
		try {
			server.update(parms.getName(), parms.getSpec());
			return new UpdateResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UndefineResult undefine(Undefine parms) throws Exception {
		try {
			server.undefine(parms.getName());
			return new UndefineResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getLogicalReaderNames(EmptyParms parms)
			throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getNames();
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public LRSpec getLRSpec(GetLRSpec parms) throws Exception {
		try {
			return server.getSpec(parms.getName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public AddReadersResult addReaders(AddReaders parms) throws Exception {
		try {
			server.addReaders(parms.getName(), parms.getReaders() != null ? parms.getReaders().getReader() : null);
			return new AddReadersResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public SetReadersResult setReaders(SetReaders parms) throws Exception {
		try {
			server.setReaders(parms.getName(), parms.getReaders() != null ? parms.getReaders().getReader() : null);
			return new SetReadersResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public RemoveReadersResult removeReaders(RemoveReaders parms)
			throws Exception {
		try {
			server.removeReaders(parms.getName(), parms.getReaders() != null ? parms.getReaders().getReader() : null);
			return new RemoveReadersResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public SetPropertiesResult setProperties(SetProperties parms)
			throws Exception {
		try {
			server.setProperties(parms.getName(), parms.getProperties()
					.getProperty());
			return new SetPropertiesResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public String getPropertyValue(GetPropertyValue parms) throws Exception {
		try {
			return server.getPropertyValue(parms.getName(),
					parms.getPropertyName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public String getStandardVersion(EmptyParms parms) throws Exception {
		try {
			return server.getStandardVersion();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public String getVendorVersion(EmptyParms parms) throws Exception {
		try {
			return Main.getVendorVersionUrl(this.context);
		} catch (Exception e) {
			throw exception(e);
		}
	}

	private Exception exception(Exception exception) {
		try {
			throw exception;
		} catch (SecurityException e) {
			return new SecurityExceptionResponse(null, removeStrackTrace(e));
		} catch (DuplicateNameException e) {
			return new DuplicateNameExceptionResponse(null, removeStrackTrace(e));
		} catch (ValidationException e) {
			return new ValidationExceptionResponse(null, removeStrackTrace(e));
		} catch (InUseException e) {
			return new InUseExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchNameException e) {
			return new NoSuchNameExceptionResponse(null, removeStrackTrace(e));
		} catch (ImmutableReaderException e) {
			return new ImmutableReaderExceptionResponse(null, removeStrackTrace(e));
		} catch (NonCompositeReaderException e) {
			return new NonCompositeReaderExceptionResponse(null, removeStrackTrace(e));
		} catch (ReaderLoopException e) {
			return new ReaderLoopExceptionResponse(null, removeStrackTrace(e));
		} catch (ImplementationException e) {
			return new ImplementationExceptionResponse(null, removeStrackTrace(e));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private <T extends ALEException> T removeStrackTrace(T exception) {
		exception.setStackTrace(new StackTraceElement[0]);
		return exception;
	}

    @PreDestroy
    private void dispose() {
        server.dispose();
    }
}