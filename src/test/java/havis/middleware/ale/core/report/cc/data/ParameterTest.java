package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.report.cc.data.Parameter.Callback;
import mockit.Capturing;
import mockit.Deencapsulation;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class ParameterTest {

	@Test
	public void newParameter(@Capturing final Callback callback) {
		final Bytes bytes = new Bytes();
		final Characters characters = new Characters("abc");
		new NonStrictExpectations() {{
			callback.getBytes();
			result = bytes;
			
			callback.getCharacters();
			result = characters;
		}};
		Parameter parameter = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		Assert.assertEquals(FieldDatatype.EPC, parameter.getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_TAG, parameter.getFieldFormat());
		Assert.assertEquals(bytes, parameter.getBytes(new Tag()));
		Assert.assertEquals(characters, parameter.getCharacters(new Tag()));
		Assert.assertEquals("Parameter [datatype=" + parameter.getFieldDatatype() 
				+ ", format=" + parameter.getFieldFormat() + "]", parameter.toString());		
	}
	
	@Test
	public void incDecIsUsed(@Capturing final Callback callback) {
		Parameter parameter = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		parameter.inc();
		parameter.dec();
		Assert.assertFalse(parameter.isUsed());
	}
	
	@Test
	public void hashCodeTest(@Capturing final Callback callback) {
		Parameter parameter1 = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		Parameter parameter2 = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		Assert.assertEquals(parameter1.hashCode(), parameter2.hashCode());		
	}
	
	@Test
	public void hashCodeTestNull(@Capturing final Callback callback) {
		Parameter parameter = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		Deencapsulation.setField(parameter, "datatype", null);
		Deencapsulation.setField(parameter, "format", null);
		Assert.assertEquals(961, parameter.hashCode());		
	}
	
	@Test
	public void equals(@Capturing final Callback callback) {
		Parameter parameter1;
		Parameter parameter2;
		
		parameter1 = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		parameter2 = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_TAG);
		Assert.assertTrue(parameter1.equals(parameter2));
		Assert.assertTrue(parameter1.equals(parameter1));
		
		parameter2 = null;
		Assert.assertFalse(parameter1.equals(parameter2));
		Assert.assertFalse(parameter1.equals("test"));
		
		parameter2 = new Parameter(callback, FieldDatatype.BITS, FieldFormat.EPC_TAG);
		Assert.assertFalse(parameter1.equals(parameter2));
		
		parameter2 = new Parameter(callback, FieldDatatype.EPC, FieldFormat.EPC_PURE);
		Assert.assertFalse(parameter1.equals(parameter2));
	}
}
