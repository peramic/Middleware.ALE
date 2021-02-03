package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.config.ReaderCycleType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.utils.generic.LinkedHashSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TagsTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void tags() {
        Tags tags = new Tags();
        Assert.assertEquals(0, tags.getCount());
        Assert.assertEquals(0, tags.await(null));
    }

    @Test
    public void tagsTagsListCounter() {
        LinkedHashSet<Tag> list1 = new LinkedHashSet<Tag>();
        Tag tag = new Tag(new byte[] { 0x00 });
        list1.update(tag);
        List<Tag> list2 = new ArrayList<Tag>();
        list2.add(new Tag());
        Counter counter = new Counter();
        counter.pulse();
        Tags tags = new Tags(list1, list2, counter, new AtomicBoolean(false));
        Assert.assertSame(tag, tags.get(tag));
        Assert.assertEquals(1, tags.getCount());
        Assert.assertEquals(1, tags.await(null));
    }

    @Test
    public void clear(@Mocked final ReaderCycleType type) {
        new NonStrictExpectations() {
            {
                type.getCount();
                result = Integer.valueOf(2);

                type.getLifetime();
                result = Long.valueOf(1000);
            }
        };
        Tags tags = new Tags();
        Tag firstTag = new Tag(new byte[] { 0x01 });
        Tag secondTag = new Tag(new byte[] { 0x02 });
        Tag thirdTag = new Tag(new byte[] { 0x03 });

        tags.add(firstTag, firstTag);
        tags.add(secondTag, secondTag);
        tags.add(thirdTag, thirdTag);

        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x03 })));

        tags.clear();

        Assert.assertNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x03 })));

        tags.clear();

        Assert.assertNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x03 })));

        secondTag.setFirstTime(new Date(new Date().getTime() - 2000));

        tags.clear();

        Assert.assertNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x03 })));

        thirdTag.setFirstTime(new Date(new Date().getTime() - 2000));

        tags.clear();

        Assert.assertNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x03 })));
    }

    @Test
    public void rotate() {
        LinkedHashSet<Tag> list1 = new LinkedHashSet<Tag>();
        Tag tag = new Tag(new byte[] { 0x00 });
        list1.update(tag);
        List<Tag> list2 = new ArrayList<Tag>();
        list2.add(tag);
        Counter counter = new Counter();
        Tags tags = new Tags(list1, list2, counter, new AtomicBoolean(false));
        Assert.assertEquals(1, tags.getCount());
        tags.rotate();
        Assert.assertEquals(0, tags.getCount());
    }

    @Test
    public void reset() throws InterruptedException {
        final Tags tags = new Tags();
        Tag firstTag = new Tag(new byte[] { 0x01 });
        Tag secondTag = new Tag(new byte[] { 0x02 });
        Tag thirdTag = new Tag(new byte[] { 0x03 });

        tags.add(firstTag, firstTag);
        tags.pulse();
        tags.add(secondTag, secondTag);
        tags.pulse();

        Assert.assertEquals(2, tags.getCount());
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x03 })));

        tags.reset();

        Assert.assertEquals(0, tags.getCount());
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x03 })));


        tags.add(thirdTag, null);
        tags.pulse();

        // call reset in another thread, which waits because there is an unprocessed tag
        final CountDownLatch ready = new CountDownLatch(1);
        final AtomicLong waitMs = new AtomicLong(0L);
        Thread waitingTagAndPulse = new Thread(new Runnable() {
            @Override
            public void run() {
                ready.countDown();
                long start = System.currentTimeMillis();
                tags.reset();
                long end = System.currentTimeMillis();
                waitMs.set(end - start);
            }
        });
        waitingTagAndPulse.start();

        Assert.assertTrue("Thread is ready", ready.await(100, TimeUnit.MILLISECONDS));
        Thread.sleep(10);

        // reset is waiting

        Assert.assertEquals(1, tags.getCount());

        // process the tag
        tags.put(thirdTag, thirdTag);

        Assert.assertEquals(1, tags.getCount());
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNotNull(tags.get(new Tag(new byte[] { 0x03 })));

        tags.pulse();

        Thread.sleep(10);

        Assert.assertFalse("Thread is not alive", waitingTagAndPulse.isAlive());

        Assert.assertNull(tags.get(new Tag(new byte[] { 0x01 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x02 })));
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x03 })));
    }

    @Test
    public void cloneTest() {
        LinkedHashSet<Tag> list1 = new LinkedHashSet<Tag>();
        Tag tag = new Tag(new byte[] { 0x00 });
        list1.update(tag);
        List<Tag> list2 = new ArrayList<Tag>();
        list2.add(new Tag());
        Counter counter = new Counter();
        counter.pulse();
        Tags tags = new Tags(list1, list2, counter, new AtomicBoolean(false));
        Tags clone = (Tags) tags.clone();

        Assert.assertSame(tag, clone.get(tag));
        Assert.assertEquals(1, clone.getCount());
        Assert.assertEquals(1, clone.await(null));
        Assert.assertFalse(clone.isDisposed());
        Assert.assertTrue(tags.hasSameData(clone));

        tags = new Tags();
        tags.dispose();
        clone = (Tags) tags.clone();

        Assert.assertTrue(clone.isDisposed());
    }

    @Test
    public void toList() {
        Tag tag1 = new Tag(new byte[] { 0x01 });
        Tag tag2 = new Tag(new byte[] { 0x02 });
        Tag tag3 = new Tag(new byte[] { 0x03 });
        Tags tags = new Tags();
        tags.add(tag1, tag1);
        tags.add(tag2, tag2);
        tags.add(tag3, tag3);

        List<Tag> list = tags.toList();
        Assert.assertNotNull(list);
        Assert.assertEquals(3, list.size());
        Assert.assertSame(tag1, list.get(0));
        Assert.assertSame(tag2, list.get(1));
        Assert.assertSame(tag3, list.get(2));

        tags.toList().remove(0);
        list = tags.toList();
        Assert.assertNotNull(list);
        Assert.assertEquals(3, list.size());
        Assert.assertSame(tag1, list.get(0));
        Assert.assertSame(tag2, list.get(1));
        Assert.assertSame(tag3, list.get(2));
    }

    @Test
    public void add() {
        Tag tag = new Tag(new byte[] { 0x01 });
        Tags tags = new Tags();
        tags.add(tag);

        Tag added = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertSame(tag, added);
        Assert.assertEquals(0, tags.getCount()); // TODO: why is it not added to the list?
    }

    @Test
    public void remove() {
        Tag tag = new Tag(new byte[] { 0x01 });
        Tags tags = new Tags();
        tags.add(tag, tag);

        Tag added = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertSame(tag, added);
        Assert.assertEquals(1, tags.getCount());

        tags.remove(tag);

        Tag removed = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertNull(removed);
        Assert.assertEquals(1, tags.getCount()); // TODO: why is it not removed from the list?
    }

    @Test
    public void put() {
        Tag tag = new Tag(new byte[] { 0x01 });
        Tags tags = new Tags();
        tags.add(tag, null);

        Tag added = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertNull(added);

        tags.put(tag, tag);

        Tag put = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertSame(tag, put);
    }
    
    @Test
    public void contains() {
        Tag tag = new Tag(new byte[] { 0x01 });
        Tags tags = new Tags();
        tags.add(tag, null);

        Tag added = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertNull(added);
        Assert.assertTrue(tags.contains(new Tag(new byte[] { 0x01 })));

        tags.put(tag, tag);

        Tag put = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertSame(tag, put);
        Assert.assertTrue(tags.contains(new Tag(new byte[] { 0x01 })));
    }
    
    @Test
    public void removeLifetimeExceededAndCheckWhetherSeen() throws InterruptedException {
        Tag tag = new Tag(new byte[] { 0x01 });
        Tags tags = new Tags();
        tags.add(tag, null);

        Tag added = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertNull(added);
        Assert.assertTrue(tags.removeLifetimeExceededAndCheckWhetherSeen(new Tag(new byte[] { 0x01 })));

        tags.put(tag, tag);

        Tag put = tags.get(new Tag(new byte[] { 0x01 }));
        Assert.assertSame(tag, put);
        Assert.assertTrue(tags.removeLifetimeExceededAndCheckWhetherSeen(new Tag(new byte[] { 0x01 })));
        
        long delay = 50;

        tag = new Tag(new byte[] { 0x02 });
        tag.setFirstTime(new Date(new Date().getTime() - (Config.getInstance().getGlobal().getReaderCycle().getLifetime() - delay)));
        tags = new Tags();
        tags.add(tag, null);

        added = tags.get(new Tag(new byte[] { 0x02 }));
        Assert.assertNull(added);
        Assert.assertTrue(tags.removeLifetimeExceededAndCheckWhetherSeen(new Tag(new byte[] { 0x02 })));

        tags.put(tag, tag);

        put = tags.get(new Tag(new byte[] { 0x02 }));
        Assert.assertSame(tag, put);
        
        Thread.sleep(delay + 10);
               
        Assert.assertFalse(tags.removeLifetimeExceededAndCheckWhetherSeen(new Tag(new byte[] { 0x02 })));
        Assert.assertFalse(tags.contains(new Tag(new byte[] { 0x02 })));
    }

    @Test
    public void dispose() throws Exception {
        Tag tag = new Tag(new byte[] { 0x01 });
        final Tags tags = new Tags();

        Assert.assertFalse(tags.isDisposed());

        tags.add(tag, null);
        tags.pulse();

        // call reset in another thread, which waits because there is an unprocessed tag
        final CountDownLatch ready = new CountDownLatch(1);
        final AtomicLong waitMs = new AtomicLong(0L);
        Thread waitingTagAndPulse = new Thread(new Runnable() {
            @Override
            public void run() {
                ready.countDown();
                long start = System.currentTimeMillis();
                tags.reset();
                long end = System.currentTimeMillis();
                waitMs.set(end - start);
            }
        });
        waitingTagAndPulse.start();

        Assert.assertTrue("Thread is ready", ready.await(100, TimeUnit.MILLISECONDS));
        Thread.sleep(10);

        // reset is waiting
        Assert.assertEquals(1, tags.getCount());

        // process the tag
        tags.dispose();

        Assert.assertTrue(tags.isDisposed());
		Assert.assertEquals(0, tags.await(Long.valueOf(1)));

        Assert.assertEquals(0, tags.getCount());
        Assert.assertNull(tags.get(new Tag(new byte[] { 0x01 })));

        Thread.sleep(10);

        Assert.assertFalse("Thread is not alive", waitingTagAndPulse.isAlive());
    }
}
