package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.trigger.Trigger.Callback;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class HttpTriggerServiceTest {

	@Test
	public void addRemove() throws ValidationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		HttpTriggerService service = HttpTriggerService.getInstance();
		HttpTrigger t1 = new HttpTrigger("1", "urn:havis:ale:trigger:http:name", callback);
		List<HttpTrigger> list = service.triggers.get("name");
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.size());
		Assert.assertSame(t1, list.get(0));

		HttpTrigger t2 = new HttpTrigger("2", "urn:havis:ale:trigger:http:name", callback);
		list = service.triggers.get("name");
		Assert.assertNotNull(list);
		Assert.assertEquals(2, list.size());
		Assert.assertSame(t2, list.get(1));

		// remove
		service.remove(t1);
		list = service.triggers.get("name");
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.size());
		Assert.assertSame(t2, list.get(0));

		service.remove(t2);
		Assert.assertNull(service.triggers.get("name"));
	}

	@Test
	public void handle() throws ValidationException {
		HttpTriggerService service = HttpTriggerService.getInstance();

		final AtomicInteger count1 = new AtomicInteger(0);
		final AtomicBoolean result1 = new AtomicBoolean(true);
		Callback c1 = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				count1.incrementAndGet();
				return result1.get();
			}
		};

		final AtomicInteger count2 = new AtomicInteger(0);
		final AtomicBoolean result2 = new AtomicBoolean(true);
		Callback c2 = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				count2.incrementAndGet();
				return result2.get();
			}
		};

		HttpTrigger t1 = new HttpTrigger("1", "urn:havis:ale:trigger:http:test", c1);
		HttpTrigger t2 = new HttpTrigger("2", "urn:havis:ale:trigger:http:test", c2);

		service.handle("test");

		Assert.assertEquals(1, count1.get());
		Assert.assertEquals(1, count2.get());

		count1.set(0);
		count2.set(0);

		t1.dispose();

		service.handle("test");

		Assert.assertEquals(0, count1.get());
		Assert.assertEquals(1, count2.get());

		count1.set(0);
		count2.set(0);

		t2.dispose();

		service.handle("test");

		Assert.assertEquals(0, count1.get());
		Assert.assertEquals(0, count2.get());

		count1.set(0);
		count2.set(0);

		t1 = new HttpTrigger("3", "urn:havis:ale:trigger:http:test", c1);
		t2 = new HttpTrigger("3", "urn:havis:ale:trigger:http:test", c2);

		service.handle("test");

		Assert.assertEquals(1, count1.get());
		Assert.assertEquals(0, count2.get());

		count1.set(0);
		count2.set(0);

		result1.set(false);
		result2.set(true);
		service.handle("test");

		Assert.assertEquals(1, count1.get());
		Assert.assertEquals(1, count2.get());

		count1.set(0);
		count2.set(0);

		t1.dispose();
		t2.dispose();
	}
}
