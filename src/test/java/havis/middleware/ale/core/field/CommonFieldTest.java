package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ValidationException;

import org.junit.Assert;
import org.junit.Test;

public class CommonFieldTest {

	@Test
	public void isAdvancedTest(){
		CommonField field = new CommonField();
		field.setOffset(16);
		field.setLength(8);
		Assert.assertTrue(field.isAdvanced());
		field.setOffset(0);
		field.setLength(-1);
		Assert.assertFalse(field.isAdvanced());
		field.setOffset(16);
		field.setLength(0);
		Assert.assertTrue(field.isAdvanced());
	}

	@Test
	public void commonFieldFirstTest(){
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, 3, 16, 8);
		Assert.assertEquals(3, field.getBank());
	}

	@Test
	public void commonFieldSecondTest(){
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.EPC_DECIMAL);
		Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_DECIMAL, field.getFieldFormat());
	}

	@Test
	public void commonFieldThirdTest(){
		CommonField field = new CommonField(3, 16, 8);
		Assert.assertEquals(3, field.getBank());
		Assert.assertEquals(16, field.getOffset());
		Assert.assertEquals(8, field.getLength());
	}

	@Test
	public void commonFieldFourthTest() throws ValidationException{
		new CommonField("test");
	}

	@Test(expected = ValidationException.class)
	public void commonFieldFourthExceptionTest() throws ValidationException{
		new CommonField("@test");
	}

	@Test(expected = ValidationException.class)
	public void commonFieldFourthException2Test() throws ValidationException{
		Fields.RESERVED.add("tset");
		new CommonField("tset");
	}

	@Test
	public void commonFieldFifthTest() throws ValidationException{
		CommonField field = new CommonField("test", FieldDatatype.EPC, FieldFormat.EPC_DECIMAL);
		Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_DECIMAL, field.getFieldFormat());
		Assert.assertEquals("test", field.getName());
	}

	@Test
	public void commonFieldSixthTest() throws ValidationException{
		CommonField field = new CommonField("test", FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, 3, 8, 16, true);
		Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_DECIMAL, field.getFieldFormat());
		Assert.assertEquals("test", field.getName());
		Assert.assertEquals(3, field.getBank());
		Assert.assertEquals(16, field.getOffset());
		Assert.assertEquals(8, field.getLength());
		Assert.assertTrue(field.isEpc());
	}
	@Test
	public void setterTest() {
		CommonField field = new CommonField();
		field.setBank(3);
		field.setEpc(true);
		field.setFieldDatatype(FieldDatatype.EPC);
		field.setFieldFormat(FieldFormat.EPC_DECIMAL);
		field.setLength(8);
		field.setName("test");
		field.setOffset(16);
		Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_DECIMAL, field.getFieldFormat());
		Assert.assertEquals("test", field.getName());
		Assert.assertEquals(3, field.getBank());
		Assert.assertEquals(16, field.getOffset());
		Assert.assertEquals(8, field.getLength());
		Assert.assertTrue(field.isEpc());
    }

    @Test
    public void countTest() {
        CommonField field = new CommonField();
        Assert.assertFalse(field.isUsed());
        field.inc();
        Assert.assertTrue(field.isUsed());
        field.dec();
        Assert.assertFalse(field.isUsed());
        field.inc();
        Assert.assertTrue(field.isUsed());
        field.dec();
        Assert.assertFalse(field.isUsed())
        ;
        field.inc();
        Assert.assertTrue(field.isUsed());
        field.inc();
        Assert.assertTrue(field.isUsed());
        field.inc();
        Assert.assertTrue(field.isUsed());
        field.dec();
        Assert.assertTrue(field.isUsed());
        field.dec();
        Assert.assertTrue(field.isUsed());
        field.dec();
        Assert.assertFalse(field.isUsed());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void countExceptionTest() {
        CommonField field = new CommonField();
        Assert.assertFalse(field.isUsed());
        field.inc();
        Assert.assertTrue(field.isUsed());
        field.dec();
        Assert.assertFalse(field.isUsed());
        field.inc();
        Assert.assertTrue(field.isUsed());
        field.dec();
        field.dec();
    }

    @Test
    public void equalsTest(){
		CommonField actual = new CommonField();
		actual.setBank(3);
		actual.setLength(8);
		actual.setOffset(16);
		CommonField expected = new CommonField(3, 16, 8);
		Assert.assertTrue(actual.equals(expected));
		actual.setBank(1);
		Assert.assertFalse(actual.equals(expected));
		Assert.assertFalse(actual.equals(null));
		Assert.assertFalse(actual.equals("whatever"));
	}

	@Test
	public void getFieldTest(){
		CommonField field = new CommonField("test", FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, 3, 8, 16, true);
		Assert.assertEquals(3, field.getField().getBank());
		Assert.assertEquals(16, field.getField().getOffset());
		Assert.assertEquals(16, field.getField().getLength());
		Assert.assertEquals("test", field.getField().getName());
		CommonField field2 = new CommonField("tset", FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, 3, -1, 0, true);
		Assert.assertEquals(3, field2.getField().getBank());
		Assert.assertEquals(-1, field2.getField().getLength());
		Assert.assertEquals(0, field2.getField().getOffset());
		Assert.assertEquals("tset", field2.getField().getName());
	}

	@Test
	public void hashCodeTest(){
		CommonField field = new CommonField();
		Assert.assertEquals(0, field.hashCode());

		field = new CommonField();
		field.setBank(3);
		field.setOffset(16);
		field.setLength(8);
		Assert.assertEquals(234315, field.hashCode());
	}
}
