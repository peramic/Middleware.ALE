package havis.middleware.ale.host;

import havis.middleware.ale.base.annotation.EndpointMetadata;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.ec.ArrayOfString;
import havis.middleware.ale.service.ec.Define;
import havis.middleware.ale.service.ec.DuplicateNameExceptionResponse;
import havis.middleware.ale.service.ec.DuplicateSubscriptionExceptionResponse;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.ale.service.ec.ECSpecValidationExceptionResponse;
import havis.middleware.ale.service.ec.EmptyParms;
import havis.middleware.ale.service.ec.GetECSpec;
import havis.middleware.ale.service.ec.GetSubscribers;
import havis.middleware.ale.service.ec.Immediate;
import havis.middleware.ale.service.ec.ImplementationExceptionResponse;
import havis.middleware.ale.service.ec.InvalidURIExceptionResponse;
import havis.middleware.ale.service.ec.NoSuchNameExceptionResponse;
import havis.middleware.ale.service.ec.NoSuchSubscriberExceptionResponse;
import havis.middleware.ale.service.ec.Poll;
import havis.middleware.ale.service.ec.SecurityExceptionResponse;
import havis.middleware.ale.service.ec.Subscribe;
import havis.middleware.ale.service.ec.Undefine;
import havis.middleware.ale.service.ec.Unsubscribe;
import havis.middleware.ale.service.ec.VoidHolder;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

@WebService(targetNamespace = "urn:epcglobal:ale:wsdl:1", portName = "ALEServicePort", serviceName = "ALEService", endpointInterface = "havis.middleware.ale.service.ec.ALEServicePortType", wsdlLocation = "havis/middleware/wsdl/EPCglobal-ale-1_1-ale.wsdl")
@EndpointMetadata(xsdLocations = { "havis/middleware/wsdl/EPCglobal-ale-1_1-ale.xsd", "havis/middleware/wsdl/EPCglobal.xsd", "havis/middleware/wsdl/EPCglobal-ale-1_1-common.xsd" })
public class EC {

	@Resource
	WebServiceContext context;

	havis.middleware.ale.server.EC server = havis.middleware.ale.server.EC
			.getInstance();

	@WebMethod
	public VoidHolder define(Define parms) throws Exception {
		try {
			server.define(parms.getSpecName(), parms.getSpec());
			return new VoidHolder();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public VoidHolder undefine(Undefine parms) throws Exception {
		try {
			server.undefine(parms.getSpecName());
			return new VoidHolder();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public ECSpec getECSpec(GetECSpec parms) throws Exception {
		try {
			return server.getSpec(parms.getSpecName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public ArrayOfString getECSpecNames(EmptyParms parms) throws Exception {
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

	@WebMethod
	public VoidHolder subscribe(Subscribe parms) throws Exception {
		try {
			server.subscribe(parms.getSpecName(), parms.getNotificationURI());
			return new VoidHolder();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public VoidHolder unsubscribe(Unsubscribe parms) throws Exception {
		try {
			server.unsubscribe(parms.getSpecName(), parms.getNotificationURI());
			return new VoidHolder();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public ECReports poll(Poll parms) throws Exception {
		try {
			return server.poll(parms.getSpecName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public ECReports immediate(Immediate parms) throws Exception {
		try {
			return server.immediate(parms.getSpec());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public ArrayOfString getSubscribers(final GetSubscribers parms)
			throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getSubscribers(parms.getSpecName());
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public String getStandardVersion(EmptyParms parms) throws Exception {
		try {
			return server.getStandardVersion();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
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
			return new ECSpecValidationExceptionResponse(null, removeStrackTrace(e));
		} catch (InvalidURIException e) {
			return new InvalidURIExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchNameException e) {
			return new NoSuchNameExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchSubscriberException e) {
			return new NoSuchSubscriberExceptionResponse(null, removeStrackTrace(e));
		} catch (DuplicateSubscriptionException e) {
			return new DuplicateSubscriptionExceptionResponse(null, removeStrackTrace(e));
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