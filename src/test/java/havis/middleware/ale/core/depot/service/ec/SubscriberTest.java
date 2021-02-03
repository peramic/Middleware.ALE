package havis.middleware.ale.core.depot.service.ec;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.config.SubscriberType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.EC;

import java.util.ArrayList;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SubscriberTest {

    @BeforeClass
    public static void init() {
    	Config.getInstance();
        ConfigResetter.disablePersistence();
    }

	@Test
	public void subscribe(@Mocked final EC ec) {
	    final String cycleName = "name";
	    final PropertiesType props = new PropertiesType();
	    Subscriber subscriber = new Subscriber(cycleName, new ArrayList<SubscriberType>());
		try {
			final String subscriberUri = "uri";
			new NonStrictExpectations() {
				{
					EC.getInstance();
					result = ec;
				}
			};
			for (final Boolean e : new Boolean[] { Boolean.TRUE }) {
				subscriber.subscribe(new SubscriberType() {
					{
						setName(subscriberUri);
						setEnable(e);
						setUri(subscriberUri);
						setProperties(props);
					}
				});
				new Verifications() {
					{
						ec.subscribe(cycleName, subscriberUri, props, false);
						times = 1;
					}
				};
			}
		} catch (ALEException e) {
			Assert.fail();
		}
	}

	@Test
	public void unsubscribe(@Mocked final EC ec) {
        final String cycleName = "name";
        Subscriber subscriber = new Subscriber(cycleName, new ArrayList<SubscriberType>());
        try {
			final String subscriberUri = "uri";
			new NonStrictExpectations() {
				{
					EC.getInstance();
					result = ec;
				}
			};
			for (final Boolean e : new Boolean[] { Boolean.TRUE }) {
				subscriber.unsubscribe(new SubscriberType() {
					{
						setName(subscriberUri);
						setEnable(e);
						setUri(subscriberUri);
					}
				});
				new Verifications() {
					{
						ec.unsubscribe(cycleName, subscriberUri, false);
						times = 1;
					}
				};
			}
		} catch (ALEException e) {
			Assert.fail();
		}
	}
}