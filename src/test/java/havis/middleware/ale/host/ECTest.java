package havis.middleware.ale.host;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.ec.Define;
import havis.middleware.ale.service.ec.DuplicateNameExceptionResponse;
import havis.middleware.ale.service.ec.DuplicateSubscriptionExceptionResponse;
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

import javax.xml.ws.WebServiceContext;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Test;

public class ECTest {
	
	@Test
	public void define(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		final Define define = new Define();
		define.setSpecName("testName");
		final ECSpec spec = new ECSpec();
		define.setSpec(spec);
		ec.define(define);
		new Verifications() {{
			server.define(withEqual("testName"), withEqual(spec));
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void define(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final Define define) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.define(anyString, null);
			result = new SecurityException();
		}};
		ec.define(define);
	}
	
	@Test
	public void undefine(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		final Undefine define = new Undefine();
		define.setSpecName("testName");
		ec.undefine(define);
		new Verifications() {{
			server.undefine(withEqual("testName"));
		}};
	}
	
	@Test (expected = DuplicateNameExceptionResponse.class)
	public void undefine(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final Undefine undefine) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.undefine(anyString);
			result = new DuplicateNameException();
		}};
		ec.undefine(undefine);
	}
	
	@Test
	public void getECSpec(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		final GetECSpec ecSpec = new GetECSpec();
		ecSpec.setSpecName("testName");
		ec.getECSpec(ecSpec);
		new Verifications() {{
			server.getSpec(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = ECSpecValidationExceptionResponse.class)
	public void getECSpec(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final GetECSpec ecSpec) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.getSpec(anyString);
			result = new ValidationException();
		}};
		ec.getECSpec(ecSpec);
	}
	
	@Test
	public void getECSpecNames(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		ec.getECSpecNames(new EmptyParms());
		new Verifications() {{
			server.getNames();
			times = 1;
		}};
	}
	
	@Test (expected = InvalidURIExceptionResponse.class)
	public void getECSpecNamesExc(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.getNames();
			result = new InvalidURIException();
		}};
		ec.getECSpecNames(new EmptyParms());
	}
	
	@Test
	public void subscribe(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		final Subscribe subscribe = new Subscribe();
		subscribe.setSpecName("testName");
		subscribe.setNotificationURI("testUri");
		ec.subscribe(subscribe);
		new Verifications() {{
			server.subscribe(withEqual("testName"), withEqual("testUri"));
			times = 1;
		}};
	}
	
	@Test (expected = NoSuchNameExceptionResponse.class)
	public void subscribe(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final Subscribe subscribe) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.subscribe(anyString, anyString);
			result = new NoSuchNameException();
		}};
		ec.subscribe(subscribe);
	}
	
	@Test
	public void unsubscribe(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		final Unsubscribe unsubscribe = new Unsubscribe();
		unsubscribe.setSpecName("testName");
		unsubscribe.setNotificationURI("testUri");
		ec.unsubscribe(unsubscribe);
		new Verifications() {{
			server.unsubscribe(withEqual("testName"), withEqual("testUri"));
			times = 1;
		}};
	}
	
	@Test (expected = NoSuchNameExceptionResponse.class)
	public void unsubscribe(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final Unsubscribe unsubscribe) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.unsubscribe(anyString, anyString);
			result = new NoSuchNameException();
		}};
		ec.unsubscribe(unsubscribe);
	}
	
	@Test
	public void poll(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		final Poll poll = new Poll();
		poll.setSpecName("testName");
		ec.poll(poll);
		new Verifications() {{
			server.poll(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = NoSuchSubscriberExceptionResponse.class)
	public void poll(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final Poll poll) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.poll(anyString);
			result = new NoSuchSubscriberException();
		}};
		ec.poll(poll);
	}
	
	@Test
	public void immediate(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		final Immediate immediate = new Immediate();
		final ECSpec spec = new ECSpec();
		immediate.setSpec(spec);
		ec.immediate(immediate);
		new Verifications() {{
			server.immediate(withEqual(spec));
			times = 1;
		}};
	}
	
	@Test (expected = DuplicateSubscriptionExceptionResponse.class)
	public void immediate(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final Immediate immediate) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.immediate((ECSpec)any);
			result = new DuplicateSubscriptionException();
		}};
		ec.immediate(immediate);
	}
	
	@Test
	public void getSubscribers(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		final GetSubscribers subscribers = new GetSubscribers();
		subscribers.setSpecName("testname");
		ec.getSubscribers(subscribers);
		new Verifications() {{
			server.getSubscribers(withEqual("testname"));
			times = 1;
		}};
	}
	
	@Test (expected = ImplementationExceptionResponse.class)
	public void getSubscribers(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final GetSubscribers getSubscribers) throws Exception {
		final EC ec = new EC();
		new NonStrictExpectations() {{
			server.getSubscribers(anyString);
			result = new ImplementationException();
		}};
		ec.getSubscribers(getSubscribers);
	}
	
	@Test
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.EC server) throws Exception {
		final EC ec = new EC();
		ec.getStandardVersion(new EmptyParms());
		new Verifications() {{
			server.getStandardVersion();
			times = 1;
		}};
	}
	
	@Test (expected = RuntimeException.class)
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.EC server,
			@Mocked final GetSubscribers getSubscribers) throws Exception {
		final EC ec = new EC();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			server.getStandardVersion();
			result = t;
		}};
		ec.getStandardVersion(new EmptyParms());
	}
	
	@Test
	public void getVendorVersion(@Mocked final Main main) throws Exception {
		final EC ec = new EC();
		ec.getVendorVersion(new EmptyParms());
		new Verifications() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			times = 1;
		}};
	}
	
	@Test (expected = RuntimeException.class)
	public void getVendorVersion(@Mocked final Main main,
			@Mocked final GetSubscribers getSubscribers) throws Exception {
		final EC ec = new EC();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			result = t;
		}};
		ec.getVendorVersion(new EmptyParms());
	}
}
