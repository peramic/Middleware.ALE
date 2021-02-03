package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.IFieldSpec;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class ShadowFieldTest {

	@Test
	public void shadowFieldTest(@Mocked final CommonField field, @Mocked final IFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "test";

				spec.getDatatype();
				result = null;

				field.getFieldDatatype();
				result = FieldDatatype.EPC;

				field.getFieldFormat();
				result = FieldFormat.EPC_TAG;

				field.getBank();
				result = Integer.valueOf(1);

				field.getLength();
				result = Integer.valueOf(8);

				field.getOffset();
				result = Integer.valueOf(16);

				field.getName();
				result = "test";

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};
		ShadowField shadow = new ShadowField(field, spec);
		Assert.assertEquals(1, shadow.getBank());
		Assert.assertEquals(FieldDatatype.EPC, shadow.getFieldDatatype());
		Assert.assertEquals(FieldFormat.EPC_TAG, shadow.getFieldFormat());
		Assert.assertEquals(8, shadow.getLength());
		Assert.assertEquals("test", shadow.getName());
		Assert.assertEquals(16, shadow.getOffset());
		Assert.assertTrue(shadow.isAdvanced());
		new Verifications() {
			{
				spec.getFieldname();
				times = 2;

				spec.getDatatype();
				times = 2;

				field.getFieldDatatype();
				times = 1;

				field.getFieldFormat();
				times = 1;

				field.getBank();
				times = 1;

				field.getLength();
				times = 1;

				field.getOffset();
				times = 1;

				field.getName();
				times = 1;

				field.isAdvanced();
				times = 1;
			}

		};
	}

	@Test
	public void countTest(@Mocked final CommonField field, @Mocked final IFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				field.isUsed();
				result = Boolean.TRUE;

				spec.getFieldname();
				result = "test";

				spec.getDatatype();
				result = null;

				field.getFieldDatatype();
				result = FieldDatatype.EPC;

				field.getFieldFormat();
				result = FieldFormat.EPC_TAG;
			}

		};
		ShadowField shadow = new ShadowField(field, spec);
		shadow.inc();
		shadow.dec();
		Assert.assertTrue(shadow.isUsed());
		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;

				field.isUsed();
				times = 1;
			}

		};
	}

	// TODO: test isAdvanced, getField, equals
}
