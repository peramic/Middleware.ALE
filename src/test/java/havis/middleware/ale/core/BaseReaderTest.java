package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonCompositeReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.depot.service.lr.LogicalReader;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.Reader;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.reader.Prefix;
import havis.middleware.ale.reader.Property;
import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.rc.RCConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class BaseReaderTest {

    private LRProperty createProperty(String name, String value) {
        LRProperty property = new LRProperty();
        property.setName(name);
        property.setValue(value);
        return property;
    }

    private static class ValueHolder<T> {
        private T object;

        public T set(T object) {
            this.object = object;
            return this.object;
        }

        public T get() {
            return this.object;
        }
    }

    @Test(expected = ValidationException.class)
    public void baseReaderTypeNotSpecified(@Mocked final LRSpec spec) throws ValidationException, ImplementationException, ImmutableReaderException {
        new NonStrictExpectations() {
            {
                spec.getReaders();
                result = new LRSpec.Readers();
            }
        };
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void logicalReaderNullPropertyName() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(null, "42"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void logicalReaderEmptyPropertyName() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty("", "42"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void logicalReaderDuplicateProperty() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Test", "42"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Test", "43"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void logicalReaderUnkownPrefix() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Test", "42"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Connector + "Test", "43"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Controller + "Test", "44"));
        spec.getProperties().getProperty().add(createProperty("Unkown.Test", "45"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void logicalReaderBadTagSmoothing1() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.GlimpsedTimeout, "50"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void logicalReaderBadTagSmoothing2() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "50"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void logicalReaderBadTagSmoothing3() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "abc"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void logicalReaderBadTagSmoothing4() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "-1"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void baseReaderUnkownProperty() throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty("Unknown", "test"));
        new BaseReader("name", spec);
    }

    @Test(expected = ValidationException.class)
    public void baseReaderReaderSpecified(@Mocked final LRSpec spec) throws ValidationException, ImplementationException, ImmutableReaderException {
        final LRSpec.Readers readers = new LRSpec.Readers();
        readers.getReader().add("reader");
        new NonStrictExpectations() {
            {
                spec.getReaders();
                result = readers;
            }
        };
        new BaseReader("name", spec);
    }

    @Test
    public void baseReader(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Whatever", "42"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Test", "43"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Connector + "Test", "44"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Controller + "Test", "45"));
        spec.getProperties().getProperty().add(createProperty(Property.GlimpsedTimeout, "46"));
        spec.getProperties().getProperty().add(createProperty(Property.ObservedTimeThreshold, "47"));
        spec.getProperties().getProperty().add(createProperty(Property.ObservedCountThreshold, "48"));
        spec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "49"));

        final RCConfig config = new RCConfig();

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;

                controller.getConfig();
                result = config;
            }
        };
        BaseReader baseReader = new BaseReader("name", spec);
        Assert.assertEquals("name", baseReader.getName());
        Assert.assertEquals("some type", baseReader.getType());
        Assert.assertSame(config, baseReader.getConfig());
        Assert.assertSame(spec, baseReader.getSpec());
        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
        Assert.assertEquals("43", baseReader.getPropertyValue(Prefix.Reader + "Test"));
        Assert.assertEquals("44", baseReader.getPropertyValue(Prefix.Connector + "Test"));
        Assert.assertEquals("45", baseReader.getPropertyValue(Prefix.Controller + "Test"));
        Assert.assertEquals(Integer.valueOf(46), baseReader.tagSmoothingHandler.glimpsedTimeout);
        Assert.assertEquals(Integer.valueOf(47), baseReader.tagSmoothingHandler.observedTimeThreshold);
        Assert.assertEquals(Integer.valueOf(48), baseReader.tagSmoothingHandler.observedCountThreshold);
        Assert.assertEquals(Integer.valueOf(49), baseReader.tagSmoothingHandler.lostTimeout);
    }

    @Test
    public void testLocking(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        Assert.assertFalse(baseReader.isLocked());
        baseReader.lock();
        Assert.assertTrue(baseReader.isLocked());
        baseReader.unlock();
        Assert.assertFalse(baseReader.isLocked());

        baseReader.lock();
        Assert.assertTrue(baseReader.isLocked());
        baseReader.lock();
        Assert.assertTrue(baseReader.isLocked());
        baseReader.unlock();
        Assert.assertTrue(baseReader.isLocked());
        baseReader.unlock();
        Assert.assertFalse(baseReader.isLocked());
    }

    @Test
    public void testUsage(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation,
            @Mocked final PortObservation portObservation) throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        Assert.assertFalse(baseReader.isUsed());
        baseReader.enable(tagOperation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.disable(tagOperation);
        Assert.assertFalse(baseReader.isUsed());

        baseReader.enable(tagOperation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.enable(tagOperation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.disable(tagOperation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.disable(tagOperation);
        Assert.assertFalse(baseReader.isUsed());

        baseReader.enable(portObservation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.disable(portObservation);
        Assert.assertFalse(baseReader.isUsed());

        baseReader.enable(portObservation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.enable(portObservation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.disable(portObservation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.disable(portObservation);
        Assert.assertFalse(baseReader.isUsed());

        baseReader.enable(portObservation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.enable(tagOperation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.disable(portObservation);
        Assert.assertTrue(baseReader.isUsed());
        baseReader.disable(tagOperation);
        Assert.assertFalse(baseReader.isUsed());

        new Verifications() {
            {
                controller.enable(withSameInstance(tagOperation));
                times = 4;

                controller.disable(withSameInstance(tagOperation));
                times = 4;

                controller.enable(withSameInstance(portObservation));
                times = 4;

                controller.disable(withSameInstance(portObservation));
                times = 4;
            }
        };
    }

    @Test
    public void updateWithProperty(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation,
            @Mocked final PortObservation portObservation) throws ImplementationException, ValidationException, InUseException, ReaderLoopException,
            NoSuchNameException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback1 = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Tag> tagCallback2 = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback1 = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback2 = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback1, "tag1");
        baseReader.define(tagOperation, tagCallback2, "tag2");
        baseReader.define(portObservation, portCallback1, "port1");
        baseReader.define(portObservation, portCallback2, "port2");

        Assert.assertNull(baseReader.getPropertyValue(Prefix.Reader + "Whatever"));

        LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec2.getProperties().getProperty().add(createProperty(Prefix.Reader + "Whatever", "42"));

        baseReader.update(spec2, false);

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(tagCallback1), withEqual("tag1"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback1), withEqual("port1"));
                times = 2;

                controller.define(withEqual(tagOperation), withEqual(tagCallback2), withEqual("tag2"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback2), withEqual("port2"));
                times = 2;

                controller.undefine(withEqual(tagOperation), withEqual("tag1"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port1"));
                times = 1;

                controller.undefine(withEqual(tagOperation), withEqual("tag2"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port2"));
                times = 1;

                controller.update(this.<Map<String, String>>withNotNull());
                times = 1;
            }
        };

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
    }

    @Test
    public void updateWithPersist(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation, @Mocked final PortObservation portObservation, @Mocked final LogicalReader readerDepot) throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, NoSuchIdException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback, "tag");
        baseReader.define(portObservation, portCallback, "port");

        Assert.assertNull(baseReader.getPropertyValue(Prefix.Reader + "Whatever"));

        final LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec2.getProperties().getProperty().add(createProperty(Prefix.Reader + "Whatever", "42"));

        baseReader.update(spec2, true);

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(tagCallback), withEqual("tag"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback), withEqual("port"));
                times = 2;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port"));
                times = 1;

                controller.update(this.<Map<String, String>>withNotNull());
                times = 1;

                readerDepot.update(withEqual("name"), withEqual(spec2));
                times = 1;
            }
        };

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
    }

    @Test
    public void updateWithPersistAndNoSuchId(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation, @Mocked final PortObservation portObservation, @Mocked final LogicalReader readerDepot) throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, NoSuchIdException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        final LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec2.getProperties().getProperty().add(createProperty(Prefix.Reader + "Whatever", "42"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;

                readerDepot.update(withEqual("name"), withEqual(spec2));
                result = new NoSuchIdException();
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback, "tag");
        baseReader.define(portObservation, portCallback, "port");

        Assert.assertNull(baseReader.getPropertyValue(Prefix.Reader + "Whatever"));

        try {
            baseReader.update(spec2, true);
            Assert.fail("Excpected ImplementationException");
        } catch (ImplementationException e) {
            // ignore
        }

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(tagCallback), withEqual("tag"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback), withEqual("port"));
                times = 2;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port"));
                times = 1;

                controller.update(this.<Map<String, String>>withNotNull());
                times = 2;

                readerDepot.update(withEqual("name"), withEqual(spec2));
                times = 1;
            }
        };

        Assert.assertNull(baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
    }

    @Test
	public void updateWithTypeProperty(@Mocked final Reader reader,
			@Mocked final ReaderController controller,
			@Mocked final TagOperation tagOperation,
			@Mocked final PortObservation portObservation)
			throws ImplementationException, ValidationException,
			InUseException, ReaderLoopException, NoSuchNameException,
			ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;

                reader.get("name", "some other type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback, "tag");
        baseReader.define(portObservation, portCallback, "port");

        LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, "some other type"));

        baseReader.update(spec2, false);

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(tagCallback), withEqual("tag"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback), withEqual("port"));
                times = 2;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port"));
                times = 1;

                controller.update(this.<Map<String, String>>withNotNull());
                times = 0;

                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                times = 1;

                reader.get("name", "some other type", this.<HashMap<String, String>> withNotNull());
                times = 1;
            }
        };
    }

    @Test
	public void updateWithTypePropertyError(@Mocked final Reader reader,
			@Mocked final ReaderController controller,
			@Mocked final TagOperation tagOperation,
			@Mocked final PortObservation portObservation)
			throws ImplementationException, ValidationException,
			InUseException, ReaderLoopException, NoSuchNameException,
			ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;

                reader.get("name", "some other type", this.<HashMap<String, String>> withNotNull());
                result = new ValidationException();
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback, "tag");
        baseReader.define(portObservation, portCallback, "port");

        LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, "some other type"));

		try {
			baseReader.update(spec2, false);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
                controller.define(withEqual(tagOperation), withEqual(tagCallback), withEqual("tag"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback), withEqual("port"));
                times = 2;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port"));
                times = 1;

                controller.update(this.<Map<String, String>>withNotNull());
                times = 0;

                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                times = 1;

                reader.get("name", "some other type", this.<HashMap<String, String>> withNotNull());
                times = 1;
            }
        };
    }

    @Test
    public void updateWithCompositeSpec(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation, @Mocked final PortObservation portObservation) throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback, "tag");
        baseReader.define(portObservation, portCallback, "port");

        LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec2.setIsComposite(Boolean.TRUE);

        try {
            baseReader.update(spec2, false);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(tagCallback), withEqual("tag"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback), withEqual("port"));
                times = 2;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port"));
                times = 1;
            }
        };
    }

    @Test
    public void updateWithReaderSpecified(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation, @Mocked final PortObservation portObservation) throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback, "tag");
        baseReader.define(portObservation, portCallback, "port");

        LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.getReaders().getReader().add("reader");
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        try {
            baseReader.update(spec2, false);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(tagCallback), withEqual("tag"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback), withEqual("port"));
                times = 2;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port"));
                times = 1;
            }
        };
    }

    @Test
    public void updateWithoutType(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation, @Mocked final PortObservation portObservation) throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback, "tag");
        baseReader.define(portObservation, portCallback, "port");

        LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, null));

        try {
            baseReader.update(spec2, false);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(tagCallback), withEqual("tag"));
                times = 2;

                controller.define(withEqual(portObservation), withEqual(portCallback), withEqual("port"));
                times = 2;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 1;

                controller.undefine(withEqual(portObservation), withEqual("port"));
                times = 1;
            }
        };
    }

    @Test
    public void updateWithInUse(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation, @Mocked final PortObservation portObservation) throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> tagCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        final Caller<Port> portCallback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, tagCallback, "tag");
        baseReader.define(portObservation, portCallback, "port");
        baseReader.enable(portObservation);

        LRSpec spec2 = new LRSpec();
        spec2.setReaders(new LRSpec.Readers());
        spec2.setProperties(new LRSpec.Properties());
        spec2.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        try {
            baseReader.update(spec2, false);
            Assert.fail("Expected InUseException");
        } catch (InUseException e) {
            // ignore
        }

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(tagCallback), withEqual("tag"));
                times = 1;

                controller.define(withEqual(portObservation), withEqual(portCallback), withEqual("port"));
                times = 1;

                controller.enable(withEqual(portObservation));
                times = 1;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 0;

                controller.undefine(withEqual(portObservation), withEqual("port"));
                times = 0;
            }
        };
    }

    @Test
    public void undefineReader(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ValidationException, ImplementationException,
            InUseException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.undefine();

        new Verifications() {
            {
                controller.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void undefineReaderWithUsedReader(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation) throws ValidationException,
            ImplementationException, InUseException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.enable(tagOperation);
        try {
            baseReader.undefine();
            Assert.fail("Expected InUseException");
        } catch (InUseException e) {
            // ignore
        }

        new Verifications() {
            {
                controller.enable(withEqual(tagOperation));
                times = 1;
                controller.dispose();
                times = 0;
            }
        };
    }

    @Test
    public void undefineReaderWithLockedReader(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ValidationException,
            ImplementationException, InUseException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.lock();
        try {
            baseReader.undefine();
            Assert.fail("Expected InUseException");
        } catch (InUseException e) {
            // ignore
        }

        new Verifications() {
            {
                controller.dispose();
                times = 0;
            }
        };
    }

    @Test
    public void setProperties(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ImplementationException, ValidationException, InUseException, NoSuchNameException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Whatever", "42"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
        Assert.assertNull(baseReader.getPropertyValue(Prefix.Reader + "Other"));

        List<LRProperty> properties = new ArrayList<>();
        properties.add(createProperty(Prefix.Reader + "Other", "43"));

        baseReader.setProperties(properties, false);

        new Verifications() {
            {
                controller.update(this.<Map<String, String>>withNotNull());
                times = 1;
            }
        };

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
        Assert.assertEquals("43", baseReader.getPropertyValue(Prefix.Reader + "Other"));
    }

    @Test
    public void setPropertiesOverwrite(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ImplementationException, ValidationException, InUseException, NoSuchNameException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Other", "42"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Other"));

        List<LRProperty> properties = new ArrayList<>();
        properties.add(createProperty(Prefix.Reader + "Other", "43"));

        baseReader.setProperties(properties, false);

        new Verifications() {
            {
                controller.update(this.<Map<String, String>>withNotNull());
                times = 1;
            }
        };

        Assert.assertEquals("43", baseReader.getPropertyValue(Prefix.Reader + "Other"));
    }

    @Test
    public void setPropertiesWithPersist(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final LogicalReader readerDepot) throws ImplementationException, ValidationException, InUseException, NoSuchNameException, NoSuchIdException, ReaderLoopException, ImmutableReaderException {
        final LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Whatever", "42"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
        Assert.assertNull(baseReader.getPropertyValue(Prefix.Reader + "Other"));

        List<LRProperty> properties = new ArrayList<>();
        properties.add(createProperty(Prefix.Reader + "Other", "43"));

        baseReader.setProperties(properties, true);

        new Verifications() {
            {
                controller.update(this.<Map<String, String>>withNotNull());
                times = 1;

                readerDepot.update(withEqual("name"), withNotEqual(spec)); // cloned spec
                times = 1;
            }
        };

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
        Assert.assertEquals("43", baseReader.getPropertyValue(Prefix.Reader + "Other"));
    }

    @Test
    public void setPropertiesWithReaderLoop(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final LogicalReader readerDepot) throws ImplementationException, ValidationException, InUseException, NoSuchNameException, NoSuchIdException, ReaderLoopException, ImmutableReaderException {
        final LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec.getProperties().getProperty().add(createProperty(Prefix.Reader + "Whatever", "42"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;

                readerDepot.update(withEqual("name"), this.<LRSpec>withNotNull());
                result = new ReaderLoopException();
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
        Assert.assertNull(baseReader.getPropertyValue(Prefix.Reader + "Other"));

        List<LRProperty> properties = new ArrayList<>();
        properties.add(createProperty(Prefix.Reader + "Other", "43"));

        try {
            baseReader.setProperties(properties, true);
            Assert.fail("Expected ImplementationException");
        } catch (ImplementationException e) {
            // ignore
        }

        new Verifications() {
            {
                controller.update(this.<Map<String, String>>withNotNull());
                times = 2;
            }
        };

        Assert.assertEquals("42", baseReader.getPropertyValue(Prefix.Reader + "Whatever"));
        Assert.assertNull(baseReader.getPropertyValue(Prefix.Reader + "Other"));
    }

    @Test(expected = NonCompositeReaderException.class)
    public void addReaders(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ValidationException, ImplementationException,
            NonCompositeReaderException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.add(Arrays.asList("whatever"), false);
    }

    @Test(expected = NonCompositeReaderException.class)
    public void setReaders(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ValidationException, ImplementationException,
            NonCompositeReaderException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.set(Arrays.asList("whatever"), false);
    }

    @Test(expected = NonCompositeReaderException.class)
    public void removeReaders(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ValidationException, ImplementationException,
            NonCompositeReaderException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.remove(Arrays.asList("whatever"), false);
    }

    @Test
    public void contains(@Mocked final Reader reader, @Mocked final ReaderController controller) throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        BaseReader otherBaseReader = new BaseReader("otherName", spec);
        Assert.assertTrue(baseReader.contains(baseReader));
        Assert.assertFalse(baseReader.contains(otherBaseReader));
    }

    @Test
    public void defineTagOperation(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation)
            throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> callback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, callback, "test");
        
        Assert.assertEquals(1, baseReader.tagOperations.size());

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(callback), "test");
                times = 1;
            }
        };
    }
    
    @Test
    public void defineTagOperationWithException(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation)
            throws Exception {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> callback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
                
                controller.define(withEqual(tagOperation), withEqual(callback), "test");
                result = new ImplementationException();
            }
        };

		BaseReader baseReader = new BaseReader("name", spec);

		try {
			baseReader.define(tagOperation, callback, "test");
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}

		Assert.assertEquals(0, baseReader.tagOperations.size());
	}

    @Test
    public void defineTagOperationWithTagSmoothing(@Mocked final Reader reader, @Mocked final ReaderController controller,
            @Mocked final TagOperation tagOperation) throws ValidationException, ImplementationException, InterruptedException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));
        spec.getProperties().getProperty().add(createProperty(Property.GlimpsedTimeout, "50"));
        spec.getProperties().getProperty().add(createProperty(Property.ObservedTimeThreshold, "25"));
        spec.getProperties().getProperty().add(createProperty(Property.ObservedCountThreshold, "1"));
        spec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "100"));

        final Tag tag = new Tag(new byte[] { 0x01 });

        final ValueHolder<Caller<Tag>> tagSmoothingCallback = new ValueHolder<>();
        final ValueHolder<Tag> lastSeenTag = new ValueHolder<>();

        final Caller<Tag> originalCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController readerController) {
                Assert.assertSame(controller, readerController);
                lastSeenTag.set(tag);
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, originalCallback, "test");

        new Verifications() {
            {
                Caller<Tag> replacedCallback = null;

                controller.define(withEqual(tagOperation), replacedCallback = withCapture(), "test");
                times = 1;

                tagSmoothingCallback.set(replacedCallback);
            }
        };

        Caller<Tag> callback = tagSmoothingCallback.get();
        Assert.assertNotNull(callback);
        Assert.assertNotSame(callback, originalCallback);
        callback.invoke(tag, controller);
        Assert.assertSame(tag, lastSeenTag.get());
    }

    @Test
    public void undefineTagOperation(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final TagOperation tagOperation)
            throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Tag> callback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(tagOperation, callback, "tag");
        baseReader.undefine(tagOperation, "tag");

        baseReader.define(tagOperation, callback, "test");
        baseReader.undefine(tagOperation, "test");

        new Verifications() {
            {
                controller.define(withEqual(tagOperation), withEqual(callback), withEqual("tag"));
                times = 1;

                controller.undefine(withEqual(tagOperation), withEqual("tag"));
                times = 1;

                controller.define(withEqual(tagOperation), withEqual(callback), withEqual("test"));
                times = 1;

                controller.undefine(withEqual(tagOperation), withEqual("test"));
                times = 1;
            }
        };
    }

    @Test
    public void definePortObservation(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final PortObservation portObservation)
            throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Port> callback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(portObservation, callback, "test");

        Assert.assertEquals(1, baseReader.portObservations.size());

        new Verifications() {
            {
                controller.define(withEqual(portObservation), withEqual(callback), "test");
                times = 1;
            }
        };
    }

    @Test
    public void definePortObservationWithException(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final PortObservation portObservation)
            throws Exception {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Port> callback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
                
                controller.define(withEqual(portObservation), withEqual(callback), "test");
                result = new ImplementationException();
			}
		};

		BaseReader baseReader = new BaseReader("name", spec);
		try {
			baseReader.define(portObservation, callback, "test");
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}

		Assert.assertEquals(0, baseReader.portObservations.size());
	}

    @Test
    public void undefinePortObservation(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final PortObservation portObservation)
            throws ValidationException, ImplementationException, ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        final Caller<Port> callback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.define(portObservation, callback, "port");
        baseReader.undefine(portObservation, "port");

        baseReader.define(portObservation, callback, "test");
        baseReader.undefine(portObservation, "test");

        new Verifications() {
            {
                controller.define(withEqual(portObservation), withEqual(callback), "port");
                times = 1;

                controller.undefine(withEqual(portObservation), "port");
                times = 1;

                controller.define(withEqual(portObservation), withEqual(callback), "test");
                times = 1;

                controller.undefine(withEqual(portObservation), "test");
                times = 1;
            }
        };
    }

    @Test
    public void execute(@Mocked final Reader reader, @Mocked final ReaderController controller, @Mocked final PortOperation portOperation, @Mocked final Caller<Port> callback)
			throws ImplementationException, ValidationException,
			ImmutableReaderException {
        LRSpec spec = new LRSpec();
        spec.setReaders(new LRSpec.Readers());
        spec.setProperties(new LRSpec.Properties());
        spec.getProperties().getProperty().add(createProperty(Property.ReaderType, "some type"));

        new NonStrictExpectations() {
            {
                reader.get("name", "some type", this.<HashMap<String, String>> withNotNull());
                result = controller;
            }
        };

        BaseReader baseReader = new BaseReader("name", spec);
        baseReader.execute(portOperation, callback);

        new Verifications() {
            {
                controller.execute(withEqual(portOperation), withEqual(callback));
                times = 1;
            }
        };
    }
}
