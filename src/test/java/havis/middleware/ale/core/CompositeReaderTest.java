package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonBaseReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.tag.Sighting;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.depot.service.lr.LogicalReader;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.Reader;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.reader.Property;
import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class CompositeReaderTest {

    private LRProperty createProperty(String name, String value) {
        LRProperty property = new LRProperty();
        property.setName(name);
        property.setValue(value);
        return property;
    }

    private CompositeReader lock(CompositeReader reader) {
        reader.lock();
        return reader;
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

        public void reset() {
            this.object = null;
        }
    }

    private CompositeReader createCompositeReader(String name) throws ValidationException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);
        return new CompositeReader(name, compositeSpec);
    }

    private BaseReader createBaseReader(String name, final Reader reader, final ReaderController controller) throws ImplementationException,
            ValidationException, ImmutableReaderException {
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

        return new BaseReader(name, spec);
    }

    @Test
    public void compositeReader(@Mocked final LR lr) throws ValidationException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.GlimpsedTimeout, "46"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ObservedTimeThreshold, "47"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ObservedCountThreshold, "48"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "49"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        sub1.lock();
        final CompositeReader sub2 = createCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        Assert.assertFalse(reader.isPartOfComposite());
        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertTrue(sub2.isPartOfComposite());
        Assert.assertTrue(sub1.isLocked()); // lock must still be set
        Assert.assertTrue(sub2.isLocked());
        Assert.assertEquals("root", reader.getName());
        Assert.assertSame(compositeSpec, reader.getSpec());
        Assert.assertEquals(Integer.valueOf(46), reader.tagSmoothingHandler.glimpsedTimeout);
        Assert.assertEquals(Integer.valueOf(47), reader.tagSmoothingHandler.observedTimeThreshold);
        Assert.assertEquals(Integer.valueOf(48), reader.tagSmoothingHandler.observedCountThreshold);
        Assert.assertEquals(Integer.valueOf(49), reader.tagSmoothingHandler.lostTimeout);
    }

    @Test
    public void compositeReaderWithAntennaRestriction(@Mocked final LR lr, @Mocked final Reader readerManager, @Mocked final ReaderController controller)
            throws ValidationException, ReaderLoopException, NoSuchNameException, ImplementationException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.AntennaID, "15"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.GlimpsedTimeout, "46"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ObservedTimeThreshold, "47"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ObservedCountThreshold, "48"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "49"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        final BaseReader sub1 = createBaseReader("sub1", readerManager, controller);
        sub1.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        Assert.assertFalse(reader.isPartOfComposite());
        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertTrue(sub1.isLocked()); // lock must still be set
        Assert.assertEquals("root", reader.getName());
        Assert.assertSame(compositeSpec, reader.getSpec());
        Assert.assertEquals(15, reader.antenna);
        Assert.assertTrue(reader.restricted);
        Assert.assertEquals(Integer.valueOf(46), reader.tagSmoothingHandler.glimpsedTimeout);
        Assert.assertEquals(Integer.valueOf(47), reader.tagSmoothingHandler.observedTimeThreshold);
        Assert.assertEquals(Integer.valueOf(48), reader.tagSmoothingHandler.observedCountThreshold);
        Assert.assertEquals(Integer.valueOf(49), reader.tagSmoothingHandler.lostTimeout);
    }

    @Test
    public void compositeReaderWithUndefinedReader(@Mocked final LR lr) throws ValidationException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        sub1.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = new NoSuchNameException();
            }
        };

        try {
            new CompositeReader("root", compositeSpec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        Assert.assertFalse(sub1.isLocked()); // must be unlocked
        Assert.assertFalse(sub1.isPartOfComposite());
    }

    @Test
    public void compositeReaderWithBadAntennaId(@Mocked final LR lr, @Mocked final Reader readerManager, @Mocked final ReaderController controller)
            throws ValidationException, ReaderLoopException, NoSuchNameException, ImplementationException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.AntennaID, "70000"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        final BaseReader sub1 = createBaseReader("sub1", readerManager, controller);
        sub1.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;
            }
        };

        try {
            new CompositeReader("root", compositeSpec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }
        Assert.assertFalse(sub1.isLocked()); // must be unlocked
        Assert.assertFalse(sub1.isPartOfComposite());
    }

    @Test
    public void compositeReaderWithAntennaRestrictionAndMultipleReaders(@Mocked final LR lr, @Mocked final Reader readerManager,
            @Mocked final ReaderController controller) throws ValidationException, ReaderLoopException, NoSuchNameException, ImplementationException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.AntennaID, "15"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        final BaseReader sub1 = createBaseReader("sub1", readerManager, controller);
        sub1.lock();

        final BaseReader sub2 = createBaseReader("sub2", readerManager, controller);
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        try {
            new CompositeReader("root", compositeSpec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }
        Assert.assertFalse(sub1.isLocked()); // must be unlocked
        Assert.assertFalse(sub2.isLocked()); // must be unlocked
        Assert.assertFalse(sub1.isPartOfComposite());
        Assert.assertFalse(sub2.isPartOfComposite());
    }

    @Test
    public void compositeReaderWithAntennaRestrictionAndCompositeReader(@Mocked final LR lr) throws ValidationException, ReaderLoopException,
            NoSuchNameException, ImplementationException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.AntennaID, "15"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        sub1.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;
            }
        };

        try {
            new CompositeReader("root", compositeSpec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }
        Assert.assertFalse(sub1.isLocked()); // must be unlocked
        Assert.assertFalse(sub1.isPartOfComposite());
    }

    @Test(expected = ValidationException.class)
    public void compositeReaderWithUnknownProperty(@Mocked final LR lr) throws ValidationException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ReaderType + "Whatever", "42"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        new CompositeReader("reader", compositeSpec);
    }

    @Test
    public void isPartOfComposite() throws ValidationException, ImplementationException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);
        CompositeReader root = new CompositeReader("root", compositeSpec);
        CompositeReader sub = new CompositeReader("sub", compositeSpec);

        Assert.assertFalse(root.isPartOfComposite());
        Assert.assertFalse(sub.isPartOfComposite());
        root.add(sub);
        Assert.assertTrue(root.isPartOfComposite());
        Assert.assertFalse(sub.isPartOfComposite());
        root.remove(sub);
        Assert.assertFalse(root.isPartOfComposite());
        Assert.assertFalse(sub.isPartOfComposite());

        root.add(sub);
        Assert.assertTrue(root.isPartOfComposite());
        Assert.assertFalse(sub.isPartOfComposite());
        root.add(sub);
        Assert.assertTrue(root.isPartOfComposite());
        Assert.assertFalse(sub.isPartOfComposite());
        root.remove(sub);
        Assert.assertTrue(root.isPartOfComposite());
        Assert.assertFalse(sub.isPartOfComposite());
        root.remove(sub);
        Assert.assertFalse(root.isPartOfComposite());
        Assert.assertFalse(sub.isPartOfComposite());
    }

    @Test
    public void undefineReader(@Mocked final LR lr) throws ValidationException, ReaderLoopException, InUseException, ImplementationException,
            NoSuchNameException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        sub1.lock();
        final CompositeReader sub2 = createCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertTrue(sub2.isPartOfComposite());
        Assert.assertTrue(sub1.isLocked()); // lock must still be set
        Assert.assertTrue(sub2.isLocked());

        Assert.assertTrue(reader.contains(sub1));
        Assert.assertTrue(reader.contains(sub2));

        reader.undefine();

        Assert.assertFalse(reader.contains(sub1));
        Assert.assertFalse(reader.contains(sub2));

        Assert.assertFalse(sub1.isPartOfComposite());
        Assert.assertFalse(sub2.isPartOfComposite());
        Assert.assertFalse(sub1.isLocked());
        Assert.assertFalse(sub2.isLocked());
    }

    @Test
    public void undefineReaderWithPartOfCompositeReader() throws ValidationException, ImplementationException, InUseException, ReaderLoopException, ImmutableReaderException {
        CompositeReader root = createCompositeReader("root");
        CompositeReader sub = createCompositeReader("sub");

        root.add(sub);

        try {
            root.undefine();
            Assert.fail("Expected InUseException");
        } catch (InUseException e) {
            // ignore
        }
    }

    @Test
    public void updateWithSubReaders(@Mocked final LR lr) throws ImplementationException, ValidationException, InUseException, ReaderLoopException,
            NoSuchNameException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("root");

        LRSpec compositeSpec2 = new LRSpec();
        compositeSpec2.setReaders(new LRSpec.Readers());
        compositeSpec2.getReaders().getReader().add("sub1");
        compositeSpec2.getReaders().getReader().add("sub2");
        compositeSpec2.setProperties(new LRSpec.Properties());
        compositeSpec2.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        sub1.lock();
        final CompositeReader sub2 = createCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        reader.update(compositeSpec2, false);

        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertTrue(sub2.isPartOfComposite());
        Assert.assertTrue(sub1.isLocked()); // lock must still be set
        Assert.assertTrue(sub2.isLocked());
        Assert.assertSame(compositeSpec2, reader.getSpec());
    }

    @Test
    public void updateWithAntennaRestriction(@Mocked final LR lr, @Mocked final Reader readerManager, @Mocked final ReaderController controller)
            throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("root");

        LRSpec compositeSpec2 = new LRSpec();
        compositeSpec2.setReaders(new LRSpec.Readers());
        compositeSpec2.getReaders().getReader().add("sub1");
        compositeSpec2.setProperties(new LRSpec.Properties());
        compositeSpec2.getProperties().getProperty().add(createProperty(Property.AntennaID, "15"));
        compositeSpec2.setIsComposite(Boolean.TRUE);

        final BaseReader sub1 = createBaseReader("sub1", readerManager, controller);
        sub1.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;
            }
        };

        reader.update(compositeSpec2, false);

        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertTrue(sub1.isLocked()); // lock must still be set
        Assert.assertSame(compositeSpec2, reader.getSpec());
        Assert.assertEquals(15, reader.antenna);
        Assert.assertTrue(reader.restricted);
    }

    @Test
    public void updateWithIsCompositeChange() throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("root");

        LRSpec compositeSpec2 = new LRSpec();
        compositeSpec2.setReaders(new LRSpec.Readers());
        compositeSpec2.setProperties(new LRSpec.Properties());
        compositeSpec2.setIsComposite(Boolean.FALSE);

        try {
            reader.update(compositeSpec2, false);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        Assert.assertNotSame(compositeSpec2, reader.getSpec());
    }

    @Test
    public void updateWithBadAntennaId(@Mocked final LR lr, @Mocked final Reader readerManager, @Mocked final ReaderController controller)
            throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("root");

        LRSpec compositeSpec2 = new LRSpec();
        compositeSpec2.setReaders(new LRSpec.Readers());
        compositeSpec2.getReaders().getReader().add("sub1");
        compositeSpec2.setProperties(new LRSpec.Properties());
        compositeSpec2.getProperties().getProperty().add(createProperty(Property.AntennaID, "70000"));
        compositeSpec2.setIsComposite(Boolean.TRUE);

        final BaseReader sub1 = createBaseReader("sub1", readerManager, controller);
        sub1.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;
            }
        };

        try {
            reader.update(compositeSpec2, false);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        Assert.assertFalse(sub1.isPartOfComposite());
        Assert.assertFalse(sub1.isLocked()); // must be unlocked
        Assert.assertNotSame(compositeSpec2, reader.getSpec());
        Assert.assertEquals(0, reader.antenna);
        Assert.assertFalse(reader.restricted);
    }

    @Test
    public void updateWithReaderLoop(@Mocked final LR lr, @Mocked final Reader readerManager, @Mocked final ReaderController controller)
            throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("root");

        LRSpec compositeSpec2 = new LRSpec();
        compositeSpec2.setReaders(new LRSpec.Readers());
        compositeSpec2.getReaders().getReader().add("sub1");
        compositeSpec2.getReaders().getReader().add("sub2");
        compositeSpec2.setProperties(new LRSpec.Properties());
        compositeSpec2.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        sub1.lock();
        final CompositeReader sub2 = reader;
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        try {
            reader.update(compositeSpec2, false);
            Assert.fail("Expected ReaderLoopException");
        } catch (ReaderLoopException e) {
            // ignore
        }

        Assert.assertFalse(sub1.isPartOfComposite());
        Assert.assertFalse(sub2.isPartOfComposite());
        Assert.assertFalse(sub1.isLocked());
        Assert.assertFalse(sub2.isLocked());
        Assert.assertNotSame(compositeSpec2, reader.getSpec());
    }

    @Test
    public void updateWithNullReaders(@Mocked final LR lr, @Mocked final Reader readerManager, @Mocked final ReaderController controller)
            throws ImplementationException, ValidationException, InUseException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("root");
        LRSpec compositeSpec2 = new LRSpec();
        compositeSpec2.setProperties(new LRSpec.Properties());
        compositeSpec2.setIsComposite(Boolean.TRUE);
        reader.update(compositeSpec2, false);
        Assert.assertSame(compositeSpec2, reader.getSpec());
    }

    @Test
    public void addReaders(@Mocked final LR lr) throws ValidationException, ReaderLoopException, ImplementationException, InUseException, NoSuchNameException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub0");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final CompositeReader sub0 = createCompositeReader("sub0");

        final CompositeReader sub1 = createCompositeReader("sub1");
        final CompositeReader sub2 = createCompositeReader("sub2");

        new NonStrictExpectations() {
            {
                lr.lock(with(new Delegate<String>() {
                    @SuppressWarnings("unused")
                    public boolean invoked(String name) {
                        if ("sub0".equals(name)) {
                            lock(sub0);
                            return true;
                        }
                        return false;
                    }
                }));
                result = sub0;

                lr.lock(with(new Delegate<String>() {
                    @SuppressWarnings("unused")
                    public boolean invoked(String name) {
                        if ("sub1".equals(name)) {
                            lock(sub1);
                            return true;
                        }
                        return false;
                    }
                }));
                result = sub1;

                lr.lock(with(new Delegate<String>() {
                    @SuppressWarnings("unused")
                    public boolean invoked(String name) {
                        if ("sub2".equals(name)) {
                            lock(sub2);
                            return true;
                        }
                        return false;
                    }
                }));
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        Assert.assertTrue(reader.contains(sub0));
        Assert.assertFalse(reader.contains(sub1));
        Assert.assertFalse(reader.contains(sub2));
        Assert.assertTrue(sub0.isPartOfComposite());
        Assert.assertFalse(sub1.isPartOfComposite());
        Assert.assertFalse(sub2.isPartOfComposite());
        Assert.assertTrue(sub0.isLocked()); // lock must still be set

        reader.add(Arrays.asList("sub1", "sub2"), false);

        Assert.assertTrue(reader.contains(sub0));
        Assert.assertTrue(reader.contains(sub1));
        Assert.assertTrue(reader.contains(sub2));

        Assert.assertTrue(sub0.isPartOfComposite());
        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertTrue(sub2.isPartOfComposite());
        Assert.assertTrue(sub0.isLocked()); // lock must still be set
        Assert.assertTrue(sub1.isLocked());
        Assert.assertTrue(sub2.isLocked());
    }

    @Test
    public void setReaders(@Mocked final LR lr) throws ValidationException, ReaderLoopException, ImplementationException, InUseException, NoSuchNameException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("reader");

        final CompositeReader sub1 = createCompositeReader("sub1");
        sub1.lock();
        final CompositeReader sub2 = createCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        reader.set(Arrays.asList("sub1", "sub2"), false);

        Assert.assertTrue(reader.contains(sub1));
        Assert.assertTrue(reader.contains(sub2));

        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertTrue(sub2.isPartOfComposite());
        Assert.assertTrue(sub1.isLocked()); // lock must still be set
        Assert.assertTrue(sub2.isLocked());
    }

    @Test
    public void removeReaders(@Mocked final LR lr) throws ValidationException, ReaderLoopException, ImplementationException, InUseException,
            NoSuchNameException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        final CompositeReader sub2 = createCompositeReader("sub2");

        new NonStrictExpectations() {
            {
                lr.lock(with(new Delegate<String>() {
                    @SuppressWarnings("unused")
                    public boolean invoked(String name) {
                        if ("sub1".equals(name)) {
                            lock(sub1);
                            return true;
                        }
                        return false;
                    }
                }));
                result = sub1;

                lr.lock(with(new Delegate<String>() {
                    @SuppressWarnings("unused")
                    public boolean invoked(String name) {
                        if ("sub2".equals(name)) {
                            lock(sub2);
                            return true;
                        }
                        return false;
                    }
                }));
                result = sub2;
            }
        };
        CompositeReader reader = new CompositeReader("root", compositeSpec);
        Assert.assertTrue(reader.contains(sub1));
        Assert.assertTrue(reader.contains(sub2));

        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertTrue(sub2.isPartOfComposite());
        Assert.assertTrue(sub1.isLocked()); // lock must still be set
        Assert.assertTrue(sub2.isLocked());

        reader.remove(Arrays.asList("sub2"), false);

        Assert.assertTrue(reader.contains(sub1));
        Assert.assertFalse(reader.contains(sub2));

        Assert.assertTrue(sub1.isPartOfComposite());
        Assert.assertFalse(sub2.isPartOfComposite());
        Assert.assertTrue(sub1.isLocked()); // must be unlocked
        Assert.assertFalse(sub2.isLocked());
    }

    @Test
    public void removeReadersImplementationException(@Mocked final LR lr, @Mocked final LogicalReader depot) throws ValidationException, ReaderLoopException,
            ImplementationException, InUseException, NoSuchNameException, NoSuchIdException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        final CompositeReader sub2 = createCompositeReader("sub2");

        new NonStrictExpectations() {
            {
                lr.lock(with(new Delegate<String>() {
                    @SuppressWarnings("unused")
                    public boolean invoked(String name) {
                        if ("sub1".equals(name)) {
                            lock(sub1);
                            return true;
                        }
                        return false;
                    }
                }));
                result = sub1;

                lr.lock(with(new Delegate<String>() {
                    @SuppressWarnings("unused")
                    public boolean invoked(String name) {
                        if ("sub2".equals(name)) {
                            lock(sub2);
                            return true;
                        }
                        return false;
                    }
                }));
                result = sub2;

                depot.update(withEqual("root"), this.<LRSpec> withNotNull());
                result = new ReaderLoopException();
            }
        };
        CompositeReader reader = new CompositeReader("root", compositeSpec);
        try {
            reader.remove(Arrays.asList("sub2"), true);
            Assert.fail("Expected ImplementationException");
        } catch (ImplementationException e) {
            // ignore
        }
    }

    @Test
    public void contains(@Mocked final LR lr) throws ValidationException, ReaderLoopException, InUseException, ImplementationException, NoSuchNameException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final CompositeReader sub1 = createCompositeReader("sub1");
        sub1.lock();
        final CompositeReader sub2 = createCompositeReader("sub2");
        sub2.lock();

        final CompositeReader sub3 = createCompositeReader("sub3");

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);

        Assert.assertTrue(reader.contains(reader));
        Assert.assertTrue(reader.contains(sub1));
        Assert.assertTrue(reader.contains(sub2));
        Assert.assertFalse(reader.contains(sub3));
    }

    @Test
    public void defineTagOperation(@Mocked final LR lr, @Mocked final TagOperation operation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();

        final Caller<Tag> callback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.define(operation, callback, "tag");

        Assert.assertSame(operation, sub1.getLastCall().get("define").get("operation"));
        Assert.assertSame(callback, sub1.getLastCall().get("define").get("callback"));
        Assert.assertEquals("tag-" + reader.guid, sub1.getLastCall().get("define").get("name"));

        Assert.assertSame(operation, sub2.getLastCall().get("define").get("operation"));
        Assert.assertSame(callback, sub2.getLastCall().get("define").get("callback"));
        Assert.assertEquals("tag-" + reader.guid, sub2.getLastCall().get("define").get("name"));
    }

    @Test
    public void defineTagOperationWithException(@Mocked final LR lr, @Mocked final TagOperation operation) throws Exception {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();
        sub2.setThrowExceptionOnDefine(true);

        final Caller<Tag> callback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

		CompositeReader reader = new CompositeReader("root", compositeSpec);
		try {
			reader.define(operation, callback, "tag");
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}

        Assert.assertSame(operation, sub1.getLastCall().get("define").get("operation"));
        Assert.assertSame(callback, sub1.getLastCall().get("define").get("callback"));
        Assert.assertEquals("tag-" + reader.guid, sub1.getLastCall().get("define").get("name"));
        
        // undefine the first operation on error
        Assert.assertSame(operation, sub1.getLastCall().get("undefine").get("operation"));
        Assert.assertEquals("tag-" + reader.guid, sub1.getLastCall().get("undefine").get("name"));

        Assert.assertNull(sub2.getLastCall().get("define"));
        Assert.assertNull(sub2.getLastCall().get("undefine"));
    }

    @Test
    public void defineTagOperationWithAntennaRestriction(@Mocked final LR lr, @Mocked final Reader readerManager, @Mocked final ReaderController controller,
            @Mocked final TagOperation operation) throws ValidationException, ImplementationException, NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.AntennaID, "15"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        final BaseReader sub1 = createBaseReader("sub1", readerManager, controller);
        sub1.lock();

        final ValueHolder<Caller<Tag>> antennaRestrictionCallback = new ValueHolder<>();
        final ValueHolder<Tag> lastSeenTag = new ValueHolder<>();

        final Caller<Tag> originalCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag tag, ReaderController readerController) {
                Assert.assertSame(controller, readerController);
                lastSeenTag.set(tag);
            }
        };

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;
            }
        };

        final CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.define(operation, originalCallback, "tag");

        new Verifications() {
            {
                Caller<Tag> replacedCallback = null;

                controller.define(withEqual(operation), replacedCallback = withCapture(), withEqual("tag-" + reader.guid));
                times = 1;

                antennaRestrictionCallback.set(replacedCallback);
            }
        };

        Caller<Tag> callback = antennaRestrictionCallback.get();
        Assert.assertNotNull(callback);
        Assert.assertNotSame(callback, originalCallback);

        final Tag tag = new Tag(new byte[] { 0x01 });

        callback.invoke(tag, controller);
        Assert.assertNull(lastSeenTag.get());

        lastSeenTag.reset();

        tag.setSighting(new Sighting("root", (short) 16, 1));
        callback.invoke(tag, controller);
        Assert.assertNull(lastSeenTag.get());

        lastSeenTag.reset();

        tag.setSighting(new Sighting("root", (short) 15, 1));
        callback.invoke(tag, controller);
        Assert.assertSame(tag, lastSeenTag.get());
    }

    @Test
    public void defineTagOperationWithTagSmoothing(@Mocked final LR lr, @Mocked final Reader readerManager, @Mocked final ReaderController controller,
            @Mocked final TagOperation operation) throws ValidationException, ImplementationException, NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.GlimpsedTimeout, "50"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ObservedTimeThreshold, "50"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ObservedCountThreshold, "1"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "50"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        final BaseReader sub1 = createBaseReader("sub1", readerManager, controller);
        sub1.lock();

        final ValueHolder<Caller<Tag>> tagSmoothingCallback = new ValueHolder<>();
        final ValueHolder<Tag> lastSeenTag = new ValueHolder<>();

        final Caller<Tag> originalCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag tag, ReaderController readerController) {
                Assert.assertSame(controller, readerController);
                lastSeenTag.set(tag);
            }
        };

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;
            }
        };

        final CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.define(operation, originalCallback, "tag");

        new Verifications() {
            {
                Caller<Tag> replacedCallback = null;

                controller.define(withEqual(operation), replacedCallback = withCapture(), withEqual("tag-" + reader.guid));
                times = 1;

                tagSmoothingCallback.set(replacedCallback);
            }
        };

        Caller<Tag> callback = tagSmoothingCallback.get();
        Assert.assertNotNull(callback);
        Assert.assertNotSame(callback, originalCallback);

        final Tag tag = new Tag(new byte[] { 0x01 });

        callback.invoke(tag, controller);
        Assert.assertSame(tag, lastSeenTag.get());
    }

    @Test
    public void defineTagOperationWithAntennaRestrictionAndTagSmoothing(@Mocked final LR lr, @Mocked final Reader readerManager,
            @Mocked final ReaderController controller, @Mocked final TagOperation operation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.getProperties().getProperty().add(createProperty(Property.AntennaID, "15"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.GlimpsedTimeout, "50"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ObservedTimeThreshold, "50"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.ObservedCountThreshold, "1"));
        compositeSpec.getProperties().getProperty().add(createProperty(Property.LostTimeout, "50"));
        compositeSpec.setIsComposite(Boolean.TRUE);

        final BaseReader sub1 = createBaseReader("sub1", readerManager, controller);
        sub1.lock();

        final ValueHolder<Caller<Tag>> antennaRestrictionCallback = new ValueHolder<>();
        final ValueHolder<Tag> lastSeenTag = new ValueHolder<>();

        final Caller<Tag> originalCallback = new Caller<Tag>() {
            @Override
            public void invoke(Tag tag, ReaderController readerController) {
                Assert.assertSame(controller, readerController);
                lastSeenTag.set(tag);
            }
        };

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;
            }
        };

        final CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.define(operation, originalCallback, "tag");

        new Verifications() {
            {
                Caller<Tag> replacedCallback = null;

                controller.define(withEqual(operation), replacedCallback = withCapture(), withEqual("tag-" + reader.guid));
                times = 1;

                antennaRestrictionCallback.set(replacedCallback);
            }
        };

        Caller<Tag> callback = antennaRestrictionCallback.get();
        Assert.assertNotNull(callback);
        Assert.assertNotSame(callback, originalCallback);

        final Tag tag = new Tag(new byte[] { 0x01 });

        callback.invoke(tag, controller);
        Assert.assertNull(lastSeenTag.get());

        lastSeenTag.reset();

        tag.setSighting(new Sighting("root", (short) 16, 1));
        callback.invoke(tag, controller);
        Assert.assertNull(lastSeenTag.get());

        lastSeenTag.reset();

        tag.setSighting(new Sighting("root", (short) 15, 1));
        callback.invoke(tag, controller);
        Assert.assertSame(tag, lastSeenTag.get());
    }

    @Test
    public void enableTagOperation(@Mocked final LR lr, @Mocked final TagOperation operation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.enable(operation);

        Assert.assertTrue(reader.isUsed());

        Assert.assertSame(operation, sub1.getLastCall().get("enable").get("operation"));
        Assert.assertSame(operation, sub2.getLastCall().get("enable").get("operation"));
    }

    @Test
    public void disableTagOperation(@Mocked final LR lr, @Mocked final TagOperation operation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.enable(operation);
        reader.disable(operation);

        Assert.assertFalse(reader.isUsed());

        Assert.assertSame(operation, sub1.getLastCall().get("disable").get("operation"));
        Assert.assertSame(operation, sub2.getLastCall().get("disable").get("operation"));
    }

    @Test
    public void undefineTagOperation(@Mocked final LR lr, @Mocked final TagOperation operation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.undefine(operation, "tag");

        Assert.assertSame(operation, sub1.getLastCall().get("undefine").get("operation"));
        Assert.assertEquals("tag-" + reader.guid, sub1.getLastCall().get("undefine").get("name"));

        Assert.assertSame(operation, sub2.getLastCall().get("undefine").get("operation"));
        Assert.assertEquals("tag-" + reader.guid, sub2.getLastCall().get("undefine").get("name"));
    }

    @Test
    public void definePortObservation(@Mocked final LR lr, @Mocked final PortObservation observation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();

        final Caller<Port> callback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.define(observation, callback, "port");

        Assert.assertSame(observation, sub1.getLastCall().get("define").get("observation"));
        Assert.assertSame(callback, sub1.getLastCall().get("define").get("callback"));
        Assert.assertEquals("port-" + reader.guid, sub1.getLastCall().get("define").get("name"));

        Assert.assertSame(observation, sub2.getLastCall().get("define").get("observation"));
        Assert.assertSame(callback, sub2.getLastCall().get("define").get("callback"));
        Assert.assertEquals("port-" + reader.guid, sub2.getLastCall().get("define").get("name"));
    }

    @Test
    public void definePortObservationWithException(@Mocked final LR lr, @Mocked final PortObservation observation) throws Exception {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();
        sub2.setThrowExceptionOnDefine(true);

        final Caller<Port> callback = new Caller<Port>() {
            @Override
            public void invoke(Port p, ReaderController controller) {
            }
        };

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

		CompositeReader reader = new CompositeReader("root", compositeSpec);
		try {
			reader.define(observation, callback, "port");
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}

        Assert.assertSame(observation, sub1.getLastCall().get("define").get("observation"));
        Assert.assertSame(callback, sub1.getLastCall().get("define").get("callback"));
        Assert.assertEquals("port-" + reader.guid, sub1.getLastCall().get("define").get("name"));
        
        // undefine the first operation on error
        Assert.assertSame(observation, sub1.getLastCall().get("undefine").get("observation"));
        Assert.assertEquals("port-" + reader.guid, sub1.getLastCall().get("undefine").get("name"));

        Assert.assertNull(sub2.getLastCall().get("define"));
        Assert.assertNull(sub2.getLastCall().get("undefine"));
    }

    @Test
    public void enablePortObservation(@Mocked final LR lr, @Mocked final PortObservation observation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.enable(observation);

        Assert.assertTrue(reader.isUsed());

        Assert.assertSame(observation, sub1.getLastCall().get("enable").get("observation"));
        Assert.assertSame(observation, sub2.getLastCall().get("enable").get("observation"));
    }

    @Test
    public void disablePortObservation(@Mocked final LR lr, @Mocked final PortObservation observation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.enable(observation);
        reader.disable(observation);

        Assert.assertFalse(reader.isUsed());

        Assert.assertSame(observation, sub1.getLastCall().get("disable").get("observation"));
        Assert.assertSame(observation, sub2.getLastCall().get("disable").get("observation"));
    }

    @Test
    public void undefinePortObservation(@Mocked final LR lr, @Mocked final PortObservation observation) throws ValidationException, ImplementationException,
            NoSuchNameException, ReaderLoopException, ImmutableReaderException {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setReaders(new LRSpec.Readers());
        compositeSpec.getReaders().getReader().add("sub1");
        compositeSpec.getReaders().getReader().add("sub2");
        compositeSpec.setProperties(new LRSpec.Properties());
        compositeSpec.setIsComposite(Boolean.TRUE);

        final DummyCompositeReader sub1 = new DummyCompositeReader("sub1");
        sub1.lock();
        final DummyCompositeReader sub2 = new DummyCompositeReader("sub2");
        sub2.lock();

        new NonStrictExpectations() {
            {
                lr.lock("sub1");
                result = sub1;

                lr.lock("sub2");
                result = sub2;
            }
        };

        CompositeReader reader = new CompositeReader("root", compositeSpec);
        reader.undefine(observation, "port");

        Assert.assertSame(observation, sub1.getLastCall().get("undefine").get("observation"));
        Assert.assertEquals("port-" + reader.guid, sub1.getLastCall().get("undefine").get("name"));

        Assert.assertSame(observation, sub2.getLastCall().get("undefine").get("observation"));
        Assert.assertEquals("port-" + reader.guid, sub2.getLastCall().get("undefine").get("name"));
    }

    @Test(expected = NonBaseReaderException.class)
    public void getConfig() throws NonBaseReaderException, ValidationException, ReaderLoopException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("reader");
        reader.getConfig();
    }

    @Test(expected = ValidationException.class)
    public void execute() throws ValidationException, ReaderLoopException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("reader");
        reader.execute(null, null);
    }

    @Test
    public void setPropertiesWithNullPropertiesOnSpec(@Mocked final LR lr) throws Exception {
        LRSpec compositeSpec = new LRSpec();
        compositeSpec.setIsComposite(Boolean.TRUE);
        CompositeReader reader = new CompositeReader("test", compositeSpec);

        Assert.assertNull(reader.spec.getProperties());

        reader.setProperties(new ArrayList<>(Arrays.asList(createProperty(Property.ObservedCountThreshold, "1"))), false);

        Assert.assertNotNull(reader.spec.getProperties());
        Assert.assertEquals(1, reader.spec.getProperties().getProperty().size());
        Assert.assertEquals(Property.ObservedCountThreshold, reader.spec.getProperties().getProperty().get(0).getName());
        Assert.assertEquals("1", reader.spec.getProperties().getProperty().get(0).getValue());
    }

    @Test
    public void equalsTest() throws ValidationException, ReaderLoopException, ImmutableReaderException {
        CompositeReader reader = createCompositeReader("name");
        CompositeReader readerMatch = createCompositeReader("name");
        CompositeReader readerNoMatch = createCompositeReader("different");

        Assert.assertTrue(reader.equals(reader));
        Assert.assertTrue(reader.equals(readerMatch));
        Assert.assertFalse(reader.equals(readerNoMatch));
    }

    @Test
	public void hashCodeTest() throws ValidationException, ReaderLoopException,
			ImmutableReaderException {
        CompositeReader reader1 = createCompositeReader("name");
        CompositeReader reader2 = createCompositeReader("different");

        Assert.assertEquals("name".hashCode(), reader1.hashCode());
        Assert.assertEquals("different".hashCode(), reader2.hashCode());
    }
}
