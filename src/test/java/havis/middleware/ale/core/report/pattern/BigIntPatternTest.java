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

public class BigIntPatternTest {

	@Test(expected = ValidationException.class)
	public void bigIntPatternExceptionTest() throws ValidationException{
		new BigIntPattern(PatternType.CACHE, "*", new CommonField(1, 8, 16));
	}
	
	@Test
	public void bigIntPatternTest() throws ValidationException{
		new BigIntPattern(PatternType.CACHE, "*", new CommonField(FieldDatatype.UINT, FieldFormat.HEX, 1, 8, 16));
	}
	
	@Test
	public void nextTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX, 1, 8, 16);
		BigIntPattern test = new BigIntPattern(PatternType.CACHE, "*", field);
		Assert.assertEquals(null, test.next());
	}
	
	@Test
	public void disjointTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX, 1, 8, 16);
		BigIntPattern test = new BigIntPattern(PatternType.CACHE, "*", field);
		BigIntPart input = new BigIntPart(field, "*");
		Assert.assertFalse(test.disjoint(field, input));
		CommonField dleif = new CommonField(FieldDatatype.BITS, FieldFormat.HEX, 1, 16, 8);
		Assert.assertTrue(test.disjoint(dleif, input));
	}
	
	@Test
	public void disjointPatternTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX, 1, 8, 16);
		BigIntPattern test = new BigIntPattern(PatternType.CACHE, "*", field);
		List<IPattern> pList = new ArrayList<>();
		BigIntPattern input = new BigIntPattern(PatternType.CACHE, "X", field);
		pList.add(input);
		Assert.assertFalse(test.disjoint(pList));
	}
	
	@Test
	public void matchTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX, 1, 8, 16);
		BigIntPattern test = new BigIntPattern(PatternType.CACHE, "*", field);
		TdtDefinitions tdtDefinitions = new TdtDefinitions();
		TdtTagInfo info = new TdtTagInfo(tdtDefinitions);
		Result result = new Result(ResultState.SUCCESS);
		Assert.assertFalse(test.match(info, result));
		result.setState(ResultState.EPC_CACHE_DEPLETED);
		ReadResult readResult = new ReadResult(ResultState.SUCCESS, new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
		Assert.assertTrue(test.match(info, readResult));
		readResult = new ReadResult(ResultState.ASSOCIATION_TABLE_VALUE_INVALID);
		Assert.assertFalse(test.match(info, readResult));
	}
	
	@Test
	public void nameTest() throws ValidationException {
		CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.HEX, 1, 8, 16);
		BigIntPattern test = new BigIntPattern(PatternType.CACHE, "*", field);
		BigIntPattern tset = new BigIntPattern(PatternType.CACHE, "X", field);
		TdtDefinitions tdtDefinitions = new TdtDefinitions();
		TdtTagInfo info = new TdtTagInfo(tdtDefinitions );
		Result result = new Result(ResultState.SUCCESS);		
		Assert.assertNull(test.name(info , result));
		result.setState(ResultState.MISC_ERROR_TOTAL);
		Assert.assertNull(test.name(info , result));
		ReadResult readResult = new ReadResult(ResultState.SUCCESS, new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
		Assert.assertEquals("*", test.name(info, readResult));
		readResult = new ReadResult(ResultState.ASSOCIATION_TABLE_VALUE_INVALID);
		Assert.assertNull(tset.name(info, readResult));
		//TODO how to get true result
	}
}
