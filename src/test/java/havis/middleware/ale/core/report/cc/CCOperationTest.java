package havis.middleware.ale.core.report.cc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Field;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.KillResult;
import havis.middleware.ale.base.operation.tag.result.LockResult;
import havis.middleware.ale.base.operation.tag.result.PasswordResult;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.operation.tag.result.VirtualReadResult;
import havis.middleware.ale.base.operation.tag.result.WriteResult;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Common.DataType;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.cc.CCOpDataSpec;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.tdt.ItemData;
import havis.middleware.utils.data.Calculator;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

public class CCOperationTest {

	@Test(expected = ValidationException.class)
	public void operationNoOpType() throws ValidationException {
		final String fieldName = "epc";
		CCOpSpec spec = new CCOpSpec();
		spec.setFieldspec(new ECFieldSpec(fieldName));
		CCOperation.get(spec, null);
	}

	@Test(expected = ValidationException.class)
	public void operationEmptyOpType() throws ValidationException {
		final String fieldName = "epc";
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		CCOperation.get(spec, null);
	}

	@Test
	public void operationUnknownType() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("Whatever");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void operationRead(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epc";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("READ");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof ReadOperation);
		Assert.assertEquals("READ", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationReadWithDataSpec(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epc";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("READ");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationCheckEpcBank(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("CHECK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:check:iso15962");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof CheckOperation);
		Assert.assertEquals("CHECK", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("urn:epcglobal:ale:check:iso15962", operation.getData().getValue());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationCheckUserBank(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "userBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("CHECK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:check:iso15962");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof CheckOperation);
		Assert.assertEquals("CHECK", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("urn:epcglobal:ale:check:iso15962", operation.getData().getValue());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationCheckOther(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "whatever";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("CHECK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:check:iso15962");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationCheckNoCheck(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("CHECK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:init:iso15962");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationCheckNoMatch(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("CHECK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("something");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationCheckNotLiteral(@Mocked final Fields fields, @Mocked final CommonField field, @Mocked final Common common) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				common.getType();
				result = DataType.PARAMETER;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("CHECK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("PARAMETER");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationInitializeEpcBank(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:init:iso15962:xAF.xDF.force");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof InitializeOperation);
		Assert.assertEquals("INITIALIZE", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("urn:epcglobal:ale:init:iso15962:xAF.xDF.force", operation.getData().getValue());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationInitializeEpcBankNoDsfid(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:init:iso15962:xAF");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof InitializeOperation);
		Assert.assertEquals("INITIALIZE", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("urn:epcglobal:ale:init:iso15962:xAF", operation.getData().getValue());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationInitializeEpcBankNotInit(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:check:iso15962:xAF.xDF.force");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationInitializeEpcBankNoMatch(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("something");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationInitializeUserBank(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "userBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:init:iso15962:xDF.force");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof InitializeOperation);
		Assert.assertEquals("INITIALIZE", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("urn:epcglobal:ale:init:iso15962:xDF.force", operation.getData().getValue());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationInitializeUserBankNoDsfid(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "userBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:init:iso15962:");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof InitializeOperation);
		Assert.assertEquals("INITIALIZE", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("urn:epcglobal:ale:init:iso15962:", operation.getData().getValue());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationInitializeUserBankNotInit(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "userBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:check:iso15962:xDF.force");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationInitializeUserBankNoMatch(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "userBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("something");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationInitializeOther(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "other";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationInitializeNotLiteral(@Mocked final Fields fields, @Mocked final CommonField field, @Mocked final Common common)
			throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				common.getType();
				result = DataType.PARAMETER;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("PARAMETER");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationAdd(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("ADD");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("something");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof AddOperation);
		Assert.assertEquals("ADD", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("something", operation.getData().getValue());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationWrite(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("WRITE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("something");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof WriteOperation);
		Assert.assertEquals("WRITE", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("something", operation.getData().getValue());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationDelete(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.getLength();
				result = Integer.valueOf(96);

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("DELETE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(null);
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof DeleteOperation);
		Assert.assertEquals("DELETE", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertNull(operation.getData());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationDeleteZeroLength(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.getLength();
				result = Integer.valueOf(0);

				field.isAdvanced();
				result = Boolean.FALSE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("DELETE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(null);
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof DeleteOperation);
		Assert.assertEquals("DELETE", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertTrue(operation.isAdvanced());
		Assert.assertNull(operation.getData());

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationDeleteWithDataSpec(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("DELETE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationPassword() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("PASSWORD");
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("xFFFE");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof PasswordOperation);
		Assert.assertEquals("PASSWORD", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertNull(operation.getField());
		Assert.assertFalse(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("xFFFE", operation.getData().getValue());
		Assert.assertEquals(new Bytes(FieldDatatype.UINT, new byte[] { (byte) 0xFF, (byte) 0xFE }, 16), operation.getData().getBytes(null));
	}

	@Test
	public void operationPasswordWithFieldSpec() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("PASSWORD");
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("xFFFE");
		spec.setFieldspec(new ECFieldSpec("field"));
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void operationKill() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("KILL");
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("xFFFE");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof KillOperation);
		Assert.assertEquals("KILL", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertNull(operation.getField());
		Assert.assertFalse(operation.isAdvanced());
		Assert.assertEquals(dataSpec, operation.getData().getSpec());
		Assert.assertEquals(DataType.LITERAL, operation.getData().getType());
		Assert.assertEquals("xFFFE", operation.getData().getValue());
		Assert.assertEquals(new Bytes(FieldDatatype.UINT, new byte[] { (byte) 0xFF, (byte) 0xFE }, 16), operation.getData().getBytes(null));
	}

	@Test
	public void operationKillWithFieldSpec() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("KILL");
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("xFFFE");
		spec.setFieldspec(new ECFieldSpec("field"));
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void operationLock(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("LOCK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("LOCK");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof LockOperation);
		Assert.assertEquals("LOCK", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertFalse(operation.isAdvanced());
		Assert.assertEquals("LOCK", operation.getData().getValue());
		Assert.assertEquals(new Bytes(new byte[] { 0x02 /* ordinal */}), operation.getData().getBytes(null));

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationLockUnlock(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("LOCK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("UNLOCK");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof LockOperation);
		Assert.assertEquals("LOCK", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertFalse(operation.isAdvanced());
		Assert.assertEquals("UNLOCK", operation.getData().getValue());
		Assert.assertEquals(new Bytes(new byte[] { 0x00 /* ordinal */}), operation.getData().getBytes(null));

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationLockPermalock(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("LOCK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("PERMALOCK");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof LockOperation);
		Assert.assertEquals("LOCK", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertFalse(operation.isAdvanced());
		Assert.assertEquals("PERMALOCK", operation.getData().getValue());
		Assert.assertEquals(new Bytes(new byte[] { 0x03 /* ordinal */}), operation.getData().getBytes(null));

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationLockPermaunlock(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("LOCK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("PERMAUNLOCK");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertTrue(operation instanceof LockOperation);
		Assert.assertEquals("LOCK", operation.getName());
		Assert.assertSame(spec, operation.getSpec());
		Assert.assertSame(field, operation.getField());
		Assert.assertFalse(operation.isAdvanced());
		Assert.assertEquals("PERMAUNLOCK", operation.getData().getValue());
		Assert.assertEquals(new Bytes(new byte[] { 0x01 /* ordinal */}), operation.getData().getBytes(null));

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 0;
			}
		};
	}

	@Test
	public void operationLockNotLiteral(@Mocked final Fields fields, @Mocked final CommonField field, @Mocked final Common common) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;

				common.getType();
				result = DataType.PARAMETER;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("LOCK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		spec.getDataSpec().setSpecType("PARAMETER");
		spec.getDataSpec().setData("LOCK");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationLockNoLockType(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("LOCK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		spec.getDataSpec().setSpecType("PARAMETER");
		spec.getDataSpec().setData(null);
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void operationLockUnknownLockType(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "field";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.isAdvanced();
				result = Boolean.TRUE;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("LOCK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		spec.getDataSpec().setSpecType("PARAMETER");
		spec.getDataSpec().setData("Whatever");
		try {
			CCOperation.get(spec, null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				field.inc();
				times = 1;

				field.dec();
				times = 1;
			}
		};
	}

	@Test
	public void getId() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("KILL");
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("xFFFE");
		CCOperation operation = CCOperation.get(spec, null);
		Assert.assertEquals(0, operation.getId());
		operation.setId(5);
		Assert.assertEquals(5, operation.getId());
	}

	@Test
	public void getBase(@Mocked final Fields fields, @Mocked final CommonField field, @Mocked final Field readerOperationField) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;

				field.getField();
				result = readerOperationField;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("CHECK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		final CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:check:iso15962");
		CCOperation operation = CCOperation.get(spec, null);
		operation.setId(5);
		havis.middleware.ale.base.operation.tag.Operation base = operation.getBase();
		Assert.assertNotNull(base);
		Assert.assertSame(readerOperationField, base.getField());
		Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.READ, base.getType());
		Assert.assertEquals(5, base.getId());
	}

	@Test
	public void isCompleted(@Mocked final Fields fields, @Mocked final CommonField field) throws ValidationException {
		final String fieldName = "epcBank";
		new NonStrictExpectations() {
			{
				fields.get(withEqual(fieldName));
				result = field;

				field.getName();
				result = fieldName;
			}
		};

		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("READ");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		CCOperation operation = CCOperation.get(spec, null);

		Assert.assertFalse(operation.isCompleted(null, null));
		Assert.assertTrue(operation.isCompleted(null, new FaultResult(ResultState.MISC_ERROR_TOTAL)));
		Assert.assertTrue(operation.isCompleted(null, new ReadResult()));
		Assert.assertFalse(operation.isCompleted(null, new WriteResult()));

		spec = new CCOpSpec();
		spec.setOpType("CHECK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:check:iso15962");
		operation = CCOperation.get(spec, null);

		Assert.assertTrue(operation.isCompleted(null, new ReadResult()));
		Assert.assertFalse(operation.isCompleted(null, new WriteResult()));

		spec = new CCOpSpec();
		spec.setOpType("INITIALIZE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("urn:epcglobal:ale:init:iso15962:xAF.xDF.force");
		operation = CCOperation.get(spec, null);

		Assert.assertTrue(operation.isCompleted(null, new WriteResult()));
		Assert.assertFalse(operation.isCompleted(null, new ReadResult()));

		spec = new CCOpSpec();
		spec.setOpType("ADD");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("something");
		operation = CCOperation.get(spec, null);

		Assert.assertTrue(operation.isCompleted(null, new WriteResult()));
		Assert.assertFalse(operation.isCompleted(null, new ReadResult()));

		spec = new CCOpSpec();
		spec.setOpType("WRITE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("something");
		operation = CCOperation.get(spec, null);

		Assert.assertTrue(operation.isCompleted(null, new WriteResult()));
		Assert.assertFalse(operation.isCompleted(null, new ReadResult()));

		spec = new CCOpSpec();
		spec.setOpType("DELETE");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(null);
		operation = CCOperation.get(spec, null);

		Assert.assertTrue(operation.isCompleted(null, new WriteResult()));
		Assert.assertFalse(operation.isCompleted(null, new ReadResult()));

		spec = new CCOpSpec();
		spec.setOpType("PASSWORD");
		dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("xFFFE");
		operation = CCOperation.get(spec, null);

		Assert.assertTrue(operation.isCompleted(null, new PasswordResult()));
		Assert.assertFalse(operation.isCompleted(null, new ReadResult()));

		spec = new CCOpSpec();
		spec.setOpType("KILL");
		dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("xFFFE");
		operation = CCOperation.get(spec, null);

		Assert.assertTrue(operation.isCompleted(null, new KillResult()));
		Assert.assertFalse(operation.isCompleted(null, new ReadResult()));

		spec = new CCOpSpec();
		spec.setOpType("LOCK");
		spec.setFieldspec(new ECFieldSpec(fieldName));
		spec.setDataSpec(new CCOpDataSpec());
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("LOCK");
		operation = CCOperation.get(spec, null);

		Assert.assertTrue(operation.isCompleted(null, new LockResult()));
		Assert.assertFalse(operation.isCompleted(null, new ReadResult()));
	}

	@Test
	public void apply() {
		// TODO: Operation.apply() tests
	}

	@Test
	public void get() throws ValidationException {
		// TODO: only partially covered
		final String urn = "urn:epc:tag:sgtin-96:3.0652642.102400.9";

		for (final String type : new String[] { "WRITE", "ADD" }) {
			ByRef<Result> result;
			havis.middleware.ale.base.operation.tag.Operation actual;
			CCOpSpec spec;
			CCOperation operation;

			spec = new CCOpSpec() {
				{
					setOpType(type);
					setFieldspec(new ECFieldSpec() {
						{
							setFieldname("epc");
						}
					});
					setDataSpec(new CCOpDataSpec() {
						{
							setSpecType("LITERAL");
							setData(urn);
						}
					});
				}
			};

			operation = CCOperation.get(spec, new Parameters());
			operation.setId(1);

			result = new ByRef<Result>(new ReadResult() {
				private static final long serialVersionUID = 1L;
				{
					setData(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
					setState(ResultState.SUCCESS);
				}
			});
			actual = operation.get(new Tag(new byte[] {}), result, new ArrayList<havis.middleware.ale.base.operation.tag.Operation>());
			Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.WRITE, actual.getType());
			Assert.assertEquals(new Field("epc", 1, 16, 0), actual.getField());
			Assert.assertTrue(Arrays.equals(new byte[] { 0x30, 0x00 }, Arrays.copyOf(actual.getData(), 2)));
			Assert.assertTrue(Arrays.equals(TagDecoder.getInstance().fromUrn(urn).getEpc(), Arrays.copyOfRange(actual.getData(), 2, actual.getData().length)));

			result = new ByRef<Result>(new ReadResult() {
				private static final long serialVersionUID = 1L;
				{
					setData(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
							(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
					setState(ResultState.SUCCESS);
				}
			});
			actual = operation.get(new Tag(new byte[] {}), result, new ArrayList<havis.middleware.ale.base.operation.tag.Operation>());
			Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.WRITE, actual.getType());
			Assert.assertEquals(new Field("epc", 1, 16, 0), actual.getField());
			Assert.assertTrue(Arrays.equals(new byte[] { (byte) 0x30, (byte) 0xFF }, Arrays.copyOf(actual.getData(), 2)));
			Assert.assertTrue(Arrays.equals(TagDecoder.getInstance().fromUrn(urn).getEpc(), Arrays.copyOfRange(actual.getData(), 2, actual.getData().length)));

			result = new ByRef<Result>(new ReadResult() {
				private static final long serialVersionUID = 1L;
				{
					setData(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
							(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
					setState(ResultState.SUCCESS);
				}
			});

			List<havis.middleware.ale.base.operation.tag.Operation> operations;

			operations = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();
			operations.add(new havis.middleware.ale.base.operation.tag.Operation() {
				private static final long serialVersionUID = 1L;
				{
					setId(0);
					setType(havis.middleware.ale.base.operation.tag.OperationType.WRITE);
					setField(Fields.getInstance().get(new ECFieldSpec() {
						{
							setFieldname("nsi");
						}
					}).getField());
					setData(new byte[] { 0x00, 0x00 });
				}
			});
			actual = operation.get(new Tag(new byte[] {}), result, operations);
			Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.WRITE, actual.getType());
			Assert.assertEquals(new Field("epc", 1, 16, 0), actual.getField());
			Assert.assertTrue(Arrays.equals(new byte[] { 0x30, 0x00 }, Arrays.copyOf(actual.getData(), 2)));
			Assert.assertTrue(Arrays.equals(TagDecoder.getInstance().fromUrn(urn).getEpc(), Arrays.copyOfRange(actual.getData(), 2, actual.getData().length)));

			spec = new CCOpSpec() {
				{
					setOpType(type);
					setFieldspec(new ECFieldSpec() {
						{
							setFieldname("afi");
						}
					});
					setDataSpec(new CCOpDataSpec() {
						{
							setSpecType("LITERAL");
							setData("x1");
						}
					});
				}
			};

			operation = CCOperation.get(spec, new Parameters());
			operation.setId(1);

			result = new ByRef<Result>(new ReadResult() {
				private static final long serialVersionUID = 1L;
				{
					setData(new byte[] { (byte) 0xFF, (byte) 0xFF });
					setState(ResultState.SUCCESS);
				}
			});

			operations = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();
			operations.add(new havis.middleware.ale.base.operation.tag.Operation() {
				private static final long serialVersionUID = 1L;
				{
					setId(0);
					setType(havis.middleware.ale.base.operation.tag.OperationType.WRITE);
					setField(Fields.getInstance().get(new ECFieldSpec() {
						{
							setFieldname("epc");
						}
					}).getField());
					setData(Calculator.concat(new byte[] { 0x31, 0x00 }, TagDecoder.getInstance().fromUrn(urn).getEpc()));
				}
			});
			actual = operation.get(new Tag(new byte[] {}), result, operations);
			Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.WRITE, actual.getType());
			Assert.assertEquals(new Field("afi", 1, 16, 16), actual.getField());
			Assert.assertTrue(Arrays.equals(new byte[] { 0x31, 0x01 }, actual.getData()));
		}
	}

	@Test
	public void getWriteUserBankWithVariableField() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("WRITE");
		spec.setFieldspec(new ECFieldSpec("@3.urn:oid:1.0.15961.9.11"));
		CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("171017");

		WriteOperation operation = new WriteOperation(spec, new Parameters());
		operation.setId(1);
		Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0652642.102400.9");
		tag.setResult(new HashMap<Integer, Result>());
		byte[] readData = new byte[] { (byte) 0x89, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] writeData = new byte[] { (byte) 0x89, 0x16, 0x02, (byte) 0xCA, 0x70, 0x26, 0x00, 0x00, 0x00, 0x00 };
		tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, readData));
		ByRef<Result> result = new ByRef<Result>(tag.getResult().get(Integer.valueOf(operation.getId())));
		Operation op = operation.get(tag, result, null);

		Assert.assertTrue(tag.hasItemData(3));
		ItemData itemData = tag.getItemData(3);
		Assert.assertEquals(1, itemData.getDataElements().size());

		Assert.assertEquals("urn:oid:1.0.15961.9.11", itemData.getDataElements().get(0).getKey());
		Assert.assertEquals("171017", itemData.getDataElements().get(0).getValue());

		Assert.assertNotNull(result.getValue());
		Assert.assertTrue(result.getValue() instanceof VirtualReadResult);
		Assert.assertEquals(new VirtualReadResult(ResultState.SUCCESS, writeData), result.getValue());

		Assert.assertNotNull(op);
		Assert.assertEquals(new Operation(1, OperationType.WRITE, operation.getField().getField(), writeData), op);
	}

	@Test
	public void getWriteUserBankWithVariableFieldInvalid() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("WRITE");
		spec.setFieldspec(new ECFieldSpec("@3.urn:oid:1.0.15961.9.11"));
		CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("171017X");

		WriteOperation operation = new WriteOperation(spec, new Parameters());
		operation.setId(1);
		Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0652642.102400.9");
		tag.setResult(new HashMap<Integer, Result>());
		byte[] readData = new byte[] { (byte) 0x89, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, readData));
		ByRef<Result> result = new ByRef<Result>(tag.getResult().get(Integer.valueOf(operation.getId())));
		Operation op = operation.get(tag, result, null);

		Assert.assertTrue(tag.hasItemData(3));
		ItemData itemData = tag.getItemData(3);
		Assert.assertEquals(0, itemData.getDataElements().size());

		Assert.assertNotNull(result.getValue());
		Assert.assertTrue(result.getValue() instanceof FaultResult);
		Assert.assertEquals(ResultState.OP_NOT_POSSIBLE_ERROR, result.getValue().getState());

		Assert.assertNull(op);
	}
	
	@Test
	public void getWriteEpcBankWithVariableField() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("WRITE");
		spec.setFieldspec(new ECFieldSpec("@1.urn:oid:1.0.15961.9.11"));
		CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("171017");

		WriteOperation operation = new WriteOperation(spec, new Parameters());
		operation.setId(1);
		byte[] readData = new byte[] { 0x01, 0x02 /* CRC */, 0x25, 0x01 /* PC with toggle bit set and AFI */, (byte) 0x89, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] intermediateReadData = new byte[] { 0x00, 0x00, 0x25, 0x01, (byte) 0x89, 0x16, 0x02, (byte) 0xCA, 0x70, 0x26, 0x00, 0x00 };
		byte[] writeData = new byte[] { 0x25, 0x01, (byte) 0x89, 0x16, 0x02, (byte) 0xCA, 0x70, 0x26, 0x00, 0x00 };
		Tag tag = new Tag(readData);
		tag.setResult(new HashMap<Integer, Result>());
		tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, readData));
		ByRef<Result> result = new ByRef<Result>(tag.getResult().get(Integer.valueOf(operation.getId())));
		Operation op = operation.get(tag, result, null);

		Assert.assertNotNull(result.getValue());
		Assert.assertTrue(result.getValue() instanceof VirtualReadResult);
		Assert.assertEquals(new VirtualReadResult(ResultState.SUCCESS, intermediateReadData), result.getValue());

		Assert.assertNotNull(op);
		Assert.assertEquals(new Operation(1, OperationType.WRITE, new Field("epcBank", 1, 16, 80), writeData), op);
	}

	@Test
	public void getWriteEpcBankWithVariableFieldIncreasePc() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("WRITE");
		spec.setFieldspec(new ECFieldSpec("@1.urn:oid:1.0.15961.9.11"));
		CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("171017");

		WriteOperation operation = new WriteOperation(spec, new Parameters());
		operation.setId(1);
		byte[] readData = new byte[] { 0x01, 0x02 /* CRC */, 0x15, 0x01 /* PC with toggle bit set and AFI */, (byte) 0x89, 0x00 };
		byte[] intermediateReadData = new byte[] { 0x00, 0x00, 0x25, 0x01, (byte) 0x89, 0x16, 0x02, (byte) 0xCA, 0x70, 0x26, 0x00 };
		byte[] writeData = new byte[] { 0x25, 0x01, (byte) 0x89, 0x16, 0x02, (byte) 0xCA, 0x70, 0x26, 0x00 };
		Tag tag = new Tag(readData);
		tag.setResult(new HashMap<Integer, Result>());
		tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, readData));
		ByRef<Result> result = new ByRef<Result>(tag.getResult().get(Integer.valueOf(operation.getId())));
		Operation op = operation.get(tag, result, null);

		Assert.assertNotNull(result.getValue());
		Assert.assertTrue(result.getValue() instanceof VirtualReadResult);
		Assert.assertEquals(new VirtualReadResult(ResultState.SUCCESS, intermediateReadData), result.getValue());

		Assert.assertNotNull(op);
		Assert.assertEquals(new Operation(1, OperationType.WRITE, new Field("epcBank", 1, 16, 72), writeData), op);
	}

	@Test
	public void getWriteEpcBankWithVariableFieldDecreasePc() throws ValidationException {
		CCOpSpec spec = new CCOpSpec();
		spec.setOpType("WRITE");
		spec.setFieldspec(new ECFieldSpec("@1.urn:oid:1.0.15961.9.11"));
		CCOpDataSpec dataSpec = new CCOpDataSpec();
		spec.setDataSpec(dataSpec);
		spec.getDataSpec().setSpecType("LITERAL");
		spec.getDataSpec().setData("171017");

		WriteOperation operation = new WriteOperation(spec, new Parameters());
		operation.setId(1);
		byte[] readData = new byte[] { 0x01, 0x02 /* CRC */, 0x35, 0x01 /* PC with toggle bit set and AFI */, (byte) 0x89, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] intermediateReadData = new byte[] { 0x00, 0x00, 0x25, 0x01, (byte) 0x89, 0x16, 0x02, (byte) 0xCA, 0x70, 0x26, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] writeData = new byte[] { 0x25, 0x01, (byte) 0x89, 0x16, 0x02, (byte) 0xCA, 0x70, 0x26, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		Tag tag = new Tag(readData);
		tag.setResult(new HashMap<Integer, Result>());
		tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, readData));
		ByRef<Result> result = new ByRef<Result>(tag.getResult().get(Integer.valueOf(operation.getId())));
		Operation op = operation.get(tag, result, null);

		Assert.assertNotNull(result.getValue());
		Assert.assertTrue(result.getValue() instanceof VirtualReadResult);
		Assert.assertEquals(new VirtualReadResult(ResultState.SUCCESS, intermediateReadData), result.getValue());

		Assert.assertNotNull(op);
		Assert.assertEquals(new Operation(1, OperationType.WRITE, new Field("epcBank", 1, 16, 112), writeData), op);
	}

	@Test
	public void rehash() {
		// TODO: Operation.rehash() tests
	}

	@Test
	public void getReport() {
		// TODO: Operation.getReport() tests
	}

	@Test
	public void equals() {
		// TODO: Operation.equals() tests
	}

	@Test
	public void hashCodeTest() {
		// TODO: Operation.hashCode() tests
	}

	@Test
	public void dispose() {
		// TODO: Operation.dispose() tests
	}
}