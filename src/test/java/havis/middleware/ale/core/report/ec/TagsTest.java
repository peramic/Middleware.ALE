package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.operation.tag.Sighting;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;

import java.util.ArrayList;
import java.util.HashMap;

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
		Assert.assertEquals(0, tags.getPast().size());
		Assert.assertEquals(0, tags.getPresent().size());
		tags.dispose(); // does nothing
	}

	@Test
	public void clear() throws Exception {
		Tags tags = new Tags();
		Tag tag1 = new Tag(new byte[] { 0x01 });
		tag1.setResult(new HashMap<Integer, Result>());
		tag1.getSightings().put("reader1", new ArrayList<Sighting>());
		tag1.setTimeout(1000);
		PrimaryKey pk1 = new PrimaryKey(tag1, null);
		Tag tag2 = new Tag(new byte[] { 0x02 });
		tag2.setResult(new HashMap<Integer, Result>());
		tag2.setTimeout(0);
		PrimaryKey pk2 = new PrimaryKey(tag2, null);
		Tag tag3 = new Tag(new byte[] { 0x03 });
		tag3.setResult(new HashMap<Integer, Result>());
		tag3.setTimeout(0);
		PrimaryKey pk3 = new PrimaryKey(tag3, null);

		Tag tag4 = new Tag(new byte[] { 0x04 });
		tag4.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk4 = new PrimaryKey(tag4, null);
		Tag tag5 = new Tag(new byte[] { 0x05 });
		tag5.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk5 = new PrimaryKey(tag5, null);
		Tag tag6 = new Tag(new byte[] { 0x06 });
		tag6.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk6 = new PrimaryKey(tag6, null);

		tags.getPast().put(pk1, tag1);
		tags.getPast().put(pk2, tag2);
		tags.getPast().put(pk3, tag3);

		tags.getPresent().put(pk4, tag4);
		tags.getPresent().put(pk5, tag5);
		tags.getPresent().put(pk6, tag6);

		Thread.sleep(10);

		tags.clear();

		Assert.assertEquals(3, tags.getPast().size());
		Assert.assertSame(tag1, tags.getPast().get(pk1));
		Assert.assertSame(tag2, tags.getPast().get(pk2));
		Assert.assertSame(tag3, tags.getPast().get(pk3));

		Assert.assertEquals(1, tags.getPresent().size());
		Assert.assertEquals(tag1, tags.getPresent().get(pk1));
		Assert.assertNotSame(tag1, tags.getPresent().get(pk1));

		// clone must be reset
		Tag clone = tags.getPresent().get(pk1);
		Assert.assertEquals(0, clone.getCount());
		Assert.assertNull(clone.getFirstTime());
		Assert.assertEquals(0, clone.getSightings().size());
	}

	@Test
	public void rotate() throws Exception {
		Tags tags = new Tags();
		Tag tag1 = new Tag(new byte[] { 0x01 });
		tag1.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk1 = new PrimaryKey(tag1, null);
		Tag tag2 = new Tag(new byte[] { 0x02 });
		tag2.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk2 = new PrimaryKey(tag2, null);
		Tag tag3 = new Tag(new byte[] { 0x03 });
		tag3.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk3 = new PrimaryKey(tag3, null);

		Tag tag4 = new Tag(new byte[] { 0x04 });
		tag4.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk4 = new PrimaryKey(tag4, null);
		Tag tag5 = new Tag(new byte[] { 0x05 });
		tag5.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk5 = new PrimaryKey(tag5, null);
		Tag tag6 = new Tag(new byte[] { 0x06 });
		tag6.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk6 = new PrimaryKey(tag6, null);

		tags.getPast().put(pk1, tag1);
		tags.getPast().put(pk2, tag2);
		tags.getPast().put(pk3, tag3);

		tags.getPresent().put(pk4, tag4);
		tags.getPresent().put(pk5, tag5);
		tags.getPresent().put(pk6, tag6);

		tags.rotate();

		Assert.assertEquals(3, tags.getPast().size());
		Assert.assertSame(tag4, tags.getPast().get(pk4));
		Assert.assertSame(tag5, tags.getPast().get(pk5));
		Assert.assertSame(tag6, tags.getPast().get(pk6));

		Assert.assertEquals(3, tags.getPresent().size());
		Assert.assertSame(tag4, tags.getPresent().get(pk4));
		Assert.assertSame(tag5, tags.getPresent().get(pk5));
		Assert.assertSame(tag6, tags.getPresent().get(pk6));
	}

	@Test
	public void reset() throws Exception {
		Tags tags = new Tags();
		Tag tag1 = new Tag(new byte[] { 0x01 });
		tag1.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk1 = new PrimaryKey(tag1, null);
		Tag tag2 = new Tag(new byte[] { 0x02 });
		tag2.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk2 = new PrimaryKey(tag2, null);

		tags.getPast().put(pk1, tag1);
		tags.getPresent().put(pk2, tag2);

		tags.reset();

		Assert.assertEquals(0, tags.getPast().size());
		Assert.assertEquals(0, tags.getPresent().size());
	}

	@Test
	public void get() throws Exception {
		Tags tags = new Tags();
		Tag tag1 = new Tag(new byte[] { 0x01 });
		tag1.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk1 = new PrimaryKey(tag1, null);
		Tag tag2 = new Tag(new byte[] { 0x02 });
		tag2.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk2 = new PrimaryKey(tag2, null);

		tags.getPast().put(pk1, tag1);
		tags.getPresent().put(pk2, tag2);

		Assert.assertSame(tag2, tags.get(pk2));
		Assert.assertNull(tags.get(pk1));
	}

	@Test
	public void contains() throws Exception {
		Tags tags = new Tags();
		Tag tag1 = new Tag(new byte[] { 0x01 });
		tag1.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk1 = new PrimaryKey(tag1, null);
		Tag tag2 = new Tag(new byte[] { 0x02 });
		tag2.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk2 = new PrimaryKey(tag2, null);

		tags.getPast().put(pk1, tag1);
		tags.getPresent().put(pk2, tag2);

		Assert.assertTrue(tags.contains(pk2));
		Assert.assertFalse(tags.contains(pk1));
	}
	
	@Test
	public void add() throws Exception {
		Tags tags = new Tags();
		Tag tag1 = new Tag(new byte[] { 0x01 });
		tag1.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk1 = new PrimaryKey(tag1, null);
		Tag tag2 = new Tag(new byte[] { 0x02 });
		tag2.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk2 = new PrimaryKey(tag2, null);

		tags.add(pk1, tag1);
		tags.add(pk2, tag2);
		
		Assert.assertEquals(2, tags.getPresent().size());
		Assert.assertEquals(0, tags.getPast().size());

		Assert.assertSame(tag1, tags.get(pk1));
		Assert.assertSame(tag2, tags.get(pk2));
	}

	@Test
	public void cloneTest() throws Exception {
		Tags tags = new Tags();
		Tag tag1 = new Tag(new byte[] { 0x01 });
		tag1.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk1 = new PrimaryKey(tag1, null);
		Tag tag2 = new Tag(new byte[] { 0x02 });
		tag2.setResult(new HashMap<Integer, Result>());
		PrimaryKey pk2 = new PrimaryKey(tag2, null);

		tags.getPast().put(pk1, tag1);
		tags.getPresent().put(pk2, tag2);

		Tags clone = tags.clone();

		Assert.assertNotSame(tags, clone);
		Assert.assertSame(tags.getPast(), clone.getPast());
		Assert.assertSame(tags.getPresent(), clone.getPresent());
	}

	@Test
	public void toStringTest() {
		Tags tags = new Tags();
		Tag tag1 = TagDecoder.getInstance().enable(new Tag(new byte[] { 0x01 }));
		Tag tag2 = TagDecoder.getInstance().enable(new Tag(new byte[] { 0x01 }));
		tags.getPresent().put(new PrimaryKey(tag1, null), tag1);
		tags.getPast().put(new PrimaryKey(tag2, null), tag2);

		Assert.assertEquals(
				"Tags [past={PrimaryKey [tag=urn:epc:raw:8.x01, fields=null]=urn:epc:raw:8.x01}, present={PrimaryKey [tag=urn:epc:raw:8.x01, fields=null]=urn:epc:raw:8.x01}]",
				tags.toString());
	}
}
