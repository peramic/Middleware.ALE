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
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.field.VariableField;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.cc.CCOpReport;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.tdt.EncoderException;
import havis.middleware.tdt.ItemData;
import havis.middleware.tdt.SimpleEntry;
import havis.middleware.utils.data.Calculator;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

public class WriteOperation extends CCOperation {

	protected WriteOperation(CCOpSpec spec, Parameters parameters) throws ValidationException {
		super(spec);

		try {
			field = Fields.getInstance().get(spec.getFieldspec());
			field.inc();
			if (field.getBase() instanceof VariableField) {
				if (field.getOffset() > 0) {
					throw new ValidationException("Invalid field spec, offset is not allowed when using a add/write operation on variable fields");
				}
				if (((VariableField) field.getBase()).getOID().isPattern()) {
					throw new ValidationException("Invalid field spec, add/write operations do not allow variable pattern fieldnames");
				}
			}
			advance = field.isAdvanced();
			data = new Common(spec.getDataSpec(), parameters, field);
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	@Override
	Operation get(Tag tag, ByRef<Result> result, List<Operation> operations) {
		return getOperation(tag, result, operations, false);
	}

	protected Operation getOperation(Tag tag, ByRef<Result> result, List<Operation> operations, boolean failOnExistentField) {
		if (field.getBase() instanceof VariableField) {
			return getOperationForVariableField(tag, result, failOnExistentField);
		} else {

			return getOperationForFixedField(tag, result, operations);
		}
	}

	private Operation getOperationForFixedField(Tag tag, ByRef<Result> result, List<Operation> operations) {
		ResultState state = null;
		Bytes bytes;
		this.tagData.put(Integer.valueOf(tag.getId()), bytes = data.getBytes(tag));
		if ((state = bytes.getResultState()) == ResultState.SUCCESS) {
			if ((field.getLength() > 0) && (bytes.getLength() > field.getLength())) {
				state = ResultState.OUT_OF_RANGE_ERROR;
			} else {
				if (advance) {
					if ((state = result.getValue().getState()) == ResultState.SUCCESS) {
						byte[] readData;
						if ((result.getValue() instanceof ReadResult) && ((readData = ((ReadResult) result.getValue()).getData()) != null)) {
							int length = field.getLength() > 0 ? field.getLength() : field.isEpc() ? bytes.getLength() : readData.length * 8;
							if (field.getLength() == 0 && bytes.getLength() > length) {
								state = ResultState.OUT_OF_RANGE_ERROR;
							} else {
								apply(readData, tag, operations);
								byte[] writeData;
								if (field.isEpc()) {
									writeData = Calculator.shift(bytes.getValue(), bytes.getLength(), -16);
									writeData = Calculator.apply(writeData, readData, 0, 16);
									writeData = Calculator.apply(writeData, new byte[] { (byte) (Calculator.size(length, 16) << 3) }, 0, 5);
									writeData[0] = (byte) (writeData[0] & 0xFE);
								} else {
									if (field.getFieldDatatype() != FieldDatatype.BITS) {
										writeData = Calculator.shift(bytes.getValue(), bytes.getLength(), bytes.getLength() - length);
										writeData = Calculator.apply(readData, writeData, field.getOffset() % 16, length);
									} else {
										writeData = Calculator.apply(readData, bytes.getValue(), field.getOffset() % 16, bytes.getLength());
									}
								}
								return new Operation(id, OperationType.WRITE, field.getField(), writeData);
							}
						} else {
							state = ResultState.MISC_ERROR_TOTAL;
						}
					}
				} else {
					byte[] writeData;
					if (bytes.getLength() < field.getLength()) {
						if (field.getFieldDatatype() == FieldDatatype.BITS) {
							writeData = Calculator.apply(new byte[Calculator.size(field.getLength())], bytes.getValue(), 0, bytes.getLength());
						} else {
							writeData = Arrays.copyOf(Calculator.shift(bytes.getValue(), bytes.getLength(), bytes.getLength() - field.getLength()),
									Calculator.size(field.getLength()));
						}
					} else {
						writeData = bytes.getValue();
					}
					return new Operation(id, OperationType.WRITE, field.getField(), writeData);
				}
			}
		}

		if (state instanceof ResultState) {
			result.setValue(new FaultResult(state));
		}
		return null;
	}

	private Operation getOperationForVariableField(Tag tag, ByRef<Result> result, boolean failOnExistentField) {
		Characters characters;
		this.tagData.put(Integer.valueOf(tag.getId()), characters = data.getCharacters(tag));

		if (characters.getResultState() != ResultState.SUCCESS) {
			result.setValue(new FaultResult(characters.getResultState()));
			return null;
		}
		if (!(result.getValue() instanceof ReadResult) || result.getValue().getState() != ResultState.SUCCESS) {
			result.setValue(new FaultResult(ResultState.FIELD_NOT_FOUND_ERROR));
			return null;
		}

		byte[] readData = ((ReadResult) result.getValue()).getData();

		if (!tag.hasItemData(field.getBank())) {
			tag.decodeItemData(field.getBank(), readData, ISODecoder.getInstance());
		}

		ItemData itemData = tag.getItemData(field.getBank());
		if (itemData != null) {

			// create a copy to reset if encoding fails
			ItemData previous = itemData.clone();

			boolean found = false;
			OID oid = ((VariableField) field.getBase()).getOID();
			for (Entry<String, String> entry : itemData.getDataElements()) {
				if (oid.matches(entry.getKey())) {
					if (failOnExistentField) {
						result.setValue(new FaultResult(ResultState.FIELD_EXISTS_ERROR));
						return null;
					} else {
						found = true;
						entry.setValue(characters.getValue());
						break;
					}
				}
			}

			if (!found) {
				// add if not found
				itemData.getDataElements().add(new SimpleEntry<String, String>(oid.getOid(), characters.getValue()));
			}

			byte[] encodedData = null;
			try {
				encodedData = itemData.encode();
			} catch (EncoderException e) {
				// TODO log error?
			}
			if (encodedData == null) {
				// encoding failed, data is invalid, revert instance
				itemData.setDataElements(previous.getDataElements());
			}

			Operation op = createWriteOperation(readData, encodedData, result);
			if (op != null) {
				return op;
			}
			result.setValue(new FaultResult(ResultState.OP_NOT_POSSIBLE_ERROR));
			return null;
		} else {
			result.setValue(new FaultResult(ResultState.FIELD_NOT_FOUND_ERROR));
			return null;
		}
	}

	@Override
	protected boolean isCompleted(Result result) {
		return result instanceof WriteResult;
	}

	@Override
	CCOpReport getReport(Tag tag, Result result) {
		String data = null;
		ResultState resultState = result != null ? result.getState() : null;

		if (resultState == ResultState.SUCCESS) {
			boolean variableField = field.getBase() instanceof VariableField;
			RawData rawData = this.tagData.get(Integer.valueOf(tag.getId()));

			if (!variableField && rawData instanceof Bytes) {
				data = Fields.toString(field.getFieldDatatype(), field.getFieldFormat(), (Bytes) rawData);
			} else if (variableField && rawData instanceof Characters) {
				data = ((Characters) rawData).getValue();
			}
		}

		this.tagData.remove(Integer.valueOf(tag.getId()));
		return getReport(tag, resultState, data);
	}
}