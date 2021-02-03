package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.AssocTableValidationException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidAssocTableEntryException;
import havis.middleware.ale.base.exception.InvalidEPCException;
import havis.middleware.ale.base.exception.InvalidPatternException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.Tag.Property;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.cc.AssocTableEntry;
import havis.middleware.ale.service.cc.AssocTableSpec;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mockit.Capturing;
import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AssociationTest {
	
	private static String name = "ValidName";
	private static Association assoc;
	
	@BeforeClass
	public static void init() throws Exception {
		assoc = new Association(name, createSpec("EPC"), createEntryList("EPC"));
	}
	/*
	 *  Constructor tests
	 */
	
	@Test
	public void associationWithInvalidName() throws InvalidAssocTableEntryException {
		try {
			new Association("Test Name", new AssocTableSpec(), null);
			Assert.fail("AssocTableValidationException expected");
		} catch (AssocTableValidationException e) {
			Assert.assertEquals("Name 'Test Name' enthält ein UNICODE 'Pattern_White_Space' Zeichen",
					e.getMessage());
		}
	}
	
	@Test
	public void datatypeValidationException(@Mocked final AssocTableSpec spec) throws Exception {
		final String reason = "Unknown datatype";
		
		new NonStrictExpectations() {{
			spec.getDatatype();
			result = new ValidationException(reason);
		}};
		try {
			new Association(name, spec, null);
			Assert.fail("AssocTableValidationException expected");
		} catch (AssocTableValidationException e) {
			Assert.assertEquals("The specification of association '" + name + "' is invalid. " + reason,
					e.getMessage());
		}		
	}
	
	@Test
	public void datatypeIsNull() throws InvalidAssocTableEntryException {
		try {
			new Association(name, new AssocTableSpec(), null);
			Assert.fail("AssocTableValidationException expected");
		} catch (AssocTableValidationException e) {
			Assert.assertEquals("The specification of association '" + name + "' is invalid. Datatype could not be null",
					e.getMessage());
		}
	}
	
	@Test
	public void formatIsNull() throws InvalidAssocTableEntryException {
		AssocTableSpec spec = new AssocTableSpec();
		spec.setDatatype("epc");
		try {
			new Association(name, spec, null);
			Assert.fail("AssocTableValidationException expected");
		} catch (AssocTableValidationException e) {
			Assert.assertEquals("The specification of association '" + name + "' is invalid. Format could not be null",
					e.getMessage());
		}
	}
	
	@Test
	public void noEpcGlobal(@Mocked final AssocTableEntry entry, @Mocked final Tag tag,@Mocked final TdtTagInfo info) throws Exception {		
		AssocTableSpec spec = new AssocTableSpec();
		spec.setDatatype("epc");
		spec.setFormat("epc-pure");
		LinkedList<AssocTableEntry> entries = new LinkedList<AssocTableEntry>();
		entries.add(entry);
		final String key = "urn:epc:tag:sgtin-96:3.0614141.812345.6789";
		
		new NonStrictExpectations() {{
			entry.getKey();
			result = key;
			
			tag.getProperty(Property.TAG_INFO);
			result = info;
			
			info.isEpcGlobal();
			result = Boolean.FALSE;
		}};
		
		try {
			new Association(name, spec, entries);
			Assert.fail("InvalidAssocTableEntryException expected");
		} catch (InvalidAssocTableEntryException e) {
			Assert.assertEquals("Tag '" + key + "' is not conform to EPCglobal",
					e.getMessage());
		}
	}
	
	@Test
	public void validAssociationEPC() throws Exception {
		AssocTableSpec spec = createSpec("EPC");
		List<AssocTableEntry> entries = createEntryList("EPC");
		Association association = new Association(name, spec, entries);
		
		Assert.assertEquals("Association [name=" + name 
				+ ", datatype="	+ association.getFieldDatatype()
				+ ", format=" + association.getFieldFormat()
				+ ", entries={urn:epc:tag:sgtin-96:3.0614141.812345.6789=Bytes [datatype=EPC,"
				+ " value=[1, 3, 7, 15, 31, 63, 127, 63, 31, 15, 7, 3], length=96, state=SUCCESS]}]", association.toString());
		Assert.assertEquals(spec, association.getSpec());
	}
	
	@Test
	public void validAssociationISO() throws Exception {
		AssocTableSpec spec = createSpec("ISO");
		List<AssocTableEntry> entries = createEntryList("ISO");
		Association association = new Association(name, spec, entries);
		
		Assert.assertEquals("Association [name=" + name 
				+ ", datatype="	+ association.getFieldDatatype()
				+ ", format=" + association.getFieldFormat()
				+ ", entries={urn:epc:tag:grai-96:1.00000050.2437.547365=Characters [value=0123456789, state=SUCCESS]}]", association.toString());
		Assert.assertEquals(spec, association.getSpec());
	}
	
	/*
	 *  getEntries() tests
	 */
	@Test
	public void getEntriesException(@Mocked final Tag tag, @Mocked final TdtTagInfo info) throws Exception {
		AssocTableEntry entry = new AssocTableEntry();
		entry.setKey("urn:epc:tag:grai-96");
		entry.setValue("urn:epc:tag:sgtin-96:3.0614141.812345.6789");		
		new NonStrictExpectations() {{
			tag.getProperty(Property.TAG_INFO);
			result = info;

			info.isEpcGlobal();
			result = Boolean.TRUE;
			
			info.getUriTag();
			result = new TdtTranslationException();
			
			tag.toString();
			result = "urn:epc:tag:sgtin-96:3.0614141.812345.6789";
		}};
		
		HashMap<Tag, RawData> map = new HashMap<Tag, RawData>();
		map.put(tag, Fields.toBytes(Fields.getDatatype(null, "epc"), FieldFormat.EPC_TAG, entry.getValue()));
		
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));		
		Deencapsulation.setField(association, "entries", map);
		
		try {
			association.getEntries();
		} catch (ImplementationException e) {
			Assert.assertEquals("Invalid association table entry 'urn:epc:tag:sgtin-96:3.0614141.812345.6789'", e.getMessage());
		}		
	}
	
	@Test
	public void getEntries() throws Exception {		
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		
		List<AssocTableEntry> list = association.getEntries();
		Assert.assertEquals(1, list.size());
		AssocTableEntry entry = list.get(0);
		Assert.assertEquals("urn:epc:tag:sgtin-96:3.0614141.812345.6789", entry.getKey());
		Assert.assertEquals("urn:epc:raw:96.x0103070F1F3F7F3F1F0F0703", entry.getValue());
	}
	
	@Test
	public void getEntriesISO() throws Exception {		
		Association association = new Association(name, createSpec("ISO"), createEntryList("ISO"));
		
		List<AssocTableEntry> list = association.getEntries();
		Assert.assertEquals(1, list.size());
		AssocTableEntry entry = list.get(0);
		Assert.assertEquals("urn:epc:tag:grai-96:1.00000050.2437.547365", entry.getKey());
		Assert.assertEquals("0123456789", entry.getValue());
	}
	
	/*
	 * putEntries() tests
	 */
	
	@Test
	public void putEntries() throws Exception {
		Association association = new Association(name, createSpec("EPC"), null);
		List<AssocTableEntry> entries = createEntryList("EPC");
		association.putEntries(entries);
		
		Assert.assertEquals(1, association.getEntries().size());
		Assert.assertEquals("urn:epc:tag:sgtin-96:3.0614141.812345.6789", association.getEntries().get(0).getKey());
		Assert.assertEquals("urn:epc:raw:96.x0103070F1F3F7F3F1F0F0703", association.getEntries().get(0).getValue());
	}
	
	@Test
	public void putEntriesException(@Mocked final Tag tag, @Mocked final TdtTagInfo info) throws Exception {		
		Association association = new Association(name, createSpec("EPC"), null);
		
		new NonStrictExpectations() {{
			tag.getProperty(Property.TAG_INFO);
			result = info;
			
			info.isEpcGlobal();
			result = Boolean.FALSE;
		}};
		try {
			association.putEntries(createEntryList("EPC"));
			Assert.fail("InvalidAssocTableEntryException expected");
		} catch (InvalidAssocTableEntryException e) {
			Assert.assertEquals("Tag 'urn:epc:tag:sgtin-96:3.0614141.812345.6789' is not conform to EPCglobal", e.getMessage());
		}		
	}
	
	/*
	 * getValue() tests
	 */
	
	@Test
	public void getValueBytes() throws Exception {		
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		
		Map<Tag, RawData> entries;
		entries = Deencapsulation.getField(association, "entries");
		
		Assert.assertTrue(entries.get(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[0])) instanceof Bytes);
		Assert.assertEquals("urn:epc:raw:96.x0103070F1F3F7F3F1F0F0703",
				association.getValue("urn:epc:tag:sgtin-96:3.0614141.812345.6789"));
	}
	
	@Test
	public void getValueString() throws Exception {
		Association association = new Association(name, createSpec("ISO"), createEntryList("ISO"));
		
		Map<Tag, RawData> entries;
		entries = Deencapsulation.getField(association, "entries");
		Object data = entries.get(TagDecoder.getInstance().fromUrn("urn:epc:tag:grai-96:1.00000050.2437.547365", new byte[0]));
		Assert.assertTrue(data instanceof Characters);
		Assert.assertEquals("0123456789",
				association.getValue("urn:epc:tag:grai-96:1.00000050.2437.547365"));
	}
	
	@Test
	public void getValueNull(@Mocked final Map<Tag, RawData> map) throws Exception {
		final RawData data = new Characters("abAB");
		new NonStrictExpectations() {{
			map.get(any);
			result = data;
		}};
		
		Association association = new Association(name, createSpec("EPC"), null);
		String value = association.getValue("urn:epc:tag:grai-96:1.00000050.2437.547365");
		Assert.assertEquals(null, value);
		
	}
	
	/*
	 * getEntries(List<String> list) tests
	 */
	
	@Test
	public void getEntriesPatternException() throws Exception {		
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		
		List<String> list = new LinkedList<String>();
		try {
			association.getEntries(list);
			Assert.fail("InvalidPatternException expected");
		} catch (InvalidPatternException e) {
			Assert.assertEquals("Pattern list could not be null or empty", e.getMessage());
		}
	}
	// PATTERN = "^urn:epc:(?<type>(id)?pat):(?<scheme>.*):(?<data>.*)$"
	@Test
	public void getEntriesValidPatternEPC(@Mocked final Patterns patterns) throws Exception {		
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));		
		List<String> list = new LinkedList<String>();
		list.add("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		
		new NonStrictExpectations() {{
			patterns.match(TagDecoder.getInstance().fromUrn(createEntry("EPC").getKey(), new byte[0]));
			result = Boolean.TRUE;
		}};		
		List<AssocTableEntry> entrylist = new LinkedList<AssocTableEntry>();
		entrylist = association.getEntries(list);
		
		Assert.assertEquals(1, entrylist.size());
		Assert.assertEquals("urn:epc:tag:sgtin-96:3.0614141.812345.6789", entrylist.get(0).getKey());
		Assert.assertEquals("urn:epc:raw:96.x0103070F1F3F7F3F1F0F0703", entrylist.get(0).getValue());
	}
	
	@Test
	public void getEntriesValidPatternISO(@Mocked final Patterns patterns) throws Exception {		
		Association association = new Association(name, createSpec("ISO"), createEntryList("ISO"));		
		List<String> list = new LinkedList<String>();
		list.add("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		
		new NonStrictExpectations() {{
			patterns.match(TagDecoder.getInstance().fromUrn(createEntry("ISO").getKey(), new byte[0]));
			result = Boolean.TRUE;
		}};		
		List<AssocTableEntry> entrylist = new LinkedList<AssocTableEntry>();
		entrylist = association.getEntries(list);
		
		Assert.assertEquals(1, entrylist.size());
		Assert.assertEquals("urn:epc:tag:grai-96:1.00000050.2437.547365", entrylist.get(0).getKey());
		Assert.assertEquals("0123456789", entrylist.get(0).getValue());
	}
	
	@Test
	public void getEntriesTdtTranslationException(@Mocked final Patterns patterns, @Capturing final Entry<Tag, RawData> sentry,
			@Mocked final Tag tag, @Mocked final TdtTagInfo info) throws Exception {
		
		List<String> list = new LinkedList<String>();
		list.add("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
	
		new NonStrictExpectations() {{
			sentry.getKey();
			result = tag;
			
			patterns.match(withAny(tag));
			result = Boolean.TRUE;
			
			tag.getProperty(Property.TAG_INFO);
			result = info;
			
			info.getUriTag();
			result = new TdtTranslationException();
			
			tag.toString();
			result = "urn:epc:tag:sgtin-96:3.0614141.812345.6789";
		}};
		try {
			assoc.getEntries(list);
			Assert.fail("InvalidPatternException expected");
		} catch (InvalidPatternException e) {
			Assert.assertEquals("Invalid pattern list entry 'urn:epc:tag:sgtin-96:3.0614141.812345.6789'", e.getMessage());
		}
	}
	
	/*
	 * removeEntry() tests
	 */
	
	@Test
	public void removeEntryCouldntParse(@Mocked final Tag tag) throws Exception {		
		new NonStrictExpectations() {{			
			tag.getProperty(Property.TAG_INFO);
			result = null;
		}};
		
		try {
			assoc.removeEntry("test");
			Assert.fail("InvalidEPCException expected");
		} catch (InvalidEPCException e) {
			Assert.assertEquals("Could not parse urn 'test'", e.getMessage());
		}
	}
	
	@Test
	public void removeEntryNotEpcGlobal() throws Exception {
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		String urn = "010101"; // non global
		try {
			association.removeEntry(urn);
			Assert.fail("InvalidEPCException expected");
		} catch (InvalidEPCException e) {
			Assert.assertEquals("Tag '" + urn + "' is not conform to EPCglobal", e.getMessage());
		}
	}
	
	@Test
	public void removeEntry() throws Exception {
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		association.removeEntry(createEntry("EPC").getKey());
		
		Assert.assertEquals(0, association.getEntries().size());
	}
	
	/*
	 * removeEntries() tests
	 */
	
	@Test
	public void removeEntriesInvalidPattern() throws Exception {
		Association association = new Association(name, createSpec("EPC"), null);
		List<String> list = new LinkedList<String>();
		try {
			association.removeEntries(list);
			Assert.fail("InvalidPatternException expected");
		} catch (InvalidPatternException e) {
			Assert.assertEquals("Pattern list could not be null or empty", e.getMessage());
		}
	}
	
	@Test
	public void removeEntries(@Mocked final Patterns patterns) throws Exception {
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));	
		Assert.assertEquals(1, association.getEntries().size());
		List<String> list = new LinkedList<String>();
		list.add("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		
		new NonStrictExpectations() {{
			patterns.match(TagDecoder.getInstance().fromUrn(createEntry("EPC").getKey(), new byte[0]));
			result = Boolean.TRUE;
		}};		
		association.removeEntries(list);
		
		Assert.assertEquals(0, association.getEntries().size());
	}
	
	/*
	 * toList test
	 */
	
	@Test
	public void toList() throws Exception {
		Map<String, String> entries = new LinkedHashMap<String, String>();
		entries.put("key1", "value1");
		entries.put("key2", "value2");
		List<AssocTableEntry> list = Association.toList(entries);
		
		Assert.assertEquals(2, list.size());
		Assert.assertEquals("value1", list.get(0).getValue());
		Assert.assertEquals("value2", list.get(1).getValue());
	}
	
	/*
	 * toMap tests
	 */
	
	@Test
	public void toMapCouldntDecode(@Mocked final Tag tag) throws Exception {
		Association association = new Association(name, createSpec("EPC"), null);
		
		new NonStrictExpectations() {{
			tag.getProperty(Property.TAG_INFO);
			result = null;
		}};
		try {
			association.toMap(createEntryList("ISO"));
			Assert.fail("InvalidAssocTableEntryException expected");
		} catch (InvalidAssocTableEntryException e) {
			Assert.assertEquals("Could not decode urn 'urn:epc:tag:grai-96:1.00000050.2437.547365'", e.getMessage());
		}		
	}
	@Test
	public void toMapDuplicate() throws Exception {
		Association association = new Association(name, createSpec("EPC"), null);
		List<AssocTableEntry> entries = createEntryList("EPC");
		entries.add(createEntry("EPC"));
		try {
			association.toMap(entries);
			Assert.fail("InvalidAssocTableEntryException expected");
		} catch (InvalidAssocTableEntryException e) {
			Assert.assertEquals("Duplicate key 'urn:epc:tag:sgtin-96:3.0614141.812345.6789' not allowed", e.getMessage());
		}
		
	}
	
	@Test
	public void toMapPutException() throws Exception {
		Association association = new Association(name, createSpec("EPC"), null);
		List<AssocTableEntry> entries = createEntryList("EPC");
		AssocTableEntry entry = new AssocTableEntry();
		entry.setKey("urn:epc:tag:sgtin-96:3.0614141.812345.6788");
		entries.add(entry);
		
		try {
			association.toMap(entries);
			Assert.fail("InvalidAssocTableEntryException expected");
		} catch (InvalidAssocTableEntryException e) {
			Assert.assertEquals("Data value is not specified", e.getMessage());
		}
	}
	
	/*
	 * inc,dec,isUsed tests
	 */
	
	@Test
	public void incThenDecThenIsUsed() throws Exception {
		Association association = new Association(name, createSpec("EPC"), null);
		
		association.inc();
		Assert.assertEquals(Integer.valueOf(1), Deencapsulation.getField(association, "count"));
		association.dec();
		Assert.assertEquals(Integer.valueOf(0), Deencapsulation.getField(association, "count"));
		Assert.assertFalse(association.isUsed());	
	}
	
	/*
	 * getBytes/getCharacter tests
	 */
	
	@Test
	public void getBytesCharacter() throws Exception {
		List<AssocTableEntry> entries = createEntryList("EPC");
		Association association = new Association(name, createSpec("EPC"), entries);
		List<AssocTableEntry> entries2 = createEntryList("ISO");
		Association association2 = new Association(name, createSpec("ISO"), entries2);

		Assert.assertEquals("[1, 3, 7, 15, 31, 63, 127, 63, 31, 15, 7, 3]", Arrays.toString((
				association.getBytes(TagDecoder.getInstance().fromUrn(createEntry("EPC").getKey(), new byte[0])).getValue())));
		Assert.assertEquals("0123456789",
				association2.getCharacters(TagDecoder.getInstance().fromUrn(createEntry("ISO").getKey(), new byte[0])).getValue());
	}
	
	@Test
	public void getBytesNull(@Capturing final Map<Tag, RawData> map, @Mocked final Tag tag) throws Exception {
		//[]
		final byte[] b = {0001, 0001};
		final Characters characters = new Characters("123");
		new NonStrictExpectations() {{
			tag.getEpc();
			result = b;
			
			map.containsKey(tag);
			result = Boolean.TRUE;
			
			map.get(tag);
			result = characters;
		}};		
		Assert.assertEquals("[]", Arrays.toString(assoc.getBytes(tag).getValue()));
		Assert.assertNull(assoc.getCharacters(tag).getValue());
	}
	
	@Test
	public void getCharactersNull(@Capturing final Map<Tag, RawData> map, @Mocked final Tag tag) throws Exception {
		//null
		final byte[] b = {0001, 0001};
		final Bytes bytes = new Bytes(b);
		new NonStrictExpectations() {{
			tag.getEpc();
			result = b;
			
			map.containsKey(tag);
			result = Boolean.TRUE;
			
			map.get(tag);
			result = bytes;
		}};		
		Assert.assertNull(assoc.getCharacters(tag).getValue());
	}
	
	/*
	 * hashCode tests
	 */
	
	@Test
	public void hashCodeTest() throws Exception {
		Association association1 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Association association2 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Assert.assertEquals(association1.hashCode(), association2.hashCode());
	}
	
	@Test
	public void hashCodeTestNull() throws Exception {
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Deencapsulation.setField(association, "datatype", null);
		Deencapsulation.setField(association, "entries", null);
		Deencapsulation.setField(association, "format", null);
		Assert.assertEquals(31*31*31, association.hashCode());
	}
	
	/*
	 * equals tests
	 */
	
	@Test
	public void equalsTest() throws Exception {
		Association association1 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Association association2 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Assert.assertTrue(association1.equals(association2));
		
		association1 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Assert.assertTrue(association1.equals(association1));
		
		association1 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		association2 = new Association(name, createSpec("ISO"), createEntryList("EPC"));
		Assert.assertFalse(association1.equals(association2));
		
		association1 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		association2 = null;
		Assert.assertFalse(association1.equals(association2));
		
		association1 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		String s = new String();
		Assert.assertFalse(association1.equals(s));
		
		association1 = new Association(name, createSpec("EPC"), null);
		association2 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Assert.assertFalse(association1.equals(association2));
		
		association1 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		association2 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Deencapsulation.setField(association2, "format", FieldFormat.EPC_HEX);
		Assert.assertFalse(association1.equals(association2));
		
		association1 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Deencapsulation.setField(association1, "entries", null);
		association2 = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		Map<Tag, RawData> map = new LinkedHashMap<Tag, RawData>();
		map.put(TagDecoder.getInstance().fromUrn(createEntry("ISO").getKey()), new Characters("abc"));
		Deencapsulation.setField(association2, "entries", map);
		Assert.assertFalse(association1.equals(association2));
	}
	
	/*
	 * dispose test
	 */
	
	@Test
	public void dispose() throws Exception {
		Association association = new Association(name, createSpec("EPC"), createEntryList("EPC"));
		association.dispose();
		
		Assert.assertEquals(0, association.getEntries().size());
	}
	
	/*
	 * Methods to create Association elements (AssocTableSpec/AssocTableEntry)
	 */
	
	private static AssocTableSpec createSpec(String type) {
		AssocTableSpec spec = new AssocTableSpec();
		spec.setDatatype((type.equals("EPC") ? "epc" : "iso-15962-string"));
		spec.setFormat((type.equals("EPC") ? "epc-tag" : null));
		return spec;
	}
	
	private static List<AssocTableEntry> createEntryList(String type) {
		List<AssocTableEntry> entries = new LinkedList<AssocTableEntry>();
		entries.add(createEntry(type));
		return entries;
		
	}
	
	private static AssocTableEntry createEntry(String type) {
		AssocTableEntry entry = new AssocTableEntry();
		entry.setKey((type.equals("EPC") 
				? "urn:epc:tag:sgtin-96:3.0614141.812345.6789" 
				: "urn:epc:tag:grai-96:1.00000050.2437.547365"));
		entry.setValue((type.equals("EPC") 
				? "000000010000001100000111000011110001111100111111011111110011111100011111000011110000011100000011" 
				: "0123456789"));		
		return entry;
	}
}

