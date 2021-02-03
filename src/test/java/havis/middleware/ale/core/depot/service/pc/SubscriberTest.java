package havis.middleware.ale.core.depot.service.pc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.PC;

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
    public void subscribe(@Mocked final PC pc) throws ALEException {
        final String subscriberName = "name";
        final String subscriberUri = "uri";
        final PropertiesType props = new PropertiesType();
        new NonStrictExpectations() {
            {
                PC.getInstance();
                result = pc;
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
                pc.subscribe(subscriberName, subscriberUri, props, false);
            }
        };
    }

    @Test
    public void unsubscribe(@Mocked final PC pc) throws ALEException {
        final String subscriberName = "name";
        final String subscriberUri = "uri";
        new NonStrictExpectations() {
            {
                PC.getInstance();
                result = pc;
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
                pc.unsubscribe(subscriberName, subscriberUri, false);
            }
        };
    }
}
