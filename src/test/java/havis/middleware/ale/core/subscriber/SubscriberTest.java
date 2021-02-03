package havis.middleware.ale.core.subscriber;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.config.SubscriberConnectorType;
import havis.middleware.ale.config.SubscriberConnectorsType;
import havis.middleware.ale.core.subscriber.Subscriber;

import org.junit.Assert;
import org.junit.Test;

public class SubscriberTest {

    @Test
    public void constructorTest() {
        Subscriber actual = new Subscriber(new SubscriberConnectorsType());
        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertTrue(actual.getConnectorTypes().isEmpty());

        SubscriberConnectorsType connectors = new SubscriberConnectorsType();
        SubscriberConnectorType type = new SubscriberConnectorType();
        type.setName("test");
        type.setEnable(Boolean.FALSE);
        connectors.getSubscriber().add(type);
        actual = new Subscriber(connectors);
        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertTrue(actual.getConnectorTypes().isEmpty());

        connectors = new SubscriberConnectorsType();
        type = new SubscriberConnectorType();
        type.setName("test");
        type.setEnable(Boolean.TRUE);
        connectors.getSubscriber().add(type);
        actual = new Subscriber(connectors);
        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertFalse(actual.getConnectorTypes().isEmpty());
        Assert.assertNotNull(actual.getConnectorTypes().get("test"));
        Assert.assertSame(type, actual.getConnectorTypes().get("test"));
    }

    @Test(expected = DuplicateNameException.class)
    public void addExceptionTest() throws DuplicateNameException {
        Subscriber actual = new Subscriber(new SubscriberConnectorsType());
        SubscriberConnectorType connector1 = new SubscriberConnectorType();
        connector1.setName("a");
        actual.add(connector1);
        SubscriberConnectorType connector2 = new SubscriberConnectorType();
        connector2.setName("a");
        actual.add(connector2);
    }

    @Test
    public void addTest() throws DuplicateNameException {
        Subscriber actual = new Subscriber(new SubscriberConnectorsType());
        SubscriberConnectorType connector1 = new SubscriberConnectorType();
        connector1.setName("a");
        actual.add(connector1);
        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertFalse(actual.getConnectorTypes().isEmpty());
        Assert.assertNotNull(actual.getConnectorTypes().get("a"));
        Assert.assertSame(connector1, actual.getConnectorTypes().get("a"));
    }

    @Test
    public void removeTest() throws NoSuchNameException {
        SubscriberConnectorsType connectors = new SubscriberConnectorsType();
        SubscriberConnectorType type = new SubscriberConnectorType();
        type.setName("test");
        type.setEnable(Boolean.TRUE);
        connectors.getSubscriber().add(type);
        Subscriber actual = new Subscriber(connectors);
        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertFalse(actual.getConnectorTypes().isEmpty());

        actual.remove("test");

        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertTrue(actual.getConnectorTypes().isEmpty());
    }

    @Test(expected = NoSuchNameException.class)
    public void removeExceptionTest() throws NoSuchNameException {
        Subscriber actual = new Subscriber(new SubscriberConnectorsType());
        actual.remove("test");
    }

    @Test
    public void updateChangeNameTest() throws NoSuchNameException, DuplicateNameException {
        SubscriberConnectorsType connectors = new SubscriberConnectorsType();
        SubscriberConnectorType type = new SubscriberConnectorType();
        type.setName("test");
        type.setEnable(Boolean.TRUE);
        connectors.getSubscriber().add(type);
        Subscriber actual = new Subscriber(connectors);
        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertFalse(actual.getConnectorTypes().isEmpty());

        SubscriberConnectorType type2 = new SubscriberConnectorType();
        type2.setName("test2");
        type2.setEnable(Boolean.TRUE);

        actual.update("test", type2);

        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertFalse(actual.getConnectorTypes().isEmpty());
        Assert.assertNull(actual.getConnectorTypes().get("test"));
        Assert.assertNotNull(actual.getConnectorTypes().get("test2"));
        Assert.assertSame(type2, actual.getConnectorTypes().get("test2"));
    }

    @Test
    public void updateEnabledTest() throws NoSuchNameException, DuplicateNameException {
        SubscriberConnectorsType connectors = new SubscriberConnectorsType();
        SubscriberConnectorType type = new SubscriberConnectorType();
        type.setName("test");
        type.setEnable(Boolean.TRUE);
        connectors.getSubscriber().add(type);
        Subscriber actual = new Subscriber(connectors);
        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertFalse(actual.getConnectorTypes().isEmpty());
        Assert.assertTrue(actual.getConnectorTypes().get("test").isEnable());

        SubscriberConnectorType type2 = new SubscriberConnectorType();
        type2.setName("test");
        type2.setEnable(Boolean.FALSE);

        actual.update("test", type2);

        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertFalse(actual.getConnectorTypes().isEmpty());
        Assert.assertNotNull(actual.getConnectorTypes().get("test"));
        Assert.assertFalse(actual.getConnectorTypes().get("test").isEnable());
        Assert.assertSame(type2, actual.getConnectorTypes().get("test"));
    }

    @Test(expected = DuplicateNameException.class)
    public void updateDublicateExceptionTest() throws DuplicateNameException, NoSuchNameException {
        SubscriberConnectorsType connectors = new SubscriberConnectorsType();
        SubscriberConnectorType type1 = new SubscriberConnectorType();
        type1.setName("test1");
        type1.setEnable(Boolean.TRUE);
        connectors.getSubscriber().add(type1);
        SubscriberConnectorType type2 = new SubscriberConnectorType();
        type2.setName("test2");
        type2.setEnable(Boolean.TRUE);
        connectors.getSubscriber().add(type2);
        Subscriber actual = new Subscriber(connectors);
        Assert.assertNotNull(actual.getConnectorTypes());
        Assert.assertFalse(actual.getConnectorTypes().isEmpty());

        SubscriberConnectorType type3 = new SubscriberConnectorType();
        type3.setName("test1");
        type3.setEnable(Boolean.TRUE);

        actual.update("test2", type3);
    }

    @Test(expected = NoSuchNameException.class)
    public void updateMissingExceptionTest() throws DuplicateNameException, NoSuchNameException {
        Subscriber actual = new Subscriber(new SubscriberConnectorsType());
        actual.update("test", new SubscriberConnectorType());
    }

}
