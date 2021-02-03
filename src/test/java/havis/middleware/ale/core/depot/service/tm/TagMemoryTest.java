package havis.middleware.ale.core.depot.service.tm;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.ConfigType;
import havis.middleware.ale.config.FieldType;
import havis.middleware.ale.config.FieldsType;
import havis.middleware.ale.config.ServiceType;
import havis.middleware.ale.config.TMType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.TM;
import havis.middleware.ale.service.mc.MCTagMemorySpec;
import havis.middleware.ale.service.tm.TMFixedFieldListSpec;
import havis.middleware.ale.service.tm.TMSpec;
import havis.middleware.ale.service.tm.TMVariableFieldListSpec;

import java.util.List;
import java.util.UUID;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TagMemoryTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void getInstance() {
        Assert.assertNotNull(TagMemory.getInstance());
    }

    @Test
    public void getList(@Mocked final ConfigType config) {
        final ServiceType service = new ServiceType();
        service.setTm(new TMType());
        service.getTm().setFields(new FieldsType());
        service.getTm().getFields().getField().add(new FieldType("field", true, new TMFixedFieldListSpec()));

        new NonStrictExpectations() {
            {
                config.getService();
                result = service;
            }
        };

        List<FieldType> list = new TagMemory().getList();
        Assert.assertSame(service.getTm().getFields().getField(), list);
    }

    @Test
    public void getFieldType() {
        TMSpec tmSpec = new TMFixedFieldListSpec();
        MCTagMemorySpec spec = new MCTagMemorySpec("field1", true, tmSpec);
        FieldType field = new TagMemory().get(spec);
        Assert.assertEquals("field1", field.getName());
        Assert.assertTrue(field.isEnable());
        Assert.assertSame(tmSpec, field.getSpec());

        tmSpec = new TMVariableFieldListSpec();
        spec = new MCTagMemorySpec("field2", false, tmSpec);
        field = new TagMemory().get(spec);
        Assert.assertEquals("field2", field.getName());
        Assert.assertFalse(field.isEnable());
        Assert.assertSame(tmSpec, field.getSpec());
    }

    @Test
    public void getMCTagMemorySpec() {
        TMSpec tmSpec = new TMFixedFieldListSpec();
        FieldType field = new FieldType("field1", true, tmSpec);
        MCTagMemorySpec spec = new TagMemory().get(field);
        Assert.assertEquals("field1", spec.getName());
        Assert.assertTrue(spec.isEnable());
        Assert.assertSame(tmSpec, spec.getSpec());

        tmSpec = new TMVariableFieldListSpec();
        field = new FieldType("field2", false, tmSpec);
        spec = new TagMemory().get(field);
        Assert.assertEquals("field2", spec.getName());
        Assert.assertFalse(spec.isEnable());
        Assert.assertSame(tmSpec, spec.getSpec());
    }

    @Test
    public void define(@Mocked final TM tm) throws DuplicateNameException, ValidationException, ImplementationException {
        final TMSpec spec = new TMVariableFieldListSpec();

        new TagMemory().define(new FieldType("field", true, spec));

        new Verifications() {
            {
                tm.define(withEqual("field"), withEqual(spec), false);
                times = 1;
            }
        };
    }

    @Test
    public void undefine(@Mocked final TM tm) throws NoSuchNameException, InUseException, ImplementationException {
        new TagMemory().undefine(new FieldType("field", true, null));

        new Verifications() {
            {
                tm.undefine(withEqual("field"), false);
                times = 1;
            }
        };
    }

    @Test
    public void setEnable(@Mocked final TM tm) throws NoSuchNameException, InUseException, ImplementationException, DuplicateNameException, ValidationException {
        final TMSpec spec = new TMVariableFieldListSpec();

        new TagMemory().setEnable(new FieldType("field", true, spec), true);
        new TagMemory().setEnable(new FieldType("field", true, spec), false);

        new VerificationsInOrder() {
            {
                tm.define(withEqual("field"), withEqual(spec), false);
                times = 1;
                tm.undefine(withEqual("field"), false);
                times = 1;
            }
        };
    }

    @Test
    public void add(@Mocked final ConfigType config) throws NoSuchIdException {
        final ServiceType service = new ServiceType();
        service.setTm(new TMType());
        service.getTm().setFields(new FieldsType());
        new NonStrictExpectations() {
            {
                config.getService();
                result = service;
            }
        };

        final TMSpec spec = new TMVariableFieldListSpec();
        TagMemory tagMemory = new TagMemory();
        tagMemory.add("field", spec);

        List<String> list = tagMemory.toList();
        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());

        MCTagMemorySpec mcSpec = tagMemory.get(UUID.fromString(list.get(0)));

        Assert.assertEquals("field", mcSpec.getName());
        Assert.assertTrue(mcSpec.isEnable());
        Assert.assertSame(spec, mcSpec.getSpec());
    }
}
