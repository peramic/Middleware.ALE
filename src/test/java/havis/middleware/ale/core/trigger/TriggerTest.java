package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.trigger.Trigger.Callback;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class TriggerTest {

	@Test
	public void getInstance(@Mocked final LR lr, final @Mocked LogicalReader reader) throws ValidationException, ImplementationException, NoSuchNameException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		Assert.assertEquals("urn:epcglobal:ale:trigger:rtc:1000.500.-08:00", Trigger
				.getInstance("1", "urn:epcglobal:ale:trigger:rtc:1000.500.-08:00", callback).getUri());
		Assert.assertEquals("urn:havis:ale:trigger:http:test", Trigger.getInstance("1", "urn:havis:ale:trigger:http:test", callback).getUri());
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = reader;
			}
		};
		Assert.assertEquals("urn:havis:ale:trigger:port:test:in:1.1", Trigger.getInstance("1", "urn:havis:ale:trigger:port:test:in:1.1", callback).getUri());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInstanceNoCallback() throws ValidationException, ImplementationException {
		Trigger.getInstance("1", "urn:havis:ale:trigger:http:test", null);
	}

	@Test(expected = ValidationException.class)
	public void getInstanceEpcGlobalNotSupported() throws ValidationException, ImplementationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		Trigger.getInstance("1", "urn:epcglobal:ale:trigger:abc:1000.500.-08:00", callback);
	}

	@Test(expected = ValidationException.class)
	public void getInstanceUnsupportedHavisTrigger() throws ValidationException, ImplementationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		Trigger.getInstance("1", "urn:havis:ale:trigger:abc:test", callback);
	}

	@Test(expected = ValidationException.class)
	public void getInstanceUnknownScheme() throws ValidationException, ImplementationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		Trigger.getInstance("1", "urn:abc:ale:trigger:http:test", callback);
	}

	@Test(expected = ValidationException.class)
	public void getInstanceNoMatch() throws ValidationException, ImplementationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		Trigger.getInstance("1", "abc", callback);
	}

	@Test(expected = ValidationException.class)
	public void getInstanceEmpty() throws ValidationException, ImplementationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		Trigger.getInstance("1", "", callback);
	}
}
