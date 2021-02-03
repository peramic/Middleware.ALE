package havis.middleware.ale.host;

import javax.xml.ws.WebServiceContext;

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
import havis.middleware.ale.service.lr.AddReaders.Readers;
import havis.middleware.ale.service.lr.Define;
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
import havis.middleware.ale.service.lr.SecurityExceptionResponse;
import havis.middleware.ale.service.lr.SetProperties;
import havis.middleware.ale.service.lr.SetProperties.Properties;
import havis.middleware.ale.service.lr.SetReaders;
import havis.middleware.ale.service.lr.Undefine;
import havis.middleware.ale.service.lr.Update;
import havis.middleware.ale.service.lr.ValidationExceptionResponse;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Test;

public class LRTest {
	
	@Test
	public void define(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final Define define = new Define();
		define.setName("testname");
		final LRSpec value = new LRSpec();
		define.setSpec(value);
		lr.define(define);
		new Verifications() {{
			server.define(withEqual("testname"), withEqual(value));
			times = 1;
		}};
	}
	
	@Test (expected = SecurityExceptionResponse.class)
	public void define(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final Define define) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.define(anyString, null);
			result = new SecurityException();
		}};
		lr.define(define);
	}
	
	@Test
	public void update(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final Update update = new Update();
		update.setName("testname");
		final LRSpec value = new LRSpec();
		update.setSpec(value);
		lr.update(update);
		new Verifications() {{
			server.update(withEqual("testname"), withEqual(value));
			times = 1;
		}};
	}
	
	@Test (expected = DuplicateNameExceptionResponse.class)
	public void update(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final Update update) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.update(anyString, null);
			result = new DuplicateNameException();
		}};
		lr.update(update);
	}
	
	@Test
	public void undefine(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final Undefine undefine = new Undefine();
		undefine.setName("testname");
		lr.undefine(undefine);
		new Verifications() {{
			server.undefine(withEqual("testname"));
			times = 1;
		}};
	}
	
	@Test (expected = ValidationExceptionResponse.class)
	public void undefine(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final Undefine undefine) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.undefine(anyString);
			result = new ValidationException();
		}};
		lr.undefine(undefine);
	}
	
	@Test
	public void getLogicalReaderNames(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		lr.getLogicalReaderNames(new EmptyParms());
		new Verifications() {{
			server.getNames();
			times = 1;
		}};
	}
	
	@Test (expected = InUseExceptionResponse.class)
	public void getLogicalReaderNames(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final Undefine undefine) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.getNames();
			result = new InUseException();
		}};
		lr.getLogicalReaderNames(new EmptyParms());
	}
	
	@Test
	public void getLRSpec(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final GetLRSpec getLRSpec = new GetLRSpec();
		getLRSpec.setName("testname");
		lr.getLRSpec(getLRSpec);
		new Verifications() {{
			server.getSpec(withEqual("testname"));
			times = 1;
		}};
	}
	
	@Test (expected = NoSuchNameExceptionResponse.class)
	public void getLRSpec(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final GetLRSpec getLRSpec) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.getSpec(anyString);
			result = new NoSuchNameException();
		}};
		lr.getLRSpec(getLRSpec);
	}
	
	@Test
	public void addReaders(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final AddReaders addReaders = new AddReaders();
		addReaders.setName("testname");
		Readers readers = new Readers();
		addReaders.setReaders(readers);
		lr.addReaders(addReaders);
		new Verifications() {{
			server.addReaders(withEqual("testname"), null);
			times = 1;
		}};
	}
	
	@Test (expected = ImmutableReaderExceptionResponse.class)
	public void addReaders(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final AddReaders addReaders) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.addReaders(anyString, null);
			result = new ImmutableReaderException();
		}};
		lr.addReaders(addReaders);
	}
	
	@Test
	public void setReaders(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final SetReaders setReaders = new SetReaders();
		setReaders.setName("testname");
		havis.middleware.ale.service.lr.SetReaders.Readers readers = new havis.middleware.ale.service.lr.SetReaders.Readers();
		setReaders.setReaders(readers);
		lr.setReaders(setReaders);
		new Verifications() {{
			server.setReaders(withEqual("testname"), null);
			times = 1;
		}};
	}
	
	@Test (expected = NonCompositeReaderExceptionResponse.class)
	public void setReaders(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final SetReaders setReaders) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.setReaders(anyString, null);
			result = new NonCompositeReaderException();
		}};
		lr.setReaders(setReaders);
	}
	
	@Test
	public void removeReaders(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final RemoveReaders removeReaders = new RemoveReaders();
		removeReaders.setName("testname");
		havis.middleware.ale.service.lr.RemoveReaders.Readers readers = new havis.middleware.ale.service.lr.RemoveReaders.Readers();
		removeReaders.setReaders(readers);
		lr.removeReaders(removeReaders);
		new Verifications() {{
			server.removeReaders(withEqual("testname"), null);
			times = 1;
		}};
	}
	
	@Test (expected = ReaderLoopExceptionResponse.class)
	public void removeReaders(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final RemoveReaders removeReaders) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.removeReaders(anyString, null);
			result = new ReaderLoopException();
		}};
		lr.removeReaders(removeReaders);
	}
	
	@Test
	public void setProperties(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final SetProperties setProperties = new SetProperties();
		setProperties.setName("testname");
		Properties properties = new Properties();
		setProperties.setProperties(properties);
		lr.setProperties(setProperties);
		new Verifications() {{
			server.setProperties(withEqual("testname"), null);
			times = 1;
		}};
	}
	
	@Test (expected = ImplementationExceptionResponse.class)
	public void setProperties(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final SetProperties setProperties) throws Exception {
		final LR lr = new LR();
		new NonStrictExpectations() {{
			server.setProperties(anyString, null);
			result = new ImplementationException();
		}};
		lr.setProperties(setProperties);
	}
	
	@Test
	public void getPropertyValue(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		final GetPropertyValue getPropertyValue = new GetPropertyValue();
		getPropertyValue.setName("testname");
		getPropertyValue.setPropertyName("propertyname");
		lr.getPropertyValue(getPropertyValue);
		new Verifications() {{
			server.getPropertyValue(withEqual("testname"), withEqual("propertyname"));
			times = 1;
		}};
	}
	
	@Test (expected = RuntimeException.class)
	public void getPropertyValue(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final GetPropertyValue getPropertyValue) throws Exception {
		final LR lr = new LR();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			server.getPropertyValue(anyString, anyString);
			result = t;
		}};
		lr.getPropertyValue(getPropertyValue);
	}
	
	@Test
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.LR server) throws Exception {
		final LR lr = new LR();
		lr.getStandardVersion(new EmptyParms());
		new Verifications() {{
			server.getStandardVersion();
			times = 1;
		}};
	}
	
	@Test (expected = RuntimeException.class)
	public void getStandardVersion(@Mocked final havis.middleware.ale.server.LR server,
			@Mocked final GetPropertyValue getPropertyValue) throws Exception {
		final LR lr = new LR();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			server.getStandardVersion();
			result = t;
		}};
		lr.getStandardVersion(new EmptyParms());
	}
	
	@Test
	public void getVendorVersion(@Mocked final Main main) throws Exception {
		final LR lr = new LR();
		lr.getVendorVersion(new EmptyParms());
		new Verifications() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			times = 1;
		}};
	}
	
	@Test (expected = RuntimeException.class)
	public void getVendorVersion(@Mocked final Main main,
			@Mocked final GetPropertyValue getPropertyValue) throws Exception {
		final LR lr = new LR();
		final Throwable t = new IllegalArgumentException();
		new NonStrictExpectations() {{
			Main.getVendorVersionUrl((WebServiceContext) any);
			result = t;
		}};
		lr.getVendorVersion(new EmptyParms());
	}
}
