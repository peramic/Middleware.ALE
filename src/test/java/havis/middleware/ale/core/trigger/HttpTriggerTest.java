package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.trigger.Trigger.Callback;

import java.util.concurrent.atomic.AtomicInteger;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.junit.Assert;
import org.junit.Test;

public class HttpTriggerTest {

    @Test
    public void httpTrigger(@Mocked final Name name, @Mocked final HttpTriggerService service) throws ValidationException {
        new NonStrictExpectations() {
            {
                Name.isValid("test");
                result = Boolean.TRUE;
            }
        };

        Callback callback = new Callback() {
            @Override
            public boolean invoke(Trigger trigger) {
            	return true;
            }
        };

        final HttpTrigger trigger = new HttpTrigger("1", "urn:havis:ale:trigger:http:test", callback);
        Assert.assertEquals("urn:havis:ale:trigger:http:test", trigger.getUri());
        Assert.assertEquals("test", trigger.getName());

        new Verifications() {
            {
                service.add(withSameInstance(trigger));
                times = 1;
            }
        };
    }

    @Test
    public void httpTriggerInvalidName(@Mocked final Name name, @Mocked final HttpTriggerService service) throws ValidationException {
        new NonStrictExpectations() {
            {
                Name.isValid("test");
                result = Boolean.FALSE;
            }
        };

        Callback callback = new Callback() {
            @Override
            public boolean invoke(Trigger trigger) {
            	return true;
            }
        };

        final HttpTrigger trigger = new HttpTrigger("1", "urn:havis:ale:trigger:http:test", callback);
        Assert.assertEquals("urn:havis:ale:trigger:http:test", trigger.getUri());
        Assert.assertEquals("test", trigger.getName());

        new Verifications() {
            {
                service.add(withSameInstance(trigger));
                times = 0;
            }
        };
    }

	@Test(expected = ValidationException.class)
	public void httpTriggerNoMatch() throws ValidationException {
        Callback callback = new Callback() {
            @Override
            public boolean invoke(Trigger trigger) {
            	return true;
            }
        };
		new HttpTrigger("1", "testUri", callback);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void httpTriggerNoCallback() throws ValidationException {
		new HttpTrigger("1", "urn:havis:ale:trigger:http:test", null);
	}

	@Test
	public void invoke(@Mocked final Name name, @Mocked final HttpTriggerService service) throws ValidationException {
        new NonStrictExpectations() {
            {
                Name.isValid("test");
                result = Boolean.TRUE;
            }
        };

        final AtomicInteger count = new AtomicInteger(0);
        Callback callback = new Callback() {
            @Override
            public boolean invoke(Trigger trigger) {
                count.incrementAndGet();
                Assert.assertNotNull(trigger);
                return true;
            }
        };

        HttpTrigger trigger = new HttpTrigger("1", "urn:havis:ale:trigger:http:test", callback);
        trigger.invoke();

        Assert.assertEquals(1, count.intValue());
	}

	@Test
	public void dispose(@Mocked final Name name, @Mocked final HttpTriggerService service) throws ValidationException {
        new NonStrictExpectations() {
            {
                Name.isValid("test");
                result = Boolean.TRUE;
            }
        };

        Callback callback = new Callback() {
            @Override
            public boolean invoke(Trigger trigger) {
            	return true;
            }
        };

        final HttpTrigger trigger = new HttpTrigger("1", "urn:havis:ale:trigger:http:test", callback);
        trigger.dispose();

        new VerificationsInOrder() {
            {
                service.add(withSameInstance(trigger));
                times = 1;

                service.remove(withSameInstance(trigger));
                times = 1;
            }
        };
	}
}
