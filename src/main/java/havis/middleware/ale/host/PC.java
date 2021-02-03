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
import havis.middleware.ale.service.pc.ArrayOfString;
import havis.middleware.ale.service.pc.Define;
import havis.middleware.ale.service.pc.DuplicateNameExceptionResponse;
import havis.middleware.ale.service.pc.DuplicateSubscriptionExceptionResponse;
import havis.middleware.ale.service.pc.EmptyParms;
import havis.middleware.ale.service.pc.Execute;
import havis.middleware.ale.service.pc.ExecuteResult;
import havis.middleware.ale.service.pc.GetPCSpec;
import havis.middleware.ale.service.pc.GetSubscribers;
import havis.middleware.ale.service.pc.Immediate;
import havis.middleware.ale.service.pc.ImplementationExceptionResponse;
import havis.middleware.ale.service.pc.InvalidURIExceptionResponse;
import havis.middleware.ale.service.pc.NoSuchNameExceptionResponse;
import havis.middleware.ale.service.pc.NoSuchSubscriberExceptionResponse;
import havis.middleware.ale.service.pc.PCOpReports;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.service.pc.PCSpec;
import havis.middleware.ale.service.pc.PCSpecValidationExceptionResponse;
import havis.middleware.ale.service.pc.Poll;
import havis.middleware.ale.service.pc.SecurityExceptionResponse;
import havis.middleware.ale.service.pc.Subscribe;
import havis.middleware.ale.service.pc.Undefine;
import havis.middleware.ale.service.pc.Unsubscribe;
import havis.middleware.ale.service.pc.VoidHolder;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

@WebService(targetNamespace = "urn:havis:alepc:wsdl:1", portName = "ALEPCServicePort", serviceName = "ALEPCService", endpointInterface = "havis.middleware.ale.service.pc.ALEPCServicePortType", wsdlLocation = "havis/middleware/wsdl/Ha-VIS-ale-1_1-alepc.wsdl")
@EndpointMetadata(xsdLocations = { "havis/middleware/wsdl/Ha-VIS-ale-1_1-alepc.xsd", "havis/middleware/wsdl/EPCglobal.xsd", "havis/middleware/wsdl/EPCglobal-ale-1_1-common.xsd" })
public class PC {

	@Resource
	WebServiceContext context;

	havis.middleware.ale.server.PC server = havis.middleware.ale.server.PC
			.getInstance();

	public VoidHolder define(Define parms) throws Exception {
		try {
			server.define(parms.getSpecName(), parms.getSpec());
			return new VoidHolder();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public VoidHolder undefine(Undefine parms) throws Exception {
		try {
			server.undefine(parms.getSpecName());
			return new VoidHolder();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public PCSpec getPCSpec(GetPCSpec parms) throws Exception {
		try {
			return server.getSpec(parms.getSpecName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getPCSpecNames(EmptyParms parms) throws Exception {
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

	public VoidHolder subscribe(Subscribe parms) throws Exception {
		try {
			server.subscribe(parms.getSpecName(), parms.getNotificationURI());
			return new VoidHolder();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public VoidHolder unsubscribe(Unsubscribe parms) throws Exception {
		try {
			server.unsubscribe(parms.getSpecName(), parms.getNotificationURI());
			return new VoidHolder();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public PCReports poll(Poll parms) throws Exception {
		try {
			return server.poll(parms.getSpecName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public PCReports immediate(Immediate parms) throws Exception {
		try {
			return server.immediate(parms.getSpec());
		} catch (Exception e) {
			throw exception(e);
		}
	}

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

	public ExecuteResult execute(final Execute parms) throws Exception {
		try {
			return new ExecuteResult() {
				{
					opReports = new PCOpReports() {
						{
							opReport = server.execute(parms.getOpSpecs() != null ? parms.getOpSpecs().getOpSpec() : null);
						}
					};
				}
			};
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
			return new PCSpecValidationExceptionResponse(null, removeStrackTrace(e));
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