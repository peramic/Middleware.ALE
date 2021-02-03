package havis.middleware.ale.core.report;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ec.ECGroupSpec;
import havis.middleware.ale.service.ec.ECGroupSpecExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GroupTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test(expected = ValidationException.class)
    public void groupValidationException(@Mocked final ECGroupSpec spec) throws ValidationException {
        new NonStrictExpectations() {
            {
                spec.getPattern();
                result = new ArrayList<String>();

            }
        };
        new Group(spec);
    }

    @Test
    public void group(@Mocked final ECGroupSpec spec, @Mocked final ECGroupSpecExtension extension) throws ValidationException {
        final List<String> input = new ArrayList<>();
        input.add("urn:epc:pat:sgtin-96:1.392177.1234567.4711");
        final ECFieldSpec ecSpec = new ECFieldSpec("epc");
        new NonStrictExpectations() {
            {
                spec.getPattern();
                result = input;
                spec.getExtension();
                result = extension;
                extension.getFieldspec();
                result = ecSpec;
            }
        };
        new Group(spec);
        new Verifications() {
            {
                spec.getPattern();
                times = 1;
                spec.getExtension();
                times = 2;
                extension.getFieldspec();
                times = 1;
            }
        };
    }

    @Test
    public void dispose(@Mocked final Patterns patterns) throws ValidationException {
        ECGroupSpec spec = new ECGroupSpec();
        Group group = new Group(spec);
        group.dispose();

        new Verifications() {
            {
                patterns.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void getOperation(@Mocked final Patterns patterns) throws ValidationException {
        ECGroupSpec spec = new ECGroupSpec();
        Group group = new Group(spec);
        final Operation operation = new Operation();

        new NonStrictExpectations() {
            {
                patterns.getOperation();
                result = operation;
            }
        };

        Assert.assertSame(operation, group.getOperation());
    }

    @Test
    public void nameTag(@Mocked final Patterns patterns) throws ValidationException {
        ECGroupSpec spec = new ECGroupSpec();
        Group group = new Group(spec);
        final Tag tag = new Tag();
        final List<String> nameResult = Arrays.asList("a", "b");

        new NonStrictExpectations() {
            {
                patterns.name(withSameInstance(tag));
                result = nameResult;
            }
        };

        Assert.assertSame(nameResult, group.name(tag));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void namePort(@Mocked final Patterns patterns) throws ValidationException {
        ECGroupSpec spec = new ECGroupSpec();
        Group group = new Group(spec);
        group.name(new Port());
    }
}
