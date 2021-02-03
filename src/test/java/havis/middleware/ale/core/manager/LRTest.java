package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonBaseReaderException;
import havis.middleware.ale.base.exception.NonCompositeReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.BaseReader;
import havis.middleware.ale.core.CompositeReader;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.depot.service.lr.LogicalReader;
import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.rc.RCConfig;

import java.util.Arrays;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class LRTest {

	@BeforeClass
	public static void init() {
		ConfigResetter.reset();
		ConfigResetter.disablePersistence();
	}

	@Before
	public void reset() {
		LR.reset();
	}

	@Test
	public void defineInvalidName(@Mocked Name name)
			throws ValidationException, DuplicateNameException,
			ImplementationException, ImmutableReaderException {

		new NonStrictExpectations() {
			{
				Name.isValid(withEqual("name"));
				result = Boolean.FALSE;
			}
		};

		LR lr = LR.getInstance();
		lr.define("name", null, false);
		Assert.assertTrue(lr.getNames().isEmpty());
	}

	@Test
	public void defineDuplicate(
			@Mocked Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			ImmutableReaderException {

		new NonStrictExpectations() {
			{
				Name.isValid(withEqual("name"));
				result = Boolean.TRUE;
			}
		};

		final LR lr = LR.getInstance();
		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.FALSE);
		lr.define("name", spec, false);

		final LRSpec spec2 = new LRSpec();
		spec.setIsComposite(Boolean.FALSE);
		try {
			lr.define("name", spec2, false);
			Assert.fail("Expected DuplicateNameException");
		} catch (DuplicateNameException e) {
			// ignore
		}
	}

	@Test
	public void defineNoSpec(
			@Mocked Name name)
			throws ValidationException, DuplicateNameException,
			ImplementationException, ImmutableReaderException {

		new NonStrictExpectations() {
			{
				Name.isValid(withEqual("name"));
				result = Boolean.TRUE;
			}
		};

		final LR lr = LR.getInstance();
		try {
			lr.define("name", null, false);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void defineReaderLoop(
			@Mocked Name name,
			@Mocked final CompositeReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			ReaderLoopException, ImmutableReaderException {

		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.TRUE);

		new NonStrictExpectations() {
			{
				Name.isValid(withEqual("name"));
				result = Boolean.TRUE;

				new CompositeReader(withEqual("name"), withEqual(spec));
				result = new ReaderLoopException();
			}
		};

		final LR lr = LR.getInstance();
		try {
			lr.define("name", spec, false);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void defineBaseReader(@Mocked final Name name,
			@Mocked final BaseReader reader, @Mocked final LogicalReader depot)
			throws ValidationException, DuplicateNameException,
			ImplementationException, ImmutableReaderException {

		new NonStrictExpectations() {
			{
				Name.isValid(withEqual("name"));
				result = Boolean.TRUE;
			}
		};

		final LR lr = LR.getInstance();
		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.FALSE);
		lr.define("name", spec, true);
		List<String> names = lr.getNames();
		Assert.assertTrue(names.size() == 1);
		Assert.assertTrue(names.contains("name"));

		new Verifications() {
			{
				new BaseReader(withEqual("name"), withEqual(spec));
				times = 1;

				depot.add(withEqual("name"), withEqual(spec));
				times = 1;
			}
		};
	}

	@Test
	public void defineCompositeReader(@Mocked final Name name,
			@Mocked final CompositeReader reader,
			@Mocked final LogicalReader depot) throws ValidationException,
			DuplicateNameException, ImplementationException,
			ReaderLoopException, ImmutableReaderException {

		new NonStrictExpectations() {
			{
				Name.isValid(withEqual("name"));
				result = Boolean.TRUE;
			}
		};

		final LR lr = LR.getInstance();
		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.TRUE);
		lr.define("name", spec, true);
		List<String> names = lr.getNames();
		Assert.assertTrue(names.size() == 1);
		Assert.assertTrue(names.contains("name"));

		new Verifications() {
			{
				new CompositeReader(withEqual("name"), withEqual(spec));
				times = 1;

				depot.add(withEqual("name"), withEqual(spec));
				times = 1;
			}
		};
	}

	private void defineSimpleReader(LR lr, final String name, boolean composite)
			throws ValidationException, DuplicateNameException,
			ImplementationException, ImmutableReaderException {

		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.valueOf(composite));

		new NonStrictExpectations() {
			{
				Name.isValid(withEqual(name));
				result = Boolean.TRUE;
			}
		};

		lr.define(name, spec, false);
	}

	@Test
	public void update(@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		defineSimpleReader(lr, "update", false);

		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.FALSE);

		lr.update("update", spec, false);

		new Verifications() {
			{
				BaseReader baseReader = new BaseReader(withEqual("update"),
						this.<LRSpec> withNotNull());
				times = 1;

				baseReader.update(withEqual(spec), false);
				times = 1;
			}
		};
	}

	@Test
	public void updateNoSuchNameException()
			throws ValidationException, DuplicateNameException,
			ImplementationException, InUseException, ReaderLoopException,
			NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.FALSE);

		try {
			lr.update("update", spec, false);
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void updateImplementationException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.FALSE);

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("update"), this.<LRSpec> withNotNull());
				result = reader;

				reader.update(withEqual(spec), false);
				result = new ImplementationException();
			}
		};

		final LR lr = LR.getInstance();
		defineSimpleReader(lr, "update", false);

		try {
			lr.update("update", spec, false);
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}
	}

	@Test
	public void updateInUseException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.FALSE);

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("update"), this.<LRSpec> withNotNull());
				result = reader;

				reader.update(withEqual(spec), false);
				result = new InUseException();
			}
		};

		final LR lr = LR.getInstance();
		defineSimpleReader(lr, "update", false);

		try {
			lr.update("update", spec, false);
			Assert.fail("Expected InUseException");
		} catch (InUseException e) {
			// ignore
		}
	}

	@Test
	public void updateValidationException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LRSpec spec = new LRSpec();
		spec.setIsComposite(Boolean.FALSE);

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("update"), this.<LRSpec> withNotNull());
				result = reader;

				reader.update(withEqual(spec), false);
				result = new ValidationException();
			}
		};

		final LR lr = LR.getInstance();
		defineSimpleReader(lr, "update", false);

		try {
			lr.update("update", spec, false);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void undefine(@Mocked final Name name,
			@Mocked final BaseReader reader, @Mocked final LogicalReader depot)
			throws ValidationException, DuplicateNameException,
			ImplementationException, InUseException, ReaderLoopException,
			NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		defineSimpleReader(lr, "undefine", false);

		List<String> names = lr.getNames();
		Assert.assertTrue(names.size() == 1);
		Assert.assertTrue(names.contains("undefine"));

		lr.undefine("undefine", true);

		names = lr.getNames();
		Assert.assertTrue(names.isEmpty());

		new Verifications() {
			{
				BaseReader baseReader = new BaseReader(withEqual("undefine"),
						this.<LRSpec> withNotNull());
				times = 1;

				baseReader.undefine();
				times = 1;

				depot.remove(withEqual("undefine"));
				times = 1;
			}
		};
	}

	@Test
	public void undefineInUseException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("undefine"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.undefine();
				result = new InUseException();
			}
		};

		final LR lr = LR.getInstance();
		defineSimpleReader(lr, "undefine", false);

		List<String> names = lr.getNames();
		Assert.assertTrue(names.size() == 1);
		Assert.assertTrue(names.contains("undefine"));

		try {
			lr.undefine("undefine", false);
			Assert.fail("Expected InUseException");
		} catch (InUseException e) {
			// ignore
		}

		names = lr.getNames();
		Assert.assertTrue(names.size() == 1);
		Assert.assertTrue(names.contains("undefine"));
	}

	@Test
	public void undefineImplementationException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("undefine"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.undefine();
				result = new ImplementationException();
			}
		};

		final LR lr = LR.getInstance();
		defineSimpleReader(lr, "undefine", false);

		List<String> names = lr.getNames();
		Assert.assertTrue(names.size() == 1);
		Assert.assertTrue(names.contains("undefine"));

		try {
			lr.undefine("undefine", false);
			Assert.fail("Expected InUseException");
		} catch (ImplementationException e) {
			// ignore
		}

		names = lr.getNames();
		Assert.assertTrue(names.isEmpty());
	}

	@Test
	public void undefineNoSuchNameException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();

		try {
			lr.undefine("undefine", false);
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void getNames(@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			ImmutableReaderException {

		final LR lr = LR.getInstance();

		List<String> names = lr.getNames();
		Assert.assertNotNull(names);
		Assert.assertEquals(0, names.size());

		defineSimpleReader(lr, "1", false);
		names = lr.getNames();
		Assert.assertNotNull(names);
		Assert.assertEquals(1, names.size());
		Assert.assertEquals("1", names.get(0));

		defineSimpleReader(lr, "2", false);
		names = lr.getNames();
		Assert.assertNotNull(names);
		Assert.assertEquals(2, names.size());
		Assert.assertEquals("1", names.get(0));
		Assert.assertEquals("2", names.get(1));

		defineSimpleReader(lr, "3", false);
		names = lr.getNames();
		Assert.assertNotNull(names);
		Assert.assertEquals(3, names.size());
		Assert.assertEquals("1", names.get(0));
		Assert.assertEquals("2", names.get(1));
		Assert.assertEquals("3", names.get(2));
	}

	@Test
	public void getSpec(@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final LRSpec spec = new LRSpec();

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("getSpec"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.getSpec();
				result = spec;
			}
		};

		defineSimpleReader(lr, "getSpec", false);

		LRSpec actual = lr.getSpec("getSpec");
		Assert.assertSame(spec, actual);
	}

	@Test
	public void getSpecNoSuchNameException()
			throws ValidationException, DuplicateNameException,
			ImplementationException, NoSuchNameException {

		final LR lr = LR.getInstance();

		try {
			lr.getSpec("getSpec");
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void addReaders(@Mocked final Name name,
			@Mocked final CompositeReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			NoSuchNameException, NonCompositeReaderException, InUseException,
			ReaderLoopException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");
		defineSimpleReader(lr, "addReaders", true);

		lr.addReaders("addReaders", readers, false);

		new Verifications() {
			{
				CompositeReader compositeReader = new CompositeReader(
						withEqual("addReaders"), this.<LRSpec> withNotNull());
				times = 1;

				compositeReader.add(withEqual(readers), false);
				times = 1;
			}
		};
	}

	@Test
	public void addReadersNonCompositeReaderException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			NoSuchNameException, NonCompositeReaderException, InUseException,
			ReaderLoopException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("addReaders"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.add(withEqual(readers), false);
				result = new NonCompositeReaderException();
			}
		};

		defineSimpleReader(lr, "addReaders", false);

		try {
			lr.addReaders("addReaders", readers, false);
			Assert.fail("Expected NonCompositeReaderException");
		} catch (NonCompositeReaderException e) {
			// ignore
		}
	}

	@Test
	public void addReadersNoSuchNameException(
			@Mocked final Name name)
			throws ValidationException, DuplicateNameException,
			ImplementationException, NoSuchNameException,
			NonCompositeReaderException, InUseException, ReaderLoopException,
			ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");

		try {
			lr.addReaders("addReaders", readers, false);
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void setReaders(@Mocked final Name name,
			@Mocked final CompositeReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			NoSuchNameException, NonCompositeReaderException, InUseException,
			ReaderLoopException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");
		defineSimpleReader(lr, "setReaders", true);

		lr.setReaders("setReaders", readers, false);

		new Verifications() {
			{
				CompositeReader compositeReader = new CompositeReader(
						withEqual("setReaders"), this.<LRSpec> withNotNull());
				times = 1;

				compositeReader.set(withEqual(readers), false);
				times = 1;
			}
		};
	}

	@Test
	public void setReadersNonCompositeReaderException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			NoSuchNameException, NonCompositeReaderException, InUseException,
			ReaderLoopException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("setReaders"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.set(withEqual(readers), false);
				result = new NonCompositeReaderException();
			}
		};

		defineSimpleReader(lr, "setReaders", false);

		try {
			lr.setReaders("setReaders", readers, false);
			Assert.fail("Expected NonCompositeReaderException");
		} catch (NonCompositeReaderException e) {
			// ignore
		}
	}

	@Test
	public void setReadersNoSuchNameException(
			@Mocked final Name name)
			throws ValidationException, DuplicateNameException,
			ImplementationException, NoSuchNameException,
			NonCompositeReaderException, InUseException, ReaderLoopException,
			ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");

		try {
			lr.setReaders("setReaders", readers, false);
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void removeReaders(@Mocked final Name name,
			@Mocked final CompositeReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			NoSuchNameException, NonCompositeReaderException, InUseException,
			ReaderLoopException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");
		defineSimpleReader(lr, "removeReaders", true);

		lr.removeReaders("removeReaders", readers, false);

		new Verifications() {
			{
				CompositeReader compositeReader = new CompositeReader(
						withEqual("removeReaders"), this.<LRSpec> withNotNull());
				times = 1;

				compositeReader.remove(withEqual(readers), false);
				times = 1;
			}
		};
	}

	@Test
	public void removeReadersNonCompositeReaderException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException,
			NoSuchNameException, NonCompositeReaderException, InUseException,
			ReaderLoopException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("removeReaders"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.remove(withEqual(readers), false);
				result = new NonCompositeReaderException();
			}
		};

		defineSimpleReader(lr, "removeReaders", false);

		try {
			lr.removeReaders("removeReaders", readers, false);
			Assert.fail("Expected NonCompositeReaderException");
		} catch (NonCompositeReaderException e) {
			// ignore
		}
	}

	@Test
	public void removeReadersNoSuchNameException(
			@Mocked final Name name)
			throws ValidationException, DuplicateNameException,
			ImplementationException, NoSuchNameException,
			NonCompositeReaderException, InUseException, ReaderLoopException,
			ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<String> readers = Arrays.asList("1", "2");

		try {
			lr.removeReaders("removeReaders", readers, false);
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void setProperties(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<LRProperty> properties = Arrays.asList(new LRProperty(),
				new LRProperty());
		defineSimpleReader(lr, "setProperties", false);

		lr.setProperties("setProperties", properties, false);

		new Verifications() {
			{
				BaseReader baseReader = new BaseReader(
						withEqual("setProperties"), this.<LRSpec> withNotNull());
				times = 1;

				baseReader.setProperties(withEqual(properties), false);
				times = 1;
			}
		};
	}

	@Test
	public void setPropertiesNoSuchNameException()
			throws ValidationException, DuplicateNameException,
			ImplementationException, InUseException, ReaderLoopException,
			NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<LRProperty> properties = Arrays.asList(new LRProperty(),
				new LRProperty());

		try {
			lr.setProperties("setProperties", properties, false);
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void setPropertiesImplementationException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<LRProperty> properties = Arrays.asList(new LRProperty(),
				new LRProperty());

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("setProperties"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.setProperties(withEqual(properties), false);
				result = new ImplementationException();
			}
		};

		defineSimpleReader(lr, "setProperties", false);

		try {
			lr.setProperties("setProperties", properties, false);
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}
	}

	@Test
	public void setPropertiesInUseException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<LRProperty> properties = Arrays.asList(new LRProperty(),
				new LRProperty());

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("setProperties"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.setProperties(withEqual(properties), false);
				result = new InUseException();
			}
		};

		defineSimpleReader(lr, "setProperties", false);

		try {
			lr.setProperties("setProperties", properties, false);
			Assert.fail("Expected InUseException");
		} catch (InUseException e) {
			// ignore
		}
	}

	@Test
	public void setPropertiesValidationException(
			@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();
		final List<LRProperty> properties = Arrays.asList(new LRProperty(),
				new LRProperty());

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("setProperties"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.setProperties(withEqual(properties), false);
				result = new ValidationException();
			}
		};

		defineSimpleReader(lr, "setProperties", false);

		try {
			lr.setProperties("setProperties", properties, false);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void getPropertyValue(@Mocked final Name name,
			@Mocked final BaseReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, ImmutableReaderException {

		final LR lr = LR.getInstance();

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("getPropertyValue"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.getPropertyValue(withEqual("property"));
				result = "value";
			}
		};

		defineSimpleReader(lr, "getPropertyValue", false);

		String value = lr.getPropertyValue("getPropertyValue", "property");
		Assert.assertEquals("value", value);
	}

	@Test
	public void getPropertyValueNoSuchNameException()
			throws ValidationException, DuplicateNameException,
			ImplementationException, InUseException, ReaderLoopException,
			NoSuchNameException {

		final LR lr = LR.getInstance();

		try {
			lr.getPropertyValue("getPropertyValue", "property");
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void lock(@Mocked final Name name, @Mocked final BaseReader reader)
			throws ValidationException, DuplicateNameException,
			ImplementationException, InUseException, ReaderLoopException,
			NoSuchNameException, ImmutableReaderException {
		final LR lr = LR.getInstance();

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("lock"), this.<LRSpec> withNotNull());
				result = reader;
			}
		};

		defineSimpleReader(lr, "lock", false);

		havis.middleware.ale.core.LogicalReader locked = lr.lock("lock");
		Assert.assertEquals(reader.toString(), locked.toString());

		new Verifications() {
			{
				BaseReader baseReader = new BaseReader(withEqual("lock"),
						this.<LRSpec> withNotNull());
				times = 1;

				baseReader.lock();
				times = 1;
			}
		};
	}

	@Test(expected = NoSuchNameException.class)
	public void lockNoSuchNameException() throws NoSuchNameException {
		final LR lr = LR.getInstance();
		lr.lock("lock");
	}

	@Test
	public void getConfig(@Mocked final Name name,
			@Mocked final CompositeReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, NonBaseReaderException,
			ImmutableReaderException {
		final LR lr = LR.getInstance();
		final RCConfig config = new RCConfig();

		new NonStrictExpectations() {
			{
				new CompositeReader(withEqual("getConfig"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.getConfig();
				result = config;
			}
		};

		defineSimpleReader(lr, "getConfig", true);

		RCConfig actual = lr.getConfig("getConfig");
		Assert.assertSame(config, actual);
	}

	@Test(expected = NoSuchNameException.class)
	public void getConfigNoSuchNameException() throws NoSuchNameException,
			ImplementationException, NonBaseReaderException {
		final LR lr = LR.getInstance();
		lr.getConfig("getConfig");
	}

	@Test
	public void getConfigImplementationException(@Mocked final Name name,
			@Mocked final CompositeReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, NonBaseReaderException,
			ImmutableReaderException {
		final LR lr = LR.getInstance();

		new NonStrictExpectations() {
			{
				new CompositeReader(withEqual("getConfig"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.getConfig();
				result = new ImplementationException();
			}
		};

		defineSimpleReader(lr, "getConfig", true);

		try {
			lr.getConfig("getConfig");
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}
	}

	@Test
	public void getConfigNonBaseReaderException(@Mocked final Name name,
			@Mocked final CompositeReader reader) throws ValidationException,
			DuplicateNameException, ImplementationException, InUseException,
			ReaderLoopException, NoSuchNameException, NonBaseReaderException,
			ImmutableReaderException {
		final LR lr = LR.getInstance();

		new NonStrictExpectations() {
			{
				new CompositeReader(withEqual("getConfig"),
						this.<LRSpec> withNotNull());
				result = reader;

				reader.getConfig();
				result = new NonBaseReaderException();
			}
		};

		defineSimpleReader(lr, "getConfig", true);

		try {
			lr.getConfig("getConfig");
			Assert.fail("Expected NonBaseReaderException");
		} catch (NonBaseReaderException e) {
			// ignore
		}
	}

	@Test
	public void getStandardVersion() {
		Assert.assertEquals("1.1", LR.getStandardVersion());
	}

	@Test
	public void dispose(@Mocked final Name name,
			@Mocked final BaseReader reader1, @Mocked final BaseReader reader2)
			throws ValidationException, DuplicateNameException,
			ImplementationException, InUseException, ReaderLoopException,
			NoSuchNameException, ImmutableReaderException {
		final LR lr = LR.getInstance();

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("b1"), this.<LRSpec> withNotNull());
				result = reader1;

				new BaseReader(withEqual("a2"), this.<LRSpec> withNotNull());
				result = reader2;
			}
		};

		defineSimpleReader(lr, "b1", false);
		defineSimpleReader(lr, "a2", false);

		lr.dispose();

		new VerificationsInOrder() {
			{
				reader2.undefine();
				times = 1;

				reader1.undefine();
				times = 1;
			}
		};
	}

	@Test
	public void disposeALEException(@Mocked final Name name,
			@Mocked final BaseReader reader1, @Mocked final BaseReader reader2)
			throws ValidationException, DuplicateNameException,
			ImplementationException, InUseException, NoSuchNameException,
			ImmutableReaderException {
		final LR lr = LR.getInstance();

		new NonStrictExpectations() {
			{
				new BaseReader(withEqual("b1"), this.<LRSpec> withNotNull());
				result = reader1;

				new BaseReader(withEqual("a2"), this.<LRSpec> withNotNull());
				result = reader2;

				reader2.undefine();
				result = new ImplementationException();
			}
		};

		defineSimpleReader(lr, "b1", false);
		defineSimpleReader(lr, "a2", false);

		lr.dispose();

		new VerificationsInOrder() {
			{
				reader2.undefine();
				times = 1;

				reader1.undefine();
				times = 1;
			}
		};
	}
}
