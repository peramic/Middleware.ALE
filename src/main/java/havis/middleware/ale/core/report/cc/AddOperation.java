package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.cc.CCOpSpec;

import java.util.List;

public class AddOperation extends WriteOperation {

	protected AddOperation(CCOpSpec spec, Parameters parameters) throws ValidationException {
		super(spec, parameters);
	}

	@Override
	Operation get(Tag tag, ByRef<Result> result, List<Operation> operations) {
		return super.getOperation(tag, result, operations, true);
	}
}