package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.field.VariableField;
import havis.middleware.ale.core.report.cc.data.Common.DataType;
import havis.middleware.ale.core.report.cc.data.Parameter.Callback;
import havis.middleware.ale.service.cc.CCOpDataSpec;
import havis.middleware.ale.service.cc.CCParameterListEntry;
import havis.middleware.ale.service.cc.EPCCacheSpec;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mockit.Capturing;
import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class CommonTest {
	
	
	@Test
	public void commonWithoutSpec() throws Exception {
		try {
			new Common(null, null);
			Assert.fail("ValidationException expected");
		} catch (ValidationException e) {
			Assert.assertEquals("Command cycle operation data specification could not be null", e.getMessage());
		}		
	}
	
	@Test
	public void commonInvalidSpecType(@Mocked final CCOpDataSpec spec) throws Exception {
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = "";
		}};
		
		try {
			new Common(spec, null, null);
			Assert.fail("ValidationException expected");
		} catch (ValidationException e) {
			Assert.assertEquals("Command cycle operation data type could not be null or empty", e.getMessage());
		}		
	}
	
	@Test
	public void commonInvalidDataType(@Mocked final CCOpDataSpec spec) throws Exception {
		final String type = "InvalidType";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
		}};
		
		try {
			new Common(spec, null);
			Assert.fail("ValidationException expected");
		} catch (ValidationException e) {
			Assert.assertEquals("Unknown command cycle operation data type '" + type + "'", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeLiteral(@Mocked final CCOpDataSpec spec) throws Exception {
		final String type = "LITERAL";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			spec.getData();
			result = "123456789";
		}};		
		Common common = new Common(spec, null, createField(), createRawData());
		//Validates the specification, the datatype and the data
		Assert.assertEquals(spec, common.getSpec());
		Assert.assertEquals(DataType.LITERAL, common.getType());
		Data data = Deencapsulation.getField(common, "data");
		Assert.assertTrue(data.equals(new Literal(createField(), "123456789", createRawData())));
	}
	@Test
	public void commonTypeParameter(@Mocked final CCOpDataSpec spec) throws Exception {
		final String type = "PARAMETER";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			spec.getData();
			result = "testentry";
		}};		
		Common common = new Common(spec, createParameters(), createField(), null);
		//Validates the specification and the datatype
		Assert.assertEquals(spec, common.getSpec());
		Assert.assertEquals(DataType.PARAMETER, common.getType());
	}
	
	@Test
	public void commonTypeCacheVariableField(@Mocked final CCOpDataSpec spec, @Mocked final CommonField field, @Mocked final VariableField variableField)
			throws Exception {
		final String type = "CACHE";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			field.getBase();
			result = variableField;
		}};	
		try {
			new Common(spec, null, field, null);
			Assert.fail("ValidationException expected");
		} catch (ValidationException e) {
			Assert.assertEquals("CACHE data type not allowed for variable fields", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeCacheDataIsNull(@Mocked final CCOpDataSpec spec, @Mocked final Caches caches) throws Exception {
		final String type = "CACHE";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Caches.getInstance().get(spec.getData());
			result = null;
		}};	
		try {
			new Common(spec, null, createField(), null);
			Assert.fail("ValidationException expected");
		} catch (ValidationException e) {
			Assert.assertEquals("Unknown epc cache 'null'", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeCacheNotEpc(@Mocked final CCOpDataSpec spec, @Mocked final CommonField field, @Mocked final Caches caches) throws Exception {
		final String type = "CACHE";
		List<String> patternList = new LinkedList<String>();
		patternList.add("urn:epc:pat:sgtin-96:3.0614141.812345.6790");
		final Cache cache = new Cache("testname", new EPCCacheSpec(), patternList);
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Caches.getInstance().get(spec.getData());
			result = cache;
			
			field.getFieldDatatype();
			result = FieldDatatype.BITS;
		}};	
		try {
			new Common(spec, null, field, null);
			Assert.fail("ValidationException expected");
		} catch (ValidationException e) {
			Assert.assertEquals("Field datatype for epc cache data source must be epc", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeCacheNotEpcTag(@Mocked final CCOpDataSpec spec, @Mocked final CommonField field, @Mocked final Caches caches) throws Exception {
		final String type = "CACHE";
		List<String> patternList = new LinkedList<String>();
		patternList.add("urn:epc:pat:sgtin-96:3.0614141.812345.6790");
		final Cache cache = new Cache("testname", new EPCCacheSpec(), patternList);
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Caches.getInstance().get(spec.getData());
			result = cache;
			
			field.getFieldDatatype();
			result = FieldDatatype.EPC;
			
			field.getFieldFormat();
			result = FieldFormat.EPC_HEX;
		}};	
		try {
			new Common(spec, null, field, null);
			Assert.fail("ValidationException expected");
		} catch (ValidationException e) {
			Assert.assertEquals("Field format for epc cache data source must be epc tag", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeCache(@Mocked final CCOpDataSpec spec, @Mocked final Caches caches) throws Exception {		
		List<String> patternList = new LinkedList<String>();
		patternList.add("urn:epc:pat:sgtin-96:3.0614141.812345.6790");
		final Cache cache = new Cache("testname", new EPCCacheSpec(), patternList);
		final String type = "CACHE";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Caches.getInstance().get(spec.getData());
			result = cache;
		}};
		
		new Common(spec, null, createField());
		//Validates that the field count from cache was increased
		Assert.assertTrue(cache.isUsed());
		Assert.assertEquals(Integer.valueOf(1), Deencapsulation.getField(cache, "count"));
	}
	
	@Test
	public void commonTypeAssociationUnknown(@Mocked final CCOpDataSpec spec) throws Exception {
		final String type = "ASSOCIATION";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
		}};		
		try {
			new Common(spec, null, createField());
		} catch (ValidationException e) {
			Assert.assertEquals("Unknown association table 'null'", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeAssociationInvalidDataType(@Mocked final CCOpDataSpec spec, 
			@Mocked final Associations associations, @Mocked final Association association) throws Exception {
		final String type = "ASSOCIATION";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Associations.getInstance().get(spec.getData());
			result = association;
			
			association.getFieldDatatype();
			result = FieldDatatype.UINT;
		}};		
		try {
			new Common(spec, null, createField());
		} catch (ValidationException e) {
			Assert.assertEquals("Field '" + createField().getName() + "' datatype deviates from datatype of association table 'null'", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeAssociation(@Mocked final CCOpDataSpec spec, 
			@Mocked final Associations associations, @Mocked final Association association) throws Exception {
		final String type = "ASSOCIATION";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Associations.getInstance().get(spec.getData());
			result = association;
			
			association.getFieldDatatype();
			result = FieldDatatype.EPC;
		}};	
		
		Common common = new Common(spec, null, createField());
		
		new Verifications() {{
			association.inc();
			times = 1;
		}};
		//Validates that the field data is a Association
		Assert.assertEquals(association, Deencapsulation.getField(common, "data"));
	}
	
	@Test
	public void commonTypeRandomVariableField(@Mocked final CCOpDataSpec spec,
			@Mocked final CommonField field, @Mocked final VariableField variableField) throws Exception {		
		final String type = "RANDOM";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			field.getBase();
			result = variableField;
		}};	
		try {
			new Common(spec, null, field, null);
			Assert.fail("ValidationException expected");
		} catch (ValidationException e) {
			Assert.assertEquals("RANDOM data type not allowed for variable fields", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeRandomUnknown(@Mocked final CCOpDataSpec spec) throws Exception {
		final String type = "RANDOM";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
		}};
		try {
			new Common(spec, null, createField());
		} catch (ValidationException e) {
			Assert.assertEquals("Unknown random number generator 'null'", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeRandomInvalidDataType(@Mocked final CCOpDataSpec spec,
			@Mocked final CommonField field, @Mocked final Randoms randoms, @Mocked final Random random) throws Exception {
		final String type = "RANDOM";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Randoms.getInstance().get(spec.getData());
			result = random;
			
			field.getFieldDatatype();
			result = FieldDatatype.EPC;
		}};
		try {
			new Common(spec, null, field);
		} catch (ValidationException e) {
			Assert.assertEquals("Field datatype for random data source must be uint", e.getMessage());
		}
	}
	 
	@Test
	public void commonTypeRandomInvalidFormat(@Mocked final CCOpDataSpec spec,
			@Mocked final CommonField field, @Mocked final Randoms randoms, @Mocked final Random random) throws Exception {
		final String type = "RANDOM";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Randoms.getInstance().get(spec.getData());
			result = random;
			
			field.getFieldDatatype();
			result = FieldDatatype.UINT;
			
			field.getFieldFormat();
			result = FieldFormat.DECIMAL;
		}};
		try {
			new Common(spec, null, field);
		} catch (ValidationException e) {
			Assert.assertEquals("Field format for random data source must be hex", e.getMessage());
		}
	}
	
	@Test
	public void commonTypeRandom(@Mocked final CCOpDataSpec spec,
			@Mocked final CommonField field, @Mocked final Randoms randoms, @Mocked final Random random) throws Exception {
		final String type = "RANDOM";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Randoms.getInstance().get(spec.getData());
			result = random;
			
			field.getFieldDatatype();
			result = FieldDatatype.UINT;
			
			field.getFieldFormat();
			result = FieldFormat.HEX;
		}};
		
		Common common = new Common(spec, null, field);
		new Verifications() {{
			random.inc();
			times = 1;
		}};
		//Validates that the field data is a Association
		Assert.assertEquals(random, Deencapsulation.getField(common, "data"));
	}
	
	@Test
	public void getValueBytesCharacters(@Mocked final CCOpDataSpec spec, @Mocked final Associations associations,
			@Mocked final Association association, @Capturing final Map<Tag, RawData> entries, @Mocked final Tag tag) throws Exception{
		final byte[] b = {00000001, 0001, 0001};
		final Bytes bytes = new Bytes(b);
		final Characters characters = new Characters(ResultState.SUCCESS);
		final String type = "ASSOCIATION";
		Deencapsulation.setField(association, "entries", entries);
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			Associations.getInstance().get(spec.getData());
			result = association;
			
			association.getFieldDatatype();
			result = FieldDatatype.EPC;
			
			spec.getData();
			result = "testData";
			
			association.getBytes(tag);
			result = bytes;
			
			association.getCharacters(tag);
			result = characters;
		}};		
		Common common = new Common(spec, null, createField());
		//Validates the value Bytes and Characters
		Assert.assertEquals("testData", common.getValue());
		Bytes resultBytes = common.getBytes(tag);
		Assert.assertEquals("[1, 1, 1]", Arrays.toString(resultBytes.getValue()));
		Characters resultCharacters = common.getCharacters(tag);
		Assert.assertEquals(ResultState.SUCCESS, resultCharacters.getResultState());
	}
	
	@Test
	public void hashCodeTest(@Mocked final CCOpDataSpec spec) throws Exception {
		final String type = "LITERAL";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			spec.getData();
			result = "000000010000001100000111000011110001111100111111011111110011111100011111000011110000011100000011";
		}};
		Common common1 = new Common(spec, null, createField());
		Common common2 = new Common(spec, null, createField());
		Assert.assertEquals(common1.hashCode(), common2.hashCode());		
	}
	
	@Test
	public void hashCodeTestNull(@Mocked final CCOpDataSpec spec) throws Exception {
		final String type = "LITERAL";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			spec.getData();
			result = "000000010000001100000111000011110001111100111111011111110011111100011111000011110000011100000011";
		}};
		Common common = new Common(spec, null, createField());
		Deencapsulation.setField(common, "data", null);
		Deencapsulation.setField(common, "type", null);
		Assert.assertEquals(961, common.hashCode());
	}

	@Test
	public void equals(@Mocked final CCOpDataSpec spec, @Capturing final Callback callback) throws Exception {
		final String type = "LITERAL";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			spec.getData();
			result = "000000010000001100000111000011110001111100111111011111110011111100011111000011110000011100000011";
		}};
		Common common1;
		Common common2;
		
		common1 = new Common(spec, null, createField());
		common2 = new Common(spec, null, createField());
		Assert.assertTrue(common1.equals(common2));
		Assert.assertTrue(common1.equals(common1));
		
		common2 = null;
		Assert.assertFalse(common1.equals(common2));
		Assert.assertFalse(common1.equals("test"));
		
		Deencapsulation.setField(common1, "data", null);
		common2 = new Common(spec, null, createField());
		Assert.assertFalse(common1.equals(common2));
		
		Deencapsulation.setField(common1, "data", new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG));
		Assert.assertFalse(common1.equals(common2));
		
		Deencapsulation.setField(common2, "type", null);
		Assert.assertFalse(common1.equals(common2));
	}
	
	@Test
	public void dispose(@Mocked final CCOpDataSpec spec) throws Exception {
		final String type = "LITERAL";
		new NonStrictExpectations() {{
			spec.getSpecType();
			result = type;
			
			spec.getData();
			result = "000000010000001100000111000011110001111100111111011111110011111100011111000011110000011100000011";
		}};		
		Common common1 = new Common(spec, null, createField());
		common1.dispose();
		Assert.assertNull(Deencapsulation.getField(common1, "data"));
	}
	
	private CommonField createField() {
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.EPC_TAG, 3, 8, 16);
		field.setName("testfield");
		return field;
	}
	
	private RawData createRawData() {
		RawData data = new RawData(ResultState.SUCCESS);
		return data;
	}
	
	private Parameters createParameters() throws ParameterException {
		Parameters params = new Parameters();
		params.updateParameterValues(createEntryList());		
		return params;
	}

	private List<CCParameterListEntry> createEntryList() {
		List<CCParameterListEntry> list = new LinkedList<CCParameterListEntry>();
		CCParameterListEntry entry = new CCParameterListEntry();
		entry.setName("testentry");
		entry.setValue("123");
		list.add(entry);
		return list;
	}
}
