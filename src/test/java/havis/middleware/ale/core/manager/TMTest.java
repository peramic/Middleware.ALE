package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.depot.service.tm.TagMemory;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.service.tm.TMFixedFieldListSpec;
import havis.middleware.ale.service.tm.TMFixedFieldListSpec.FixedFields;
import havis.middleware.ale.service.tm.TMFixedFieldListSpecExtension;
import havis.middleware.ale.service.tm.TMFixedFieldSpec;
import havis.middleware.ale.service.tm.TMFixedFieldSpecExtension;
import havis.middleware.ale.service.tm.TMSpec;
import havis.middleware.ale.service.tm.TMSpecExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TMTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Before
    public void reset() {
        TM.getInstance().dispose();
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(TM.getInstance());
    }

    @Test
    public void define(@Mocked final TagMemory depot) throws DuplicateNameException, ValidationException, ImplementationException {
        TMFixedFieldSpec fieldSpec = new TMFixedFieldSpec();
        fieldSpec.setBank(1);
        fieldSpec.setFieldname("somefield");
        fieldSpec.setLength(32);
        fieldSpec.setOffset(16);
        fieldSpec.setDefaultDatatype("uint");
        fieldSpec.setDefaultFormat("hex");
        fieldSpec.setExtension(new TMFixedFieldSpecExtension());

        final TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        spec.setSchemaVersion(BigDecimal.valueOf(1));
        spec.setCreationDate(new Date());
        spec.setExtension(new TMFixedFieldListSpecExtension());
        spec.setBaseExtension(new TMSpecExtension());
        spec.setFixedFields(new FixedFields());
        spec.getFixedFields().getFixedField().add(fieldSpec);

        TM tm = TM.getInstance();
        tm.define("spec", spec, true);
        List<String> names = tm.getNames();
        Assert.assertNotNull(names);
        Assert.assertEquals(1, names.size());
        Assert.assertEquals("spec", names.get(0));

        Assert.assertTrue(Fields.getInstance().containsKey("somefield"));
        CommonField commonField = Fields.getInstance().get("somefield");
        Assert.assertEquals("somefield", commonField.getName());
        Assert.assertEquals(1, commonField.getBank());
        Assert.assertEquals(32, commonField.getLength());
        Assert.assertEquals(16, commonField.getOffset());
        Assert.assertEquals(FieldDatatype.UINT, commonField.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, commonField.getFieldFormat());

        new Verifications() {
            {
                depot.add(withEqual("spec"), withEqual(spec));
                times = 1;
            }
        };
    }

    @Test(expected = ValidationException.class)
    public void defineNoSpec(@Mocked CommonField field) throws DuplicateNameException, ValidationException, ImplementationException {
        TM.getInstance().define("exception", null, true);
    }

    @Test(expected = DuplicateNameException.class)
    public void defineDuplicate(@Mocked TMFixedFieldListSpec spec) throws DuplicateNameException, ValidationException, ImplementationException {
        TM tm = TM.getInstance();
        tm.define("handler", spec, true);
        tm.define("handler", spec, true);
    }

    @Test
    public void defineDuplicateField() throws DuplicateNameException, ValidationException, ImplementationException {
        TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        FixedFields fields = new FixedFields();
        TMFixedFieldSpec fieldSpec = new TMFixedFieldSpec();

        fieldSpec.setBank(1);
        fieldSpec.setFieldname("testTag");
        fieldSpec.setLength(32);
        fieldSpec.setOffset(16);
        fieldSpec.setDefaultDatatype("uint");
        fieldSpec.setDefaultFormat("hex");
        fieldSpec.setExtension(new TMFixedFieldSpecExtension());

        fields.getFixedField().add(fieldSpec);

        spec.setSchemaVersion(BigDecimal.valueOf(1));
        spec.setCreationDate(new Date());
        spec.setExtension(new TMFixedFieldListSpecExtension());
        spec.setBaseExtension(new TMSpecExtension());
        spec.setFixedFields(fields);

        CommonField field = new CommonField(1, 16, 32);
        Fields.getInstance().set("field", field);

        TM tm = TM.getInstance();
        tm.define("noException", spec, false);

        try {
            tm.define("exception", spec, false);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }
    }

    @Test(expected = ValidationException.class)
    public void defineDuplicateFieldName() throws DuplicateNameException, ValidationException, ImplementationException {
        TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        FixedFields fields = new FixedFields();
        TMFixedFieldSpec fieldSpec = new TMFixedFieldSpec();

        fieldSpec.setBank(1);
        fieldSpec.setFieldname("exceptionField");
        fieldSpec.setLength(32);
        fieldSpec.setOffset(16);
        fieldSpec.setDefaultDatatype("uint");
        fieldSpec.setDefaultFormat("hex");
        fieldSpec.setExtension(new TMFixedFieldSpecExtension());

        fields.getFixedField().add(fieldSpec);
        fields.getFixedField().add(fieldSpec);

        spec.setSchemaVersion(BigDecimal.valueOf(1));
        spec.setCreationDate(new Date());
        spec.setExtension(new TMFixedFieldListSpecExtension());
        spec.setBaseExtension(new TMSpecExtension());
        spec.setFixedFields(fields);

        CommonField field = new CommonField(1, 16, 32);
        Fields.getInstance().set("fieldException", field);

        TM.getInstance().define("exceptionThrown", spec, false);
    }

    @Test
    public void undefineNoSuchNameException(@Mocked TMFixedFieldListSpec spec) throws DuplicateNameException, ValidationException, ImplementationException,
            NoSuchNameException, InUseException {
        try {
            TM.getInstance().undefine("undefine", false);
            Assert.fail("Expected NoSuchNameException");
        } catch (NoSuchNameException e) {
        	// ignore
        }
    }

    @Test
    public void undefine(@Mocked final TagMemory depot) throws DuplicateNameException, ValidationException, ImplementationException, NoSuchNameException,
            InUseException {
        TMFixedFieldSpec fieldSpec = new TMFixedFieldSpec();
        fieldSpec.setBank(1);
        fieldSpec.setFieldname("unField");
        fieldSpec.setLength(32);
        fieldSpec.setOffset(16);
        fieldSpec.setDefaultDatatype("uint");
        fieldSpec.setDefaultFormat("hex");
        fieldSpec.setExtension(new TMFixedFieldSpecExtension());

        final TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        spec.setSchemaVersion(BigDecimal.valueOf(1));
        spec.setCreationDate(new Date());
        spec.setExtension(new TMFixedFieldListSpecExtension());
        spec.setBaseExtension(new TMSpecExtension());
        spec.setFixedFields(new FixedFields());
        spec.getFixedFields().getFixedField().add(fieldSpec);

        CommonField field = new CommonField(1, 16, 32);
        Fields.getInstance().set("unName", field);

        TM tm = TM.getInstance();
        tm.define("un", spec, true);

        List<String> names = tm.getNames();
        Assert.assertNotNull(names);
        Assert.assertEquals(1, names.size());
        Assert.assertEquals("un", names.get(0));

        Assert.assertTrue(Fields.getInstance().containsKey("unField"));
        CommonField commonField = Fields.getInstance().get("unField");
        Assert.assertEquals("unField", commonField.getName());
        Assert.assertEquals(1, commonField.getBank());
        Assert.assertEquals(32, commonField.getLength());
        Assert.assertEquals(16, commonField.getOffset());
        Assert.assertEquals(FieldDatatype.UINT, commonField.getFieldDatatype());
        Assert.assertEquals(FieldFormat.HEX, commonField.getFieldFormat());

        tm.undefine("un", true);

        names = tm.getNames();
        Assert.assertNotNull(names);
        Assert.assertEquals(0, names.size());

        Assert.assertFalse(Fields.getInstance().containsKey("unField"));

        new VerificationsInOrder() {
            {
                depot.add(withEqual("un"), withEqual(spec));
                times = 1;

                depot.remove(withEqual("un"));
                times = 1;
            }
        };
    }

    @Test(expected = ImplementationException.class)
    public void undefineImplementationException() throws DuplicateNameException, ValidationException, ImplementationException, NoSuchNameException,
            InUseException {
        TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        FixedFields fields = new FixedFields();
        TMFixedFieldSpec fieldSpec = new TMFixedFieldSpec();

        fieldSpec.setBank(1);
        fieldSpec.setFieldname("werner");
        fieldSpec.setLength(32);
        fieldSpec.setOffset(16);
        fieldSpec.setDefaultDatatype("uint");
        fieldSpec.setDefaultFormat("hex");
        fieldSpec.setExtension(new TMFixedFieldSpecExtension());

        fields.getFixedField().add(fieldSpec);

        spec.setSchemaVersion(BigDecimal.valueOf(1));
        spec.setCreationDate(new Date());
        spec.setExtension(new TMFixedFieldListSpecExtension());
        spec.setBaseExtension(new TMSpecExtension());
        spec.setFixedFields(fields);

        CommonField field = new CommonField(1, 16, 32);
        Fields.getInstance().set("jochen", field);

        TM tm = TM.getInstance();
        tm.define("franz", spec, false);

        Fields.getInstance().dispose();

        tm.undefine("franz", false);
    }

    @Test
    public void undefineComplex() throws DuplicateNameException, ValidationException, ImplementationException, NoSuchNameException, InUseException {
        TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        FixedFields fields = new FixedFields();
        TMFixedFieldSpec fieldSpec = new TMFixedFieldSpec();
        TMFixedFieldSpec fieldSpec2 = new TMFixedFieldSpec();

        fieldSpec.setBank(1);
        fieldSpec.setFieldname("one");
        fieldSpec.setLength(32);
        fieldSpec.setOffset(16);
        fieldSpec.setDefaultDatatype("uint");
        fieldSpec.setDefaultFormat("hex");
        fieldSpec.setExtension(new TMFixedFieldSpecExtension());
        fields.getFixedField().add(fieldSpec);
        fieldSpec2.setBank(2);
        fieldSpec2.setFieldname("two");
        fieldSpec2.setLength(32);
        fieldSpec2.setOffset(16);
        fieldSpec2.setDefaultDatatype("uint");
        fieldSpec2.setDefaultFormat("hex");
        fieldSpec2.setExtension(new TMFixedFieldSpecExtension());
        fields.getFixedField().add(fieldSpec2);

        spec.setSchemaVersion(BigDecimal.valueOf(1));
        spec.setCreationDate(new Date());
        spec.setExtension(new TMFixedFieldListSpecExtension());
        spec.setBaseExtension(new TMSpecExtension());
        spec.setFixedFields(fields);

        CommonField fieldOne = new CommonField(1, 16, 32);
        CommonField fieldTwo = new CommonField(3, 16, 32);

        Fields.getInstance().set("listOfFields", fieldOne);
        Fields.getInstance().set("listOfFields", fieldTwo);

        TM tm = TM.getInstance();
        tm.define("fieldList", spec, false);
        tm.undefine("fieldList", false);

        Assert.assertEquals(new ArrayList<String>(), tm.getNames());
    }

    @Test
    public void undefineInUseException() throws DuplicateNameException, ValidationException, ImplementationException, NoSuchNameException, InUseException {
        TMFixedFieldListSpec spec = new TMFixedFieldListSpec();
        FixedFields fields = new FixedFields();
        TMFixedFieldSpec fieldSpec = new TMFixedFieldSpec();
        TMFixedFieldSpec fieldSpec2 = new TMFixedFieldSpec();

        fieldSpec.setBank(1);
        fieldSpec.setFieldname("six");
        fieldSpec.setLength(32);
        fieldSpec.setOffset(16);
        fieldSpec.setDefaultDatatype("uint");
        fieldSpec.setDefaultFormat("hex");
        fieldSpec.setExtension(new TMFixedFieldSpecExtension());
        fields.getFixedField().add(fieldSpec);
        fieldSpec2.setBank(2);
        fieldSpec2.setFieldname("seven");
        fieldSpec2.setLength(32);
        fieldSpec2.setOffset(16);
        fieldSpec2.setDefaultDatatype("uint");
        fieldSpec2.setDefaultFormat("hex");
        fieldSpec2.setExtension(new TMFixedFieldSpecExtension());
        fields.getFixedField().add(fieldSpec2);

        spec.setSchemaVersion(BigDecimal.valueOf(1));
        spec.setCreationDate(new Date());
        spec.setExtension(new TMFixedFieldListSpecExtension());
        spec.setBaseExtension(new TMSpecExtension());
        spec.setFixedFields(fields);

        CommonField fieldOne = new CommonField(1, 16, 32);
        CommonField fieldTwo = new CommonField(3, 16, 32);

        Fields.getInstance().set("listException", fieldOne);
        Fields.getInstance().set("listExceptionOther", fieldTwo);

        TM tm = TM.getInstance();
        tm.define("exceptionList", spec, false);

        Fields.getInstance().get("seven").inc();
        try {
            tm.undefine("exceptionList", false);
            Assert.fail("Expected InUseException");
        } catch (InUseException e) {
            // ignore
        }

        Assert.assertEquals(1, Fields.getInstance().get("six").getBank());
        Assert.assertEquals(32, Fields.getInstance().get("six").getLength());
        Assert.assertEquals(16, Fields.getInstance().get("six").getOffset());
    }

    @Test
    public void getSpecNoSuchNameException() throws NoSuchNameException {
        try {
            TM.getInstance().getSpec("tryMeBetter");
            Assert.fail("Expected NoSuchNameException");
        } catch (NoSuchNameException e) {
            // ignore
        }
    }

    @Test
    public void getSpec(@Mocked final TMFixedFieldListSpec spec) throws DuplicateNameException, ValidationException, ImplementationException,
            NoSuchNameException {
        TM tm = TM.getInstance();
        tm.define("word", spec, false);

        new NonStrictExpectations() {
            {
                spec.getSchemaVersion();
                result = BigDecimal.valueOf(1);
            }
        };

        TMSpec actual = tm.getSpec("word");
        Assert.assertEquals(BigDecimal.valueOf(1), actual.getSchemaVersion());
        Assert.assertSame(spec, actual);
    }

    @Test
    public void getStandardVersionTest() {
        Assert.assertEquals("1.1", TM.getStandardVersion());
    }

    @Test
    public void dispose(@Mocked final TMFixedFieldListSpec spec, @Mocked final Fields fields) throws DuplicateNameException, ValidationException,
            ImplementationException {
        TM tm = TM.getInstance();
        tm.define("whatever", spec, false);
        List<String> names = tm.getNames();
        Assert.assertNotNull(names);
        Assert.assertEquals(1, names.size());
        Assert.assertEquals("whatever", names.get(0));

        tm.dispose();
        names = tm.getNames();
        Assert.assertNotNull(names);
        Assert.assertEquals(0, names.size());

        new Verifications() {
            {
                fields.dispose();
                times = 1;
            }
        };
    }
}
