package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.CustomResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Common.DataType;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.cc.CCOpReport;
import havis.middleware.ale.service.cc.CCOpSpec;

import java.util.List;

public class CustomOperation extends CCOperation {

	private CommonField virtualField = new CommonField(FieldDatatype.BITS, FieldFormat.HEX);

	protected CustomOperation(CCOpSpec spec, Parameters parameters) throws ValidationException {
		super(spec);

		try {
			if (spec.getFieldspec() != null) {
				throw new ValidationException("Field must be omitted in custom operation");
			}
			if (spec.getDataSpec() != null) {
				if (DataType.CACHE.toString().equals(spec.getDataSpec().getSpecType())) {
					throw new ValidationException("CACHE type not allowed for custom operation");
				}
				if (DataType.RANDOM.toString().equals(spec.getDataSpec().getSpecType())) {
					throw new ValidationException("RANDOM type not allowed for custom operation");
				}
			}

			data = new Common(spec.getDataSpec(), parameters, virtualField);
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	@Override
	Operation get(Tag tag, ByRef<Result> result, List<Operation> operations) {
		Bytes bytes = data.getBytes(tag);
		return new Operation(id, OperationType.CUSTOM, bytes.getValue(), bytes.getLength());
	}

	@Override
	protected boolean isCompleted(Result result) {
		return result instanceof CustomResult;
	}

	@Override
	CCOpReport getReport(Tag tag, Result result) {
		String data = null;
		ResultState resultState = result != null ? result.getState() : null;

		if (resultState == ResultState.SUCCESS && result instanceof CustomResult) {
			data = Fields.toString(virtualField, ((CustomResult) result).getData());
		}

		return getReport(tag, resultState, data);
	}
}