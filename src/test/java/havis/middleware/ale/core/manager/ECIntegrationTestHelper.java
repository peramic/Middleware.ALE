package havis.middleware.ale.core.manager;

import havis.middleware.ale.Helper;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Assert;

public class ECIntegrationTestHelper extends Helper<ECSpec> {

    static EC manager = EC.getInstance();

    public static ECSpec define(String path, String name)
            throws FileNotFoundException {
        return define(path, name, null);
    }

    public static ECSpec define(String path, String name, ALEException exception)
            throws FileNotFoundException {
        ECSpec spec = get(path, name, ECSpec.class);
        define(name, spec, exception);
        return spec;
    }

    public static void define(String name, ECSpec spec, ALEException exception)
            throws FileNotFoundException {
        try {
            manager.define(name, spec, false);
            Assert.assertNull(exception);
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static <T extends ALEException> void undefine(String name) {
        undefine(name, null);
    }

    public static <T extends ALEException> void undefine(String name,
            ALEException exception) {
        try {
            manager.undefine(name, false);
            Assert.assertNull(exception);
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static <T extends ALEException> ECSpec getECSpec(String name) {
        return getECSpec(name, null);
    }

    public static <T extends ALEException> ECSpec getECSpec(String name,
            ALEException exception) {
        try {
            ECSpec spec = manager.getSpec(name);
            Assert.assertNull(exception);
            return spec;
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
        return null;
    }

    public static <T extends ALEException> List<String> getECSpecNames() {
        return getECSpecNames(null);
    }

    public static <T extends ALEException> List<String> getECSpecNames(
            ALEException exception) {
        try {
            List<String> list = manager.getNames();
            Assert.assertNull(exception);
            return list;
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
        return null;
    }

    public static String getStandardVersion() {
        return Config.getInstance().getService().getEc().getVersion()
                .getStandard();
    }

    public static String getVendorVersion() {
        return Config.getInstance().getService().getEc().getVersion()
                .getVendor();
    }

    public static <T extends ALEException> void subscribe(String name,
            String uri) {
        subscribe(name, uri, null);
    }

    public static <T extends ALEException> void subscribe(String name,
            String uri, ALEException exception) {
        try {
            manager.subscribe(name, uri, null, false);
            Assert.assertNull(exception);
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static <T extends ALEException> void unsubscribe(String name,
            String uri) {
        unsubscribe(name, uri, null);
    }

    public static <T extends ALEException> void unsubscribe(String name,
            String uri, ALEException exception) {
        try {
            manager.unsubscribe(name, uri, false);
            Assert.assertNull(exception);
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
    }

    public static <T extends ALEException> List<String> getSubscribers(
            String name) {
        return getSubscribers(name, null);
    }

    public static <T extends ALEException> List<String> getSubscribers(
            String name, ALEException exception) {
        try {
            List<String> list = manager.getSubscribers(name);
            Assert.assertNull(exception);
            return list;
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
        return null;
    }

    public static <T extends ALEException> ECReports immediate(String path,
            String name) {
        return immediate(path, name, null);
    }

    public static <T extends ALEException> ECReports immediate(String path,
            String name, ALEException exception) {
        ECSpec spec = get(path, name, ECSpec.class);
        return immediate(spec, exception);
    }

    public static <T extends ALEException> ECReports immediate(ECSpec spec,
            ALEException exception) {
        try {
            ECReports reports = manager.immediate(spec);
            Assert.assertNull(exception);
            return reports;
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
        return null;
    }

    public static <T extends ALEException> ECReports poll(String name) {
        return poll(name, null);
    }

    public static <T extends ALEException> ECReports poll(String name,
            ALEException exception) {
        try {
            ECReports reports = manager.poll(name);
            Assert.assertNull(exception);
            return reports;
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage(), exception);
            Assert.assertEquals(e.getClass(), exception.getClass());
            Assert.assertEquals(e.getMessage(), exception.getMessage());
        }
        return null;
    }
}