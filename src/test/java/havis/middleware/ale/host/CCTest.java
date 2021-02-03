package havis.middleware.ale.host;

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
import havis.middleware.ale.service.cc.CCParameterList;
import havis.middleware.ale.service.cc.CCParameterList.Entries;
import havis.middleware.ale.service.cc.CCParameterListEntry;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.ale.service.cc.CCSpecValidationExceptionResponse;
import havis.middleware.ale.service.cc.Define;
import havis.middleware.ale.service.cc.DefineAssocTable;
import havis.middleware.ale.service.cc.DefineEPCCache;
import havis.middleware.ale.service.cc.DefineRNG;
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
import havis.middleware.ale.service.cc.RNGSpec;
import havis.middleware.ale.service.cc.RemoveAssocTableEntries;
import havis.middleware.ale.service.cc.RemoveAssocTableEntry;
import havis.middleware.ale.service.cc.ReplenishEPCCache;
import havis.middleware.ale.service.cc.SecurityExceptionResponse;
import havis.middleware.ale.service.cc.Subscribe;
import havis.middleware.ale.service.cc.Undefine;
import havis.middleware.ale.service.cc.UndefineAssocTable;
import havis.middleware.ale.service.cc.UndefineEPCCache;
import havis.middleware.ale.service.cc.UndefineRNG;
import havis.middleware.ale.service.cc.Unsubscribe;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.WebServiceContext;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class CCTest {
	
	@Test
	public void define(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final Define params = new Define();
		params.setSpecName("testSpecName");
		final CCSpec ccSpec = new CCSpec();
		params.setSpec(ccSpec);
		
		cc.define(params);
		new Verifications() {{
			String capturedName;
			CCSpec capturedSpec;
			server.define(capturedName = withCapture(), capturedSpec = withCapture());
			times = 1;
			Assert.assertEquals("testSpecName", capturedName);
			Assert.assertEquals(ccSpec, capturedSpec);
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void defineException(@Mocked final havis.middleware.ale.server.CC server, @Mocked final Define define) throws Exception {
		final CC cc = new CC();
		final CCSpec ccSpec = new CCSpec();
		
		new NonStrictExpectations() {{
			define.getSpecName();
			result = "testSpecName";
			define.getSpec();
			result = ccSpec;
			
			server.define("testSpecName", ccSpec);
			result = new SecurityException();
		}};
		cc.define(define);		
	}
	
	@Test
	public void undefine(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final Undefine params = new Undefine();
		params.setSpecName("testSpecName");
		
		cc.undefine(params);
		new Verifications() {{
			String capturedName;
			server.undefine(capturedName = withCapture());
			times = 1;
			Assert.assertEquals("testSpecName", capturedName);
		}};
	}
	
	@Test (expected = DuplicateNameExceptionResponse.class)
	public void undefineException(@Mocked final havis.middleware.ale.server.CC server, @Mocked final Undefine undefine) throws Exception {
		final CC cc = new CC();
		
		new NonStrictExpectations() {{
			undefine.getSpecName();
			result = "testSpecName";
			
			server.undefine("testSpecName");
			result = new DuplicateNameException();
		}};
		cc.undefine(undefine);		
	}
	
	@Test
	public void getCCSpec(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		GetCCSpec getCCSpec = new GetCCSpec();
		getCCSpec.setSpecName("testSpecName");
		
		cc.getCCSpec(getCCSpec);
		new Verifications() {{
			String capturedName;
			server.getSpec(capturedName = withCapture());
			times = 1;
			Assert.assertEquals("testSpecName", capturedName);
		}};
	}
	
	@Test (expected = CCSpecValidationExceptionResponse.class)
	public void getCCSpecException(@Mocked final havis.middleware.ale.server.CC server, @Mocked final GetCCSpec getCCSpec) throws Exception {
		final CC cc = new CC();
		
		new NonStrictExpectations() {{
			getCCSpec.getSpecName();
			result = "testSpecName";
			
			server.getSpec("testSpecName");
			result = new ValidationException();
		}};
		cc.getCCSpec(getCCSpec);		
	}
	
	@Test
	public void getCCSpecNames(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final List<String> list = new ArrayList<String>();
		
		new NonStrictExpectations() {{
			server.getNames();
			result = list;
		}};
		ArrayOfString arrayOfString = cc.getCCSpecNames(new EmptyParms());
		Assert.assertEquals(list, arrayOfString.getString());
	}
	
	@Test (expected = InvalidURIExceptionResponse.class)
	public void getCCSpecNamesException(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		
		new NonStrictExpectations() {{
			
			server.getNames();
			result = new InvalidURIException();
		}};
		cc.getCCSpecNames(new EmptyParms());		
	}
	
	@Test
	public void subscribe(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final Subscribe subscribe = new Subscribe();
		subscribe.setSpecName("testSpecName");
		subscribe.setNotificationURI("testURI");
		
		cc.subscribe(subscribe);
		new Verifications() {{
			String capName;
			String capURI;
			server.subscribe(capName = withCapture(), capURI = withCapture());
			Assert.assertEquals("testSpecName", capName);
			Assert.assertEquals("testURI", capURI);
		}};
	}
	
	@Test (expected = NoSuchNameExceptionResponse.class)
	public void subscribeException(@Mocked final havis.middleware.ale.server.CC server, @Mocked final Subscribe subscribe) throws Exception {
		final CC cc = new CC();
		
		new NonStrictExpectations() {{
			subscribe.getSpecName();
			result = "testSpecName";
			subscribe.getNotificationURI();
			result = "testURI";
			
			server.subscribe("testSpecName", "testURI");
			result = new NoSuchNameException();
		}};
		cc.subscribe(subscribe);		
	}
	
	@Test
	public void unsubscribe(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final Unsubscribe unsubscribe = new Unsubscribe();
		unsubscribe.setSpecName("testSpecName");
		unsubscribe.setNotificationURI("testURI");
		
		cc.unsubscribe(unsubscribe);
		new Verifications() {{
			String capName;
			String capURI;
			server.unsubscribe(capName = withCapture(), capURI = withCapture());
			Assert.assertEquals("testSpecName", capName);
			Assert.assertEquals("testURI", capURI);
		}};
	}
	
	@Test (expected = NoSuchSubscriberExceptionResponse.class)
	public void unsubscribeException(@Mocked final havis.middleware.ale.server.CC server, @Mocked final Unsubscribe unsubscribe) throws Exception {
		final CC cc = new CC();
		
		new NonStrictExpectations() {{
			unsubscribe.getSpecName();
			result = "testSpecName";
			unsubscribe.getNotificationURI();
			result = "testURI";
			
			server.unsubscribe("testSpecName", "testURI");
			result = new NoSuchSubscriberException();
		}};
		cc.unsubscribe(unsubscribe);		
	}
	
	@Test
	public void poll(@Mocked final havis.middleware.ale.server.CC server, @Mocked final CCParameterList list, @Mocked final Entries entries) throws Exception {
		final CC cc = new CC();
		final Poll poll = new Poll();		
		poll.setSpecName("testSpecName");
		poll.setParams(list);
		
		final List<CCParameterListEntry> entryList = new ArrayList<CCParameterListEntry>();
		new NonStrictExpectations() {{
			list.getEntries();
			result = entries;
			
			entries.getEntry();
			result = entryList;
		}};
		cc.poll(poll);
		new Verifications() {{
			String capName;
			List<CCParameterListEntry> capEntryList; 
			server.poll(capName = withCapture(), capEntryList = withCapture());
			times = 1;
			Assert.assertEquals("testSpecName", capName);
			Assert.assertEquals(entryList, capEntryList);
		}};
	}
	
	@SuppressWarnings("unchecked")
	@Test (expected = DuplicateSubscriptionExceptionResponse.class)
	public void pollException(@Mocked final havis.middleware.ale.server.CC server, @Mocked final Poll poll) throws Exception {
		final CC cc = new CC();
		
		new NonStrictExpectations() {{			
			server.poll(anyString, (List<CCParameterListEntry>) any);
			result = new DuplicateSubscriptionException();
		}};
		cc.poll(poll);		
	}
	
	@Test
	public void immediate(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final Immediate immediate = new Immediate();
		final CCSpec ccSpec = new CCSpec();
		immediate.setSpec(ccSpec);
		
		cc.immediate(immediate);
		new Verifications() {{
			server.immediate(ccSpec);
			times = 1;
		}};
	}
	
	@Test (expected = ParameterExceptionResponse.class)
	public void immediateException(@Mocked final havis.middleware.ale.server.CC server, @Mocked final Immediate immediate) throws Exception {
		final CC cc = new CC();
		final CCSpec ccSpec = new CCSpec();
		new NonStrictExpectations() {{
			immediate.getSpec();
			result = ccSpec;
			
			server.immediate(ccSpec);
			result = new ParameterException();
		}};
		cc.immediate(immediate);		
	}
	
	@Test
	public void getSubscribers(@Mocked final havis.middleware.ale.server.CC server, @Mocked final GetSubscribers getSubs) throws Exception {
		final CC cc = new CC();
		
		new NonStrictExpectations() {{
			getSubs.getSpecName();
			result = "testSpecName";
		}};
		
		cc.getSubscribers(getSubs);
		new Verifications() {{
			server.getSubscribers("testSpecName");
			times = 1;
		}};
	}
	
	@Test (expected = ParameterForbiddenExceptionResponse.class)
	public void getSubscribersException(@Mocked final havis.middleware.ale.server.CC server, @Mocked final GetSubscribers getSubs) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			getSubs.getSpecName();
			String specName = "testSpecName";
			result = specName;
			
			server.getSubscribers(specName);
			result = new ParameterForbiddenException();
		}};
		cc.getSubscribers(getSubs);		
	}
	
	@Test
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();		
		Assert.assertEquals("1.1", cc.getStandardVersion(new EmptyParms()));
	}
	
	@Test(expected = ImplementationExceptionResponse.class)
	public void getStandardVersion(@Mocked final havis.middleware.ale.core.manager.CC ccm) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			havis.middleware.ale.core.manager.CC.getStandardVersion();
			result = new ImplementationException();
		}};
		cc.getStandardVersion(new EmptyParms());
	}
	
	@Test
	public void getVendorVersion(@Mocked final Main main) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			result = "//testURL";
		}};
		
		Assert.assertEquals("//testURL", cc.getVendorVersion(new EmptyParms()));
	}

	@Test (expected = RuntimeException.class)
	public void getVendorVersionException(@Mocked final Main main) throws Exception {
		final CC cc = new CC();
		final Throwable throwable = new IllegalArgumentException();
		new NonStrictExpectations() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			result = throwable;
		}};
		
		cc.getVendorVersion(new EmptyParms());
	}
	
	@Test
	public void defineEPCCache(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		DefineEPCCache dEPCCache = new DefineEPCCache();
		dEPCCache.setCacheName("testName");
		EPCPatternList pList = new EPCPatternList();
		dEPCCache.setReplenishment(pList);
		final EPCCacheSpec spec = new EPCCacheSpec();
		dEPCCache.setSpec(spec);		
		
		cc.defineEPCCache(dEPCCache);
		new Verifications() {{
			server.defineEPCCache("testName", spec, null);
			times = 1;
		}};
	}
	
	@SuppressWarnings("unchecked")
	@Test (expected = SecurityExceptionResponse.class)
	public void defineEPCCacheException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final DefineEPCCache defineEPCCache) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.defineEPCCache(anyString, (EPCCacheSpec) any, (List<String>) any);
			result = new SecurityException();
		}};
		cc.defineEPCCache(defineEPCCache);		
	}
	
	@Test
	public void undefineEPCCache(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		UndefineEPCCache undEPCCache = new UndefineEPCCache();
		undEPCCache.setCacheName("testName");		
		
		cc.undefineEPCCache(undEPCCache);
		new Verifications() {{
			server.undefineEPCCache("testName");
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void undefineEPCCacheException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final UndefineEPCCache undefineEPCCache) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.undefineEPCCache(anyString);
			result = new SecurityException();
		}};
		cc.undefineEPCCache(undefineEPCCache);		
	}
	
	@Test
	public void getEPCCache(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		GetEPCCache epcCache = new GetEPCCache();
		epcCache.setCacheName("testname");
		
		cc.getEPCCache(epcCache);
		new Verifications() {{
			server.getEPCCache("testname");
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getEPCCacheException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final GetEPCCache getEPCCache) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.getEPCCache(anyString);
			result = new SecurityException();
		}};
		cc.getEPCCache(getEPCCache);		
	}
	
	@Test
	public void getEPCCacheNames(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		cc.getEPCCacheNames(new EmptyParms());
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getEPCCacheNamesException(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		
		new NonStrictExpectations() {{
			server.getEPCCacheNames();
			result = new SecurityException();
		}};
		
		cc.getEPCCacheNames(new EmptyParms());
	}
	
	@Test
	public void replenishEPCCache(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final ReplenishEPCCache replenishEPCCache) throws Exception {
		final CC cc = new CC();
		cc.replenishEPCCache(replenishEPCCache);
		new Verifications() {{
			replenishEPCCache.getCacheName();
			times = 1;
			replenishEPCCache.getReplenishment();
			times = 3;
		}};
	}
	
	@SuppressWarnings("unchecked")
	@Test (expected = SecurityExceptionResponse.class)
	public void replenishEPCCacheException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final ReplenishEPCCache replenishEPCCache) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.replenishEPCCache(anyString, (List<String>)any);
			result = new SecurityException();
		}};
		cc.replenishEPCCache(replenishEPCCache);
	}
	
	@Test
	public void depleteEPCCache(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		DepleteEPCCache epcCache = new DepleteEPCCache();
		epcCache.setCacheName("testName");
		cc.depleteEPCCache(epcCache);
		new Verifications() {{
			server.depleteEPCCache("testName");
			times = 1;
		}};		
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void depleteEPCCacheException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final DepleteEPCCache epcCache) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.depleteEPCCache(anyString);
			result = new SecurityException();
		}};
		cc.depleteEPCCache(epcCache);
	}
	
	@Test
	public void getEPCCacheContents(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final GetEPCCacheContents cacheContents = new GetEPCCacheContents();
		cacheContents.setCacheName("testName");
		cc.getEPCCacheContents(cacheContents);
		new Verifications() {{
			server.getEPCCacheContents(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getEPCCacheContentsException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final GetEPCCacheContents cacheContents) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.getEPCCacheContents(anyString);
			result = new SecurityException();
		}};
		cc.getEPCCacheContents(cacheContents);
	}
	
	@Test
	public void defineAssocTable(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final DefineAssocTable assocTable = new DefineAssocTable();
		assocTable.setTableName("testName");
		final AssocTableSpec spec = new AssocTableSpec();
		assocTable.setSpec(spec);
		assocTable.setEntries(new AssocTableEntryList());
		cc.defineAssocTable(assocTable);
		new Verifications() {{
			server.defineAssocTable(withEqual("testName"), spec, null);
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void defineAssocTableException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final DefineAssocTable assocTable) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.defineAssocTable(anyString, null, null);
			result = new SecurityException();
		}};
		cc.defineAssocTable(assocTable);
	}
	
	@Test
	public void undefineAssocTable(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final UndefineAssocTable assocTable = new UndefineAssocTable();
		assocTable.setTableName("testName");
		cc.undefineAssocTable(assocTable);
		new Verifications() {{
			server.undefineAssocTable(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void undefineAssocTableException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final UndefineAssocTable assocTable) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.undefineAssocTable(anyString);
			result = new SecurityException();
		}};
		cc.undefineAssocTable(assocTable);
	}
	
	@Test
	public void getAssocTableNames(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		cc.getAssocTableNames(new EmptyParms());
		new Verifications() {{
			server.getAssocTableNames();
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getAssocTableNamesException(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.getAssocTableNames();
			result = new SecurityException();
		}};
		cc.getAssocTableNames(new EmptyParms());
	}
	
	@Test
	public void getAssocTable(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final GetAssocTable assocTable = new GetAssocTable();
		assocTable.setTableName("testName");
		cc.getAssocTable(assocTable);
		new Verifications() {{
			server.getAssocTable(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getAssocTableException(@Mocked final havis.middleware.ale.server.CC server,
			@Mocked final GetAssocTable assocTable) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.getAssocTable(anyString);
			result = new SecurityException();
		}};
		cc.getAssocTable(assocTable);
	}
	
	@Test
	public void putAssocTableEntries(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final PutAssocTableEntries assocTableEntries = new PutAssocTableEntries();
		assocTableEntries.setTableName("testName");
		AssocTableEntryList list = new AssocTableEntryList();
		assocTableEntries.setEntries(list);
		cc.putAssocTableEntries(assocTableEntries);
		new Verifications() {{
			server.putAssocTableEntries(withEqual("testName"), null);
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void putAssocTableEntriesException(@Mocked final havis.middleware.ale.server.CC server,
			 @Mocked final PutAssocTableEntries tableEntries) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.putAssocTableEntries(anyString, null);
			result = new SecurityException();
		}};
		cc.putAssocTableEntries(tableEntries);
	}
	
	@Test
	public void getAssocTableValue(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final GetAssocTableValue assocTableValue = new GetAssocTableValue();
		assocTableValue.setTableName("testName");
		assocTableValue.setEpc("testEpc");
		cc.getAssocTableValue(assocTableValue);
		new Verifications() {{
			server.getAssocTableValue(withEqual("testName"), withEqual("testEpc"));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getAssocTableValueException(@Mocked final havis.middleware.ale.server.CC server,
			 @Mocked final GetAssocTableValue tableValue) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.getAssocTableValue(anyString, anyString);
			result = new SecurityException();
		}};
		cc.getAssocTableValue(tableValue);
	}
	
	@Test
	public void getAssocTableEntries(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final GetAssocTableEntries assocTableEntries = new GetAssocTableEntries();
		assocTableEntries.setTableName("testName");
		EPCPatternList patternList = new EPCPatternList();
		assocTableEntries.setPatList(patternList);
		cc.getAssocTableEntries(assocTableEntries);
		new Verifications() {{
			server.getAssocTableEntries(withEqual("testName"),null);
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getAssocTableEntriesException(@Mocked final havis.middleware.ale.server.CC server,
			 @Mocked final GetAssocTableEntries tableEntries) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.getAssocTableEntries(anyString, null);
			result = new SecurityException();
		}};
		cc.getAssocTableEntries(tableEntries);
	}
	
	@Test
	public void removeAssocTableEntry(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final RemoveAssocTableEntry assocTableEntry = new RemoveAssocTableEntry();
		assocTableEntry.setTableName("testName");
		assocTableEntry.setEpc("testEpc");
		cc.removeAssocTableEntry(assocTableEntry);
		new Verifications() {{
			server.removeAssocTableEntry(withEqual("testName"), withEqual("testEpc"));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void removeAssocTableEntryException(@Mocked final havis.middleware.ale.server.CC server,
			 @Mocked final RemoveAssocTableEntry tableEntry) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.removeAssocTableEntry(anyString, anyString);
			result = new SecurityException();
		}};
		cc.removeAssocTableEntry(tableEntry);
	}
	
	@Test
	public void removeAssocTableEntries(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final RemoveAssocTableEntries tableEntries = new RemoveAssocTableEntries();
		tableEntries.setTableName("testName");
		EPCPatternList list = new EPCPatternList();
		tableEntries.setPatList(list);
		cc.removeAssocTableEntries(tableEntries);
		new Verifications() {{
			server.removeAssocTableEntries(withEqual("testName"), null);
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void removeAssocTableEntriesException(@Mocked final havis.middleware.ale.server.CC server,
			 @Mocked final RemoveAssocTableEntries tableEntries) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.removeAssocTableEntries(anyString, null);
			result = new SecurityException();
		}};
		cc.removeAssocTableEntries(tableEntries);
	}
	
	@Test
	public void defineRNG(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final DefineRNG rng = new DefineRNG();
		rng.setRngName("testName");
		final RNGSpec spec = new RNGSpec();
		rng.setRngSpec(spec);
		cc.defineRNG(rng);
		new Verifications() {{
			server.defineRNG(withEqual("testName"), withEqual(spec));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void defineRNGException(@Mocked final havis.middleware.ale.server.CC server,
			 @Mocked final DefineRNG rng) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.defineRNG(anyString, null);
			result = new SecurityException();
		}};
		cc.defineRNG(rng);
	}
	
	@Test
	public void undefineRNG(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final UndefineRNG rng = new UndefineRNG();
		rng.setRngName("testName");
		cc.undefineRNG(rng);
		new Verifications() {{
			server.undefineRNG(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void undefineRNGException(@Mocked final havis.middleware.ale.server.CC server,
			 @Mocked final UndefineRNG rng) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.undefineRNG(anyString);
			result = new SecurityException();
		}};
		cc.undefineRNG(rng);
	}
	
	@Test
	public void getRNGNames(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		cc.getRNGNames(new EmptyParms());
		new Verifications() {{
			server.getRNGNames();
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getRNGNamesException(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.getRNGNames();
			result = new SecurityException();
		}};
		cc.getRNGNames(new EmptyParms());
	}
	
	@Test
	public void getRNG(@Mocked final havis.middleware.ale.server.CC server) throws Exception {
		final CC cc = new CC();
		final GetRNG rng = new GetRNG();
		rng.setRngName("testName");
		cc.getRNG(rng);
		new Verifications() {{
			server.getRNG(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void getRNGException(@Mocked final havis.middleware.ale.server.CC server,
			 @Mocked final GetRNG rng) throws Exception {
		final CC cc = new CC();
		new NonStrictExpectations() {{
			server.getRNG(anyString);
			result = new SecurityException();
		}};
		cc.getRNG(rng);
	}
}
