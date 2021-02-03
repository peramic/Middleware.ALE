package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.KillResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.utils.data.Calculator;

import java.util.List;

public class KillOperation extends CCOperation {

	protected KillOperation(CCOpSpec spec, Parameters parameters) throws ValidationException {
		super(spec);

		try {
			if (spec.getFieldspec() != null) {
				throw new ValidationException("Field must be omitted in kill operation");
			}
			data = new Common(spec.getDataSpec(), parameters, new CommonField(FieldDatatype.UINT, FieldFormat.HEX));
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	@Override
	Operation get(Tag tag, ByRef<Result> result, List<Operation> operations) {
		ResultState state = null;

		Bytes bytes = data.getBytes(tag);
		if ((state = bytes.getResultState()) == ResultState.SUCCESS) {
			try {
				CommonField _field = Fields.getInstance().get(new ECFieldSpec("killPwd"));
				if (bytes.getLength() > _field.getLength()) {
					state = ResultState.PASSWORD_ERROR;
				} else {
					int length = _field.getLength() > 0 ? _field.getLength() : bytes.getLength();
					byte[] killData = Calculator.shift(bytes.getValue(), bytes.getLength(), bytes.getLength() - length);
					return new Operation(id, OperationType.KILL, killData);
				}
			} catch (ValidationException e) {
			}
		}
		if (state instanceof ResultState) {
			result.setValue(new FaultResult(state));
		}
		return null;
	}

	@Override
	protected boolean isCompleted(Result result) {
		return result instanceof KillResult;
	}
}