package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.tdt.TdtDefinitions;
import havis.middleware.tdt.TdtTagInfo;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class UIntPatternTest {

	@Test(expected = ValidationException.class)
	public void ConstructorExceptionTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.STRING);
		new UIntPattern(PatternType.CACHE, "x12", field);
	}

	@Test(expected = ValidationException.class)
	public void ConstructorTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX);
		new UIntPattern(PatternType.CACHE, "12", field);
	}

	@Test
	public void matchFalseTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX);
		UIntPattern test = new UIntPattern(PatternType.CACHE, "x12", field);
		TdtDefinitions tdtDefinitions = new TdtDefinitions();
		TdtTagInfo info = new TdtTagInfo(tdtDefinitions);
		Result result = new Result(ResultState.SUCCESS);
		Assert.assertFalse(test.match(info , result));
	}

	@Test
	public void matchTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX);
		UIntPattern test = new UIntPattern(PatternType.CACHE, "*", field);
		TdtDefinitions tdtDefinitions = new TdtDefinitions();
		TdtTagInfo info = new TdtTagInfo(tdtDefinitions);
		ReadResult result = new ReadResult(ResultState.SUCCESS, new byte[]{1, 2});
		Assert.assertTrue(test.match(info , result));
	}

	@Test
	public void nameTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX);
		UIntPattern test = new UIntPattern(PatternType.CACHE, "*", field);
		TdtDefinitions tdtDefinitions = new TdtDefinitions();
		TdtTagInfo info = new TdtTagInfo(tdtDefinitions);
		ReadResult result = new ReadResult(ResultState.SUCCESS, new byte[]{1, 2});
		Assert.assertEquals("*", test.name(info, result));
	}

	@Test
	public void nameNullTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX);
		UIntPattern test = new UIntPattern(PatternType.CACHE, "x12", field);
		TdtDefinitions tdtDefinitions = new TdtDefinitions();
		TdtTagInfo info = new TdtTagInfo(tdtDefinitions);
		Result result = new Result(ResultState.SUCCESS);
		Assert.assertNull(test.name(info , result));
	}

	@Test
	public void nextTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX);
		UIntPattern test = new UIntPattern(PatternType.CACHE, "x12", field);
		Assert.assertNull(test.next());
	}
	@Test
	public void disjointFieldTest() throws ValidationException {
		// TODO
	}

	@Test
	public void disjointPatternsTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX);
		UIntPattern test = new UIntPattern(PatternType.CACHE, "x12", field);
		UIntPattern input = new UIntPattern(PatternType.CACHE, "*", field);
		UIntPattern listin = new UIntPattern(PatternType.CACHE, "x10", field);
		List<IPattern> patterns = new ArrayList<>();
		patterns.add(input);
		Assert.assertFalse(test.disjoint(patterns));
		patterns.clear();
		patterns.add(listin);
		Assert.assertTrue(test.disjoint(patterns));
	}
}
