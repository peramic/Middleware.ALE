package havis.middleware.ale.core;

import havis.middleware.ale.base.operation.tag.Tag;

import java.util.Date;

import mockit.Expectations;
import mockit.Mocked;

import org.junit.Assert;
import org.junit.Test;

public class TagSmoothingEntryTest {

    @Test
    public void tagSmoothingEntry() {
        Tag tag = new Tag();
        long now = new Date().getTime();
        TagSmoothingEntry entry = new TagSmoothingEntry(tag);
        Assert.assertSame(tag, entry.getTag());
        Assert.assertTrue(entry.getFirstSeen() >= now && entry.getFirstSeen() < now + 10);
        Assert.assertEquals(entry.getFirstSeen(), entry.getLastSeen());
        Assert.assertEquals(0, entry.getSeenCount());
        Assert.assertFalse(entry.isObserved());
        entry.setObserved(true);
        Assert.assertTrue(entry.isObserved());
        entry.setObserved(false);
        Assert.assertFalse(entry.isObserved());

        Tag tag2 = new Tag();
        entry.setTag(tag2);
        Assert.assertSame(tag2, entry.getTag());
    }

    @Test
    public void seen() throws InterruptedException {
        Tag tag = new Tag();
        long start = new Date().getTime();
        TagSmoothingEntry entry = new TagSmoothingEntry(tag);
        Assert.assertTrue(entry.getFirstSeen() >= start && entry.getFirstSeen() < start + 10);
        Assert.assertEquals(entry.getFirstSeen(), entry.getLastSeen());
        Assert.assertEquals(0, entry.getSeenCount());

        Thread.sleep(25);

        long now = new Date().getTime();
        entry.seen();
        Assert.assertTrue(entry.getFirstSeen() >= start && entry.getFirstSeen() < start + 10);
        Assert.assertTrue(entry.getLastSeen() >= now && entry.getLastSeen() < now + 10);
        Assert.assertEquals(1, entry.getSeenCount());

        Thread.sleep(25);

        now = new Date().getTime();
        entry.seen();
        Assert.assertTrue(entry.getFirstSeen() >= start && entry.getFirstSeen() < start + 10);
        Assert.assertTrue(entry.getLastSeen() >= now && entry.getLastSeen() < now + 10);
        Assert.assertEquals(2, entry.getSeenCount());
    }

    @Test
    public void equalsTest(@Mocked final Tag tag) {
        new Expectations() {
            {
                tag.equals(tag);
                result = Boolean.TRUE;
                tag.equals(null);
                result = Boolean.FALSE;
            }
        };
        TagSmoothingEntry entry = new TagSmoothingEntry(tag);
        TagSmoothingEntry entryEqual = new TagSmoothingEntry(tag);
        TagSmoothingEntry entryNotEqual = new TagSmoothingEntry(null);
        Assert.assertTrue(entry.equals(entryEqual));
        Assert.assertFalse(entry.equals(entryNotEqual));
        Assert.assertFalse(entry.equals(new String()));
    }

    @Test
    public void hashCodeTest(@Mocked final Tag tag) {
        new Expectations() {
            {
                tag.hashCode();
                result = Integer.valueOf(10);
            }
        };
        Assert.assertEquals(10, new TagSmoothingEntry(tag).hashCode());
    }

    @Test
    public void toStringTest(@Mocked final Tag tag) {
        new Expectations() {
            {
                tag.toString();
                result = "tag";
            }
        };
        Assert.assertEquals("tag", new TagSmoothingEntry(tag).toString());
    }
}
