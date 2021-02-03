package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.service.tm.TMFixedFieldListSpec;
import havis.middleware.ale.service.tm.TMSpec;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;

public class TMIntegrationTestHelper {

	static TM manager = TM.getInstance();

    private static Unmarshaller unmarshaller;

    static {
        {
            try {
                JAXBContext context = JAXBContext.newInstance(TMFixedFieldListSpec.class);
                unmarshaller = context.createUnmarshaller();
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
    }

    static TMFixedFieldListSpec get(String path, String name) {
        String s = path + "/" + name + ".xml";
        InputStream stream = TMIntegrationTestHelper.class.getResourceAsStream(s);
        try {
            Assert.assertNotNull("Resource stream is null '" + s + "'", stream);
            return unmarshaller.unmarshal(new StreamSource(stream), TMFixedFieldListSpec.class).getValue();
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

	public static TMFixedFieldListSpec define(String path, String name)
			throws FileNotFoundException {
		return define(path, name, null);
	}

	public static TMFixedFieldListSpec define(String path, String name,
			ALEException exception) throws FileNotFoundException {
		TMFixedFieldListSpec spec = get(path, name);
		define(name, spec, exception);
		return spec;
	}

	public static void define(String name, TMFixedFieldListSpec spec,
			ALEException exception) throws FileNotFoundException {
		try {
			manager.define(name, spec, false);
			Assert.assertNull(exception);
		} catch (Exception e) {
			Assert.assertNotNull(e.getMessage(), exception);
			Assert.assertEquals(e.getClass(), exception.getClass());
			Assert.assertEquals(e.getMessage(), exception.getMessage());
		}
	}

	public static void getStandardVersion(ALEException exception) {
		Config.getInstance().getService().getEc().getVersion().getStandard();
	}

	public static TMSpec getTMSpec(String name, ALEException exception) {
		try {
			manager.getSpec(name);
		} catch (Exception e) {
			Assert.assertSame(e, exception);
			Assert.assertEquals(e.getMessage(), exception.getMessage());
		}
		return null;
	}

	public static List<String> getTMSpecNames(ALEException exception) {
		try {
			return manager.getNames();
		} catch (Exception e) {
			Assert.assertSame(e, exception);
			Assert.assertEquals(e.getMessage(), exception.getMessage());
		}
		return null;
	}

	public static void getVendorVersion(ALEException exception) {
		Config.getInstance().getService().getEc().getVersion().getVendor();
	}

	public static void undefineTMSpec(String name) {
		undefineTMSpec(name, null);
	}

	public static void undefineTMSpec(String name, ALEException exception) {
		try {
			manager.undefine(name, false);
		} catch (Exception e) {
			Assert.assertSame(e, exception);
			Assert.assertEquals(e.getMessage(), exception.getMessage());
		}
	}
}
