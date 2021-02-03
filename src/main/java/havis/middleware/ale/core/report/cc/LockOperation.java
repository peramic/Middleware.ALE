package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.LockType;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.LockResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Common.DataType;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.cc.CCOpSpec;

import java.util.List;

public class LockOperation extends CCOperation {

	protected LockOperation(CCOpSpec spec, Parameters parameters) throws ValidationException {
		super(spec);

		try {
			field = Fields.getInstance().get(spec.getFieldspec());
			field.inc();
			if (spec.getDataSpec().getData() != null) {
				LockType lockType;
				try {
					lockType = LockType.valueOf(spec.getDataSpec().getData());
				} catch (IllegalArgumentException e) {
					throw new ValidationException("Unkown synchronized operation defined: '" + spec.getDataSpec().getData() + "'");
				}
				data = new Common(spec.getDataSpec(), parameters, null, new Bytes(new byte[] { (byte) lockType.ordinal() }));
				if (data.getType() != DataType.LITERAL) {
					throw new ValidationException("Data type must be literal by synchronized  operation");
				}
			} else {
				throw new ValidationException("No synchronized operation defined");
			}
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
			return new Operation(id, OperationType.LOCK, field.getField(), bytes.getValue());
		}
		if (state instanceof ResultState) {
			result.setValue(new FaultResult(state));
		}
		return null;
	}

	@Override
	protected boolean isCompleted(Result result) {
		return result instanceof LockResult;
	}
}