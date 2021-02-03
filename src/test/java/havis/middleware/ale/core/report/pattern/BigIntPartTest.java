package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;

import org.junit.Assert;
import org.junit.Test;

public class BigIntPartTest {

    @Test
    public void disjointWildcardTest() throws ValidationException {
        CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.DECIMAL, 0, 0, 8);
        BigIntPart wildcard1 = new BigIntPart(field, "*");
        BigIntPart wildcard2 = new BigIntPart(field, "*");
        BigIntPart value = new BigIntPart(field, "255");
        Assert.assertFalse(value.disjoint(wildcard1));
        Assert.assertFalse(wildcard1.disjoint(value));
        Assert.assertFalse(wildcard1.disjoint(wildcard2));
        Assert.assertFalse(wildcard2.disjoint(wildcard1));
    }

    @Test
    public void disjointRangeTest() throws ValidationException{
        CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.DECIMAL, 0, 0, 32);
        BigIntPart range = new BigIntPart(field, "[4294967290-4294967292]");
        BigIntPart lower = new BigIntPart(field, "[4294967288-4294967289]");
        BigIntPart greater = new BigIntPart(field, "[4294967293-4294967294]");
        BigIntPart lowerValue = new BigIntPart(field, "4294967289");
        BigIntPart greaterValue = new BigIntPart(field, "4294967293");
        BigIntPart exactMatch = new BigIntPart(field, "[4294967290-4294967292]");
        BigIntPart insideValue = new BigIntPart(field, "4294967291");
        BigIntPart insideUpperMatch = new BigIntPart(field, "[4294967291-4294967292]");
        BigIntPart insideLowerMatch = new BigIntPart(field, "[4294967290-4294967291]");
        BigIntPart greaterMatch = new BigIntPart(field, "[4294967292-4294967294]");
        BigIntPart lowerMatch = new BigIntPart(field, "[4294967288-4294967290]");
        BigIntPart outsideMatch = new BigIntPart(field, "[4294967288-4294967294]");

        Assert.assertTrue(range.disjoint(lower));
        Assert.assertTrue(range.disjoint(greater));
        Assert.assertTrue(range.disjoint(lowerValue));
        Assert.assertTrue(range.disjoint(greaterValue));
        Assert.assertFalse(range.disjoint(exactMatch));
        Assert.assertFalse(range.disjoint(insideValue));
        Assert.assertFalse(range.disjoint(insideUpperMatch));
        Assert.assertFalse(range.disjoint(insideLowerMatch));
        Assert.assertFalse(range.disjoint(greaterMatch));
        Assert.assertFalse(range.disjoint(lowerMatch));
        Assert.assertFalse(range.disjoint(outsideMatch));

        Assert.assertTrue(lowerValue.disjoint(range));
        Assert.assertTrue(greaterValue.disjoint(range));
        Assert.assertFalse(insideValue.disjoint(range));
    }

    @Test
    public void disjointValueHexTest() throws ValidationException{
        CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 0, 0, 8);
        BigIntPart hexValue = new BigIntPart(field, "xFE");
        BigIntPart match = new BigIntPart(field, "xFE");
        BigIntPart noMatch = new BigIntPart(field, "xFD");
        Assert.assertFalse(hexValue.disjoint(match));
        Assert.assertTrue(hexValue.disjoint(noMatch));
    }

    @Test
    public void disjointValueDecTest() throws ValidationException{
        CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.DECIMAL, 0, 0, 8);
        BigIntPart decValue = new BigIntPart(field, "192");
        BigIntPart match = new BigIntPart(field, "192");
        BigIntPart noMatch = new BigIntPart(field, "193");
        Assert.assertFalse(decValue.disjoint(match));
        Assert.assertTrue(decValue.disjoint(noMatch));
    }

    @Test
    public void disjointMaskTest() throws ValidationException {
        CommonField field1 = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 0, 0, 13);
        BigIntPart range = new BigIntPart(field1, "[x2-x7]");
        Assert.assertFalse(range.disjoint(new BigIntPart(field1, "&x1=x1")));
        Assert.assertTrue(range.disjoint(new BigIntPart(field1, "&x17=x0")));
        Assert.assertFalse(range.disjoint(new BigIntPart(field1, "&x10=x0")));
        Assert.assertFalse(range.disjoint(new BigIntPart(field1, "&x9=x0")));
        Assert.assertTrue(range.disjoint(new BigIntPart(field1, "&xe=x1")));
        Assert.assertFalse(range.disjoint(new BigIntPart(field1, "&x0=x1")));

        CommonField field2 = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 0, 0, 8);
        BigIntPart mask = new BigIntPart(field2, "&xFF=x01");
        BigIntPart match = new BigIntPart(field2, "&x01=x81");
        BigIntPart noMatch = new BigIntPart(field2, "&x01=x80");
        BigIntPart rangeMatch = new BigIntPart(field2, "[x01-x02]");
        //BigIntPart rangeNoMatch = new BigIntPart(field2, "[x02-x03]");
        BigIntPart valueMatch = new BigIntPart(field2, "x01");
        BigIntPart valueNoMatch = new BigIntPart(field2, "x02");
        Assert.assertFalse(mask.disjoint(match));
        Assert.assertTrue(mask.disjoint(noMatch));
        Assert.assertFalse(mask.disjoint(rangeMatch));
        //Assert.assertTrue(mask.disjoint(rangeNoMatch)); // TODO: see BigIntPart:313
        Assert.assertFalse(mask.disjoint(valueMatch));
        Assert.assertTrue(mask.disjoint(valueNoMatch));

        BigIntPart value = new BigIntPart(field2, "x01");
        BigIntPart maskMatch = new BigIntPart(field2, "&x01=x81");
        BigIntPart maskNoMatch = new BigIntPart(field2, "&xFF=x81");
        Assert.assertFalse(value.disjoint(maskMatch));
        Assert.assertTrue(value.disjoint(maskNoMatch));
    }

	@Test(expected = ValidationException.class)
	public void BigIntConstructorDecimalExceptionTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.DECIMAL, 0, 0, 13);
		new BigIntPart(field, "x");
	}

	@Test(expected = ValidationException.class)
	public void BigIntConstructorHexExceptionTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 0, 0, 13);
		new BigIntPart(field, "b");
	}

	@Test(expected = ValidationException.class)
	public void BigIntConstructorHexSecondExceptionTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 0, 0, 13);
		new BigIntPart(field, "x,.-");
	}

	@Test
	public void BigIntConstructorXStarTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 0, 0, 13);
		BigIntPart x = new BigIntPart(field, "X");
		BigIntPart wildcard = new BigIntPart(field, "*");
		Assert.assertTrue(x.isGroup());
		Assert.assertFalse(wildcard.isGroup());
		Assert.assertTrue(wildcard.isWildcard());
	}

	@Test(expected = ValidationException.class)
	public void BigIntConstructorHexThirdExceptionTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 0, 0, 13);
		new BigIntPart(field, "&,.-");
	}

	@Test(expected = ValidationException.class)
	public void BigIntConstructorPatternExceptionTest() throws ValidationException{
		CommonField field = new CommonField(null, FieldFormat.STRING, 0, 0, 13);
		new BigIntPart(field, "test");
	}

	@Test
	public void matchTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.DECIMAL, 1, 0, 8);
		CommonField fieldRange = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 1, 0, 8);
		BigIntPart actual = new BigIntPart(field, "1234");
		Assert.assertFalse(actual.match(new byte[] {1, 2}));
		Assert.assertTrue(actual.match(new byte[] {-102, 64}));
		actual = new BigIntPart(field, "X");
		Assert.assertTrue(actual.match(null));
		actual = new BigIntPart(field, "*");
		Assert.assertTrue(actual.match(null));
		actual = new BigIntPart(field, "[1-3]");
		Assert.assertTrue(actual.match(new byte[] { -128 }));
		Assert.assertTrue(actual.match(new byte[] { -64 }));
		Assert.assertFalse(actual.match(new byte[] { 0 }));
		actual = new BigIntPart(fieldRange, "&x2=x1");
		Assert.assertTrue(actual.match(new byte[] { 0 }));
	}

	@Test
	public void nameXTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.DECIMAL, 1, 16, 8);
		BigIntPart actual = new BigIntPart(field, "X");
		Assert.assertEquals("020A", actual.name(new byte[] { 2, 10 }));
	}

	@Test
	public void nameAsterixTest() throws ValidationException{
		CommonField field = new CommonField(FieldDatatype.EPC, FieldFormat.DECIMAL, 1, 16, 8);
		BigIntPart actual = new BigIntPart(field, "*");
		Assert.assertEquals("*", actual.name(new byte[] { 3, 56 }));
	}

	@Test
	public void nameMaskTest() throws ValidationException{
		CommonField fieldRange = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 3, 8, 32);
		BigIntPart actual = new BigIntPart(fieldRange, "&x8=x0");
		Assert.assertEquals("00000102", actual.name(new byte[] { 0, 0, 1, 2 }));
	}

	@Test
	public void nameValueTest() throws ValidationException{
		CommonField fieldRange = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 3, 8, 32);
		BigIntPart actual = new BigIntPart(fieldRange, "x0102");
		Assert.assertEquals("00000102", actual.name(new byte[] { 0, 0, 1, 2 }));
	}

	@Test
	public void nameRangeTest() throws ValidationException{
		CommonField fieldRange = new CommonField(FieldDatatype.EPC, FieldFormat.HEX, 3, 8, 32);
		BigIntPart actual = new BigIntPart(fieldRange, "[x1-x3]");
		Assert.assertEquals("[00000001-00000003]", actual.name(new byte[] { 0, 0, 0, 2}));
		Assert.assertNull(actual.name(new byte[] { 1, 2, 3, 4}));
	}
}