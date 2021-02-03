package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.tdt.FieldX;
import havis.middleware.tdt.LevelTypeList;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class PatternTest {

    @Test
    public void matchTest(@Mocked final TdtTagInfo infoMatch, @Mocked final TdtTagInfo infoNoMatch1, @Mocked final TdtTagInfo infoNoMatch2,
            @Mocked final FieldX field0, @Mocked final FieldX field1, @Mocked final FieldX field2, @Mocked final FieldX field3) throws ValidationException,
            TdtTranslationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0000000";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9999999";
                field1.getMaximum();
                result = Long.valueOf(9999999);
                field1.getLength();
                result = BigInteger.valueOf(7);
                field1.getCharacterSet();
                result = "[0-9]*";

                field2.getDecimalMinimum();
                result = "000000";
                field2.getMinimum();
                result = Long.valueOf(0);
                field2.getDecimalMaximum();
                result = "999999";
                field2.getMaximum();
                result = Long.valueOf(999999);
                field2.getLength();
                result = BigInteger.valueOf(6);
                field2.getCharacterSet();
                result = "[0-9]*";

                field3.getDecimalMinimum();
                result = "0000";
                field3.getMinimum();
                result = Long.valueOf(0);
                field3.getDecimalMaximum();
                result = "9999";
                field3.getMaximum();
                result = Long.valueOf(9999);
                field3.getLength();
                result = BigInteger.valueOf(4);
                field3.getCharacterSet();
                result = "[0-9]*";

                infoMatch.getUriTag();
                result = "urn:epc:tag:sgtin-96:3.0614141.812345.6789";

                infoMatch.getUriId();
                result = "urn:epc:id:sgtin:0614141.812345.6789";

                infoNoMatch1.getUriTag();
                result = "urn:epc:tag:sgtin-198:3.0614142.812345.6789";

                infoNoMatch1.getUriId();
                result = "urn:epc:id:sgtin:0614142.812345.6789";

                infoNoMatch2.getUriTag();
                result = "urn:epc:tag:sgtin-96:3.0614142.812345.6789";

                infoNoMatch2.getUriId();
                result = "urn:epc:id:sgtin:0614142.812345.6789";
            }
        };

        Pattern tagPattern = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:sgtin-96", new String[] { "3", "0614141", "812345", "6789" }, Arrays.asList(field0, field1, field2, field3));
        Pattern idPattern = new Pattern(PatternType.FILTER, LevelTypeList.PURE_IDENTITY, "urn:epc:id:sgtin", new String[] { "0614141", "812345", "6789" }, Arrays.asList(field1, field2, field3));

        Assert.assertTrue(tagPattern.match(infoMatch, null));
        Assert.assertTrue(idPattern.match(infoMatch, null));

        Assert.assertFalse(tagPattern.match(infoNoMatch1, null));
        Assert.assertFalse(idPattern.match(infoNoMatch1, null));

        Assert.assertFalse(tagPattern.match(infoNoMatch2, null));
        Assert.assertFalse(idPattern.match(infoNoMatch2, null));

        Pattern binaryIdPattern = new Pattern(PatternType.FILTER, LevelTypeList.BINARY, "urn:epc:id:sgtin", new String[] { "0614141", "812345", "6789" }, Arrays.asList(field1, field2, field3));
        Assert.assertFalse(binaryIdPattern.match(infoMatch, null));
        Assert.assertFalse(binaryIdPattern.match(infoNoMatch1, null));
        Assert.assertFalse(binaryIdPattern.match(infoNoMatch2, null));
    }

    @Test
    public void nameTest(@Mocked final TdtTagInfo info, @Mocked final FieldX field0, @Mocked final FieldX field1, @Mocked final FieldX field2,
            @Mocked final FieldX field3) throws ValidationException, TdtTranslationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0000000";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9999999";
                field1.getMaximum();
                result = Long.valueOf(9999999);
                field1.getLength();
                result = BigInteger.valueOf(7);
                field1.getCharacterSet();
                result = "[0-9]*";

                field2.getDecimalMinimum();
                result = "000000";
                field2.getMinimum();
                result = Long.valueOf(0);
                field2.getDecimalMaximum();
                result = "999999";
                field2.getMaximum();
                result = Long.valueOf(999999);
                field2.getLength();
                result = BigInteger.valueOf(6);
                field2.getCharacterSet();
                result = "[0-9]*";

                field3.getDecimalMinimum();
                result = "0000";
                field3.getMinimum();
                result = Long.valueOf(0);
                field3.getDecimalMaximum();
                result = "9999";
                field3.getMaximum();
                result = Long.valueOf(9999);
                field3.getLength();
                result = BigInteger.valueOf(4);
                field3.getCharacterSet();
                result = "[0-9]*";

                info.getUriTag();
                result = "urn:epc:tag:sgtin-96:3.0614141.812345.6789";

                info.getUriId();
                result = "urn:epc:id:sgtin:0614141.812345.6789";
            }
        };

        Pattern tagPattern = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:sgtin-96", new String[] { "3", "0614141", "812345", "6789" }, Arrays.asList(field0, field1, field2, field3));
        Pattern idPattern = new Pattern(PatternType.FILTER, LevelTypeList.PURE_IDENTITY, "urn:epc:id:sgtin", new String[] { "0614141", "812345", "6789" }, Arrays.asList(field1, field2, field3));

        Assert.assertEquals("urn:epc:pat:sgtin-96:3.0614141.812345.6789", tagPattern.name(info, null));
        Assert.assertEquals("urn:epc:idpat:sgtin:0614141.812345.6789", idPattern.name(info, null));

        Pattern binaryIdPattern = new Pattern(PatternType.FILTER, LevelTypeList.BINARY, "urn:epc:id:sgtin", new String[] { "0614141", "812345", "6789" }, Arrays.asList(field1, field2, field3));
        Assert.assertNull(binaryIdPattern.name(info, null));

        Pattern tagPattern2 = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:sgtin-96", new String[] { "4", "0614141", "812345", "6789" }, Arrays.asList(field0, field1, field2, field3));
        Assert.assertNull(tagPattern2.name(info, null));

        Pattern idPattern2 = new Pattern(PatternType.FILTER, LevelTypeList.PURE_IDENTITY, "urn:epc:id:sgtin", new String[] { "0614142", "812345", "6789" }, Arrays.asList(field1, field2, field3));
        Assert.assertNull(idPattern2.name(info, null));

        Pattern tagPattern3 = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:sgtin-192", new String[] { "3", "0614141", "812345", "6789" }, Arrays.asList(field0, field1, field2, field3));
        Assert.assertNull(tagPattern3.name(info, null));
    }

    @Test
    public void nextTest(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException, TdtTranslationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = BigInteger.valueOf(1);
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };

        Pattern pattern = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "[0-9]", "[0-9]" }, Arrays.asList(field0, field1));

        for (int i = 0; i <= 99; i++)
        {
            StringBuilder next = new StringBuilder("urn:epc:tag:xx:");
            next.append(i / 10);
            next.append('.');
            next.append(i % 10);
            Assert.assertEquals(next.toString(), pattern.next());
        }

        Assert.assertNull(pattern.next());
    }

    @Test
    public void toStringTest(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException, TdtTranslationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = BigInteger.valueOf(1);
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };

        Pattern pattern = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "[0-9]", "[0-9]" }, Arrays.asList(field0, field1));
        Assert.assertEquals("urn:epc:pat:xx:[0-9].[0-9]", pattern.toString());

        for (int i = 0; i <= 98; i++)
        {
            StringBuilder next = new StringBuilder("urn:epc:pat:xx:");
            int firstDigit = i / 10;
            int secondDigit = i % 10;

            if (firstDigit >= 8) {
                next.append(9);
            } else {
                next.append('[').append(firstDigit + 1).append("-9]");
            }

            next.append('.');

            if (secondDigit >= 8) {
                next.append(9);
            } else {
                next.append('[').append(secondDigit + 1).append("-9]");
            }

            pattern.next();
            Assert.assertEquals(next.toString(), pattern.toString());
        }

        pattern.next();
        Assert.assertNull(pattern.toString());
        pattern.next();
        Assert.assertEquals("urn:epc:pat:xx:[0-9].[0-9]", pattern.toString());

        Pattern patternX = new Pattern(PatternType.GROUP, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "X", "X" }, Arrays.asList(field0, field1));
        Assert.assertNull(patternX.toString());
    }

    @Test
    public void disjointTest(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException, TdtTranslationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = BigInteger.valueOf(1);
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };

        Pattern patternWithoutParts = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[0], Collections.<FieldX>emptyList());
        Assert.assertFalse(patternWithoutParts.disjoint("urn:epc:tag:xx", Collections.<Part>emptyList()));
        Assert.assertTrue(patternWithoutParts.disjoint("urn:epc:tag:xy", Collections.<Part>emptyList()));


        Pattern pattern = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "1", "2" }, Arrays.asList(field0, field1));
        Assert.assertFalse(pattern.disjoint("urn:epc:tag:xx", Arrays.asList(new Part("1", field0), new Part("2", field1))));
        Assert.assertTrue(pattern.disjoint("urn:epc:tag:xy", Arrays.asList(new Part("1", field0), new Part("2", field1))));
        Assert.assertTrue(pattern.disjoint("urn:epc:tag:xx", Arrays.asList(new Part("1", field0), new Part("3", field1))));
        Assert.assertTrue(pattern.disjoint("urn:epc:tag:xx", Arrays.asList(new Part("3", field0), new Part("2", field1))));

        Pattern patternMatch = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "1", "2" }, Arrays.asList(field0, field1));
        Pattern patternNoMatch = new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "2", "1" }, Arrays.asList(field0, field1));

        Assert.assertFalse(pattern.disjoint(Arrays.<IPattern>asList(patternMatch)));
        Assert.assertFalse(pattern.disjoint(Arrays.<IPattern>asList(patternMatch, patternMatch)));
        Assert.assertTrue(pattern.disjoint(Arrays.<IPattern>asList(patternNoMatch)));
        Assert.assertFalse(pattern.disjoint(Arrays.<IPattern>asList(patternNoMatch, patternMatch)));
        Assert.assertFalse(pattern.disjoint(Arrays.<IPattern>asList(patternMatch, patternNoMatch)));
        Assert.assertTrue(pattern.disjoint(Arrays.<IPattern>asList(patternNoMatch, patternNoMatch)));
        Assert.assertTrue(pattern.disjoint(Arrays.<IPattern>asList(new IPattern() {
            @Override
            public String next() {
                return null;
            }
            @Override
            public String name(TdtTagInfo info, Result result) {
                return null;
            }
            @Override
            public boolean match(TdtTagInfo info, Result result) {
                return false;
            }
            @Override
            public boolean disjoint(Iterable<IPattern> patterns) {
                return false;
            }
        })));
    }

    @Test(expected = ValidationException.class)
    public void constructorBadArguments() throws ValidationException {
        new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "1" }, Collections.<FieldX> emptyList());
    }

    @Test(expected = ValidationException.class)
    public void constructorBadPart(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = BigInteger.valueOf(1);
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };
        new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "10", "2" }, Arrays.asList(field0, field1));
    }

    @Test(expected = ValidationException.class)
    public void constructorValueAfterRange(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = BigInteger.valueOf(1);
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };
        new Pattern(PatternType.CACHE, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "[0-9]", "2" }, Arrays.asList(field0, field1));
    }

    @Test(expected = ValidationException.class)
    public void constructorFilterWithGroup(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = BigInteger.valueOf(1);
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };
        new Pattern(PatternType.FILTER, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "X", "X" }, Arrays.asList(field0, field1));
    }

    @Test(expected = ValidationException.class)
    public void constructorPureIdentityRange(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = BigInteger.valueOf(1);
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };
        new Pattern(PatternType.FILTER, LevelTypeList.PURE_IDENTITY, "urn:epc:id:xx", new String[] { "[0-9]", "[0-9]" }, Arrays.asList(field0, field1));
    }

    @Test(expected = ValidationException.class)
    public void constructorCacheRangeAlready(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = BigInteger.valueOf(1);
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };
        new Pattern(PatternType.CACHE, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "[0-9]", "[0-9]" }, Arrays.asList(field0, field1));
    }

    @Test
    public void constructorCacheAsteriskIsRange(@Mocked final FieldX field0, @Mocked final FieldX field1) throws ValidationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = "0";
                field0.getMinimum();
                result = Long.valueOf(0);
                field0.getDecimalMaximum();
                result = "9";
                field0.getMaximum();
                result = Long.valueOf(9);
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";

                field1.getDecimalMinimum();
                result = "0";
                field1.getMinimum();
                result = Long.valueOf(0);
                field1.getDecimalMaximum();
                result = "9";
                field1.getMaximum();
                result = Long.valueOf(9);
                field1.getLength();
                result = null;
                field1.getCharacterSet();
                result = "[0-9]";
            }
        };
        Pattern pattern1 = new Pattern(PatternType.CACHE, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "*" }, Arrays.asList(field0));
        Assert.assertEquals("urn:epc:pat:xx:[0-9]", pattern1.toString());

        Pattern pattern2 = new Pattern(PatternType.CACHE, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "*" }, Arrays.asList(field1));
        Assert.assertEquals("urn:epc:pat:xx:[0-9]", pattern2.toString());
    }

    @Test(expected=ValidationException.class)
    public void constructorCacheAsteriskIsRangeWithoutDefinition(@Mocked final FieldX field0) throws ValidationException {
        new NonStrictExpectations() {
            {
                field0.getDecimalMinimum();
                result = null;
                field0.getMinimum();
                result = null;
                field0.getDecimalMaximum();
                result = null;
                field0.getMaximum();
                result = null;
                field0.getLength();
                result = BigInteger.valueOf(1);
                field0.getCharacterSet();
                result = "[0-9]";
            }
        };
        new Pattern(PatternType.CACHE, LevelTypeList.TAG_ENCODING, "urn:epc:tag:xx", new String[] { "*" }, Arrays.asList(field0));
    }
}
