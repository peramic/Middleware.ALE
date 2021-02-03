package havis.middleware.ale.host;

import javax.xml.ws.WebServiceContext;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.tm.DefineTMSpec;
import havis.middleware.ale.service.tm.DuplicateNameExceptionResponse;
import havis.middleware.ale.service.tm.EmptyParms;
import havis.middleware.ale.service.tm.GetTMSpec;
import havis.middleware.ale.service.tm.ImplementationExceptionResponse;
import havis.middleware.ale.service.tm.InUseExceptionResponse;
import havis.middleware.ale.service.tm.NoSuchNameExceptionResponse;
import havis.middleware.ale.service.tm.TMSpec;
import havis.middleware.ale.service.tm.TMSpecValidationExceptionResponse;
import havis.middleware.ale.service.tm.UndefineTMSpec;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Test;

public class TMTest {

	@Test
	public void defineTMSpec(@Mocked final havis.middleware.ale.server.TM server, @Mocked final TMSpec tmspec) throws Exception {
		final TM tm = new TM();
		DefineTMSpec spec = new DefineTMSpec();
		spec.setSpecName("testname");
		spec.setSpec(tmspec);
		
		tm.defineTMSpec(spec);
		new Verifications() {{
			server.define(withEqual("testname"), withEqual(tmspec));
			times = 1;
		}};
	}
	
	@Test (expected = havis.middleware.ale.service.tm.SecurityExceptionResponse.class) 
	public void defineTMSpec(@Mocked final havis.middleware.ale.server.TM server,
			@Mocked final DefineTMSpec defineTMSpec) throws Exception {
		final TM tm = new TM();
		new NonStrictExpectations() {{
			server.define(anyString, null);
			result = new SecurityException();
		}};
		tm.defineTMSpec(defineTMSpec);
	} 
	
	@Test
	public void undefineTMSpec(@Mocked final havis.middleware.ale.server.TM server) throws Exception {
		final TM tm = new TM();
		UndefineTMSpec spec = new UndefineTMSpec();
		spec.setSpecName("testname");
		
		tm.undefineTMSpec(spec);
		new Verifications() {{
			server.undefine(withEqual("testname"));
			times = 1;
		}};
	}
	
	@Test (expected = DuplicateNameExceptionResponse.class) 
	public void undefineTMSpec(@Mocked final havis.middleware.ale.server.TM server,
			@Mocked final UndefineTMSpec undefineTMSpec) throws Exception {
		final TM tm = new TM();
		new NonStrictExpectations() {{
			server.undefine(anyString);
			result = new DuplicateNameException();
		}};
		tm.undefineTMSpec(undefineTMSpec);
	}
	
	@Test
	public void getTMSpec(@Mocked final havis.middleware.ale.server.TM server) throws Exception {
		final TM tm = new TM();
		GetTMSpec spec = new GetTMSpec();
		spec.setSpecName("testname");
		
		tm.getTMSpec(spec);
		new Verifications() {{
			server.getSpec(withEqual("testname"));
			times = 1;
		}};
	}
	
	@Test (expected = TMSpecValidationExceptionResponse.class) 
	public void getTMSpec(@Mocked final havis.middleware.ale.server.TM server,
			@Mocked final GetTMSpec getTMSpec) throws Exception {
		final TM tm = new TM();
		new NonStrictExpectations() {{
			server.getSpec(anyString);
			result = new ValidationException();
		}};
		tm.getTMSpec(getTMSpec);
	}
	
	@Test
	public void getTMSpecNames(@Mocked final havis.middleware.ale.server.TM server) throws Exception {
		final TM tm = new TM();
		
		tm.getTMSpecNames(new EmptyParms());
		new Verifications() {{
			server.getNames();
			times = 1;
		}};
	}
	
	@Test (expected = NoSuchNameExceptionResponse.class) 
	public void getTMSpecNames(@Mocked final havis.middleware.ale.server.TM server,
			@Mocked final GetTMSpec getTMSpec) throws Exception {
		final TM tm = new TM();
		new NonStrictExpectations() {{
			server.getNames();
			result = new NoSuchNameException();
		}};
		tm.getTMSpecNames(new EmptyParms());
	}
	
	@Test
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.TM server) throws Exception {
		final TM tm = new TM();
		
		tm.getStandardVersion(new EmptyParms());
		new Verifications() {{
			server.getStandardVersion();
			times = 1;
		}};
	}
	
	@Test (expected = InUseExceptionResponse.class) 
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.TM server,
			@Mocked final GetTMSpec getTMSpec) throws Exception {
		final TM tm = new TM();
		new NonStrictExpectations() {{
			server.getStandardVersion();
			result = new InUseException();
		}};
		tm.getStandardVersion(new EmptyParms());
	}
	
	@Test (expected = RuntimeException.class) 
	public void getStandardVersiont(@Mocked final havis.middleware.ale.server.TM server,
			@Mocked final GetTMSpec getTMSpec) throws Exception {
		final TM tm = new TM();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			server.getStandardVersion();
			result = t;
		}};
		tm.getStandardVersion(new EmptyParms());
	}
	
	@Test
	public void getVendorVersion(@Mocked final Main server) throws Exception {
		final TM tm = new TM();
		
		tm.getVendorVersion(new EmptyParms());
		new Verifications() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			times = 1;
		}};
	}
	
	@Test (expected = ImplementationExceptionResponse.class) 
	public void getVendorVersion(@Mocked final Main server,
			@Mocked final GetTMSpec getTMSpec) throws Exception {
		final TM tm = new TM();
		new NonStrictExpectations() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			result = new ImplementationException();
		}};
		tm.getVendorVersion(new EmptyParms());
	}
}
