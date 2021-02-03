package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.field.FieldFormat;

import org.junit.Assert;
import org.junit.Test;

public class UIntPartTest {

	@Test
	public void constructorTest() throws ValidationException{
		new UIntPart(FieldFormat.HEX, "x11223344");
		new UIntPart(FieldFormat.DECIMAL, "X");
		new UIntPart(FieldFormat.HEX, "*");
		new UIntPart(FieldFormat.HEX, "[x11223344-x22334455]");
		new UIntPart(FieldFormat.DECIMAL, "[11223344-22334455]");
		new UIntPart(FieldFormat.HEX, "&x1=xA");
		new UIntPart(FieldFormat.DECIMAL, "12");
	}

	@Test(expected = ValidationException.class)
	public void constructorHexExceptionTest() throws ValidationException{
		new UIntPart(FieldFormat.HEX, "xGH");
	}

	@Test(expected = ValidationException.class)
	public void constructorMaskExceptionTest() throws ValidationException{
		new UIntPart(FieldFormat.HEX, "&test");
	}

	@Test(expected = ValidationException.class)
	public void constructorHexRangeExceptionTest() throws ValidationException{
		new UIntPart(FieldFormat.HEX, "[x11223344-22334455]");
	}

	@Test(expected = ValidationException.class)
	public void constructorDecimalExceptionTest() throws ValidationException{
		new UIntPart(FieldFormat.DECIMAL, "[11223344-x22334455]");
	}

	@Test(expected = ValidationException.class)
	public void constructorFormatExceptionTest() throws ValidationException{
		new UIntPart(FieldFormat.STRING, "");
	}

	@Test
	public void isWildcardTest() throws ValidationException{
		UIntPart actual = new UIntPart(FieldFormat.DECIMAL, "12");
		Assert.assertFalse(actual.isWildcard());
		actual = new UIntPart(FieldFormat.DECIMAL, "*");
		Assert.assertTrue(actual.isWildcard());
		actual = new UIntPart(FieldFormat.DECIMAL, "X");
		Assert.assertTrue(actual.isWildcard());
	}

	@Test
	public void isRangeTest() throws ValidationException{
		UIntPart actual = new UIntPart(FieldFormat.DECIMAL, "12");
		Assert.assertFalse(actual.isRange());
		actual = new UIntPart(FieldFormat.HEX, "[x11223344-x22334455]");
		Assert.assertTrue(actual.isRange());
	}

	@Test
	public void isGroupTest() throws ValidationException{
		UIntPart actual = new UIntPart(FieldFormat.DECIMAL, "12");
		Assert.assertFalse(actual.isGroup());
		actual = new UIntPart(FieldFormat.HEX, "X");
		Assert.assertTrue(actual.isGroup());
	}

	@Test
	public void matchWildcardTest() throws ValidationException{
		UIntPart x = new UIntPart(FieldFormat.DECIMAL, "X");
		UIntPart asterix = new UIntPart(FieldFormat.DECIMAL, "*");
		Assert.assertTrue(x.match(12));
		Assert.assertTrue(asterix.match(0));
	}

	@Test
	public void matchValueTest() throws ValidationException{
		UIntPart hex = new UIntPart(FieldFormat.HEX, "x12");
		UIntPart dec = new UIntPart(FieldFormat.DECIMAL, "1234");
		Assert.assertTrue(hex.match(18));
		Assert.assertTrue(dec.match(1234));
		Assert.assertFalse(dec.match(4321));
	}

	@Test
	public void matchRangeTest() throws ValidationException{
		UIntPart hex = new UIntPart(FieldFormat.HEX, "[x00000012-x00000014]");
		UIntPart dec = new UIntPart(FieldFormat.DECIMAL, "[12-14]");
		Assert.assertTrue(hex.match(19));
		Assert.assertTrue(dec.match(13));
		Assert.assertFalse(dec.match(21));
	}

	@Test
	public void matchMaskTest() throws ValidationException{
		UIntPart hex = new UIntPart(FieldFormat.HEX, "&x1=xA");
		Assert.assertTrue(hex.match(10));
		Assert.assertFalse(hex.match(9));
	}

	@Test
	public void nameXTest() throws ValidationException{
		UIntPart hex = new UIntPart(FieldFormat.HEX, "X");
		Assert.assertEquals("x14", hex.name(20));
	}

	@Test
	public void nameAsterixTest() throws ValidationException{
		UIntPart hex = new UIntPart(FieldFormat.HEX, "*");
		Assert.assertEquals("*", hex.name(20));
	}

	@Test
	public void nameRangeTest() throws ValidationException{
		UIntPart dec = new UIntPart(FieldFormat.DECIMAL, "[10-20]");
		Assert.assertEquals("[xa-x14]", dec.name(15));
		Assert.assertEquals("[xa-x14]", dec.name(12));
	}

	@Test
	public void nameValueTest() throws ValidationException{
		UIntPart dec = new UIntPart(FieldFormat.DECIMAL, "12");
		Assert.assertEquals("xc", dec.name(12)); // TODO: should it be decimal?
		dec = new UIntPart(FieldFormat.HEX, "xC");
		Assert.assertEquals("xc", dec.name(12));
	}

	@Test
	public void nameMaskTest() throws ValidationException{
		UIntPart dec = new UIntPart(FieldFormat.HEX, "&x1=xf");
		Assert.assertNull(dec.name(8));
		Assert.assertEquals("xf", dec.name(15));
	}

	@Test
	public void disjointPartTest() throws ValidationException {
		UIntPart range = new UIntPart(FieldFormat.HEX, "[x3-x7]");
		UIntPart value = new UIntPart(FieldFormat.HEX, "x3");
		Assert.assertFalse(range.disjoint(new UIntPart(FieldFormat.HEX, "&x1=x1")));
		Assert.assertTrue(range.disjoint(new UIntPart(FieldFormat.HEX, "&x17=x0")));
		Assert.assertFalse(range.disjoint(new UIntPart(FieldFormat.HEX, "&x10=x0")));
		Assert.assertFalse(range.disjoint(new UIntPart(FieldFormat.HEX, "&x9=x0")));
		Assert.assertTrue(range .disjoint(new UIntPart(FieldFormat.HEX, "&xe=x1")));
		Assert.assertFalse(range.disjoint(new UIntPart(FieldFormat.HEX, "&x0=x1")));
		Assert.assertTrue(range.disjoint(new UIntPart(FieldFormat.HEX, "[x11223344-x22334455]")));
		Assert.assertTrue(range.disjoint(new UIntPart(FieldFormat.HEX, "[x1-x2]")));
		Assert.assertTrue(range.disjoint(new UIntPart(FieldFormat.HEX, "x1")));
		Assert.assertTrue(range.disjoint(new UIntPart(FieldFormat.HEX, "x8")));
		Assert.assertTrue(value.disjoint(new UIntPart(FieldFormat.HEX, "x8")));
		Assert.assertFalse(value.disjoint(new UIntPart(FieldFormat.HEX, "x3")));
		Assert.assertTrue(value.disjoint(new UIntPart(FieldFormat.HEX, "&x17=x0")));
		Assert.assertTrue(value.disjoint(new UIntPart(FieldFormat.HEX, "[x1-x2]")));

		UIntPart mask = new UIntPart(FieldFormat.HEX, "&x3=x3");
        Assert.assertTrue(mask.disjoint(new UIntPart(FieldFormat.HEX, "[x01-x02]")));
        Assert.assertFalse(mask.disjoint(new UIntPart(FieldFormat.HEX, "[x01-x03]")));
        Assert.assertFalse(mask.disjoint(new UIntPart(FieldFormat.HEX, "[x02-x03]")));
        Assert.assertFalse(mask.disjoint(new UIntPart(FieldFormat.HEX, "[x03-x04]")));
        Assert.assertTrue(mask.disjoint(new UIntPart(FieldFormat.HEX, "[x04-x05]")));

        UIntPart mask2 = new UIntPart(FieldFormat.HEX, "&xFF=x01");
        UIntPart match = new UIntPart(FieldFormat.HEX, "&x01=x81");
        UIntPart noMatch = new UIntPart(FieldFormat.HEX, "&x01=x80");
        Assert.assertFalse(mask2.disjoint(match));
        Assert.assertTrue(mask2.disjoint(noMatch));
	}
}