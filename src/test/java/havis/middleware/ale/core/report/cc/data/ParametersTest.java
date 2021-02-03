package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.report.cc.data.Parameter.Callback;
import havis.middleware.ale.service.cc.CCParameterListEntry;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ParametersTest {

	private static Parameters parameters;
	private static Callback callback;

	@BeforeClass
	public static void init() {
		parameters = new Parameters();
		Parameter param1 = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		Parameter param2 = new Parameter(callback, FieldDatatype.EPC, FieldFormat.STRING);
		Parameter param3 = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		Map<String, Parameter> parameterMap = Deencapsulation.getField(parameters, "parameters");
		parameterMap.put("param1", param1);
		parameterMap.put("param2", param2);
		parameterMap.put("param3", param3);
	}

	@AfterClass
	public static void finalCleanUp() {
		if (parameters != null) {
			parameters = null;
		}
	}

	@After
	public void clean() {
		parameters.clearParameterValues();
	}

	@Test
	public void updateParameterValuesMissigParams() throws ParameterException {
		try {
			parameters.updateParameterValues(createEntryList(1));
		} catch (ParameterException e) {
			Assert.assertEquals("Missing parameters, not all parameters have been specified", e.getMessage());
		}
	}

	@Test
	public void updateParameterValuesEntryWithoutName() throws ParameterException {
		List<CCParameterListEntry> entries = createEntryList(2);
		CCParameterListEntry entry = entries.get(1);
		entry.setName(null);
		entries.set(1, entry);
		try {
			parameters.updateParameterValues(entries);
		} catch (ParameterException e) {
			Assert.assertEquals("Parameters contain an entry without a name", e.getMessage());
		}
	}

	@Test
	public void updateParameterValuesEntryWithoutValue() throws ParameterException {
		List<CCParameterListEntry> entries = createEntryList(2);
		CCParameterListEntry entry = entries.get(1);
		entry.setValue(null);
		entries.set(1, entry);
		try {
			parameters.updateParameterValues(entries);
		} catch (ParameterException e) {
			Assert.assertEquals("Parameters contain an entry without a value", e.getMessage());
		}
	}

	@Test
	public void updateParameterValuesEntryWithSameName() throws ParameterException {
		List<CCParameterListEntry> entries = createEntryList(2);
		CCParameterListEntry entry = entries.get(1);
		entry.setName("param1");
		entries.set(1, entry);
		try {
			parameters.updateParameterValues(entries);
		} catch (ParameterException e) {
			Assert.assertEquals("Parameter 'param1' already specified", e.getMessage());
		}
	}

	@Test
	public void updateParameterValues() throws ParameterException {
		parameters.updateParameterValues(createEntryList(3));
		Assert.assertTrue(parameters.hasParameters());
		Map<String, Parameter> params = Deencapsulation.getField(parameters, "parameters");
		Map<String, RawData> dataValues = Deencapsulation.getField(parameters, "dataValues");
		Assert.assertEquals(3, params.size());
		Assert.assertTrue(params.containsKey("param1"));
		Assert.assertTrue(params.containsKey("param2"));
		Assert.assertTrue(params.containsKey("param3"));
		Assert.assertEquals("Parameter [datatype=EPC, format=EPC_TAG]", params.get("param1").toString());
		Assert.assertEquals("Parameter [datatype=EPC, format=STRING]", params.get("param2").toString());
		Assert.assertEquals("Parameter [datatype=EPC, format=EPC_TAG]", params.get("param3").toString());
		Assert.assertEquals(3, dataValues.size());
		Assert.assertTrue(dataValues.containsKey("param1"));
		Assert.assertTrue(dataValues.containsKey("param2"));
		Assert.assertTrue(dataValues.containsKey("param3"));
		Assert.assertTrue(parameters.hasParameters());
		Assert.assertEquals("Bytes [datatype=EPC, value=[48, 116, 37, 123, -9, 25, 78, 64, 0, 0, 26, -123], length=96, state=SUCCESS]", dataValues
				.get("param1").toString());
		Assert.assertEquals("Characters [value=123456789, state=SUCCESS]", dataValues.get("param2").toString());
		Assert.assertEquals("Bytes [datatype=EPC, value=[48, 116, 37, 123, -9, 25, 78, 64, 0, 0, 26, -123], length=96, state=SUCCESS]", dataValues
				.get("param3").toString());
	}

	@Test
	public void toBytesIsNull(@Mocked final Fields fields) throws ParameterException, ValidationException {
		new NonStrictExpectations() {
			{
				Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_TAG, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
				result = null;
			}
		};
		try {
			parameters.updateParameterValues(createEntryList(1));
		} catch (ParameterException e) {
			Assert.assertEquals("Failed to parse value for parameter 'param1'.", e.getMessage());
		}
	}

	@Test
	public void toBytesExceptionl(@Mocked final Fields fields) throws ParameterException, ValidationException {
		final String reason = "Data value is not specified";
		new NonStrictExpectations() {
			{
				Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_TAG, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
				result = new ValidationException(reason);
			}
		};
		try {
			parameters.updateParameterValues(createEntryList(1));
		} catch (ParameterException e) {
			Assert.assertEquals("Invalid value for parameter 'param1'. " + reason, e.getMessage());
		}
	}

	@Test
	public void get() throws ValidationException {
		Map<String, Parameter> parameterMap = Deencapsulation.getField(parameters, "parameters");
		Assert.assertFalse(parameterMap.containsKey("param4"));
		Parameter parameter = parameters.get("param4", FieldDatatype.EPC, FieldFormat.EPC_TAG);
		parameterMap = Deencapsulation.getField(parameters, "parameters");
		Assert.assertTrue(parameterMap.containsKey("param4"));
		Assert.assertEquals(parameter, parameters.get("param4", FieldDatatype.EPC, FieldFormat.EPC_TAG));

		parameterMap.remove("param4");
		Deencapsulation.setField(parameters, "parameters", parameterMap);
	}

	@Test
	public void getDifferentTypeFormat() throws ValidationException {
		try {
			parameters.get("param1", FieldDatatype.EPC, FieldFormat.EPC_PURE);
		} catch (ValidationException e) {
			Assert.assertEquals("Different datatype or format for parameter 'param1'", e.getMessage());
		}
	}

	@Test
	public void getBytes() throws Exception {
		Parameters parameters = new Parameters();
		List<CCParameterListEntry> list = createEntryList(1);
		Parameter param = parameters.get("param1", FieldDatatype.EPC, FieldFormat.EPC_TAG);
		parameters.updateParameterValues(list);
		Bytes bytes = param.callback.getBytes();
		Assert.assertEquals("[48, 116, 37, 123, -9, 25, 78, 64, 0, 0, 26, -123]", Arrays.toString(bytes.getValue()));
	}

	@Test
	public void getBytesDataValueNotBytes() throws Exception {
		Parameters parameters = new Parameters();
		List<CCParameterListEntry> list = createEntryList(1);
		Parameter param = parameters.get("param1", FieldDatatype.EPC, FieldFormat.STRING);
		parameters.updateParameterValues(list);
		Bytes bytes = param.callback.getBytes();
		Assert.assertEquals(ResultState.MISC_ERROR_TOTAL, bytes.getResultState());
	}

	@Test
	public void getCharacters() throws Exception {
		Parameters parameters = new Parameters();
		List<CCParameterListEntry> list = createEntryList(1);
		Parameter param = parameters.get("param1", FieldDatatype.ISO, FieldFormat.STRING);
		parameters.updateParameterValues(list);
		Characters characters = param.callback.getCharacters();
		Assert.assertEquals("urn:epc:tag:sgtin-96:3.0614141.812345.6789", characters.getValue());
	}

	@Test
	public void getCharactersDataValueNotCharacters() throws Exception {
		Parameters parameters = new Parameters();
		List<CCParameterListEntry> list = createEntryList(1);
		Parameter param = parameters.get("param1", FieldDatatype.EPC, FieldFormat.EPC_TAG);
		parameters.updateParameterValues(list);
		Characters characters = param.callback.getCharacters();
		Assert.assertEquals(ResultState.MISC_ERROR_TOTAL, characters.getResultState());
	}

	@Test
	public void hashCodeTest() throws Exception {
		Assert.assertEquals(parameters.hashCode(), parameters.hashCode());
	}

	@Test
	public void hashCodeTestNull() throws Exception {
		Map<String, Parameter> paramMap = Deencapsulation.getField(parameters, "parameters");
		Map<String, RawData> valueMap = Deencapsulation.getField(parameters, "dataValues");

		Deencapsulation.setField(parameters, "parameters", null);
		Deencapsulation.setField(parameters, "dataValues", null);
		Assert.assertEquals(961, parameters.hashCode());

		Deencapsulation.setField(parameters, "parameters", paramMap);
		Deencapsulation.setField(parameters, "dataValues", valueMap);
	}

	@Test
	public void equals() throws Exception {
		Parameters params1;
		Parameters params2;

		params1 = new Parameters();
		params2 = new Parameters();
		Assert.assertTrue(params1.equals(params2));
		Assert.assertTrue(params1.equals(params1));

		params2 = null;
		Assert.assertFalse(params1.equals(params2));
		Assert.assertFalse(params1.equals("test"));

		Deencapsulation.setField(params1, "dataValues", null);
		params2 = new Parameters();
		Assert.assertFalse(params1.equals(params2));

		params1 = new Parameters();
		Deencapsulation.setField(params1, "parameters", null);
		Assert.assertFalse(params1.equals(""));

		params1 = new Parameters();
		params2.get("param2", FieldDatatype.UINT, FieldFormat.DECIMAL);
		Assert.assertFalse(params1.equals(params2));

		Deencapsulation.setField(params2, "dataValues", null);
		Assert.assertFalse(params1.equals(params2));
	}

	private List<CCParameterListEntry> createEntryList(int size) {
		List<CCParameterListEntry> list = new LinkedList<CCParameterListEntry>();
		CCParameterListEntry entry;
		for (int i = 1; i <= size; i++) {
			entry = new CCParameterListEntry();
			entry.setName("param" + i);
			entry.setValue((i == 2) ? "123456789" : "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
			list.add(entry);
		}
		return list;
	}
}
