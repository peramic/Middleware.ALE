package havis.middleware.ale.core.subscriber;

import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.service.ec.ECReports;

import org.junit.Assert;
import org.junit.Test;

public class SubscriberListenerTest {
    @Test
    public void listenerTest() {
        SubscriberListener<ECReports> actual = new SubscriberListener<>(5);
        Assert.assertNull(actual.getURI());
        Assert.assertTrue(actual.getActive());
        Assert.assertFalse(actual.getStale());
        Assert.assertFalse(actual.getStale());
        Assert.assertFalse(actual.getStale());
        Assert.assertFalse(actual.getStale());
        Assert.assertTrue(actual.getStale());
        actual.setCycles(2);
        Assert.assertFalse(actual.getStale());
        Assert.assertTrue(actual.getStale());
    }
}
