package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.operation.Event;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

public class EventsTest {

    @Test
    public void events() {
        Events events = new Events();
        Assert.assertEquals(0, events.getCount());
        Assert.assertEquals(0, events.await(null));
    }

    @Test
    public void add() {
        Events events = new Events();
        Event event = new Event("test");
        Assert.assertNull(events.get(event));
        events.add(event, event);
        Assert.assertEquals(event, events.get(event));

        Event event2 = new Event("test2");
        Assert.assertNull(events.get(event2));
        events.add(event2, null);
        Assert.assertNull(events.get(event2));
    }

    @Test
    public void remove() {
        Events events = new Events();
        Event event = new Event("test");
        Assert.assertNull(events.get(event));
        events.add(event, event);
        Assert.assertEquals(event, events.get(event));

        events.remove(event);
        Assert.assertNull(events.get(event));
    }

    @Test
    public void clear() {
        Events events = new Events();
        Event event = new Event("test");
        events.add(event, event);
        events.pulse();
        Assert.assertEquals(1, events.getCount());
        Assert.assertEquals(1, events.await(null));

        events.clear();
        Assert.assertEquals(0, events.getCount());
        Assert.assertEquals(1, events.await(null));
    }

    @Test
    public void rotate() {
        Events events = new Events();
        Event event = new Event("test");
        events.add(event, event);
        events.pulse();
        Assert.assertEquals(1, events.getCount());
        Assert.assertEquals(1, events.await(null));

        events.rotate(); // does nothing

        Assert.assertEquals(1, events.getCount());
        Assert.assertEquals(1, events.await(null));
    }

    @Test
    public void reset() throws InterruptedException {
        final Events events = new Events();
        Event firstEvent = new Event("1");
        Event secondEvent = new Event("2");
        Event thirdEvent = new Event("3");

        events.add(firstEvent, firstEvent);
        events.pulse();
        events.add(secondEvent, secondEvent);
        events.pulse();

        Assert.assertEquals(2, events.getCount());
        Assert.assertNotNull(events.get(new Event("1")));
        Assert.assertNotNull(events.get(new Event("2")));
        Assert.assertNull(events.get(new Event("3")));

        events.reset();

        Assert.assertEquals(0, events.getCount());
        Assert.assertNull(events.get(new Event("1")));
        Assert.assertNull(events.get(new Event("2")));
        Assert.assertNull(events.get(new Event("3")));


        events.add(thirdEvent, null);
        events.pulse();

        // call reset in another thread, which waits because there is an unprocessed event
        final CountDownLatch ready = new CountDownLatch(1);
        final AtomicLong waitMs = new AtomicLong(0L);
        Thread waitingTagAndPulse = new Thread(new Runnable() {
            @Override
            public void run() {
                ready.countDown();
                long start = System.currentTimeMillis();
                events.reset();
                long end = System.currentTimeMillis();
                waitMs.set(end - start);
            }
        });
        waitingTagAndPulse.start();

        Assert.assertTrue("Thread is ready", ready.await(100, TimeUnit.MILLISECONDS));
        Thread.sleep(10);

        // reset is waiting

        Assert.assertEquals(1, events.getCount());

        // process the tag
        events.set(thirdEvent, thirdEvent);

        Assert.assertEquals(1, events.getCount());
        Assert.assertNull(events.get(new Event("1")));
        Assert.assertNull(events.get(new Event("2")));
        Assert.assertNotNull(events.get(new Event("3")));

        events.pulse();

        Thread.sleep(10);

        Assert.assertFalse("Thread is not alive", waitingTagAndPulse.isAlive());

        Assert.assertNull(events.get(new Event("1")));
        Assert.assertNull(events.get(new Event("2")));
		Assert.assertNull(events.get(new Event("3")));
	}

	@Test
	public void cloneTest() {
		Events events = new Events();
		Event event = new Event("test");
		events.add(event, event);
		events.pulse();
		Assert.assertEquals(1, events.getCount());
		Assert.assertEquals(1, events.await(null));

		Events clone = events.clone();
		Assert.assertEquals(1, clone.getCount());
		Assert.assertEquals(1, clone.await(null));
		Assert.assertFalse(clone.isDisposed());

		events.clear();
		Assert.assertEquals(1, clone.getCount());
		Assert.assertEquals(1, clone.await(null));

		events = new Events();
		events.dispose();
		clone = (Events) events.clone();

		Assert.assertTrue(clone.isDisposed());
	}

	@Test
    public void set() {
        Events events = new Events();
        Event event = new Event("test");
        Assert.assertNull(events.get(event));
        events.add(event, null);
        Assert.assertNull(events.get(event));

        events.set(event, event);
        Assert.assertEquals(event, events.get(event));
    }
    
    @Test
    public void contains() {
        Events events = new Events();
        Event event = new Event("test");
        Assert.assertNull(events.get(event));
        events.add(event, null);
        Assert.assertNull(events.get(event));
        Assert.assertTrue(events.contains(event));

        events.set(event, event);
        Assert.assertEquals(event, events.get(event));
        Assert.assertTrue(events.contains(event));
    }

    @Test
    public void toList() {
        Events events = new Events();
        Event event1 = new Event("test1");
        events.add(event1, event1);
        Event event2 = new Event("test2");
        events.add(event2, event2);

        Set<Event> list = events.toList();
        Assert.assertNotNull(list);
        Assert.assertEquals(2, list.size());
        Iterator<Event> iterator = list.iterator();
        Assert.assertEquals(event1, iterator.next());
        Assert.assertEquals(event2, iterator.next());
	}

	@Test
	public void dispose() throws Exception {
		Event event = new Event("1");
		final Events events = new Events();

		Assert.assertFalse(events.isDisposed());

		events.add(event, null);
		events.pulse();

		// call reset in another thread, which waits because there is an unprocessed event
		final CountDownLatch ready = new CountDownLatch(1);
		final AtomicLong waitMs = new AtomicLong(0L);
		Thread waitingTagAndPulse = new Thread(new Runnable() {
			@Override
			public void run() {
				ready.countDown();
				long start = System.currentTimeMillis();
				events.reset();
				long end = System.currentTimeMillis();
				waitMs.set(end - start);
			}
		});
		waitingTagAndPulse.start();

		Assert.assertTrue("Thread is ready", ready.await(100, TimeUnit.MILLISECONDS));
		Thread.sleep(10);

		// reset is waiting

		Assert.assertEquals(1, events.getCount());

		events.dispose();

		Assert.assertTrue(events.isDisposed());
		Assert.assertEquals(0, events.await(Long.valueOf(1)));

		Assert.assertEquals(0, events.getCount());
		Assert.assertNull(events.get(new Event("1")));

		Thread.sleep(10);

		Assert.assertFalse("Thread is not alive", waitingTagAndPulse.isAlive());
	}

	@Test
	public void toStringTest() {
		Events events = new Events();
		Event event = new Event("1");
		event.setFirstTime(new Date(0));
		event.setLastTime(new Date(0));
		events.add(event, null);

		Assert.assertEquals("Events [events={Event [uri=1, result={}, firstTime=Thu Jan 01 01:00:00 CET 1970, lastTime=Thu Jan 01 01:00:00 CET 1970, count=0]=null}, counter=Counter [counter=0]]", events.toString());
	}
}
