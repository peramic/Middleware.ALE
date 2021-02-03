package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;

import java.util.Arrays;

import mockit.Deencapsulation;

import org.junit.Assert;
import org.junit.Test;

public class LiteralTest {
	
	@Test
	public void newLiteralWithoutRawData() throws Exception {		
		String data = "000000010000001100000111000011110001111100111111011111110011111100011111000011110000011100000011";
		Literal literal = new Literal(createCommonField("EPC"), data, null);
		
		Assert.assertEquals("[1, 3, 7, 15, 31, 63, 127, 63, 31, 15, 7, 3]", Arrays.toString((literal.getBytes(new Tag())).getValue()));
		Assert.assertEquals(FieldDatatype.EPC, literal.getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_TAG, literal.getFieldFormat());
	}
	
	@Test
	public void newLiteralWithRawData() throws Exception {
		byte[] b = {0001, 0001, 0001};
		Bytes predefinedData = new Bytes(b);		
		Literal literal = new Literal(null, null, predefinedData);
		
		Assert.assertEquals(ResultState.MISC_ERROR_TOTAL, literal.getCharacters(new Tag()).getResultState());
		Assert.assertEquals(Arrays.toString(b), Arrays.toString((literal.getBytes(new Tag())).getValue()));
		Assert.assertNull(literal.getFieldDatatype());
		Assert.assertNull(literal.getFieldFormat());
	}
	
	@Test
	public void newLiteralWithoutRawDataISO() throws Exception {		
		String data = "0123456789";
		Literal literal = new Literal(createCommonField("ISO"), data, null);
		
		Assert.assertEquals(ResultState.MISC_ERROR_TOTAL, literal.getBytes(new Tag()).getResultState());
		Assert.assertEquals(data, literal.getCharacters(new Tag()).getValue());
		Assert.assertEquals(FieldDatatype.ISO, literal.getFieldDatatype());
		Assert.assertEquals(FieldFormat.STRING, literal.getFieldFormat());
		Object dataObject = Deencapsulation.getField(literal, "data");
		Assert.assertEquals("Literal [data=" + dataObject
				+ ", datatype=" + literal.getFieldDatatype()
				+ ", format=" + literal.getFieldFormat() + "]", literal.toString());
	}
	
	@Test
	public void incDecIsUsed() throws Exception {
		String data = "0123456789";
		Literal literal = new Literal(createCommonField("ISO"), data, null);
		literal.inc();
		literal.dec();
		Assert.assertFalse(literal.isUsed());
	}
	
	@Test
	public void hashCodeTest() throws Exception {
		String data = "0123456789";
		Literal literal1 = new Literal(createCommonField("ISO"), data, null);
		Literal literal2 = new Literal(createCommonField("ISO"), data, null);
		Assert.assertEquals(literal1.hashCode(), literal2.hashCode());
	}
	
	@Test
	public void hashCodeTestNull() throws Exception {
		String data = "0123456789";
		Literal literal = new Literal(createCommonField("ISO"), data, null);
		Deencapsulation.setField(literal, "data", null);
		Deencapsulation.setField(literal, "datatype", null);
		Deencapsulation.setField(literal, "format", null);
		Assert.assertEquals(29791, literal.hashCode());
	}
	
	@Test
	public void equals() throws Exception {
		String data = "0123456789";
		Literal literal1;
		Literal literal2;
		
		literal1 = new Literal(createCommonField("ISO"), data, null);
		literal2 = new Literal(createCommonField("ISO"), data, null);
		Assert.assertTrue(literal1.equals(literal2));
		Assert.assertTrue(literal1.equals(literal1));
		
		literal2 = null;
		Assert.assertFalse(literal1.equals(literal2));
		Assert.assertFalse(literal1.equals("test"));
		
		Deencapsulation.setField(literal1, "data", null);
		literal2 = new Literal(createCommonField("ISO"), data, null);
		Assert.assertFalse(literal1.equals(literal2));
		
		String data2 = "000000010000001100000111000011110001111100111111011111110011111100011111000011110000011100000011";
		literal1 = new Literal(createCommonField("EPC"), data2, null);
		Assert.assertFalse(literal1.equals(literal2));
		
		literal2 = new Literal(createCommonField("EPC"), data2, null);
		Deencapsulation.setField(literal2, "datatype", FieldDatatype.UINT);
		Assert.assertFalse(literal1.equals(literal2));
		
		literal2 = new Literal(createCommonField("EPC"), data2, null);
		Deencapsulation.setField(literal2, "format", FieldFormat.EPC_PURE);
		Assert.assertFalse(literal1.equals(literal2));
	}
	
	private CommonField createCommonField(String type) {
		CommonField field = new CommonField(3, 8, 16);
		field.setFieldDatatype(FieldDatatype.valueOf(type));
		field.setFieldFormat((type.equals("EPC")
				? FieldFormat.EPC_TAG
				: FieldFormat.STRING));
		return field;
	}
}
