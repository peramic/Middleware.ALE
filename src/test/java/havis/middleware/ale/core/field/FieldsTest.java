package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.IFieldSpec;
import havis.middleware.ale.service.tm.TMFixedFieldListSpec;
import havis.middleware.ale.service.tm.TMFixedFieldSpec;
import havis.middleware.ale.service.tm.TMSpec;
import havis.middleware.ale.service.tm.TMVariableFieldListSpec;
import havis.middleware.ale.service.tm.TMVariableFieldSpec;
import havis.middleware.tdt.TdtTranslationException;
import havis.middleware.utils.data.Calculator;

import java.util.concurrent.atomic.AtomicInteger;

import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class FieldsTest {

    @Test
    public void getInstance() {
        Fields instance = Fields.getInstance();
        Assert.assertNotNull(instance);
        Assert.assertSame(instance, Fields.getInstance());
    }

    @Test
    public void get() {
        Fields fields = new Fields();
        CommonField field = new CommonField();
        fields.set("test", field);
        Assert.assertSame(field, fields.get("test"));
    }

    @Test
    public void set() {
        Fields fields = new Fields();
        CommonField field = new CommonField();
        fields.set("test", field);
        Assert.assertSame(field, fields.get("test"));

        fields.set("test", null);
        Assert.assertNull(fields.get("test"));
    }

    @Test
    public void containsKey() {
        Fields fields = new Fields();
        CommonField field = new CommonField();
        fields.set("test", field);
        Assert.assertTrue(fields.containsKey("test"));
        Assert.assertFalse(fields.containsKey("test2"));
    }

    @Test
    public void getDatatype() throws ValidationException {
        Assert.assertEquals(FieldDatatype.BITS, Fields.getDatatype("test", "bits"));
        Assert.assertEquals(FieldDatatype.EPC, Fields.getDatatype("test", "epc"));
        Assert.assertEquals(FieldDatatype.UINT, Fields.getDatatype("test", "uint"));
        Assert.assertEquals(FieldDatatype.ISO, Fields.getDatatype("test", "iso-15962-string"));
        Assert.assertNull(Fields.getDatatype("test", null));
    }

    @Test(expected = ValidationException.class)
    public void getDatatypeExceptionBlock1() throws ValidationException {
        Fields.getDatatype(null, "iso");
    }

    @Test(expected = ValidationException.class)
    public void getDatatypeExceptionBlock2() throws ValidationException {
        Fields.getDatatype("test", "iso");
    }

    @Test
    public void getFormatEPC() throws ValidationException {
        Assert.assertEquals(FieldFormat.EPC_PURE, Fields.getFormat("test", "epc", "epc-pure", FieldDatatype.EPC));
        Assert.assertEquals(FieldFormat.EPC_TAG, Fields.getFormat("test", "epc", "epc-tag", FieldDatatype.EPC));
        Assert.assertEquals(FieldFormat.EPC_HEX, Fields.getFormat("test", "epc", "epc-hex", FieldDatatype.EPC));
        Assert.assertEquals(FieldFormat.EPC_DECIMAL, Fields.getFormat("test", "epc", "epc-decimal", FieldDatatype.EPC));
        Assert.assertNull(Fields.getFormat("test", "epc", null, FieldDatatype.EPC));
    }

    @Test
    public void getFormatEPCException() throws ValidationException {
        try {
            Fields.getFormat("test", "epc", "test", FieldDatatype.EPC);
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Unknown format 'test' for datatype epc", e.getMessage());
        }
    }

    @Test
    public void getFormatEPCGlobalException() throws ValidationException {
        try {
            Fields.getFormat("test", "test", "test", FieldDatatype.EPC);
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Datatype for epc shall be epc not 'test'", e.getMessage());
        }
    }

    @Test
    public void getFormatUINT() throws ValidationException {
        Assert.assertEquals(FieldFormat.HEX, Fields.getFormat("test", "uint", "hex", FieldDatatype.UINT));
        Assert.assertEquals(FieldFormat.DECIMAL, Fields.getFormat("test", "uint", "decimal", FieldDatatype.UINT));
        Assert.assertNull(Fields.getFormat("test", "uint", null, FieldDatatype.UINT));
    }

    @Test
    public void getFormatUINTException() throws ValidationException {
        try {
            Fields.getFormat("test", "uint", "test", FieldDatatype.UINT);
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Unknown format 'test' for datatype uint", e.getMessage());
        }
    }

    @Test(expected = ValidationException.class)
    public void getFormatUINTGlobalException() throws ValidationException {
        Fields.getFormat("test", "test", "test", FieldDatatype.UINT);
    }

    @Test(expected = ValidationException.class)
    public void getFormatUINTGlobal2Exception() throws ValidationException {
        Fields.getFormat(null, "test", "test", FieldDatatype.UINT);
    }

    @Test
    public void getFormatBITS() throws ValidationException {
        Assert.assertEquals(FieldFormat.HEX, Fields.getFormat("test", "bits", "hex", FieldDatatype.BITS));
        Assert.assertNull(Fields.getFormat("test", "bits", null, FieldDatatype.BITS));
    }

    @Test
    public void getFormatBITSException() throws ValidationException {
        try {
            Fields.getFormat("test", "bits", "test", FieldDatatype.BITS);
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Unknown format 'test' for datatype bits", e.getMessage());
        }
    }

    @Test(expected = ValidationException.class)
    public void getFormatBITSGlobalException() throws ValidationException {
        Fields.getFormat("test", "test", "test", FieldDatatype.BITS);
    }

    @Test(expected = ValidationException.class)
    public void getFormatBITSGlobal2Exception() throws ValidationException {
        Fields.getFormat(null, "test", "test", FieldDatatype.BITS);
    }

    @Test
    public void getFormatISO() throws ValidationException {
        Assert.assertEquals(FieldFormat.STRING, Fields.getFormat("test", null, null, FieldDatatype.ISO));
        Assert.assertEquals(FieldFormat.STRING, Fields.getFormat("test", "iso-15962-string", null, FieldDatatype.ISO));
        Assert.assertEquals(FieldFormat.STRING, Fields.getFormat("test", null, "string", FieldDatatype.ISO));

    }

    @Test
    public void getFormatISOException() throws ValidationException {
        try {
            Fields.getFormat("test", null, "test", FieldDatatype.ISO);
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Unknown format 'test' for datatype iso-15962-string. Format shall be string", e.getMessage());
        }
    }

    @Test
    public void getFormatISOGlobalException() throws ValidationException {
        try {
            Fields.getFormat(null, "test", "test", FieldDatatype.ISO);
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Datatype shall be iso-15962-string not 'test'", e.getMessage());
        }
    }

    @Test
    public void getFormatSpecDatatype(@Mocked final IFieldSpec spec) throws ValidationException {
        new Expectations() {
            {
                spec.getFieldname();
                result = "test";
                times = 1;
                spec.getDatatype();
                result = "epc";
                times = 1;
                spec.getFormat();
                result = "epc-pure";
                times = 1;
            }
        };
        Assert.assertEquals(FieldFormat.EPC_PURE, Fields.getFormat(spec, FieldDatatype.EPC));
    }

    @Test
    public void getFieldNoLengthNoOffset(@Mocked final ECFieldSpec spec) throws ValidationException {
        new NonStrictExpectations() {
            {
                spec.getFieldname();
                result = "epc";

                spec.getDatatype();
                result = "epc";

                spec.getFormat();
                result = "epc-pure";
            }
        };
        CommonField field = new Fields().getField(spec, FieldDatatype.EPC, FieldFormat.EPC_PURE, 1);
        Assert.assertEquals("epc", field.getName());
        Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.EPC_PURE, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(0, field.getLength());
        Assert.assertEquals(0, field.getOffset());
        Assert.assertTrue(field.isEpc());
    }

    @Test
    public void getFieldNoOffset(@Mocked final ECFieldSpec spec) throws ValidationException {
        new NonStrictExpectations() {
            {
                spec.getFieldname();
                result = "epc";

                spec.getDatatype();
                result = "epc";

                spec.getFormat();
                result = "epc-pure";
            }
        };
        CommonField field = new Fields().getField(spec, FieldDatatype.EPC, FieldFormat.EPC_PURE, 1, 16);
        Assert.assertEquals("epc", field.getName());
        Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.EPC_PURE, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(16, field.getLength());
        Assert.assertEquals(0, field.getOffset());
        Assert.assertTrue(field.isEpc());
    }

    @Test
    public void getField(@Mocked final ECFieldSpec spec) throws ValidationException {
        new NonStrictExpectations() {
            {
                spec.getFieldname();
                result = "epc";

                spec.getDatatype();
                result = "epc";

                spec.getFormat();
                result = "epc-pure";
            }
        };
        CommonField field = new Fields().getField(spec, FieldDatatype.EPC, FieldFormat.EPC_PURE, 1, 16, 2);
        Assert.assertEquals("epc", field.getName());
        Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.EPC_PURE, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(16, field.getLength());
        Assert.assertEquals(2, field.getOffset());
        Assert.assertTrue(field.isEpc());
    }

    @Test
    public void getFieldNotEpc(@Mocked final ECFieldSpec spec) throws ValidationException {
        new NonStrictExpectations() {
            {
                spec.getFieldname();
                result = "test";

                spec.getDatatype();
                result = "uint";

                spec.getFormat();
                result = null;
            }
        };
        CommonField field = new Fields().getField(spec, FieldDatatype.UINT, FieldFormat.HEX, 1, 16, 2);
        Assert.assertEquals("test", field.getName());
        Assert.assertEquals(FieldDatatype.UINT, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(16, field.getLength());
        Assert.assertEquals(2, field.getOffset());
        Assert.assertFalse(field.isEpc());
    }

    @Test
    public void getFormatFieldDatatype() throws ValidationException {
        Assert.assertEquals(FieldFormat.HEX, Fields.getFormat(FieldDatatype.BITS));
        Assert.assertEquals(FieldFormat.HEX, Fields.getFormat(FieldDatatype.UINT));
        Assert.assertEquals(FieldFormat.EPC_TAG, Fields.getFormat(FieldDatatype.EPC));
        Assert.assertEquals(FieldFormat.STRING, Fields.getFormat(FieldDatatype.ISO));
    }

    @Test
    public void getFieldStatic(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "epc";

                Name.isValid("epc", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = Fields.getField(spec);
        Assert.assertEquals("epc", field.getName());
        Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.EPC_TAG, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(0, field.getLength());
        Assert.assertEquals(16, field.getOffset());
    }

    @Test
    public void getWithEpcSpec(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "epc";

                Name.isValid("epc", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("epc", field.getName());
        Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.EPC_TAG, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(0, field.getLength());
        Assert.assertEquals(16, field.getOffset());
    }

    @Test
    public void getWithKillPwd(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "killPwd";

                Name.isValid("killPwd", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("killPwd", field.getName());
        Assert.assertEquals(FieldDatatype.UINT, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, field.getFieldFormat());
        Assert.assertEquals(0, field.getBank());
        Assert.assertEquals(32, field.getLength());
        Assert.assertEquals(0, field.getOffset());

    }

    @Test
    public void getWithAccessPwd(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "accessPwd";

                Name.isValid("accessPwd", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("accessPwd", field.getName());
        Assert.assertEquals(FieldDatatype.UINT, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, field.getFieldFormat());
        Assert.assertEquals(0, field.getBank());
        Assert.assertEquals(32, field.getLength());
        Assert.assertEquals(32, field.getOffset());
    }

    @Test
    public void getWithEpcBank(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "epcBank";

                Name.isValid("epcBank", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("epcBank", field.getName());
        Assert.assertEquals(FieldDatatype.BITS, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(0, field.getLength());
        Assert.assertEquals(0, field.getOffset());
    }

    @Test
    public void getWithTidBank(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "tidBank";

                Name.isValid("tidBank", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("tidBank", field.getName());
        Assert.assertEquals(FieldDatatype.BITS, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, field.getFieldFormat());
        Assert.assertEquals(2, field.getBank());
        Assert.assertEquals(0, field.getLength());
        Assert.assertEquals(0, field.getOffset());
    }

    @Test
    public void getWithUserBank(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "userBank";

                Name.isValid("userBank", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("userBank", field.getName());
        Assert.assertEquals(FieldDatatype.BITS, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, field.getFieldFormat());
        Assert.assertEquals(3, field.getBank());
        Assert.assertEquals(0, field.getLength());
        Assert.assertEquals(0, field.getOffset());
    }

    @Test
    public void getWithAfi(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "afi";

                Name.isValid("afi", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("afi", field.getName());
        Assert.assertEquals(FieldDatatype.UINT, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(8, field.getLength());
        Assert.assertEquals(24, field.getOffset());
    }

    @Test
    public void getWithNsi(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "nsi";

                Name.isValid("nsi", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("nsi", field.getName());
        Assert.assertEquals(FieldDatatype.UINT, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(9, field.getLength());
        Assert.assertEquals(23, field.getOffset());
    }

    @Test
    public void getShadowField(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "test";

                Name.isValid("test", false);
                result = Boolean.TRUE;
            }
        };

        Fields fields = new Fields();
        CommonField shadowedField = new CommonField();
        shadowedField.setFieldDatatype(FieldDatatype.EPC);
        shadowedField.setFieldFormat(FieldFormat.EPC_PURE);
        shadowedField.setName("testName");
        shadowedField.setBank(1);
        shadowedField.setLength(8);
        shadowedField.setOffset(16);
        fields.set("test", shadowedField);
        CommonField field = fields.get(spec);
        Assert.assertEquals("testName", field.getName());
        Assert.assertEquals(FieldDatatype.EPC, field.getFieldDatatype());
        Assert.assertEquals(FieldFormat.EPC_PURE, field.getFieldFormat());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(8, field.getLength());
        Assert.assertEquals(16, field.getOffset());
    }

    @Test
    public void getInvalidName(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "test";

                Name.isValid("test", false);
                result = Boolean.FALSE;
            }
        };
        Assert.assertNull(new Fields().get(spec));
    }

    @Test(expected = ValidationException.class)
    public void getNoSpecExcpetion() throws ValidationException {
        new Fields().get((IFieldSpec) null);
    }

    @Test(expected = ValidationException.class)
    public void getInvalidSpec(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "test";

                Name.isValid("test", false);
                result = Boolean.TRUE;
            }
        };
        new Fields().get(spec);
    }

    @Test
    public void getWithGeneric(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "@1.8.16";

                spec.getDatatype();
                result = "uint";

                Name.isValid("test", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("@1.8.16", field.getName());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(8, field.getLength());
        Assert.assertEquals(16, field.getOffset());
        Assert.assertEquals(FieldDatatype.UINT, field.getFieldDatatype());
    }

    @Test
    public void getWithGenericWithoutOffset(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "@1.8";

                spec.getDatatype();
                result = "uint";

                Name.isValid("test", false);
                result = Boolean.TRUE;
            }
        };
        CommonField field = new Fields().get(spec);
        Assert.assertEquals("@1.8", field.getName());
        Assert.assertEquals(1, field.getBank());
        Assert.assertEquals(8, field.getLength());
        Assert.assertEquals(0, field.getOffset());
        Assert.assertEquals(FieldDatatype.UINT, field.getFieldDatatype());
    }

    @Test
    public void getWithOid(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "@3.urn:oid:1.0.*";

                Name.isValid("test", false);
                result = Boolean.TRUE;
            }
        };

        CommonField field = new Fields().get(spec);
        Assert.assertEquals("@3.urn:oid:1.0.*", field.getName());
        Assert.assertEquals(3, field.getBank());
        Assert.assertEquals(0, field.getLength());
        Assert.assertEquals(0, field.getOffset());
        Assert.assertEquals(FieldDatatype.ISO, field.getFieldDatatype());
    }

    @Test(expected = ValidationException.class)
    public void getWithoutAt(@Mocked final IFieldSpec spec) throws ValidationException {
        new NonStrictExpectations(Name.class) {
            {
                spec.getFieldname();
                result = "1.8.16";

                spec.getDatatype();
                result = "uint";

                Name.isValid("test", false);
                result = Boolean.TRUE;
            }
        };
        new Fields().get(spec);
    }

    @Test
    public void getTmSpecFixedFields(@Mocked final TMFixedFieldSpec fixedField1, @Mocked final TMFixedFieldSpec fixedField2)
            throws ImplementationException, ValidationException {
        TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        spec.setFixedFields(new TMFixedFieldListSpec.FixedFields());
        spec.getFixedFields().getFixedField().add(fixedField1);
        spec.getFixedFields().getFixedField().add(fixedField2);

        new NonStrictExpectations() {
            {
                fixedField1.getFieldname();
                result = "test1";
                fixedField1.getDefaultDatatype();
                result = "epc";
                fixedField1.getDefaultFormat();
                result = "epc-tag";
                fixedField1.getBank();
                result = Integer.valueOf(3);
                fixedField1.getLength();
                result = Integer.valueOf(8);
                fixedField1.getOffset();
                result = Integer.valueOf(16);

                fixedField2.getFieldname();
                result = "test2";
                fixedField2.getDefaultDatatype();
                result = "uint";
                fixedField2.getDefaultFormat();
                result = "hex";
                fixedField2.getBank();
                result = Integer.valueOf(1);
                fixedField2.getLength();
                result = Integer.valueOf(16);
                fixedField2.getOffset();
                result = Integer.valueOf(8);
            }
        };
        CommonField[] fields = new Fields().get(spec);
        Assert.assertNotNull(fields);
        Assert.assertEquals(2, fields.length);
        Assert.assertEquals("test1", fields[0].getName());
        Assert.assertEquals(3, fields[0].getBank());
        Assert.assertEquals(8, fields[0].getLength());
        Assert.assertEquals(16, fields[0].getOffset());
        Assert.assertEquals(FieldDatatype.EPC, fields[0].getFieldDatatype());
        Assert.assertEquals("test2", fields[1].getName());
        Assert.assertEquals(1, fields[1].getBank());
        Assert.assertEquals(16, fields[1].getLength());
        Assert.assertEquals(8, fields[1].getOffset());
        Assert.assertEquals(FieldDatatype.UINT, fields[1].getFieldDatatype());
    }

    @Test
    public void getTmSpecVariableFields(@Mocked final TMVariableFieldSpec varField1, @Mocked final TMVariableFieldSpec varField2) throws ImplementationException, ValidationException {
        TMVariableFieldListSpec spec = new TMVariableFieldListSpec();
        spec.setVariableFields(new TMVariableFieldListSpec.VariableFields());
        spec.getVariableFields().getVariableField().add(varField1);
        spec.getVariableFields().getVariableField().add(varField2);

        new NonStrictExpectations() {
            {
                varField1.getFieldname();
                result = "test1";
                varField1.getBank();
                result = Integer.valueOf(3);
                varField1.getOid();
                result = "urn:oid:1.15962.*";

                varField2.getFieldname();
                result = "test2";
                varField2.getBank();
                result = Integer.valueOf(3);
                varField2.getOid();
                result = "urn:oid:1.15963.*";
            }
        };
        CommonField[] fields = new Fields().get(spec);
        Assert.assertNotNull(fields);
        Assert.assertEquals(2, fields.length);
        Assert.assertEquals("test1", fields[0].getName());
        Assert.assertEquals(3, fields[0].getBank());
        Assert.assertEquals(0, fields[0].getLength());
        Assert.assertEquals(0, fields[0].getOffset());
        Assert.assertEquals(FieldDatatype.ISO, fields[0].getFieldDatatype());
        Assert.assertEquals(FieldFormat.STRING, fields[0].getFieldFormat());
        Assert.assertTrue(fields[0] instanceof VariableField);
        Assert.assertNotNull(((VariableField) fields[0]).getOID());
        Assert.assertEquals("test2", fields[1].getName());
        Assert.assertEquals(3, fields[1].getBank());
        Assert.assertEquals(0, fields[1].getLength());
        Assert.assertEquals(0, fields[1].getOffset());
        Assert.assertEquals(FieldDatatype.ISO, fields[1].getFieldDatatype());
        Assert.assertEquals(FieldFormat.STRING, fields[1].getFieldFormat());
        Assert.assertTrue(fields[1] instanceof VariableField);
        Assert.assertNotNull(((VariableField) fields[1]).getOID());
    }

    @Test(expected = ImplementationException.class)
    public void getTmSpecException(@Mocked TMSpec spec) throws ImplementationException, ValidationException {
        new Fields().get(spec);
    }

    @Test
    public void getNamesFixedFields()
            throws ImplementationException, ValidationException {
        TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        spec.setFixedFields(new TMFixedFieldListSpec.FixedFields());
        TMFixedFieldSpec spec1 = new TMFixedFieldSpec();
        spec1.setFieldname("test1");
        spec.getFixedFields().getFixedField().add(spec1);
        TMFixedFieldSpec spec2 = new TMFixedFieldSpec();
        spec2.setFieldname("test2");
        spec.getFixedFields().getFixedField().add(spec2);

        String[] names = Fields.getNames(spec);
        Assert.assertEquals(2, names.length);
        Assert.assertEquals("test1", names[0]);
        Assert.assertEquals("test2", names[1]);
    }

    @Test(expected = ImplementationException.class)
    public void getNamesException(@Mocked TMSpec spec) throws ImplementationException, ValidationException {
        Fields.getNames(spec);
    }

    @Test
    public void getNamesVariableFields() throws ImplementationException, ValidationException {
        TMVariableFieldListSpec spec = new TMVariableFieldListSpec();
        spec.setVariableFields(new TMVariableFieldListSpec.VariableFields());
        TMVariableFieldSpec spec1 = new TMVariableFieldSpec();
        spec1.setFieldname("test1");
        spec.getVariableFields().getVariableField().add(spec1);
        TMVariableFieldSpec spec2 = new TMVariableFieldSpec();
        spec2.setFieldname("test2");
        spec.getVariableFields().getVariableField().add(spec2);

        String[] names = Fields.getNames(spec);
        Assert.assertEquals(2, names.length);
        Assert.assertEquals("test1", names[0]);
        Assert.assertEquals("test2", names[1]);
    }
    
    @Test
    public void toBytes() {
        Assert.assertArrayEquals(new byte[] { 0x11 }, Fields.toBytes(new CommonField(0, 24, 8), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0x30, (byte) 0x11 })));
        Assert.assertArrayEquals(new byte[] { 0x40 }, Fields.toBytes(new CommonField(0, 2, 2), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0xD0, (byte) 0x00 })));
        Assert.assertArrayEquals(new byte[] { 0x40 }, Fields.toBytes(new CommonField(0, 2, 2), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0xD0, (byte) 0xFF })));
        Assert.assertArrayEquals(new byte[] { (byte) 0x80 }, Fields.toBytes(new CommonField(0, 15, 1), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0xFF, (byte) 0xFF })));
        Assert.assertArrayEquals(new byte[] { (byte) 0x80 }, Fields.toBytes(new CommonField(0, 17, 1), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0xD5, (byte) 0xFF })));
    }

    @Test
    public void toLong() {
        Assert.assertEquals(0x11L, Fields.toLong(new CommonField(0, 24, 8), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0x30, (byte) 0x11 })));
        Assert.assertEquals(0x01L, Fields.toLong(new CommonField(0, 2, 2), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0xD0, (byte) 0x00 })));
        Assert.assertEquals(0x01L, Fields.toLong(new CommonField(0, 2, 2), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0xD0, (byte) 0xFF })));
        Assert.assertEquals(0x01L, Fields.toLong(new CommonField(0, 15, 1), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0xFF, (byte) 0xFF })));
        Assert.assertEquals(0x01L, Fields.toLong(new CommonField(0, 17, 1), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0xD5, (byte) 0xFF })));
    }

    @Test
    public void toBytesNoData() throws ValidationException {
        try {
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_TAG, null);
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data value is not specified", e.getReason());
        }
    }

    @Test
    public void toBytesWithEpcTag() throws ValidationException {
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32, (byte) 0x80, (byte) 0x78,
                (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90 }, 96),
                Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_TAG, "urn:epc:tag:sgtin-96:3.0036000.123456.400"));
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x31, (byte) 0x74, (byte) 0x00, (byte) 0xC0, (byte) 0xE4, (byte) 0xBB,
                (byte) 0x40, (byte) 0xE6, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00 }, 96),
                Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_TAG, "urn:epc:tag:sscc-96:3.0012345.3141592600"));
    }

    @Test
    public void toBytesWithEpcTagExceptions() throws ValidationException {

        try {
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_TAG, "urn:epc:raw:96.x307402328078900000000190");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc tag value 'urn:epc:raw:96.x307402328078900000000190' is not valid", e.getReason());
        }

        try {
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_TAG, "urn:epc:raw:96.317400C0E4BB40E618000000");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc tag value 'urn:epc:raw:96.317400C0E4BB40E618000000' is not valid", e.getReason());
        }
    }

    @Test
    public void toBytesWithEpcHex() throws ValidationException {
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32, (byte) 0x80, (byte) 0x78,
                (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90 }, 96),
                Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:raw:96.x307402328078900000000190"));
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x31, (byte) 0x74, (byte) 0x00, (byte) 0xC0, (byte) 0xE4, (byte) 0xBB,
                (byte) 0x40, (byte) 0xE6, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00 }, 96),
                Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:raw:96.x317400C0E4BB40E618000000"));
    }

    @Test
    public void toBytesWithEpcHexExceptions() throws ValidationException {
        try {
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:tag:sgtin-96:3.0036000.123456.400");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc raw value 'urn:epc:tag:sgtin-96:3.0036000.123456.400' is not valid", e.getReason());
        }

        try {
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:raw:96.317400C0E4BB40E618000000");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc raw value 'urn:epc:raw:96.317400C0E4BB40E618000000' is not valid", e.getReason());
        }
    }

    @Test
    public void toBytesWithEpcDecimal() throws ValidationException {
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32, (byte) 0x80, (byte) 0x78,
                (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90 }, 96),
                Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, "urn:epc:raw:96.307402328078900000000190"));
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x31, (byte) 0x74, (byte) 0x00, (byte) 0xC0, (byte) 0xE4, (byte) 0xBB,
                (byte) 0x40, (byte) 0xE6, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00 }, 96),
                Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, "urn:epc:raw:96.317400C0E4BB40E618000000"));
    }

    @Test
    public void toBytesWithEpcDecimalExceptions() throws ValidationException {
        try {
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, "urn:epc:tag:sgtin-96:3.0036000.123456.400");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc raw value 'urn:epc:tag:sgtin-96:3.0036000.123456.400' is not valid", e.getReason());
        }

        try {
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, "urn:epc:raw:96.x317400C0E4BB40E618000000");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc raw value 'urn:epc:raw:96.x317400C0E4BB40E618000000' is not valid", e.getReason());
        }

        try {
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, "urn:epc:raw:864.317400C0E4BB40E618000000");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Data epc raw length 'urn:epc:raw:864.317400C0E4BB40E618000000' is not valid. Length to big", e.getMessage());
        }
    }

    @Test
    public void toBytesWithEpcRaw() throws ValidationException {
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32, (byte) 0x80, (byte) 0x78,
                (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90 }, 96),
                Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:raw:96.x307402328078900000000190"));

    }

    @Test
    public void toBytesWithEpcRawExceptions() throws ValidationException {
        try {
            // Length not a number
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:raw:X.x317400C0E4BB40E618000000");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc raw value 'urn:epc:raw:X.x317400C0E4BB40E618000000' is not valid", e.getReason());
        }
        try {
            // Length to big
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:raw:2147483648.x317400C0E4BB40E618000000");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc raw length 'urn:epc:raw:2147483648.x317400C0E4BB40E618000000' is not valid. Length is not a valid number",
                    e.getReason());
        }
        try {
            // Length not modulo 16 != 0
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:raw:1.x317400C0E4BB40E618000000");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc raw length 'urn:epc:raw:1.x317400C0E4BB40E618000000' is not valid. Length not modulo 16", e.getReason());
        }
        try {
            // Data > Length
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_HEX, "urn:epc:raw:16.xAFFEAFFE");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data epc raw length 'urn:epc:raw:16.xAFFEAFFE' is not valid. Hex string is to long", e.getReason());
        }
    }

    @Test
    public void toBytesWithEpcPureExceptions() throws ValidationException {
        try {
            // Invalid Field Format
            Fields.toBytes(FieldDatatype.EPC, FieldFormat.EPC_PURE, "urn:epc:id:x317400C0E4BB40E618000000");
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Field format epc_pure is not valid for write operation", e.getReason());
        }
    }

    @Test
    public void toBytesWithUintHex() throws ValidationException {
        AtomicInteger length = new AtomicInteger(24);
        byte[] bytes = Calculator.trunc(new byte[] { (byte) 0x12, (byte) 0xD6, (byte) 0x87 }, length);
        Assert.assertEquals(new Bytes(FieldDatatype.UINT, bytes, length.intValue()), Fields.toBytes(FieldDatatype.UINT, FieldFormat.HEX, "x12D687"));
    }

    @Test(expected = ValidationException.class)
    public void toBytesWithUintHexException() throws ValidationException {
        Assert.assertEquals(new Bytes(FieldDatatype.UINT, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32, (byte) 0x80, (byte) 0x78,
                (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90 }, 96),
                Fields.toBytes(FieldDatatype.UINT, FieldFormat.HEX, "12529658"));
    }

    @Test
    public void toBytesWithUintDecimal() throws ValidationException {
        AtomicInteger length = new AtomicInteger(24);
        byte[] bytes = Calculator.trunc(new byte[] { (byte) 0x12, (byte) 0xD6, (byte) 0x87 }, length);
        Assert.assertEquals(new Bytes(FieldDatatype.UINT, bytes, length.intValue()), Fields.toBytes(FieldDatatype.UINT, FieldFormat.DECIMAL, "1234567"));
    }

    @Test(expected = ValidationException.class)
    public void toBytesWithUintDecimalException() throws ValidationException {
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32, (byte) 0x80, (byte) 0x78,
                (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90 }, 96),
                Fields.toBytes(FieldDatatype.UINT, FieldFormat.DECIMAL, "x307402328078900000000190"));

    }

    @Test
    public void toBytesWithBitsHex() throws ValidationException {
        Assert.assertEquals(new Bytes(FieldDatatype.BITS, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32 }, 32),
                Fields.toBytes(FieldDatatype.BITS, FieldFormat.HEX, "32:x30740232"));
    }

    @Test
    public void toBytesWithBitsHexException() throws ValidationException {
        try {
            Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32, (byte) 0x80, (byte) 0x78,
                    (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90 }, 96),
                    Fields.toBytes(FieldDatatype.BITS, FieldFormat.HEX, "01010101"));
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertEquals("Data bits value '01010101' is not in hex format", e.getReason());
        }
    }

    @Test
    public void toBytesField() throws ValidationException {
        CommonField field = new CommonField();
        field.setFieldDatatype(FieldDatatype.EPC);
        field.setFieldFormat(FieldFormat.EPC_HEX);
        Assert.assertEquals(new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x30, (byte) 0x74, (byte) 0x02, (byte) 0x32, (byte) 0x80, (byte) 0x78,
                (byte) 0x90, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x90 }, 96),
                Fields.toBytes(field, "urn:epc:raw:96.x307402328078900000000190"));
    }

    @Test
    public void toString(@Mocked final CommonField field) {
        new NonStrictExpectations() {
            {
                field.getOffset();
                result = Integer.valueOf(0);

                field.isEpc();
                result = Boolean.TRUE;

                field.isAdvanced();
                result = Boolean.FALSE;

                field.getLength();
                result = Integer.valueOf(48);

                field.getFieldDatatype();
                result = FieldDatatype.EPC;

                field.getFieldFormat();
                result = FieldFormat.EPC_HEX;
            }
        };
        Assert.assertEquals("urn:epc:raw:48.x020202020202", Fields.toString(field, new byte[] { 0x02, 0x02, 0x02, 0x02, 0x02, 0x02 }));
    }

    @Test
    public void toStringWithoutLength(@Mocked final CommonField field) {
        new NonStrictExpectations() {
            {
                field.getOffset();
                result = Integer.valueOf(0);

                field.isEpc();
                result = Boolean.TRUE;

                field.isAdvanced();
                result = Boolean.FALSE;

                field.getLength();
                result = Integer.valueOf(0);

                field.getFieldDatatype();
                result = FieldDatatype.EPC;

                field.getFieldFormat();
                result = FieldFormat.EPC_HEX;
            }
        };
        Assert.assertEquals("urn:epc:raw:48.x020202020202", Fields.toString(field, new byte[] { 0x02, 0x02, 0x02, 0x02, 0x02, 0x02 }));
    }

    @Test
    public void toStringWithAdvanced(@Mocked final CommonField field) {
        new NonStrictExpectations() {
            {
                field.getOffset();
                result = Integer.valueOf(1);

                field.isEpc();
                result = Boolean.TRUE;

                field.isAdvanced();
                result = Boolean.TRUE;

                field.getLength();
                result = Integer.valueOf(0);

                field.getFieldDatatype();
                result = FieldDatatype.EPC;

                field.getFieldFormat();
                result = FieldFormat.EPC_HEX;
            }
        };
        Assert.assertEquals("urn:epc:raw:48.x020202020202", Fields.toString(field, new byte[] { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 }));
    }

    @Test
    public void toStringBytes() {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF, (byte) 0xFE }, 8);
        Assert.assertEquals("FFFE", Fields.toString(bytes, 8));
    }

    @Test
    public void toStringWithBitsHex() {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8);
        Assert.assertEquals("8:xFF", Fields.toString(FieldDatatype.BITS, FieldFormat.HEX, bytes));

        bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 4);
        Assert.assertEquals("4:xF", Fields.toString(FieldDatatype.BITS, FieldFormat.HEX, bytes));

        bytes = new Bytes(FieldDatatype.EPC, new byte[0], 0);
        Assert.assertEquals("0:x", Fields.toString(FieldDatatype.BITS, FieldFormat.HEX, bytes));
    }

    @Test
    public void toStringWithEpc() throws TdtTranslationException {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { 0x30, 0x74, 0x27, (byte) 0xD5, (byte) 0x88, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09 }, 96);
        Assert.assertEquals("urn:epc:id:sgtin:0652642.102400.9", Fields.toString(FieldDatatype.EPC, FieldFormat.EPC_PURE, bytes));
        Assert.assertEquals("urn:epc:tag:sgtin-96:3.0652642.102400.9", Fields.toString(FieldDatatype.EPC, FieldFormat.EPC_TAG, bytes));
        Assert.assertEquals("urn:epc:raw:96.x307427D58864000000000009", Fields.toString(FieldDatatype.EPC, FieldFormat.EPC_HEX, bytes));
        Assert.assertEquals("urn:epc:raw:96.14995703977777160186182500361", Fields.toString(FieldDatatype.EPC, FieldFormat.EPC_DECIMAL, bytes));
    }

    @Test
    public void toStringWithIso() throws TdtTranslationException {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8);
        Assert.assertEquals("xFF", Fields.toString(FieldDatatype.ISO, null, bytes));
    }

    @Test
    public void toStringWithUint() throws TdtTranslationException {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF, (byte) 0xFE }, 16);
        Assert.assertEquals("65534", Fields.toString(FieldDatatype.UINT, FieldFormat.DECIMAL, bytes));

        bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF, (byte) 0xFE }, 16);
        Assert.assertEquals("xFFFE", Fields.toString(FieldDatatype.UINT, FieldFormat.HEX, bytes));

        bytes = new Bytes(FieldDatatype.EPC, new byte[] { 0x00, 0x00, (byte) 0xFF, (byte) 0xFE }, 16);
        Assert.assertEquals("xFFFE", Fields.toString(FieldDatatype.UINT, FieldFormat.HEX, bytes));

        bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x00, (byte) 0xFF }, 16);
        Assert.assertEquals("xFF", Fields.toString(FieldDatatype.UINT, FieldFormat.HEX, bytes));

        bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0x00 }, 8);
        Assert.assertEquals("x0", Fields.toString(FieldDatatype.UINT, FieldFormat.HEX, bytes));
    }

    @Test
    public void toStringNull() throws TdtTranslationException {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { 0x30, 0x74, 0x27, (byte) 0xD5, (byte) 0x88, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09 }, 96);
        Assert.assertNull(Fields.toString(FieldDatatype.EPC, FieldFormat.STRING, bytes));
        Assert.assertNull(Fields.toString(FieldDatatype.BITS, FieldFormat.STRING, null));
        Assert.assertNull(Fields.toString(FieldDatatype.UINT, FieldFormat.STRING, null));
    }

    @Test
    public void dispose() {
        Fields fields = new Fields();
        fields.set("test", new CommonField());
        Assert.assertTrue(fields.containsKey("test"));
        fields.dispose();
        Assert.assertFalse(fields.containsKey("test"));
    }
}
