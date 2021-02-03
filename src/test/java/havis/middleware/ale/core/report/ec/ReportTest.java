package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ec.ECFilterSpec;
import havis.middleware.ale.service.ec.ECFilterSpec.IncludePatterns;
import havis.middleware.ale.service.ec.ECReportOutputFieldSpec;
import havis.middleware.ale.service.ec.ECReportOutputFieldSpecExtension;
import havis.middleware.ale.service.ec.ECReportOutputSpec;
import havis.middleware.ale.service.ec.ECReportOutputSpecExtension;
import havis.middleware.ale.service.ec.ECReportOutputSpecExtension.FieldList;
import havis.middleware.ale.service.ec.ECReportSetSpec;
import havis.middleware.ale.service.ec.ECReportSpec;
import havis.middleware.ale.service.ec.ECReportSpecExtension;
import havis.middleware.ale.service.ec.ECReportSpecExtension.StatProfileNames;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ReportTest {

	@Test(expected = ValidationException.class)
	public void ecReportFilterExceptionTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		ECFilterSpec filterSpecInput = new ECFilterSpec();
		IncludePatterns value = new IncludePatterns();

		value.getIncludePattern().add("errorCode");

		filterSpecInput.setIncludePatterns(value);

		input.setReportName("test");
		input.setFilterSpec(filterSpecInput);
		new Report(input);
	}

	@Test(expected = ValidationException.class)
	public void ecReportOutputExceptionTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		input.setReportName("test");

		new Report(input);
	}


	@Test(expected = ValidationException.class)
	public void ecReportOutputValueNullExceptionTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		input.setReportName("test");

		ECReportOutputSpec outSpec = new ECReportOutputSpec();

		outSpec.setIncludeEPC(Boolean.FALSE);
		outSpec.setIncludeTag(Boolean.FALSE);
		outSpec.setIncludeRawDecimal(Boolean.FALSE);
		outSpec.setIncludeRawHex(Boolean.FALSE);
		outSpec.setIncludeCount(Boolean.FALSE);
		outSpec.setExtension(null);

		input.setOutput(outSpec);

		new Report(input);
	}

	@Test(expected = ValidationException.class)
	public void ecReportOutputValueFieldListNullExceptionTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		input.setReportName("test");

		ECReportOutputSpec outSpec = new ECReportOutputSpec();

		outSpec.setIncludeEPC(Boolean.FALSE);
		outSpec.setIncludeTag(Boolean.FALSE);
		outSpec.setIncludeRawDecimal(Boolean.FALSE);
		outSpec.setIncludeRawHex(Boolean.FALSE);
		outSpec.setIncludeCount(Boolean.FALSE);
		outSpec.setExtension(new ECReportOutputSpecExtension());

		input.setOutput(outSpec);

		new Report(input);
	}

	@Test(expected = ValidationException.class)
	public void ecReportOutputSizeExceptionTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		input.setReportName("test");

		ECReportOutputSpec outSpec = new ECReportOutputSpec();

		outSpec.setIncludeEPC(Boolean.FALSE);
		outSpec.setIncludeTag(Boolean.FALSE);
		outSpec.setIncludeRawDecimal(Boolean.FALSE);
		outSpec.setIncludeRawHex(Boolean.FALSE);
		outSpec.setIncludeCount(Boolean.FALSE);
		outSpec.setExtension(new ECReportOutputSpecExtension());

		FieldList value = new FieldList();
		outSpec.getExtension().setFieldList(value);

		input.setOutput(outSpec);

		new Report(input);
	}

	@Test //(expected = ValidationException.class)
	public void ecReportOperationsFieldExceptionTest() throws ValidationException{
		//TODO thrown exception is wrong. expected "Field test is not defined"
	}

	@Test(expected = ValidationException.class)
	public void ecReportUnknownSetExceptionTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		ECReportSetSpec setSpec = new ECReportSetSpec();
		ECFieldSpec fieldSpec = new ECFieldSpec("epc");
		ECReportSetSpec reportSetSpec = new ECReportSetSpec();

		input.setReportName("test");

		ECReportOutputSpec outSpec = new ECReportOutputSpec();

		outSpec.setIncludeEPC(Boolean.TRUE);
		outSpec.setIncludeTag(Boolean.FALSE);
		outSpec.setIncludeRawDecimal(Boolean.FALSE);
		outSpec.setIncludeRawHex(Boolean.FALSE);
		outSpec.setIncludeCount(Boolean.FALSE);
		outSpec.setExtension(new ECReportOutputSpecExtension());

		setSpec.setSet("CURRENT");

		FieldList value = new FieldList();
		ECReportOutputFieldSpec element = new ECReportOutputFieldSpec();

		element.setExtension(new ECReportOutputFieldSpecExtension());
		element.setFieldspec(fieldSpec);
		element.setIncludeFieldSpecInReport(Boolean.TRUE);
		element.setName("test");

		value.getField().add(element);
		outSpec.getExtension().setFieldList(value);

		reportSetSpec.setSet("affe");

		input.setOutput(outSpec);
		input.setReportSet(setSpec);
		input.setReportSet(reportSetSpec);

		new Report(input);
	}

	@Test(expected = ValidationException.class)
	public void ecReportNoSetExceptionTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		ECReportSetSpec setSpec = new ECReportSetSpec();
		ECFieldSpec fieldSpec = new ECFieldSpec("epc");

		input.setReportName("test");

		ECReportOutputSpec outSpec = new ECReportOutputSpec();

		outSpec.setIncludeEPC(Boolean.TRUE);
		outSpec.setIncludeTag(Boolean.FALSE);
		outSpec.setIncludeRawDecimal(Boolean.FALSE);
		outSpec.setIncludeRawHex(Boolean.FALSE);
		outSpec.setIncludeCount(Boolean.FALSE);
		outSpec.setExtension(new ECReportOutputSpecExtension());

		setSpec.setSet("CURRENT");

		FieldList value = new FieldList();
		ECReportOutputFieldSpec element = new ECReportOutputFieldSpec();

		element.setExtension(new ECReportOutputFieldSpecExtension());
		element.setFieldspec(fieldSpec);
		element.setIncludeFieldSpecInReport(Boolean.TRUE);
		element.setName("test");

		value.getField().add(element);
		outSpec.getExtension().setFieldList(value);

		input.setOutput(outSpec);
		input.setReportSet(setSpec);
		input.setReportSet(null);

		new Report(input);
	}

	@Test
	public void ecReportProfilesTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		ECReportSetSpec setSpec = new ECReportSetSpec();
		ECFieldSpec fieldSpec = new ECFieldSpec("epc");
		ECReportSpecExtension specExtension = new ECReportSpecExtension();
		StatProfileNames names = new StatProfileNames();

		input.setReportName("test");

		ECReportOutputSpec outSpec = new ECReportOutputSpec();

		outSpec.setIncludeEPC(Boolean.TRUE);
		outSpec.setIncludeTag(Boolean.FALSE);
		outSpec.setIncludeRawDecimal(Boolean.FALSE);
		outSpec.setIncludeRawHex(Boolean.FALSE);
		outSpec.setIncludeCount(Boolean.FALSE);
		outSpec.setExtension(new ECReportOutputSpecExtension());

		setSpec.setSet("CURRENT");

		FieldList value = new FieldList();
		ECReportOutputFieldSpec element = new ECReportOutputFieldSpec();

		element.setExtension(new ECReportOutputFieldSpecExtension());
		element.setFieldspec(fieldSpec);
		element.setIncludeFieldSpecInReport(Boolean.TRUE);
		element.setName("test");

		value.getField().add(element);
		outSpec.getExtension().setFieldList(value);

		names.getStatProfileName().add("TagTimestamps");
		names.getStatProfileName().add("ReaderNames");
		names.getStatProfileName().add("ReaderSightingSignals");

		specExtension.setStatProfileNames(names);

		input.setOutput(outSpec);
		input.setReportSet(setSpec);
		input.setExtension(specExtension);

		Report actual = new Report(input);

		List<String> expected = new ArrayList<>();
		expected.add("TagTimestamps");
		expected.add("ReaderNames");
		expected.add("ReaderSightingSignals");

		Assert.assertEquals(expected, actual.getSpec().getExtension().getStatProfileNames().getStatProfileName());
	}

	@Test(expected = ValidationException.class)
	public void ecReportProfilesDefaultExceptionTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		ECReportSetSpec setSpec = new ECReportSetSpec();
		ECFieldSpec fieldSpec = new ECFieldSpec("epc");
		ECReportSpecExtension specExtension = new ECReportSpecExtension();
		StatProfileNames names = new StatProfileNames();

		input.setReportName("test");

		ECReportOutputSpec outSpec = new ECReportOutputSpec();

		outSpec.setIncludeEPC(Boolean.TRUE);
		outSpec.setIncludeTag(Boolean.FALSE);
		outSpec.setIncludeRawDecimal(Boolean.FALSE);
		outSpec.setIncludeRawHex(Boolean.FALSE);
		outSpec.setIncludeCount(Boolean.FALSE);
		outSpec.setExtension(new ECReportOutputSpecExtension());

		setSpec.setSet("CURRENT");

		FieldList value = new FieldList();
		ECReportOutputFieldSpec element = new ECReportOutputFieldSpec();

		element.setExtension(new ECReportOutputFieldSpecExtension());
		element.setFieldspec(fieldSpec);
		element.setIncludeFieldSpecInReport(Boolean.TRUE);
		element.setName("test");

		value.getField().add(element);
		outSpec.getExtension().setFieldList(value);

		names.getStatProfileName().add("affe");

		specExtension.setStatProfileNames(names);

		input.setOutput(outSpec);
		input.setReportSet(setSpec);
		input.setExtension(specExtension);

		new Report(input);
	}

	@Test
	public void getFieldsTest() throws ValidationException{
		ECReportSpec input = new ECReportSpec();
		ECReportSetSpec setSpec = new ECReportSetSpec();
		ECFieldSpec fieldSpec = new ECFieldSpec("epc");

		input.setReportName("test");

		ECReportOutputSpec outSpec = new ECReportOutputSpec();

		outSpec.setIncludeEPC(Boolean.TRUE);
		outSpec.setIncludeTag(Boolean.FALSE);
		outSpec.setIncludeRawDecimal(Boolean.FALSE);
		outSpec.setIncludeRawHex(Boolean.FALSE);
		outSpec.setIncludeCount(Boolean.FALSE);
		outSpec.setExtension(new ECReportOutputSpecExtension());

		setSpec.setSet("CURRENT");

		FieldList value = new FieldList();
		ECReportOutputFieldSpec element = new ECReportOutputFieldSpec();

		element.setExtension(new ECReportOutputFieldSpecExtension());
		element.setFieldspec(fieldSpec);
		element.setIncludeFieldSpecInReport(Boolean.TRUE);
		element.setName("test");

		value.getField().add(element);
		outSpec.getExtension().setFieldList(value);


		input.setOutput(outSpec);
		input.setReportSet(setSpec);

		Report actual = new Report(input);

		Assert.assertEquals(FieldDatatype.EPC, actual.getFields().get(0).getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_TAG, actual.getFields().get(0).getFieldFormat());
		Assert.assertEquals(1, actual.getFields().get(0).getBank());
		Assert.assertEquals(0, actual.getFields().get(0).getLength());
		Assert.assertEquals(16, actual.getFields().get(0).getOffset());
	}
}
