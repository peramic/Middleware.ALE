package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.tm.TMVariableFieldSpec;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class VariableFieldTest {

	@Test
	public void variableFieldTest(@Mocked final TMVariableFieldSpec spec ) throws ValidationException{

		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";

				spec.getBank();
				result = Integer.valueOf(3);

				spec.getOid();
				result = "urn:oid:1.0.*";
			}
		};
		VariableField variableField = new VariableField(spec);
		new Verifications() {
			{
				spec.getFieldname();
				times = 1;

				spec.getBank();
				times = 1;

				spec.getOid();
				times = 2;
			}
		};
		Assert.assertEquals("iso", variableField.getName());
		Assert.assertEquals(3, variableField.getBank());
		Assert.assertEquals(FieldDatatype.ISO, variableField.getFieldDatatype());
		Assert.assertEquals(FieldFormat.STRING, variableField.getFieldFormat());
		Assert.assertEquals(0, variableField.getOffset());
		Assert.assertEquals(0, variableField.getLength());
		Assert.assertTrue(variableField.isAdvanced());
		Assert.assertNotNull(variableField.getOID());
	}

	@Test(expected = ValidationException.class)
	public void variableFieldExceptionOIDTest(@Mocked final TMVariableFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";

				spec.getBank();
				result = Integer.valueOf(3);

				spec.getOid();
				result = "oid:1.0.*";
			}
		};
		new VariableField(spec);
	}

	@Test(expected = ValidationException.class)
	public void variableFieldExceptionBankTest(@Mocked final TMVariableFieldSpec spec) throws ValidationException{
		new NonStrictExpectations(Fields.class) {
			{
				spec.getFieldname();
				result = "iso";

				spec.getBank();
				result = Integer.valueOf(-1);
			}
		};
		new VariableField(spec);
	}

	// TODO: test isAdvanced, getField, equals
}
