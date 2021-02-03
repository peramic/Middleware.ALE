package havis.middleware.ale.core.report;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

public class CounterTest {

    @Test
    public void awaitImmediateZero() {
        Counter counter = new Counter();
        long c = counter.await(null);
        Assert.assertEquals(0, c);
    }

    @Test
    public void awaitImmediateOne() {
        Counter counter = new Counter();
        counter.pulse();
        long c = counter.await(Long.valueOf(0));
        Assert.assertEquals(1, c);
        c = counter.await(Long.valueOf(2));
        Assert.assertEquals(1, c);
    }

    @Test
    public void awaitImmediateTwo() {
        Counter counter = new Counter();
        counter.pulse();
        counter.pulse();
        long c = counter.await(Long.valueOf(0));
        Assert.assertEquals(2, c);
        c = counter.await(Long.valueOf(1));
        Assert.assertEquals(2, c);
        c = counter.await(Long.valueOf(3));
        Assert.assertEquals(2, c);
    }

    @Test
    public void awaitOne() throws InterruptedException {
        awaitCount(1);
    }

    @Test
    public void awaitTwo() throws InterruptedException {
        awaitCount(2);
    }

    @Test
    public void awaitThree() throws InterruptedException {
        awaitCount(3);
    }

    private void awaitCount(final long countToAwait) throws InterruptedException {
        final Counter counter = new Counter();
        final CountDownLatch ready = new CountDownLatch(1);
        final AtomicLong awaitResult = new AtomicLong(0);

        for (int i = 1; i < countToAwait; i++)
            counter.pulse();

        Thread waitingForCounter = new Thread(new Runnable() {
            @Override
            public void run() {
                ready.countDown();
                long start = System.currentTimeMillis();
                long c = counter.await(Long.valueOf(countToAwait - 1));
                long end = System.currentTimeMillis();
                if (end - start > 25) // if we actually waited
                    awaitResult.set(c);
            }
        });
        waitingForCounter.start();

        boolean success = false;

        try {
            Assert.assertTrue("Thread is ready", ready.await(100, TimeUnit.MILLISECONDS));

            // thread is waiting now, let it do that for some time
            Thread.sleep(50);
            Assert.assertTrue("Waiting for counter to increase", waitingForCounter.isAlive());

            counter.pulse();

            waitingForCounter.join(100);

            Assert.assertFalse("Counter increased, not waiting", waitingForCounter.isAlive());
            Assert.assertEquals(countToAwait, awaitResult.get());

            success = true;
        } finally {
            if (!success)
                waitingForCounter.interrupt();
        }
    }

    @Test
    public void awaitMultiple() throws InterruptedException {
        final Counter counter = new Counter();
        final CountDownLatch ready = new CountDownLatch(3);
        final AtomicLong await1Result = new AtomicLong(0);
        final AtomicLong await2Result = new AtomicLong(0);
        final AtomicLong await3Result = new AtomicLong(0);

        counter.pulse();

        Thread waitingForCounter1 = new Thread(new Runnable() {
            @Override
            public void run() {
                ready.countDown();
                long start = System.currentTimeMillis();
                long c = counter.await(Long.valueOf(1));
                long end = System.currentTimeMillis();
                if (end - start > 25) // if we actually waited
                    await1Result.set(c);
            }
        });
        waitingForCounter1.start();

        Thread waitingForCounter2 = new Thread(new Runnable() {
            @Override
            public void run() {
                ready.countDown();
                long start = System.currentTimeMillis();
                long c = counter.await(Long.valueOf(1));
                long end = System.currentTimeMillis();
                if (end - start > 25) // if we actually waited
                    await2Result.set(c);
            }
        });
        waitingForCounter2.start();

        Thread waitingForCounter3 = new Thread(new Runnable() {
            @Override
            public void run() {
                ready.countDown();
                long start = System.currentTimeMillis();
                long c = counter.await(Long.valueOf(1));
                long end = System.currentTimeMillis();
                if (end - start > 25) // if we actually waited
                    await3Result.set(c);
            }
        });
        waitingForCounter3.start();

        boolean success = false;

        try {
            Assert.assertTrue("Threads are ready", ready.await(100, TimeUnit.MILLISECONDS));

            // threads are waiting now, let them do that for some time
            Thread.sleep(50);
            Assert.assertTrue("Waiting for counter to increase (1)", waitingForCounter1.isAlive());
            Assert.assertTrue("Waiting for counter to increase (2)", waitingForCounter2.isAlive());
            Assert.assertTrue("Waiting for counter to increase (3)", waitingForCounter3.isAlive());

            counter.pulse();

            waitingForCounter1.join(100);
            waitingForCounter2.join(100);
            waitingForCounter3.join(100);

            Assert.assertFalse("Counter increased, not waiting (1)", waitingForCounter1.isAlive());
            Assert.assertFalse("Counter increased, not waiting (2)", waitingForCounter2.isAlive());
            Assert.assertFalse("Counter increased, not waiting (3)", waitingForCounter3.isAlive());

            Assert.assertEquals(2, await1Result.get());
            Assert.assertEquals(2, await2Result.get());
            Assert.assertEquals(2, await3Result.get());

            success = true;
        } finally {
            if (!success) {
                waitingForCounter1.interrupt();
                waitingForCounter2.interrupt();
                waitingForCounter3.interrupt();
            }
        }
    }
}
