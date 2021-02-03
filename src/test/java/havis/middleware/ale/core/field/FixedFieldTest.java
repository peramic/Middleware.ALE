package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.tm.TMFixedFieldSpec;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class FixedFieldTest {

	@Test
	public void fixedFieldTest(@Mocked final TMFixedFieldSpec spec ) throws ValidationException{

		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";

				spec.getBank();
				result = Integer.valueOf(3);

				spec.getLength();
				result = Integer.valueOf(8);

				spec.getOffset();
				result = Integer.valueOf(16);

				spec.getDefaultDatatype();
				result = "epc";

				spec.getDefaultFormat();
				result = "epc-pure";
			}
		};
		FixedField fixedField = new FixedField(spec);
		new Verifications() {
			{
				spec.getFieldname();
				times = 3;

				spec.getBank();
				times = 1;

				spec.getLength();
				times = 1;

				spec.getOffset();
				times = 1;

				spec.getDefaultDatatype();
				times = 2;

				spec.getDefaultFormat();
				times = 1;
			}
		};
		Assert.assertEquals(8, fixedField.getLength());
		Assert.assertEquals("iso", fixedField.getName());
		Assert.assertEquals(3, fixedField.getBank());
		Assert.assertEquals(16, fixedField.getOffset());
		Assert.assertEquals("iso", fixedField.getFieldname());
		Assert.assertEquals("epc", fixedField.getDatatype());
		Assert.assertEquals("epc-pure", fixedField.getFormat());

	}

	@Test(expected = ValidationException.class)
	public void fixedFieldExceptionLengthTest(@Mocked final TMFixedFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";
			}
		};
		new FixedField(spec);
	}

	@Test(expected = ValidationException.class)
	public void fixedFieldExceptionBankTest(@Mocked final TMFixedFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";

				spec.getBank();
				result = Integer.valueOf(-1);
			}
		};
		new FixedField(spec);
	}

	@Test(expected = ValidationException.class)
	public void fixedFieldExceptionOffsetTest(@Mocked final TMFixedFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";

				spec.getBank();
				result = Integer.valueOf(3);

				spec.getLength();
				result = Integer.valueOf(8);

				spec.getOffset();
				result = Integer.valueOf(-1);
			}
		};
		new FixedField(spec);
	}

	@Test(expected = ValidationException.class)
	public void fixedFieldExceptionDatatypeTest(@Mocked final TMFixedFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";

				spec.getBank();
				result = Integer.valueOf(3);

				spec.getLength();
				result = Integer.valueOf(8);

				spec.getOffset();
				result = Integer.valueOf(16);
			}
		};
		new FixedField(spec);
	}

	@Test(expected = ValidationException.class)
	public void fixedFieldExceptionFormatTest(@Mocked final TMFixedFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";

				spec.getBank();
				result = Integer.valueOf(3);

				spec.getLength();
				result = Integer.valueOf(8);

				spec.getOffset();
				result = Integer.valueOf(16);

				spec.getDefaultDatatype();
				result = "epc";
			}
		};
		new FixedField(spec);
	}

	// TODO: test isAdvanced, getField, equals
}
