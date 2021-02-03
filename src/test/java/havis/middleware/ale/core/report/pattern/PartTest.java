package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.tdt.FieldX;

import java.math.BigInteger;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class PartTest {

    @Test(expected = ValidationException.class)
    public void partRangeExceptionMinTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = null;
                field.getDecimalMaximum();
                result = "3";
            }
        };
        new Part("[1-2]", field);
    }

    @Test(expected = ValidationException.class)
    public void partRangeExceptionMaxTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "0";
                field.getDecimalMaximum();
                result = null;
            }
        };
        new Part("[1-2]", field);
    }

    @Test(expected = ValidationException.class)
    public void partRangeLenghtExceptionTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "1";
                field.getDecimalMaximum();
                result = "5";
                field.getLength();
                result = BigInteger.valueOf(2);
            }
        };
        new Part("[2-4]", field);
    }

    @Test(expected = ValidationException.class)
    public void partRangeMinMaxException1Test(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "1";
                field.getMinimum();
                result = Long.valueOf(1);
                field.getDecimalMaximum();
                result = "3";
                field.getMaximum();
                result = Long.valueOf(3);
            }
        };
        new Part("[0-2]", field);
    }

    @Test(expected = ValidationException.class)
    public void partRangeMinMaxException2Test(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "0";
                field.getMinimum();
                result = Long.valueOf(0);
                field.getDecimalMaximum();
                result = "3";
                field.getMaximum();
                result = Long.valueOf(3);
            }
        };
        new Part("[2-1]", field);
    }

    @Test(expected = ValidationException.class)
    public void partRangeMinMaxException3Test(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "0";
                field.getMinimum();
                result = Long.valueOf(0);
                field.getDecimalMaximum();
                result = "3";
                field.getMaximum();
                result = Long.valueOf(3);
            }
        };
        new Part("[1-4]", field);
    }

    @Test
    public void partRangeTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "8";
                field.getMinimum();
                result = Long.valueOf(8);
                field.getDecimalMaximum();
                result = "12";
                field.getMaximum();
                result = Long.valueOf(12);
                field.getLength();
                result = BigInteger.valueOf(2);
                field.getPadChar();
                result = Character.valueOf('0');
            }
        };
        Part part = new Part("[09-11]", field);
        Assert.assertTrue(part.hasNext());
        Assert.assertTrue(part.hasNext());
        Assert.assertTrue(part.next());
        Assert.assertTrue(part.hasNext());
        Assert.assertEquals("09", part.getCurrent());
        Assert.assertTrue(part.next());
        Assert.assertTrue(part.hasNext());
        Assert.assertEquals("10", part.getCurrent());
        Assert.assertTrue(part.next());
        Assert.assertFalse(part.hasNext());
        Assert.assertEquals("11", part.getCurrent());
        Assert.assertFalse(part.next());
        Assert.assertFalse(part.hasNext());

        part.reset();

        Assert.assertTrue(part.hasNext());
        Assert.assertTrue(part.next());
        Assert.assertEquals("09", part.getCurrent());
        Assert.assertTrue(part.next());
        Assert.assertTrue(part.hasNext());
        Assert.assertEquals("10", part.getCurrent());
        Assert.assertTrue(part.next());
        Assert.assertFalse(part.hasNext());
        Assert.assertEquals("11", part.getCurrent());
        Assert.assertFalse(part.next());
        Assert.assertFalse(part.hasNext());
    }

    @Test
    public void partValueTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "10";
                field.getMinimum();
                result = Long.valueOf(10);
                field.getDecimalMaximum();
                result = "15";
                field.getMaximum();
                result = Long.valueOf(15);
                field.getLength();
                result = BigInteger.valueOf(2);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part part = new Part("11", field);
        Assert.assertTrue(part.hasNext());
        Assert.assertTrue(part.hasNext());
        Assert.assertTrue(part.next());
        Assert.assertFalse(part.hasNext());
        Assert.assertEquals("11", part.getCurrent());
        Assert.assertFalse(part.next());
        Assert.assertFalse(part.hasNext());
    }

    @Test
    public void partValueWithoutBoundaryTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getCharacterSet();
                result = "[1-5]";
            }
        };
        Part part = new Part("3", field);
        Assert.assertTrue(part.next());
        Assert.assertEquals("3", part.getCurrent());
    }

    @Test
    public void partValueWithVariableCharacterSetTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "2";
                field.getMinimum();
                result = Long.valueOf(2);
                field.getDecimalMaximum();
                result = "4";
                field.getMaximum();
                result = Long.valueOf(4);
                field.getLength();
                result = BigInteger.valueOf(1);
                field.getCharacterSet();
                result = "[0-9]*";
            }
        };
        Part part = new Part("3", field);
        Assert.assertTrue(part.hasNext());
        Assert.assertTrue(part.hasNext());
        Assert.assertTrue(part.next());
        Assert.assertFalse(part.hasNext());
        Assert.assertEquals("3", part.getCurrent());
        Assert.assertFalse(part.next());
        Assert.assertFalse(part.hasNext());
    }

    @Test(expected = ValidationException.class)
    public void partValueLengthExceptionTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getLength();
                result = BigInteger.valueOf(1);
            }
        };
        new Part("12", field);
    }

    @Test(expected = ValidationException.class)
    public void partValueNoMatchWithoutBoundaryExceptionTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getCharacterSet();
                result = "[1-2]";
            }
        };
        new Part("3", field);
    }

    @Test(expected = ValidationException.class)
    public void partValueNoMatchWithBoundaryExceptionTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "1";
                field.getMinimum();
                result = Long.valueOf(1);
                field.getDecimalMaximum();
                result = "2";
                field.getMaximum();
                result = Long.valueOf(2);
                field.getCharacterSet();
                result = "[1-2]";
            }
        };
        new Part("3", field);
    }

    @Test(expected = ValidationException.class)
    public void partValueOutOfRangeUpperExceptionTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "1";
                field.getMinimum();
                result = Long.valueOf(1);
                field.getDecimalMaximum();
                result = "2";
                field.getMaximum();
                result = Long.valueOf(2);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        new Part("3", field);
    }

    @Test(expected = ValidationException.class)
    public void partValueOutOfRangeLowerExceptionTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "1";
                field.getMinimum();
                result = Long.valueOf(1);
                field.getDecimalMaximum();
                result = "2";
                field.getMaximum();
                result = Long.valueOf(2);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        new Part("0", field);
    }

    @Test
    public void isWildcardTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "10";
                field.getMinimum();
                result = Long.valueOf(10);
                field.getDecimalMaximum();
                result = "15";
                field.getMaximum();
                result = Long.valueOf(15);
                field.getLength();
                result = BigInteger.valueOf(2);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part v = new Part("11", field);
        Part a = new Part("X", null);
        Part b = new Part("*", null);
        Assert.assertTrue(a.isWildcard());
        Assert.assertTrue(b.isWildcard());
        Assert.assertFalse(v.isWildcard());
    }

    @Test
    public void isRangeTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "0";
                field.getMinimum();
                result = Long.valueOf(0);
                field.getDecimalMaximum();
                result = "3";
                field.getMaximum();
                result = Long.valueOf(3);
                field.getLength();
                result = BigInteger.valueOf(1);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part a = new Part("[1-2]", field);
        Part b = new Part("2", field);
        Assert.assertTrue(a.isRange());
        Assert.assertFalse(b.isRange());
    }

    @Test
    public void isGroupTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "10";
                field.getMinimum();
                result = Long.valueOf(10);
                field.getDecimalMaximum();
                result = "15";
                field.getMaximum();
                result = Long.valueOf(15);
                field.getLength();
                result = BigInteger.valueOf(2);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part v = new Part("11", field);
        Part a = new Part("X", null);
        Assert.assertTrue(a.isGroup());
        Assert.assertFalse(v.isGroup());
    }

    @Test
    public void matchTest(@Mocked final FieldX field) throws ValidationException {
        Part x = new Part("X", null);
        Part asterisk = new Part("*", null);
        Assert.assertTrue(x.match("anything"));
        Assert.assertTrue(asterisk.match("anything"));

        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "0";
                field.getMinimum();
                result = Long.valueOf(0);
                field.getDecimalMaximum();
                result = "3";
                field.getMaximum();
                result = Long.valueOf(3);
                field.getLength();
                result = BigInteger.valueOf(1);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };

        Part range = new Part("[1-2]", field);
        Assert.assertTrue(range.match("1"));
        Assert.assertTrue(range.match("2"));
        Assert.assertFalse(range.match("0"));
        Assert.assertFalse(range.match("3"));

        Part value = new Part("2", field);
        Assert.assertTrue(value.match("2"));
        Assert.assertFalse(value.match("1"));
    }

    @Test
    public void nameTest(@Mocked final FieldX field) throws ValidationException {
        Part x = new Part("X", null);
        Part asterisk = new Part("*", null);
        Assert.assertEquals("anything", x.name("anything"));
        Assert.assertEquals("*", asterisk.name("anything"));

        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "0";
                field.getMinimum();
                result = Long.valueOf(0);
                field.getDecimalMaximum();
                result = "3";
                field.getMaximum();
                result = Long.valueOf(3);
                field.getLength();
                result = BigInteger.valueOf(1);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part range = new Part("[1-2]", field);
        Assert.assertEquals("[1-2]", range.name("1"));
        Assert.assertEquals("[1-2]", range.name("2"));
        Assert.assertNull(range.name("0"));
        Assert.assertNull(range.name("3"));

        Part value = new Part("2", field);
        Assert.assertEquals("2", value.name("2"));
        Assert.assertNull(value.name("1"));
    }

    @Test
    public void toStringTest(@Mocked final FieldX field) throws ValidationException {
        Part x = new Part("X", null);
        Part asterisk = new Part("*", null);
        Assert.assertNull(x.toString());
        Assert.assertNull(asterisk.toString());

        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "8";
                field.getMinimum();
                result = Long.valueOf(8);
                field.getDecimalMaximum();
                result = "12";
                field.getMaximum();
                result = Long.valueOf(12);
                field.getLength();
                result = BigInteger.valueOf(2);
                field.getPadChar();
                result = Character.valueOf('0');
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part range = new Part("[09-11]", field);
        Assert.assertEquals("[09-11]", range.toString());
        Assert.assertTrue(range.next());
        Assert.assertEquals("[10-11]", range.toString());
        Assert.assertTrue(range.next());
        Assert.assertEquals("11", range.toString());
        Assert.assertTrue(range.next());
        Assert.assertEquals("11", range.toString());
        Assert.assertFalse(range.next());
        Assert.assertNull(range.toString());

        Part value = new Part("09", field);
        Assert.assertEquals("09", value.toString());
        Assert.assertTrue(value.next());
        Assert.assertEquals("09", value.toString());
        Assert.assertFalse(value.next());
        Assert.assertNull(value.toString());
        
        
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "0";
                field.getMinimum();
                result = Long.valueOf(0);
                field.getDecimalMaximum();
                result = "2";
                field.getMaximum();
                result = Long.valueOf(2);
                field.getLength();
                result = null;
                field.getPadChar();
                result = Character.valueOf('0');
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        
        range = new Part("[0-2]", field);
        range.next();
        Assert.assertEquals("[1-2]", range.toString());
        range.next();
        Assert.assertEquals("2", range.toString());
    }

    @Test
    public void disjointWildcardTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "10";
                field.getMinimum();
                result = Long.valueOf(10);
                field.getDecimalMaximum();
                result = "15";
                field.getMaximum();
                result = Long.valueOf(15);
                field.getLength();
                result = BigInteger.valueOf(2);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part v = new Part("11", field);
        Part asterisk = new Part("*", null);
        Assert.assertFalse(asterisk.disjoint(asterisk));
        Assert.assertFalse(v.disjoint(asterisk));
        Assert.assertFalse(asterisk.disjoint(v));
    }

    @Test
    public void disjointMinMaxTest(@Mocked final FieldX field) throws ValidationException {
        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "4";
                field.getMinimum();
                result = Long.valueOf(4);
                field.getDecimalMaximum();
                result = "7";
                field.getMaximum();
                result = Long.valueOf(7);
                field.getLength();
                result = BigInteger.valueOf(1);
            }
        };
        Part range1 = new Part("[5-6]", field);

        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "1";
                field.getMinimum();
                result = Long.valueOf(1);
                field.getDecimalMaximum();
                result = "4";
                field.getMaximum();
                result = Long.valueOf(4);
                field.getLength();
                result = BigInteger.valueOf(1);
            }
        };
        Part range2 = new Part("[2-3]", field);

        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "2";
                field.getMinimum();
                result = Long.valueOf(2);
                field.getDecimalMaximum();
                result = "6";
                field.getMaximum();
                result = Long.valueOf(6);
                field.getLength();
                result = BigInteger.valueOf(1);
            }
        };
        Part range3 = new Part("[3-5]", field);

        Assert.assertTrue(range2.disjoint(range1));
        Assert.assertTrue(range1.disjoint(range2));

        Assert.assertFalse(range3.disjoint(range1));
        Assert.assertFalse(range3.disjoint(range2));
        Assert.assertFalse(range1.disjoint(range3));
        Assert.assertFalse(range2.disjoint(range3));

        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "4";
                field.getMinimum();
                result = Long.valueOf(4);
                field.getDecimalMaximum();
                result = "6";
                field.getMaximum();
                result = Long.valueOf(6);
                field.getLength();
                result = BigInteger.valueOf(1);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part value1 = new Part("5", field);

        new NonStrictExpectations() {
            {
                field.getDecimalMinimum();
                result = "10";
                field.getMinimum();
                result = Long.valueOf(10);
                field.getDecimalMaximum();
                result = "15";
                field.getMaximum();
                result = Long.valueOf(15);
                field.getLength();
                result = BigInteger.valueOf(2);
                field.getCharacterSet();
                result = "[0-9]";
            }
        };
        Part value2 = new Part("11", field);

        Assert.assertTrue(value1.disjoint(value2));
        Assert.assertFalse(value1.disjoint(value1));
        Assert.assertFalse(value2.disjoint(value2));

        Assert.assertTrue(value1.disjoint(range2));
        Assert.assertTrue(range2.disjoint(value1));

        Assert.assertFalse(value1.disjoint(range1));
        Assert.assertFalse(range1.disjoint(value1));
        Assert.assertFalse(value1.disjoint(range3));
        Assert.assertFalse(range3.disjoint(value1));

        Assert.assertTrue(value2.disjoint(range1));
        Assert.assertTrue(range1.disjoint(value2));
        Assert.assertTrue(value2.disjoint(range2));
        Assert.assertTrue(range2.disjoint(value2));
        Assert.assertTrue(value2.disjoint(range3));
        Assert.assertTrue(range3.disjoint(value2));
    }
}
