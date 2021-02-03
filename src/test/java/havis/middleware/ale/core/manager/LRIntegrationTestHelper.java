package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;

public class LRIntegrationTestHelper {

    static havis.middleware.ale.core.manager.LR manager = havis.middleware.ale.core.manager.LR
            .getInstance();

    private static Unmarshaller unmarshaller;

    static {
        {
            try {
                JAXBContext context = JAXBContext.newInstance(LRSpec.class);
                unmarshaller = context.createUnmarshaller();
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
    }

    static LRSpec get(String path, String name) {
        String s = path + "/" + name + ".xml";
        InputStream stream = LRIntegrationTestHelper.class.getResourceAsStream(s);
        try {
            Assert.assertNotNull("Resource stream is null '" + s + "'", stream);
            return unmarshaller.unmarshal(new StreamSource(stream), LRSpec.class).getValue();
        } catch (JAXBException e) {
            Assert.fail("Parsing of '" + s + "' failed! " + e.getMessage());
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }

    public static void define(String name, LRSpec spec, ALEException exception) {
        try {
            manager.define(name, spec, false);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static LRSpec define(String path, String name) {
        return define(path, name, null);
    }

    public static LRSpec define(String path, String name, ALEException exception) {
        LRSpec spec = get(path, name);
        try {
            manager.define(name, spec, false);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
        return spec;
    }

    public static void undefine(String name) {
        undefine(name, null);
    }

    public static void undefine(String name, ALEException exception) {
        try {
            manager.undefine(name, false);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static void update(String name, LRSpec spec, ALEException exception) {
        try {
            manager.update(name, spec, false);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static LRSpec getLRSpec(String name, ALEException exception) {
        try {
            return manager.getSpec(name);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
        return null;
    }

    public static List<String> getLogicalReaderNames()
            throws ImplementationException {
        return manager.getNames();
    }

    public static void addReaders(String name, List<String> readers,
            ALEException exception) {
        try {
            manager.addReaders(name, readers, false);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static void setReaders(String name, List<String> readers,
            ALEException exception) {
        try {
            manager.setReaders(name, readers, false);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static void removeReaders(String name, List<String> readers,
            ALEException exception) {
        try {
            manager.removeReaders(name, readers, false);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static void setProperties(String name, List<LRProperty> properties,
            ALEException exception) {
        try {
            manager.setProperties(name, properties, false);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static String getPropertyValue(String name, String property,
            ALEException exception) {
        try {
            return manager.getPropertyValue(name, property);
        } catch (Exception e) {
            Assert.assertSame(e, exception);
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
        return null;
    }

    public static void defineReaderOperation(String name,
            havis.middleware.ale.base.operation.tag.TagOperation operation,
            Caller<Tag> callback) throws ImplementationException,
            NoSuchNameException, ValidationException {
        get(name).define(operation, callback, "");
    }

    public static void enableReaderOperation(String name, TagOperation operation)
            throws ImplementationException, NoSuchNameException {
        get(name).enable(operation);
    }

    public static void disableReaderOperation(String name,
            TagOperation operation) throws ImplementationException,
            NoSuchNameException {
        get(name).disable(operation);
    }

    public static void undefineReaderOperation(String name,
            TagOperation operation) throws ImplementationException,
            NoSuchNameException {
        get(name).undefine(operation, "");
    }

    public static LogicalReader get(String name) throws NoSuchNameException {
        return manager.get(name);
    }
}