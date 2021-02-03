package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class PrimaryKeyTest {

    @Test
    public void match() {
        Tag tag = new Tag();
        HashMap<Integer, Result> result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
        result.put(Integer.valueOf(2), new ReadResult(ResultState.SUCCESS));
        tag.setResult(result);

        PrimaryKey primaryKey = new PrimaryKey(tag, null);
        Assert.assertTrue(primaryKey.match());

        List<CommonField> fields = new ArrayList<>();

        primaryKey = new PrimaryKey(tag, fields);
        Assert.assertTrue(primaryKey.match());

        fields = new ArrayList<>();
        CommonField epc1 = new CommonField();
        epc1.setName("epc");
        fields.add(epc1);

        primaryKey = new PrimaryKey(tag, fields);
        Assert.assertTrue(primaryKey.match());

        fields = new ArrayList<>();
        CommonField epc2 = new CommonField();
        epc2.setName("epc");
        fields.add(epc2);

        CommonField field1 = new CommonField();
        field1.setName("field1");
        field1.setFieldDatatype(FieldDatatype.UINT);
        fields.add(field1);

        CommonField field2 = new CommonField();
        field2.setName("field2");
        field2.setFieldDatatype(FieldDatatype.UINT);
        fields.add(field2);

        primaryKey = new PrimaryKey(tag, fields);
        Assert.assertTrue(primaryKey.match());
        
        boolean extended = Tag.isExtended();
        Tag.setExtended(true);
        try {
            fields = new ArrayList<>();
            CommonField epc3 = new CommonField();
            epc3.setName("epc");
            fields.add(epc3);

            CommonField tidBank1 = new CommonField();
            tidBank1.setName("tidBank"); // tidBank is always skipped
            tidBank1.setFieldDatatype(FieldDatatype.ISO);
            fields.add(tidBank1);

            primaryKey = new PrimaryKey(tag, fields);
            Assert.assertTrue(primaryKey.match());        	
        } finally {
        	Tag.setExtended(extended);
        }

        CommonField field3 = new CommonField();
        field3.setName("field3");
        field3.setFieldDatatype(FieldDatatype.ISO);
        fields.add(field3);

        primaryKey = new PrimaryKey(tag, fields);
        Assert.assertFalse(primaryKey.match());

        CommonField field4 = new CommonField();
        field4.setName("field4");
        field4.setFieldDatatype(FieldDatatype.UINT);
        fields.add(field4);

        CommonField field5 = new CommonField();
        field5.setName("field5");
        field5.setFieldDatatype(FieldDatatype.UINT);
        fields.add(field5);

        CommonField field6 = new CommonField();
        field6.setName("field6");
        field6.setFieldDatatype(FieldDatatype.UINT);
        fields.add(field6);

        primaryKey = new PrimaryKey(tag, fields);
        Assert.assertFalse(primaryKey.match());

        tag = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.MISC_ERROR_TOTAL));
        tag.setResult(result);

        CommonField field7 = new CommonField();
        field7.setName("field7");
        field7.setFieldDatatype(FieldDatatype.UINT);
        fields.add(field7);

        primaryKey = new PrimaryKey(tag, fields);
        Assert.assertFalse(primaryKey.match());

        tag = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new Result());
        tag.setResult(result);

        CommonField field8 = new CommonField();
        field8.setName("field8");
        field8.setFieldDatatype(FieldDatatype.UINT);
        fields.add(field8);

        primaryKey = new PrimaryKey(tag, fields);
        Assert.assertFalse(primaryKey.match());
    }

    @Test
    public void equals() {
        Tag tag1 = new Tag();
        tag1.setResult(new HashMap<Integer, Result>());

        PrimaryKey primaryKey1 = new PrimaryKey(tag1, null);

        Tag tag2 = new Tag();
        tag2.setResult(null);

        PrimaryKey primaryKey2 = new PrimaryKey(tag2, null);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));

        Assert.assertTrue(primaryKey2.equals(primaryKey2));

        tag1 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 });
        tag1.setResult(new HashMap<Integer, Result>());

        primaryKey1 = new PrimaryKey(tag1, null);

        tag2 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 });
        tag2.setResult(new HashMap<Integer, Result>());

        primaryKey2 = new PrimaryKey(tag2, null);

        Tag tag3 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 });
        tag3.setResult(new HashMap<Integer, Result>());

        PrimaryKey primaryKey3 = new PrimaryKey(tag3, null);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));
        Assert.assertFalse(primaryKey1.equals(primaryKey3));
        Assert.assertFalse(primaryKey3.equals(primaryKey1));

        Assert.assertTrue(primaryKey1.equals(primaryKey1));
        Assert.assertTrue(primaryKey2.equals(primaryKey2));
        Assert.assertTrue(primaryKey3.equals(primaryKey3));

        List<CommonField> fields = new ArrayList<>();
        CommonField field = new CommonField();
        field.setName("epc");
        fields.add(field);

        tag1 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 });
        tag1.setResult(new HashMap<Integer, Result>());

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 });
        tag2.setResult(new HashMap<Integer, Result>());

        primaryKey2 = new PrimaryKey(tag2, fields);

        tag3 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 });
        tag3.setResult(new HashMap<Integer, Result>());

        primaryKey3 = new PrimaryKey(tag3, fields);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));
        Assert.assertFalse(primaryKey1.equals(primaryKey3));
        Assert.assertFalse(primaryKey3.equals(primaryKey1));

        Assert.assertTrue(primaryKey1.equals(primaryKey1));
        Assert.assertTrue(primaryKey2.equals(primaryKey2));
        Assert.assertTrue(primaryKey3.equals(primaryKey3));

        boolean extended = Tag.isExtended();
        try {
            Tag.setExtended(true);

            fields = new ArrayList<>();
            field = new CommonField();
            field.setName("tidBank");
            fields.add(field);

            tag1 = new Tag();
            tag1.setTid(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 });
            tag1.setResult(new HashMap<Integer, Result>());

            primaryKey1 = new PrimaryKey(tag1, fields);

            tag2 = new Tag();
            tag2.setTid(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 });
            tag2.setResult(new HashMap<Integer, Result>());

            primaryKey2 = new PrimaryKey(tag2, fields);

            tag3 = new Tag();
            tag3.setTid(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 });
            tag3.setResult(new HashMap<Integer, Result>());

            primaryKey3 = new PrimaryKey(tag3, fields);

            Assert.assertFalse(primaryKey1.equals(primaryKey2));
            Assert.assertFalse(primaryKey2.equals(primaryKey1));
            Assert.assertFalse(primaryKey1.equals(primaryKey3));
            Assert.assertFalse(primaryKey3.equals(primaryKey1));

            Assert.assertTrue(primaryKey1.equals(primaryKey1));
            Assert.assertTrue(primaryKey2.equals(primaryKey2));
            Assert.assertTrue(primaryKey3.equals(primaryKey3));
        } finally {
            Tag.setExtended(extended);
        }

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        fields.add(field);

        tag1 = new Tag();
        HashMap<Integer, Result> result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        tag2.setResult(new HashMap<Integer, Result>());

        primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));

        Assert.assertTrue(primaryKey2.equals(primaryKey2));

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.MISC_ERROR_TOTAL));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));

        Assert.assertTrue(primaryKey2.equals(primaryKey2));

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new Result(ResultState.SUCCESS));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        field.setOffset(0);
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 }));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        tag3 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 }));
        tag3.setResult(result);

        primaryKey3 = new PrimaryKey(tag3, fields);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));
        Assert.assertFalse(primaryKey1.equals(primaryKey3));
        Assert.assertFalse(primaryKey3.equals(primaryKey1));

        Assert.assertTrue(primaryKey1.equals(primaryKey1));
        Assert.assertTrue(primaryKey3.equals(primaryKey3));

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        field.setOffset(20);
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 }));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        tag3 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 }));
        tag3.setResult(result);

        primaryKey3 = new PrimaryKey(tag3, fields);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));
        Assert.assertFalse(primaryKey1.equals(primaryKey3));
        Assert.assertFalse(primaryKey3.equals(primaryKey1));

        Assert.assertTrue(primaryKey1.equals(primaryKey1));
        Assert.assertTrue(primaryKey3.equals(primaryKey3));

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        field.setOffset(0);
        field.setLength(11 * 8);
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 }));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        tag3 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1B, (byte) 0x86 }));
        tag3.setResult(result);

        primaryKey3 = new PrimaryKey(tag3, fields);

        Assert.assertTrue(primaryKey1.equals(primaryKey2));
        Assert.assertTrue(primaryKey2.equals(primaryKey1));
        Assert.assertFalse(primaryKey1.equals(primaryKey3));
        Assert.assertFalse(primaryKey3.equals(primaryKey1));

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        field.setOffset(16 + 8);
        field.setLength(8);
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25 }));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x75, 0x25 }));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertFalse(primaryKey1.equals(primaryKey2));
        Assert.assertFalse(primaryKey2.equals(primaryKey1));

        Assert.assertTrue(primaryKey1.equals(primaryKey1));
    }

    @Test
    public void equalsObject() {
        List<CommonField> fields = new ArrayList<>();
        CommonField field = new CommonField();
        field.setName("field");
        field.setOffset(16 + 8);
        field.setLength(8);
        fields.add(field);

        Tag tag1 = new Tag();
        Map<Integer,Result> result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25 }));
        tag1.setResult(result);

        PrimaryKey primaryKey1 = new PrimaryKey(tag1, fields);

        Tag tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x75, 0x25 }));
        tag2.setResult(result);

        PrimaryKey primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertTrue(primaryKey1.equals((Object) primaryKey1));

        Assert.assertFalse(primaryKey1.equals("whatever"));
        Assert.assertFalse(primaryKey1.equals((Object) primaryKey2));
        Assert.assertFalse(primaryKey2.equals((Object) primaryKey1));
    }

    @Test
    public void hashCodeTest() {
        Tag tag1 = new Tag();
        tag1.setResult(new HashMap<Integer, Result>());

        PrimaryKey primaryKey1 = new PrimaryKey(tag1, null);

        Tag tag2 = new Tag();
        tag2.setResult(null);

        PrimaryKey primaryKey2 = new PrimaryKey(tag2, null);

        Assert.assertEquals(0, primaryKey1.hashCode()); // no result and no EPC
        Assert.assertEquals(0, primaryKey2.hashCode()); // no result and no EPC

        tag1 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 });
        tag1.setResult(new HashMap<Integer, Result>());

        primaryKey1 = new PrimaryKey(tag1, null);

        tag2 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 });
        tag2.setResult(new HashMap<Integer, Result>());

        primaryKey2 = new PrimaryKey(tag2, null);

        Tag tag3 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 });
        tag3.setResult(new HashMap<Integer, Result>());

        PrimaryKey primaryKey3 = new PrimaryKey(tag3, null);

        Assert.assertEquals(133627620, primaryKey1.hashCode());
        Assert.assertEquals(1810790363, primaryKey2.hashCode());
        Assert.assertEquals(-329323106, primaryKey3.hashCode());

        List<CommonField> fields = new ArrayList<>();
        CommonField field = new CommonField();
        field.setName("epc");
        fields.add(field);

        tag1 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 });
        tag1.setResult(new HashMap<Integer, Result>());

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 });
        tag2.setResult(new HashMap<Integer, Result>());

        primaryKey2 = new PrimaryKey(tag2, fields);

        tag3 = new Tag(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 });
        tag3.setResult(new HashMap<Integer, Result>());

        primaryKey3 = new PrimaryKey(tag3, fields);

        Assert.assertEquals(133627620, primaryKey1.hashCode());
        Assert.assertEquals(1810790363, primaryKey2.hashCode());
        Assert.assertEquals(-329323106, primaryKey3.hashCode());

        boolean extended = Tag.isExtended();
        try {
            Tag.setExtended(true);

            fields = new ArrayList<>();
            field = new CommonField();
            field.setName("tidBank");
            fields.add(field);

            tag1 = new Tag();
            tag1.setTid(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 });
            tag1.setResult(new HashMap<Integer, Result>());

            primaryKey1 = new PrimaryKey(tag1, fields);

            tag2 = new Tag();
            tag2.setTid(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 });
            tag2.setResult(new HashMap<Integer, Result>());

            primaryKey2 = new PrimaryKey(tag2, fields);

            tag3 = new Tag();
            tag3.setTid(new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 });
            tag3.setResult(new HashMap<Integer, Result>());

            primaryKey3 = new PrimaryKey(tag3, fields);

            Assert.assertEquals(133627620, primaryKey1.hashCode());
            Assert.assertEquals(1810790363, primaryKey2.hashCode());
            Assert.assertEquals(-329323106, primaryKey3.hashCode());
        } finally {
            Tag.setExtended(extended);
        }

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        fields.add(field);

        tag1 = new Tag();
        HashMap<Integer, Result> result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        tag2.setResult(new HashMap<Integer, Result>());

        primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertEquals(0, primaryKey1.hashCode()); // result, but no data to hash
        Assert.assertEquals(0, primaryKey2.hashCode()); // no result

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.MISC_ERROR_TOTAL));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertEquals(0, primaryKey1.hashCode()); // no data
        Assert.assertEquals(0, primaryKey2.hashCode()); // no data

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new Result(ResultState.SUCCESS));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertEquals(0, primaryKey1.hashCode()); // no data
        Assert.assertEquals(0, primaryKey2.hashCode()); // no data

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        field.setOffset(0);
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 }));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        tag3 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 }));
        tag3.setResult(result);

        primaryKey3 = new PrimaryKey(tag3, fields);

        Assert.assertEquals(133627620, primaryKey1.hashCode());
        Assert.assertEquals(1810790363, primaryKey2.hashCode());
        Assert.assertEquals(-329323106, primaryKey3.hashCode());

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        field.setOffset(20);
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 }));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        tag3 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85, 0x00 }));
        tag3.setResult(result);

        primaryKey3 = new PrimaryKey(tag3, fields);

        Assert.assertEquals(742988006, primaryKey1.hashCode());
        Assert.assertEquals(-842757462, primaryKey2.hashCode());
        Assert.assertEquals(582873917, primaryKey3.hashCode());

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        field.setOffset(0);
        field.setLength(11 * 8);
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 }));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        tag3 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1B, (byte) 0x86 }));
        tag3.setResult(result);

        primaryKey3 = new PrimaryKey(tag3, fields);

        Assert.assertEquals(-419248627, primaryKey1.hashCode());
        Assert.assertEquals(-419248627, primaryKey2.hashCode());
        Assert.assertEquals(-36095473, primaryKey3.hashCode());

        fields = new ArrayList<>();
        field = new CommonField();
        field.setName("field");
        field.setOffset(16 + 8);
        field.setLength(8);
        fields.add(field);

        tag1 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25 }));
        tag1.setResult(result);

        primaryKey1 = new PrimaryKey(tag1, fields);

        tag2 = new Tag();
        result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x75, 0x25 }));
        tag2.setResult(result);

        primaryKey2 = new PrimaryKey(tag2, fields);

        Assert.assertEquals(1697040329, primaryKey1.hashCode());
        Assert.assertEquals(294797628, primaryKey2.hashCode());
    }
}
