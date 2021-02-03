package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.operation.tag.result.WriteResult;
import havis.middleware.ale.base.po.OID;
import havis.middleware.ale.core.ISODecoder;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.VariableField;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.tdt.EncoderException;
import havis.middleware.tdt.ItemData;
import havis.middleware.utils.data.Calculator;

import java.util.ArrayList;
import java.util.List;

public class DeleteOperation extends CCOperation {

	protected DeleteOperation(CCOpSpec spec) throws ValidationException {
		super(spec);

		try {
			field = Fields.getInstance().get(spec.getFieldspec());
			field.inc();
			if (field.getBase() instanceof VariableField) {
				if (field.getOffset() > 0) {
					throw new ValidationException("Invalid field spec, offset is not allowed when using a delete operation on variable fields");
				}
				if (((VariableField) field.getBase()).getOID().isPattern()) {
					throw new ValidationException("A delete operation does not allow variable pattern fieldnames");
				}
			}
			advance = field.isAdvanced() || (field.getLength() == 0);
			if (spec.getDataSpec() != null) {
				throw new ValidationException("Data spec must be omitted in delete operation");
			}
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	@Override
	Operation get(Tag tag, ByRef<Result> result, List<Operation> operations) {
		ResultState state = null;
		x: {
			if (field.getBase() instanceof VariableField) {
				if (!(result.getValue() instanceof ReadResult) || result.getValue().getState() != ResultState.SUCCESS) {
					state = ResultState.FIELD_NOT_FOUND_ERROR;
					break x;
				}

				byte[] readData = ((ReadResult) result.getValue()).getData();

				if (!tag.hasItemData(field.getBank())) {
					tag.decodeItemData(field.getBank(), readData, ISODecoder.getInstance());
				}

				ItemData itemData = tag.getItemData(field.getBank());
				if (itemData != null) {
					OID oid = ((VariableField) field.getBase()).getOID();
					List<Integer> entriesToRemove = new ArrayList<>();
					for (int i = 0; i < itemData.getDataElements().size(); i++) {
						if (oid.matches(itemData.getDataElements().get(i).getKey())) {
							entriesToRemove.add(Integer.valueOf(i));
						}
					}

					if (entriesToRemove.size() == 0) {
						state = ResultState.FIELD_NOT_FOUND_ERROR;
						break x;
					}

					for (Integer remove : entriesToRemove) {
						itemData.getDataElements().remove(remove.intValue());	
					}

					Operation op = null;
					try {
						op = createWriteOperation(readData, itemData.encode(), result);
					} catch (EncoderException e) {
						// TODO: log error?
					}
					if (op != null) {
						return op;
					}
					state = ResultState.OP_NOT_POSSIBLE_ERROR;
					break x;
				} else {
					state = ResultState.FIELD_NOT_FOUND_ERROR;
					break x;
				}
			} else {
				if (advance) {
					if ((state = result.getValue().getState()) == ResultState.SUCCESS) {
						byte[] readData;
						if ((result.getValue() instanceof ReadResult) && ((readData = ((ReadResult) result.getValue()).getData()) != null)) {
							apply(readData, tag, operations);
							int length = field.getLength() > 0 ? field.getLength() : readData.length * 8;
							byte[] writeData = Calculator.apply(readData, new byte[Calculator.size(length)], field.getOffset() % 16, length);
							return new Operation(id, OperationType.WRITE, field.getField(), writeData);
						} else {
							state = ResultState.MISC_ERROR_TOTAL;
						}
					}
				} else {
					return new Operation(id, OperationType.WRITE, field.getField(), new byte[Calculator.size(field.getOffset() % 16 + field.getLength())]);
				}
			}
		}
		if (state instanceof ResultState) {
			result.setValue(new FaultResult(state));
		}
		return null;
	}

	@Override
	protected boolean isCompleted(Result result) {
		return result instanceof WriteResult;
	}
}