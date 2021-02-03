package havis.middleware.ale;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;

public class Helper<T> {

    public static Unmarshaller createUnmarshaller(Class<?> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        return context.createUnmarshaller();
    }

    public static Marshaller createMarshaller(Class<?> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        return context.createMarshaller();
    }

    public static <T> T get(String path, String name, Class<T> clazz) {
        String s = path + "/" + name + ".xml";
        InputStream stream = Helper.class.getResourceAsStream(s);
        try {
            Assert.assertNotNull("Resource stream is null '" + s + "'", stream);
            return createUnmarshaller(clazz).unmarshal(new StreamSource(stream), clazz).getValue();
        } catch (JAXBException e) {
            Assert.fail("Deserialization of '" + s + "' failed! " + e.getMessage());
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }

    public static void set(String path, String name, JAXBElement<?> element) throws IOException {
        String s = path + "/" + name + ".xml";
        try {
            createMarshaller(element.getDeclaredType()).marshal(element, new File(s));
        } catch (JAXBException e) {
            Assert.fail("Serialization of '" + s + "' failed! " + e.getMessage());
        }
    }
}