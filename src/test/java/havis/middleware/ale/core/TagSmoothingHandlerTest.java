package havis.middleware.ale.core;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import mockit.Mocked;

import org.junit.Assert;
import org.junit.Test;

public class TagSmoothingHandlerTest {

    private static class ValueHolder<T> {
        private T object;

        public T set(T object) {
            this.object = object;
            return this.object;
        }

        public T get() {
            return this.object;
        }

        public void reset()
        {
            this.object = null;
        }
    }

    @Test
    public void tagSmoothingHandler() {
        TagSmoothingHandler handler = new TagSmoothingHandler(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4));
        Assert.assertEquals(Integer.valueOf(1), handler.glimpsedTimeout);
        Assert.assertEquals(Integer.valueOf(2), handler.observedTimeThreshold);
        Assert.assertEquals(Integer.valueOf(3), handler.observedCountThreshold);
        Assert.assertEquals(Integer.valueOf(4), handler.lostTimeout);
    }

    @Test
    public void process(@Mocked final ReaderController readerController) throws InterruptedException {
        TagSmoothingHandler handler = new TagSmoothingHandler(Integer.valueOf(50), Integer.valueOf(25), Integer.valueOf(3), Integer.valueOf(100));

        final ValueHolder<Tag> lastTagSentToCycle = new ValueHolder<>();

        final Caller<Tag> callback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
                Assert.assertSame(readerController, controller);
                lastTagSentToCycle.set(t);
            }
        };

        Tag t1_1 = new Tag(new byte[] { 0x01 });
        Tag t1_2 = new Tag(new byte[] { 0x01 });
        Tag t1_3 = new Tag(new byte[] { 0x01 });
        Tag t1_4 = new Tag(new byte[] { 0x01 });

        handler.process(t1_1, callback, readerController);
        Assert.assertNull(lastTagSentToCycle.get());
        handler.process(t1_2, callback, readerController);
        Assert.assertNull(lastTagSentToCycle.get());
        handler.process(t1_3, callback, readerController);
        Assert.assertSame(t1_3, lastTagSentToCycle.get()); // ObservedCountThreshold == 3
        Assert.assertEquals(100, lastTagSentToCycle.get().getTimeout());

        lastTagSentToCycle.reset();

        handler.process(t1_4, callback, readerController);
        Assert.assertSame(t1_4, lastTagSentToCycle.get()); // was already marked as observed
        Assert.assertEquals(100, lastTagSentToCycle.get().getTimeout());

        lastTagSentToCycle.reset();

        Tag t2_1 = new Tag(new byte[] { 0x02 });
        Tag t2_2 = new Tag(new byte[] { 0x02 });

        handler.process(t2_1, callback, readerController);
        Assert.assertNull(lastTagSentToCycle.get());

        Thread.sleep(30);

        handler.process(t2_2, callback, readerController);
        Assert.assertSame(t2_2, lastTagSentToCycle.get()); // ObservedTimeThreshold > 25
        Assert.assertEquals(100, lastTagSentToCycle.get().getTimeout());

        lastTagSentToCycle.reset();

        Tag t3_1 = new Tag(new byte[] { 0x03 });
        Tag t3_2 = new Tag(new byte[] { 0x03 });

        handler.process(t3_1, callback, readerController);
        Assert.assertNull(lastTagSentToCycle.get());

        Thread.sleep(55);

        handler.process(t3_2, callback, readerController);
        Assert.assertNull(lastTagSentToCycle.get()); // GlimpsedTimeout == 50

        Tag t4_1 = new Tag(new byte[] { 0x04 });
        Tag t4_2 = new Tag(new byte[] { 0x04 });
        Tag t4_3 = new Tag(new byte[] { 0x04 });
        Tag t4_4 = new Tag(new byte[] { 0x04 });

        handler.process(t4_1, callback, readerController);
        Assert.assertNull(lastTagSentToCycle.get());
        handler.process(t4_2, callback, readerController);
        Assert.assertNull(lastTagSentToCycle.get());
        handler.process(t4_3, callback, readerController);
        Assert.assertSame(t4_3, lastTagSentToCycle.get()); // ObservedCountThreshold == 3
        Assert.assertEquals(100, lastTagSentToCycle.get().getTimeout());

        lastTagSentToCycle.reset();

        Thread.sleep(105);

        handler.process(t4_4, callback, readerController);
        Assert.assertNull(lastTagSentToCycle.get()); // LostTimeout == 100
    }

    @Test
    public void processNoLostTimeout(@Mocked final ReaderController readerController) throws InterruptedException {
        TagSmoothingHandler handler = new TagSmoothingHandler(Integer.valueOf(50), Integer.valueOf(25), Integer.valueOf(1), null);

        final ValueHolder<Tag> lastTagSentToCycle = new ValueHolder<>();

        final Caller<Tag> callback = new Caller<Tag>() {
            @Override
            public void invoke(Tag t, ReaderController controller) {
                Assert.assertSame(readerController, controller);
                lastTagSentToCycle.set(t);
            }
        };

        Tag t1 = new Tag(new byte[] { 0x01 });

        handler.process(t1, callback, readerController);
        Assert.assertSame(t1, lastTagSentToCycle.get());
        Assert.assertEquals(0, lastTagSentToCycle.get().getTimeout());
    }
}
