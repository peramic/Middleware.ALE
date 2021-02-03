package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.CC;

import java.util.ArrayList;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.BeforeClass;
import org.junit.Test;

public class SubscriberTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void subscribe(@Mocked final CC cc) throws ALEException {
        final String subscriberName = "name";
        final String subscriberUri = "uri";
        final PropertiesType props = new PropertiesType();
        new NonStrictExpectations() {
            {
                CC.getInstance();
                result = cc;
            }
        };
        new Subscriber(subscriberName, new ArrayList<SubscriberType>()).subscribe(new SubscriberType() {
            {
                setName(subscriberUri);
                setEnable(Boolean.TRUE);
                setUri(subscriberUri);
                setProperties(props);
            }
        });
        new Verifications() {
            {
                cc.subscribe(subscriberName, subscriberUri, props, false);
            }
        };
    }

    @Test
    public void unsubscribe(@Mocked final CC cc) throws ALEException {
        final String subscriberName = "name";
        final String subscriberUri = "uri";
        new NonStrictExpectations() {
            {
                CC.getInstance();
                result = cc;
            }
        };
        new Subscriber(subscriberName, new ArrayList<SubscriberType>()).unsubscribe(new SubscriberType() {
            {
                setName(subscriberUri);
                setEnable(Boolean.TRUE);
                setUri(subscriberUri);
            }
        });
        new Verifications() {
            {
                cc.unsubscribe(subscriberName, subscriberUri, false);
            }
        };
    }
}
