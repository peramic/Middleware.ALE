package havis.middleware.ale.host;

import java.util.List;

import javax.xml.ws.WebServiceContext;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.pc.Define;
import havis.middleware.ale.service.pc.DuplicateNameExceptionResponse;
import havis.middleware.ale.service.pc.DuplicateSubscriptionExceptionResponse;
import havis.middleware.ale.service.pc.EmptyParms;
import havis.middleware.ale.service.pc.Execute;
import havis.middleware.ale.service.pc.GetPCSpec;
import havis.middleware.ale.service.pc.GetSubscribers;
import havis.middleware.ale.service.pc.Immediate;
import havis.middleware.ale.service.pc.ImplementationExceptionResponse;
import havis.middleware.ale.service.pc.InvalidURIExceptionResponse;
import havis.middleware.ale.service.pc.NoSuchNameExceptionResponse;
import havis.middleware.ale.service.pc.NoSuchSubscriberExceptionResponse;
import havis.middleware.ale.service.pc.PCOpSpec;
import havis.middleware.ale.service.pc.PCSpec;
import havis.middleware.ale.service.pc.PCSpecValidationExceptionResponse;
import havis.middleware.ale.service.pc.Poll;
import havis.middleware.ale.service.pc.SecurityExceptionResponse;
import havis.middleware.ale.service.pc.Subscribe;
import havis.middleware.ale.service.pc.Undefine;
import havis.middleware.ale.service.pc.Unsubscribe;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Test;

public class PCTest {
	
	@Test
	public void define(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		Define define = new Define();
		define.setSpecName("testName");
		final PCSpec spec = new PCSpec();
		define.setSpec(spec);
		
		pc.define(define);
		new Verifications() {{
			server.define(withEqual("testName"), withEqual(spec));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class) 
	public void define(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final Define define) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.define(anyString, null);
			result = new SecurityException();
		}};
		pc.define(define);
	}
	
	@Test
	public void undefine(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		Undefine undefine = new Undefine();
		undefine.setSpecName("testName");
		
		pc.undefine(undefine);
		new Verifications() {{
			server.undefine(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = DuplicateNameExceptionResponse.class) 
	public void undefine(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final Undefine undefine) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.undefine(anyString);
			result = new DuplicateNameException();
		}};
		pc.undefine(undefine);
	}
	
	@Test
	public void getPCSpec(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		GetPCSpec getPCSpec = new GetPCSpec();
		getPCSpec.setSpecName("testName");
		
		pc.getPCSpec(getPCSpec);
		new Verifications() {{
			server.getSpec(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = PCSpecValidationExceptionResponse.class) 
	public void getPCSpec(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final GetPCSpec getPCSpec) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.getSpec(anyString);
			result = new ValidationException();
		}};
		pc.getPCSpec(getPCSpec);
	}
	
	@Test
	public void getPCSpecNames(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		
		pc.getPCSpecNames(new EmptyParms());
		new Verifications() {{
			server.getNames();
			times = 1;
		}};
	}
	
	@Test (expected = InvalidURIExceptionResponse.class) 
	public void getPCSpecNames(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final GetPCSpec getPCSpec) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.getNames();
			result = new InvalidURIException();
		}};
		pc.getPCSpecNames(new EmptyParms());
	}
	
	@Test
	public void subscribe(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		final Subscribe subscribe = new Subscribe();
		subscribe.setSpecName("testName");
		subscribe.setNotificationURI("testUri");
		
		pc.subscribe(subscribe);
		new Verifications() {{
			server.subscribe(withEqual("testName"), withEqual("testUri"));
			times = 1;
		}};
	}
	
	@Test (expected = NoSuchNameExceptionResponse.class) 
	public void subscribe(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final Subscribe subscribe) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.subscribe(anyString, anyString);
			result = new NoSuchNameException();
		}};
		pc.subscribe(subscribe);
	}
	
	@Test
	public void unsubscribe(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		final Unsubscribe unsubscribe = new Unsubscribe();
		unsubscribe.setSpecName("testName");
		unsubscribe.setNotificationURI("testUri");
		
		pc.unsubscribe(unsubscribe);
		new Verifications() {{
			server.unsubscribe(withEqual("testName"), withEqual("testUri"));
			times = 1;
		}};
	}
	
	@Test (expected = NoSuchNameExceptionResponse.class) 
	public void unsubscribe(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final Unsubscribe unsubscribe) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.unsubscribe(anyString, anyString);
			result = new NoSuchNameException();
		}};
		pc.unsubscribe(unsubscribe);
	}
	
	@Test
	public void poll(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		final Poll poll = new Poll();
		poll.setSpecName("testName");
		
		pc.poll(poll);
		new Verifications() {{
			server.poll(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = NoSuchSubscriberExceptionResponse.class) 
	public void poll(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final Poll poll) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.poll(anyString);
			result = new NoSuchSubscriberException();
		}};
		pc.poll(poll);
	}
	
	@Test
	public void immediate(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		final Immediate immediate = new Immediate();
		final PCSpec spec = new PCSpec();
		immediate.setSpec(spec);
		
		pc.immediate(immediate);
		new Verifications() {{
			server.immediate(withEqual(spec));
			times = 1;
		}};
	}
	
	@Test (expected = DuplicateSubscriptionExceptionResponse.class) 
	public void immediate(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final Immediate immediate) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.immediate(immediate.getSpec());
			result = new DuplicateSubscriptionException();
		}};
		pc.immediate(immediate);
	}
	
	@Test
	public void getSubscribers(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		final GetSubscribers getSubscribers = new GetSubscribers();
		getSubscribers.setSpecName("testName");
		
		pc.getSubscribers(getSubscribers);
		new Verifications() {{
			server.getSubscribers(withEqual("testName"));
			times = 1;
		}};
	}
	
	@Test (expected = ImplementationExceptionResponse.class) 
	public void getSubscribers(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final GetSubscribers getSubscribers) throws Exception {
		final PC pc = new PC();
		new NonStrictExpectations() {{
			server.getSubscribers(anyString);
			result = new ImplementationException();
		}};
		pc.getSubscribers(getSubscribers);
	}
	
	@Test
	public void execute(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		final Execute execute = new Execute();
		
		pc.execute(execute);
		new Verifications() {{
			server.execute(null);
			times = 1;
		}};
	}
	
	@SuppressWarnings("unchecked")
	@Test (expected = RuntimeException.class) 
	public void execute(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final Execute execute) throws Exception {
		final PC pc = new PC();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			server.execute((List<PCOpSpec>) any);
			result = t;
		}};
		pc.execute(execute);
	}
	
	@Test
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.PC server) throws Exception {
		final PC pc = new PC();
		
		pc.getStandardVersion(new EmptyParms());
		new Verifications() {{
			server.getStandardVersion();
			times = 1;
		}};
	}
	
	@Test (expected = RuntimeException.class) 
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.PC server,
			@Mocked final Execute execute) throws Exception {
		final PC pc = new PC();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			server.getStandardVersion();
			result = t;
		}};
		pc.getStandardVersion(new EmptyParms());
	}
	
	@Test
	public void getVendorVersion(@Mocked final Main server) throws Exception {
		final PC pc = new PC();
		
		pc.getVendorVersion(new EmptyParms());
		new Verifications() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			times = 1;
		}};
	}
	
	@Test (expected = RuntimeException.class) 
	public void getVendorVersion(@Mocked final Main server,
			@Mocked final Execute execute) throws Exception {
		final PC pc = new PC();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			result = t;
		}};
		pc.getVendorVersion(new EmptyParms());
	}
}
