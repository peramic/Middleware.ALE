package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.operation.tag.result.WriteResult;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Common.DataType;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.cc.CCOpSpec;

import java.util.List;
import java.util.regex.Matcher;

public class InitializeOperation extends CCOperation {

	private byte afi;
	private String dsfid;
	private boolean force;

	protected InitializeOperation(CCOpSpec spec, Parameters parameters) throws ValidationException {
		super(spec);

		try {
			field = Fields.getInstance().get(spec.getFieldspec());
			field.inc();
			advance = true;
			if (field.getOffset() > 0) {
				throw new ValidationException("Invalid field spec, offset is not allowed for initialize operation");
			}
			data = new Common(spec.getDataSpec(), parameters, null, RawData.EMPTY);
			if (data.getType() == DataType.LITERAL) {
				switch (field.getName()) {
				case "epcBank": {
					final Matcher match = URN_EPC.matcher(data.getValue());
					if (match.matches() && ("init".equals(match.group("operation")))) {
						afi = (byte) Short.parseShort(match.group("afi"), 16);
						dsfid = match.group("dsfid");
						force = match.group("force") != null;
						break;
					} else {
						throw new ValidationException("Invalid data spec value '" + data.getValue() + "' by initialize operation of memory bank '"
								+ data.getValue() + "'");
					}
				}
				case "userBank": {
					final Matcher match = URN_USR.matcher(data.getValue());
					if ((match.matches()) && ("init".equals(match.group("operation")))) {
						dsfid = match.group("dsfid");
						force = match.group("force") != null;
						break;
					} else {
						throw new ValidationException("Invalid data spec value '" + data.getValue() + "' by initialize operation of memory bank '"
								+ data.getValue() + "'");
					}
				}
				default:
					throw new ValidationException("No initialize operation defined for field '" + field.getName() + "'");
				}
			} else {
				throw new ValidationException("Data type must be literal by initialize operation");
			}
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	@Override
	Operation get(Tag tag, ByRef<Result> result, List<Operation> operations) {
		ResultState resultState = result.getValue() instanceof ReadResult ? result.getValue().getState() : null;

		if (resultState == ResultState.SUCCESS) {
			byte[] readData = ((ReadResult) result.getValue()).getData();
			Operation operation = null;
			switch (field.getName()) {
			case "epcBank":
				if (!force && epcBankHasData(readData)) {
					// omit initialization because of existing data
					break;
				}
				operation = createWriteOperation(readData, Byte.valueOf(this.afi), encode(), result);
				break;
			case "userBank":
				if (!force && userBankHasData(readData)) {
					// omit initialization because of existing data
					break;
				}
				operation = createWriteOperation(readData, encode(), result);
				break;
			}

			if (operation != null) {
				tag.resetItemData(field.getBank());
				return operation;
			}
		}

		result.setValue(new FaultResult(ResultState.OP_NOT_POSSIBLE_ERROR));
		return null;
	}

	private byte[] encode() {
		// TODO: do we want to prevent writing DSFID 03 if AFI = A1-A8?

		// if DSFID is specified, write it, otherwise don't write any data
		if (this.dsfid != null)
			return new byte[] { (byte) Short.parseShort(this.dsfid, 16) };
		return new byte[0];
	}

	private boolean userBankHasData(byte[] data) {
		// if bits 00h to 07h are non-zero, we have data
		return data != null && data.length > 0 && data[0] != 0x00;
	}

	private boolean epcBankHasData(byte[] data) {
		// if bit 17h is a one and bits 18h to 27h are non-zero, we have data
		return data != null && data.length >= 5 && (data[2] & 0x01) == 1 && data[3] != 0x00 && data[4] != 0x00;
	}

	@Override
	protected boolean isCompleted(Result result) {
		return result instanceof WriteResult;
	}
}