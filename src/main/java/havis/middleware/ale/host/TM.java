package havis.middleware.ale.host;

import havis.middleware.ale.base.annotation.EndpointMetadata;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.tm.ArrayOfString;
import havis.middleware.ale.service.tm.DefineTMSpec;
import havis.middleware.ale.service.tm.DefineTMSpecResult;
import havis.middleware.ale.service.tm.DuplicateNameExceptionResponse;
import havis.middleware.ale.service.tm.EmptyParms;
import havis.middleware.ale.service.tm.GetTMSpec;
import havis.middleware.ale.service.tm.ImplementationExceptionResponse;
import havis.middleware.ale.service.tm.InUseExceptionResponse;
import havis.middleware.ale.service.tm.NoSuchNameExceptionResponse;
import havis.middleware.ale.service.tm.SecurityExceptionResponse;
import havis.middleware.ale.service.tm.TMSpec;
import havis.middleware.ale.service.tm.TMSpecValidationExceptionResponse;
import havis.middleware.ale.service.tm.UndefineTMSpec;
import havis.middleware.ale.service.tm.UndefineTMSpecResult;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

@WebService(targetNamespace = "urn:epcglobal:aletm:wsdl:1", portName = "ALETMServicePort", serviceName = "ALETMService", endpointInterface = "havis.middleware.ale.service.tm.ALETMServicePortType", wsdlLocation = "havis/middleware/wsdl/EPCglobal-ale-1_1-aletm.wsdl")
@EndpointMetadata(xsdLocations = { "havis/middleware/wsdl/EPCglobal-ale-1_1-aletm.xsd", "havis/middleware/wsdl/EPCglobal.xsd" })
public class TM {

	@Resource
	WebServiceContext context;

	havis.middleware.ale.server.TM server = havis.middleware.ale.server.TM
			.getInstance();

	public DefineTMSpecResult defineTMSpec(DefineTMSpec parms) throws Exception {
		try {
			server.define(parms.getSpecName(), parms.getSpec());
			return new DefineTMSpecResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UndefineTMSpecResult undefineTMSpec(UndefineTMSpec parms)
			throws Exception {
		try {
			server.undefine(parms.getSpecName());
			return new UndefineTMSpecResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public TMSpec getTMSpec(GetTMSpec parms) throws Exception {
		try {
			return server.getSpec(parms.getSpecName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getTMSpecNames(EmptyParms parms) throws Exception {
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
			return new TMSpecValidationExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchNameException e) {
			return new NoSuchNameExceptionResponse(null, removeStrackTrace(e));
		} catch (InUseException e) {
			return new InUseExceptionResponse(null, removeStrackTrace(e));
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