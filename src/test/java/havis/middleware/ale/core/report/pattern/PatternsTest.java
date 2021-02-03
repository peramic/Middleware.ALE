package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.tdt.LevelTypeList;
import havis.middleware.utils.data.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class PatternsTest {

    @Test(expected = ValidationException.class)
    public void patternsThrownExceptionTest() throws ValidationException {
        new Patterns(PatternType.GROUP, Arrays.asList(new String[] { "[x1-xffffffffffffffffffffffff]" }), new ECFieldSpec() {
            {
                setFieldname("@1.96.32");
                setDatatype("");
            }
        });
    }

    @Test(expected = ValidationException.class)
    public void patternsListExceptionTest() throws ValidationException {
        new Patterns(PatternType.GROUP, Arrays.asList(new String[] {}), new ECFieldSpec() {
            {
                setFieldname("@1.96.32");
                setDatatype("");
            }
        });
    }

    @Test(expected = ValidationException.class)
    public void patternsExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        new Patterns(PatternType.CACHE, list);
    }

    @Test
    public void patternsPatTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        new Patterns(PatternType.CACHE, list);
    }

    @Test
    public void patternsIdpatTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:idpat:gid:111111111.0001.0001");
        new Patterns(PatternType.FILTER, list);
    }

    @Test(expected = ValidationException.class)
    public void patternsDisjointExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        new Patterns(PatternType.GROUP, list);
    }

    @Test(expected = ValidationException.class)
    public void patternsIpatternExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.DECIMAL, 1, 8, 96);
        Patterns.pattern(PatternType.CACHE, list, field);
    }

    @Test(expected = ValidationException.class)
    public void patternsIpatternDisjointExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("x9");
        list.add("x9");
        ECFieldSpec spec = null;

        spec = new ECFieldSpec("@1.96.12");

        new Patterns(PatternType.CACHE, list, spec);
    }

    @Test
    public void patternsSmallFieldLengthTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("10");
        CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.DECIMAL, 1, 8, 32);
        Patterns.pattern(PatternType.CACHE, list, field);
    }

    @Test(expected = ValidationException.class)
    public void patternsSmallFieldLengthExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("a");
        CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.DECIMAL, 1, 8, 32);
        Patterns.pattern(PatternType.CACHE, list, field);
    }

    @Test(expected = ValidationException.class)
    public void patternsSmallFieldLengthDisjointExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("x9");
        list.add("x9");
        ECFieldSpec spec = null;

        spec = new ECFieldSpec("@1.32.12");

        new Patterns(PatternType.CACHE, list, spec);
    }

    @Test(expected = ValidationException.class)
    public void patternsUnvalidFieldExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("10");
        CommonField field = new CommonField(FieldDatatype.ISO, FieldFormat.DECIMAL, 1, 8, 32);
        Patterns.pattern(PatternType.CACHE, list, field);
    }

    @Test(expected = ValidationException.class)
    public void patternExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:test:sgtin-96:1.426025746.0002.7777");
        Patterns a = new Patterns(PatternType.CACHE, list);
        Assert.assertTrue(a.name("urn:epc:tag:sgtin-96:1.426025746.0002.7777").isEmpty());
    }

    @Test
    public void matchTest() throws ValidationException {
        final Patterns patterns = new Patterns(PatternType.FILTER, Arrays.asList(new String[] { "[x1-xffffffffffffffffffffffff]" }), new ECFieldSpec() {
            {
                setFieldname("@1.96.32");
                setDatatype("uint");
            }
        });
        final byte[] code = Converter.hexToBytes("980365a869b548543a342242");
        @SuppressWarnings("serial")
        Boolean match = patterns.match(new Tag(code) {
            {
                setResult(new HashMap<Integer, Result>() {
                    {
                        put(Integer.valueOf(patterns.getOperation().getId()), new ReadResult() {
                            {
                                setState(ResultState.SUCCESS);
                                setData(code);
                            }
                        });
                    }
                });

            }
        });
        Assert.assertTrue(match.booleanValue());
    }

    @SuppressWarnings("serial")
    @Test
    public void matchNullTest() throws ValidationException {
        final Patterns patterns = new Patterns(PatternType.FILTER, Arrays.asList(new String[] { "[x1-xffffffffffffffffffffffff]" }), new ECFieldSpec() {
            {
                setFieldname("@1.96.32");
                setDatatype("uint");
            }
        });
        final byte[] code = Converter.hexToBytes("980365a869b548543a342242");
        Boolean match = patterns.match(new Tag(code) {
            {
                setResult(new HashMap<Integer, Result>() {
                    {
                        put(Integer.valueOf(patterns.getOperation().getId()), new ReadResult() {
                            {
                                setState(ResultState.ASSOCIATION_TABLE_VALUE_INVALID);
                                setData(code);
                            }
                        });
                    }
                });

            }
        });
        Assert.assertNull(match);
    }

    @Test
    public void matchFalseTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        Patterns a = new Patterns(PatternType.CACHE, list);
        Assert.assertFalse(a.match("urn:epc:tag:sgtin-96:1.426025746.0002.7778").booleanValue());
    }

    @Test
    public void matchURNTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        Patterns a = new Patterns(PatternType.CACHE, list);
        Assert.assertTrue(a.match("urn:epc:tag:sgtin-96:1.426025746.0002.7777").booleanValue());
    }

    @Test
    public void nameTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        Patterns a = new Patterns(PatternType.CACHE, list);
        List<String> expected = new ArrayList<>();
        expected.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        Assert.assertEquals(expected, a.name("urn:epc:tag:sgtin-96:1.426025746.0002.7777"));
    }

    @Test
    public void nextTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        Patterns a = new Patterns(PatternType.CACHE, list);
        Assert.assertEquals(1, a.toList().size());
        IPattern actual = a.next();
        Assert.assertNotNull(actual);
        Assert.assertEquals("urn:epc:pat:sgtin-96:1.426025746.0002.7777", actual.toString());
        actual = a.next();
        Assert.assertNull(actual);
    }

    @Test
    public void toListTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0001.7777");
        list.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        Patterns a = new Patterns(PatternType.CACHE, list);
        List<String> expected = new ArrayList<>();
        expected.add("urn:epc:pat:sgtin-96:1.426025746.0001.7777");
        expected.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        List<String> actual = a.toList();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void appendTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:pat:sgtin-96:1.426025746.0001.7777");
        Patterns patterns = new Patterns(PatternType.CACHE, list, new ECFieldSpec("epc"));
        List<String> add = new ArrayList<>();
        add.add("urn:epc:pat:sgtin-96:1.426025746.0002.7777");
        patterns.append(add);

        IPattern actual = (patterns.next());
        Assert.assertNotNull(actual);
        Assert.assertEquals("urn:epc:pat:sgtin-96:1.426025746.0001.7777", actual.toString());

        actual = (patterns.next());
        Assert.assertNotNull(actual);
        Assert.assertEquals("urn:epc:pat:sgtin-96:1.426025746.0002.7777", actual.toString());

        actual = (patterns.next());
        Assert.assertNull(actual);
    }

    @SuppressWarnings("static-access")
    @Test
    public void queneTest() throws ValidationException {
        Patterns patterns = new Patterns(PatternType.FILTER, Arrays.asList(new String[] { "[x1-xffffffffffffffffffffffff]" }), new ECFieldSpec() {
            {
                setFieldname("@1.96.32");
                setDatatype("uint");
            }
        });
        List<String> list = new ArrayList<>();
        patterns.pattern(PatternType.CACHE, list);
    }

    @Test
    public void queneFieldTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        CommonField field = new CommonField(FieldDatatype.UINT, FieldFormat.DECIMAL, 1, 8, 32);
        Patterns.pattern(PatternType.FILTER, list, field);
    }

    @Test
    public void clearTest() throws ValidationException {
        Patterns patterns = new Patterns(PatternType.FILTER, Arrays.asList(new String[] { "[x1-xffffffffffffffffffffffff]" }), new ECFieldSpec() {
            {
                setFieldname("@1.96.32");
                setDatatype("uint");
            }
        });
        List<String> list = new ArrayList<>();
        list.add("CACHE");
        Assert.assertFalse(patterns.toList().isEmpty());
        patterns.clear();
        Assert.assertTrue(patterns.toList().isEmpty());
    }

    @Test
    public void disposeTest(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
        new NonStrictExpectations() {
            {
                fields.get((ECFieldSpec) any);
                result = field;

                field.getField();
                result = null;

                field.getFieldDatatype();
                result = FieldDatatype.UINT;

                field.getLength();
                result = Integer.valueOf(96);

                field.getFieldFormat();
                result = FieldFormat.HEX;
            }
        };
        Patterns actual = new Patterns(PatternType.FILTER, Arrays.asList(new String[] { "[x1-xffffffffffffffffffffffff]" }), new ECFieldSpec() {
            {
                setFieldname("@1.96.32");
                setDatatype("uint");
            }
        });
        Assert.assertFalse(actual.toList().isEmpty());
        actual.dispose();
        Assert.assertTrue(actual.toList().isEmpty());
        new Verifications() {
            {
                field.dec();
                times = 1;
                field.inc();
                times = 1;
            }
        };
    }

    @Test(expected = ValidationException.class)
    public void patternPatternFormatExceptionTest() throws ValidationException {
        Patterns.pattern(PatternType.GROUP, LevelTypeList.TAG_ENCODING, "test", "test");
    }

    @Test(expected = ValidationException.class)
    public void patternTryCatchExceptionTest() throws ValidationException {
        Patterns.pattern(PatternType.CACHE, LevelTypeList.TAG_ENCODING, "urn:epc:tag:sgtin-96", "uint");
    }

    @Test(expected = ValidationException.class)
    public void patternTagSupportExceptionTest() throws ValidationException {
        List<String> list = new ArrayList<>();
        list.add("urn:epc:idpat:gid:1.426025746.0002.7777");
        new Patterns(PatternType.CACHE, list);
    }

    // match Exception Block testet in class PatternsMatchExceptionTest
}