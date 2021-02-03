package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.ISODecoder;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Common.DataType;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.cc.CCOpReport;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.tdt.ItemData;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

public class CheckOperation extends CCOperation {

	protected CheckOperation(CCOpSpec spec, Parameters parameters) throws ValidationException {
		super(spec);

		try {
			field = Fields.getInstance().get(spec.getFieldspec());
			field.inc();
			advance = true;
			data = new Common(spec.getDataSpec(), parameters, null, RawData.EMPTY);
			if (data.getType() == DataType.LITERAL) {
				Matcher match = URN.matcher(data.getValue());
				if (match.matches() && ("check".equals(match.group("operation")))) {
					switch (field.getName()) {
					case "epcBank":
					case "userBank":
						break;
					default:
						throw new ValidationException("No check operation defined for field '" + field.getName() + "'");
					}
				} else {
					throw new ValidationException("Invalid check operation '" + data.getValue() + "'");
				}
			} else {
				throw new ValidationException("Data type must be literal for check operation");
			}
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	@Override
	Operation get(Tag tag, ByRef<Result> result, List<Operation> operations) {
		// no additional operation required
		return null;
	}

	@Override
	protected boolean isCompleted(Result result) {
		return result instanceof ReadResult;
	}

	@Override
	CCOpReport getReport(Tag tag, Result result) {
		ResultState resultState = ResultState.MEMORY_CHECK_ERROR;

		if ((result instanceof ReadResult) && (result.getState() == ResultState.SUCCESS) && (((ReadResult) result).getData() != null)) {
			byte[] readData = ((ReadResult) result).getData();
			
			if (field.getBank() == 1 && checkToggleBitAndAfi(readData) || field.getBank() == 3) {
				if (!tag.hasItemData(field.getBank())) {
					tag.decodeItemData(field.getBank(), readData, ISODecoder.getInstance());
				}

				ItemData itemData = tag.getItemData(field.getBank());
				if (itemData != null) {
					boolean duplicate = false;
					Set<String> entries = new HashSet<>();
					for (Entry<String, String> entry : itemData.getDataElements()) {
						if (entries.contains(entry.getKey())) {
							duplicate = true;
							break;
						}
						entries.add(entry.getKey());
					}

					if (!duplicate) {
						resultState = ResultState.SUCCESS;
					}
				}
			}
		}
		return getReport(tag, resultState, null);
	}

	private boolean checkToggleBitAndAfi(byte[] data) {
		return data != null && data.length >= 4 && (data[2] & 0x01) == 1 && data[3] != 0x00;
	}
}