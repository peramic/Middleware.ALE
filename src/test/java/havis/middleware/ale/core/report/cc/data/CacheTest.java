package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.EPCCacheSpecValidationException;
import havis.middleware.ale.base.exception.InvalidPatternException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.Tag.Property;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.report.pattern.IPattern;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.cc.EPCCacheSpec;
import havis.middleware.ale.service.cc.EPCCacheSpecExtension;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import mockit.Capturing;
import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class CacheTest {
	private String name = "ValidName";
	private String patternURN = "urn:epc:pat:sgtin-96:3.0614141.812345.6790";
	private String patternURN1 = "urn:epc:pat:sgtin-96:3.0614141.812345.6791";
	private String tagURN = "urn:epc:tag:sgtin-96:3.0614141.812345.6790";
	private String tagURN1 = "urn:epc:tag:sgtin-96:3.0614141.812345.6791";

	@Test
	public void newCacheWithInvalidName() throws Exception {
		try {
			new Cache("Test Name", null, null);
			Assert.fail("EPCCacheSpecValidationException expected");
		} catch (EPCCacheSpecValidationException e) {
			Assert.assertEquals("Name 'Test Name' enthält ein UNICODE 'Pattern_White_Space' Zeichen", e.getMessage());
		}
	}

	@Test
	public void newCacheWithInvalidPatternList() throws Exception {
		try {
			new Cache(name, createSpec(), null);
			Assert.fail("InvalidPatternException expected");
		} catch (InvalidPatternException e) {
			Assert.assertEquals("EPC cache '" + name + "' validation failed. Pattern list could not be null or empty", e.getMessage());
		}
	}

	@Test
	public void newCache() throws Exception {
		EPCCacheSpec spec = createSpec();
		List<String> patterns = createPattern(patternURN);
		Cache cache = new Cache(name, spec, patterns);

		Assert.assertTrue(spec.equals(cache.getSpec()));
		Assert.assertEquals(name, Deencapsulation.getField(cache, "name"));
		Assert.assertEquals(patterns, Deencapsulation.getField(cache, "value"));
		IPattern pattern = Deencapsulation.getField(cache, "pattern");
		Assert.assertEquals(patternURN, pattern.toString());
		Assert.assertEquals(tagURN, pattern.next());
	}

	@Test
	public void gettBytes() throws Exception {
		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		Bytes bytes = cache.getBytes(TagDecoder.getInstance().fromUrn(tagURN, new byte[0]));
		Assert.assertEquals("[48, 116, 37, 123, -9, 25, 78, 64, 0, 0, 26, -122]", Arrays.toString(bytes.getValue()));
	}

	@Test
	public void getBytesNoPattern(@Mocked final Patterns patterns, @Capturing final IPattern iPattern) throws Exception {
		new NonStrictExpectations() {
			{
				iPattern.next();
				result = null;

				patterns.next();
				result = null;
			}
		};

		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		Deencapsulation.setField(cache, "patterns", patterns);
		Deencapsulation.setField(cache, "pattern", null);

		Bytes bytes = cache.getBytes(TagDecoder.getInstance().fromUrn(tagURN, new byte[0]));
		Assert.assertTrue(bytes.getValue().length == 0);
	}

	@Test
	public void getBytesTdtTranslationFailed(@Mocked final Tag tag, @Mocked final TdtTagInfo tagInfo) throws Exception {
		new NonStrictExpectations() {
			{
				tag.getProperty(Property.TAG_INFO);
				result = tagInfo;

				tagInfo.getEpcData();
				result = new TdtTranslationException();
			}
		};

		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		Bytes bytes = cache.getBytes(tag);
		Assert.assertNull(bytes);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getCharacters() throws Exception {
		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		cache.getCharacters(new Tag());
	}

	@Test
	public void replenish() throws Exception {
		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		Deencapsulation.setField(cache, "pattern", null);
		cache.replenish(createPattern(patternURN1));

		IPattern pattern = Deencapsulation.getField(cache, "pattern");
		Assert.assertEquals(tagURN1, pattern.next());
	}

	@Test
	public void replenishInvalidPattern() throws Exception {
		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		List<String> list = new LinkedList<String>();
		list.add("Test String@");
		try {
			cache.replenish(list);
			Assert.fail("InvalidPatternException expected");
		} catch (InvalidPatternException e) {
			Assert.assertEquals("Pattern 'Test String@' is invalid. Pattern type not supported", e.getMessage());
		}
	}

	@Test
	public void deplete() throws Exception {
		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		List<String> list = cache.deplete();

		Assert.assertEquals(patternURN, list.get(0));
		Assert.assertEquals(1, list.size());
		Patterns patterns = Deencapsulation.getField(cache, "patterns");
		Assert.assertTrue(patterns.next() == null);
		IPattern iPattern = Deencapsulation.getField(cache, "pattern");
		Assert.assertNull(iPattern);
	}

	@Test
	public void getPatterns() throws Exception {
		List<String> patterns = new LinkedList<String>();
		patterns.add(patternURN);
		patterns.add(patternURN1);
		Cache cache = new Cache(name, createSpec(), patterns);

		List<String> patternList = cache.getPatterns();
		Assert.assertEquals(2, patternList.size());
		Assert.assertTrue(patternList.contains(patternURN));
		Assert.assertTrue(patternList.contains(patternURN1));
	}

	@Test
	public void getFieldDataTypeAndFormat() throws Exception {
		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		Assert.assertEquals(FieldDatatype.EPC, cache.getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_TAG, cache.getFieldFormat());
	}

	@Test
	public void incDecIsUsed() throws Exception {
		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		cache.inc();
		cache.inc();
		cache.dec();
		Assert.assertEquals(Integer.valueOf(1), Deencapsulation.getField(cache, "count"));
		Assert.assertTrue(cache.isUsed());
	}

	@Test
	public void hashCodeTest() throws Exception {
		Cache cache1 = new Cache(name, createSpec(), createPattern(patternURN));
		Cache cache2 = new Cache(name, createSpec(), createPattern(patternURN));
		Assert.assertEquals(cache1.hashCode(), cache2.hashCode());

		Deencapsulation.setField(cache1, "value", null);
		Assert.assertEquals(31, cache1.hashCode());
	}

	@Test
	public void equals() throws Exception {
		Cache cache1;
		Cache cache2;

		cache1 = new Cache(name, createSpec(), createPattern(patternURN));
		cache2 = new Cache(name, createSpec(), createPattern(patternURN));
		Assert.assertTrue(cache1.equals(cache2));
		Assert.assertTrue(cache1.equals(cache1));

		cache2 = null;
		Assert.assertFalse(cache1.equals(cache2));
		Assert.assertFalse(cache1.equals("test"));

		Deencapsulation.setField(cache1, "value", null);
		cache2 = new Cache(name, createSpec(), createPattern(patternURN));
		Assert.assertFalse(cache1.equals(cache2));

		Deencapsulation.setField(cache1, "value", createPattern(patternURN1));
		Assert.assertFalse(cache1.equals(cache2));
	}

	@Test
	public void dispose() throws Exception {
		Cache cache = new Cache(name, createSpec(), createPattern(patternURN));
		cache.dispose();
		Patterns patterns = Deencapsulation.getField(cache, "patterns");
		Assert.assertTrue(patterns.toList().isEmpty());
	}

	private EPCCacheSpec createSpec() {
		EPCCacheSpec spec = new EPCCacheSpec();
		spec.setExtension(new EPCCacheSpecExtension());
		return spec;
	}

	private List<String> createPattern(String urn) {
		List<String> patternList = new LinkedList<String>();
		patternList.add(urn);
		return patternList;
	}
}
