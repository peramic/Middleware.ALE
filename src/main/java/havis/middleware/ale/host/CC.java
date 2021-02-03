package havis.middleware.ale.host;

import havis.middleware.ale.base.annotation.EndpointMetadata;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ParameterForbiddenException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.cc.ArrayOfString;
import havis.middleware.ale.service.cc.AssocTableEntryList;
import havis.middleware.ale.service.cc.AssocTableSpec;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.ale.service.cc.CCSpecValidationExceptionResponse;
import havis.middleware.ale.service.cc.Define;
import havis.middleware.ale.service.cc.DefineAssocTable;
import havis.middleware.ale.service.cc.DefineAssocTableResult;
import havis.middleware.ale.service.cc.DefineEPCCache;
import havis.middleware.ale.service.cc.DefineRNG;
import havis.middleware.ale.service.cc.DefineRNGResult;
import havis.middleware.ale.service.cc.DefineResult;
import havis.middleware.ale.service.cc.DepleteEPCCache;
import havis.middleware.ale.service.cc.DuplicateNameExceptionResponse;
import havis.middleware.ale.service.cc.DuplicateSubscriptionExceptionResponse;
import havis.middleware.ale.service.cc.EPCCacheSpec;
import havis.middleware.ale.service.cc.EPCPatternList;
import havis.middleware.ale.service.cc.EmptyParms;
import havis.middleware.ale.service.cc.GetAssocTable;
import havis.middleware.ale.service.cc.GetAssocTableEntries;
import havis.middleware.ale.service.cc.GetAssocTableValue;
import havis.middleware.ale.service.cc.GetCCSpec;
import havis.middleware.ale.service.cc.GetEPCCache;
import havis.middleware.ale.service.cc.GetEPCCacheContents;
import havis.middleware.ale.service.cc.GetRNG;
import havis.middleware.ale.service.cc.GetSubscribers;
import havis.middleware.ale.service.cc.Immediate;
import havis.middleware.ale.service.cc.ImplementationExceptionResponse;
import havis.middleware.ale.service.cc.InvalidURIExceptionResponse;
import havis.middleware.ale.service.cc.NoSuchNameExceptionResponse;
import havis.middleware.ale.service.cc.NoSuchSubscriberExceptionResponse;
import havis.middleware.ale.service.cc.ParameterExceptionResponse;
import havis.middleware.ale.service.cc.ParameterForbiddenExceptionResponse;
import havis.middleware.ale.service.cc.Poll;
import havis.middleware.ale.service.cc.PutAssocTableEntries;
import havis.middleware.ale.service.cc.PutAssocTableEntriesResult;
import havis.middleware.ale.service.cc.RNGSpec;
import havis.middleware.ale.service.cc.RemoveAssocTableEntries;
import havis.middleware.ale.service.cc.RemoveAssocTableEntriesResult;
import havis.middleware.ale.service.cc.RemoveAssocTableEntry;
import havis.middleware.ale.service.cc.RemoveAssocTableEntryResult;
import havis.middleware.ale.service.cc.ReplenishEPCCache;
import havis.middleware.ale.service.cc.ReplenishEPCCacheResult;
import havis.middleware.ale.service.cc.SecurityExceptionResponse;
import havis.middleware.ale.service.cc.Subscribe;
import havis.middleware.ale.service.cc.SubscribeResult;
import havis.middleware.ale.service.cc.Undefine;
import havis.middleware.ale.service.cc.UndefineAssocTable;
import havis.middleware.ale.service.cc.UndefineAssocTableResult;
import havis.middleware.ale.service.cc.UndefineEPCCache;
import havis.middleware.ale.service.cc.UndefineRNG;
import havis.middleware.ale.service.cc.UndefineRNGResult;
import havis.middleware.ale.service.cc.UndefineResult;
import havis.middleware.ale.service.cc.Unsubscribe;
import havis.middleware.ale.service.cc.UnsubscribeResult;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

@WebService(targetNamespace = "urn:epcglobal:alecc:wsdl:1", portName = "ALECCServicePort", serviceName = "ALECCService", endpointInterface = "havis.middleware.ale.service.cc.ALECCServicePortType", wsdlLocation = "havis/middleware/wsdl/EPCglobal-ale-1_1-alecc.wsdl")
@EndpointMetadata(xsdLocations = { "havis/middleware/wsdl/EPCglobal-ale-1_1-alecc.xsd", "havis/middleware/wsdl/EPCglobal.xsd", "havis/middleware/wsdl/EPCglobal-ale-1_1-common.xsd" })
public class CC {

	@Resource
	WebServiceContext context;

	havis.middleware.ale.server.CC server = havis.middleware.ale.server.CC
			.getInstance();

	/**
	 * Defines a new command cycle
	 *
	 * @param params
	 *            The definition parameters
	 * @throws Exception
	 */
	public DefineResult define(Define params) throws Exception {
		try {
			server.define(params.getSpecName(), params.getSpec());
			return new DefineResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	/**
	 * Un-defines an existing command cycle
	 *
	 * @param params
	 *            undefine params
	 *
	 * @throws Exception
	 */
	public UndefineResult undefine(Undefine params) throws Exception {
		try {
			server.undefine(params.getSpecName());
			return new UndefineResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	/**
	 * Gets the specification of a command cycle
	 *
	 * @param params
	 *            The parameters
	 * @return The command cycle specification
	 * @throws Exception
	 */
	public CCSpec getCCSpec(GetCCSpec params) throws Exception {
		try {
			return server.getSpec(params.getSpecName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	/**
	 * Gets a list of names of all existing command cycles definitions
	 *
	 * @return List of command cycle names
	 * @throws Exception
	 */
	public ArrayOfString getCCSpecNames(EmptyParms params) throws Exception {
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

	/**
	 * Subscribes to a command cycle
	 *
	 * @param params
	 *            the parameters
	 * @throws Exception
	 */
	public SubscribeResult subscribe(Subscribe params) throws Exception {
		try {
			server.subscribe(params.getSpecName(), params.getNotificationURI());
			return new SubscribeResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	/**
	 * Un-subscribes URI from named command cycle.
	 *
	 * @param params the parameters
	 * @throws Exception
	 */
	public UnsubscribeResult unsubscribe(Unsubscribe params) throws Exception {
		try {
			server.unsubscribe(params.getSpecName(), params.getNotificationURI());
			return new UnsubscribeResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	/**
	 * Polls to a named command cycle
	 *
	 * @param params
	 *            The poll parameters
	 * @return The command cycle reports
	 * @throws Exception
	 * @throws ValidationException
	 */
	public CCReports poll(Poll params) throws Exception {
		try {
			return server.poll(params.getSpecName(), params.getParams() != null && params.getParams().getEntries() != null
					? params.getParams().getEntries().getEntry()
					: null);
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public CCReports immediate(Immediate params) throws Exception {
		try {
			return server.immediate(params.getSpec());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	/**
	 * Gets a list of subscribers of the named command cycle.
	 * 
	 * @param params
	 *            The parameters
	 * @return Array of subscriber URI strings
	 * @throws Exception
	 */
	public ArrayOfString getSubscribers(final GetSubscribers params)
			throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getSubscribers(params.getSpecName());
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	/**
	 * Gets the standard version
	 *
	 * @return The standard version
	 * @throws Exception
	 */
	public String getStandardVersion(EmptyParms params) throws Exception {
		try {
			return havis.middleware.ale.core.manager.CC.getStandardVersion();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	/**
	 * Gets the vendor version
	 *
	 * @return The vendor version
	 * @throws Exception
	 */
	public String getVendorVersion(EmptyParms params) throws Exception {
		try {
			return Main.getVendorVersionUrl(this.context);
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public void defineEPCCache(DefineEPCCache params) throws Exception {
		try {
			server.defineEPCCache(params.getCacheName(), params.getSpec(),
					params.getReplenishment() != null && params.getReplenishment().getPatterns() != null
							? params.getReplenishment().getPatterns().getPattern()
							: null);
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public EPCPatternList undefineEPCCache(UndefineEPCCache params)
			throws Exception {
		try {
			return new EPCPatternList(server.undefineEPCCache(params.getCacheName()));
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public EPCCacheSpec getEPCCache(GetEPCCache params) throws Exception {
		try {
			return server.getEPCCache(params.getCacheName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getEPCCacheNames(EmptyParms params) throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getEPCCacheNames();
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ReplenishEPCCacheResult replenishEPCCache(ReplenishEPCCache params)
			throws Exception {
		try {
			server.replenishEPCCache(params.getCacheName(), params.getReplenishment() != null && params.getReplenishment().getPatterns() != null
					? params.getReplenishment().getPatterns().getPattern()
					: null);
			return new ReplenishEPCCacheResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public EPCPatternList depleteEPCCache(DepleteEPCCache params)
			throws Exception {
		try {
			return new EPCPatternList(server.depleteEPCCache(params.getCacheName()));
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public EPCPatternList getEPCCacheContents(GetEPCCacheContents params)
			throws Exception {
		try {
			return new EPCPatternList(server.getEPCCacheContents(params.getCacheName()));
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public DefineAssocTableResult defineAssocTable(DefineAssocTable params)
			throws Exception {
		try {
			server.defineAssocTable(params.getTableName(), params.getSpec(), params.getEntries() != null && params.getEntries().getEntries() != null
					? params.getEntries().getEntries().getEntry()
					: null);
			return new DefineAssocTableResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UndefineAssocTableResult undefineAssocTable(UndefineAssocTable params)
			throws Exception {
		try {
			server.undefineAssocTable(params.getTableName());
			return new UndefineAssocTableResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getAssocTableNames(EmptyParms params) throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getAssocTableNames();
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public AssocTableSpec getAssocTable(GetAssocTable params) throws Exception {
		try {
			return server.getAssocTable(params.getTableName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public PutAssocTableEntriesResult putAssocTableEntries(
			PutAssocTableEntries params) throws Exception {
		try {
			server.putAssocTableEntries(params.getTableName(), params.getEntries() != null && params.getEntries().getEntries() != null
					? params.getEntries().getEntries().getEntry()
					: null);
			return new PutAssocTableEntriesResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public String getAssocTableValue(GetAssocTableValue params) throws Exception {
		try {
			return server.getAssocTableValue(params.getTableName(), params.getEpc());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public AssocTableEntryList getAssocTableEntries(GetAssocTableEntries params)
			throws Exception {
		try {
			return new AssocTableEntryList(server.getAssocTableEntries(params.getTableName(), params.getPatList() != null
					&& params.getPatList().getPatterns() != null ? params.getPatList().getPatterns().getPattern() : null));
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public RemoveAssocTableEntryResult removeAssocTableEntry(
			RemoveAssocTableEntry params) throws Exception {
		try {
			server.removeAssocTableEntry(params.getTableName(), params.getEpc());
			return new RemoveAssocTableEntryResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public RemoveAssocTableEntriesResult removeAssocTableEntries(RemoveAssocTableEntries params) throws Exception {
		try {
			server.removeAssocTableEntries(params.getTableName(), params.getPatList() != null && params.getPatList().getPatterns() != null
					? params.getPatList().getPatterns().getPattern()
					: null);
			return new RemoveAssocTableEntriesResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public DefineRNGResult defineRNG(DefineRNG params) throws Exception {
		try {
			server.defineRNG(params.getRngName(), params.getRngSpec());
			return new DefineRNGResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UndefineRNGResult undefineRNG(UndefineRNG params) throws Exception {
		try {
			server.undefineRNG(params.getRngName());
			return new UndefineRNGResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getRNGNames(EmptyParms params) throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getRNGNames();
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public RNGSpec getRNG(GetRNG params) throws Exception {
		try {
			return server.getRNG(params.getRngName());
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
			return new CCSpecValidationExceptionResponse(null, removeStrackTrace(e));
		} catch (InvalidURIException e) {
			return new InvalidURIExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchNameException e) {
			return new NoSuchNameExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchSubscriberException e) {
			return new NoSuchSubscriberExceptionResponse(null, removeStrackTrace(e));
		} catch (DuplicateSubscriptionException e) {
			return new DuplicateSubscriptionExceptionResponse(null, removeStrackTrace(e));
		} catch (ParameterException e) {
			return new ParameterExceptionResponse(null, removeStrackTrace(e));
		} catch (ParameterForbiddenException e) {
			return new ParameterForbiddenExceptionResponse(null, removeStrackTrace(e));
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