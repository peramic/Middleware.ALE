package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.po.OID;
import havis.middleware.ale.core.ISODecoder;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.VariableField;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.CCOpReport;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.tdt.ItemData;

import java.util.List;
import java.util.Map.Entry;

public class ReadOperation extends CCOperation {

	protected ReadOperation(CCOpSpec spec) throws ValidationException {
		super(spec);

		try {
			field = Fields.getInstance().get(spec.getFieldspec());
			field.inc();
			if (spec.getDataSpec() != null) {
				throw new ValidationException("Data must be omitted by read operation");
			}
			if (field.getBase() instanceof VariableField && ((VariableField) field.getBase()).getOID().isPattern()) {
				throw new ValidationException("A read operation does not allow variable pattern fieldnames");
			}
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	@Override
	Operation get(Tag tag, ByRef<Result> result, List<Operation> operations) {
		return new Operation(id, OperationType.READ, field.getField());
	}

	@Override
	protected boolean isCompleted(Result result) {
		return result instanceof ReadResult;
	}

	@Override
	CCOpReport getReport(Tag tag, Result result) {
		String data = null;
		ResultState resultState = result != null ? result.getState() : null;

		if ((result instanceof ReadResult) && (resultState == ResultState.SUCCESS) && (((ReadResult) result).getData() != null)) {
			if (field.getBase() instanceof VariableField) {
				data = getVariableData(tag, (VariableField) field.getBase(), ((ReadResult) result).getData());
				if (data == null) {
					resultState = ResultState.FIELD_NOT_FOUND_ERROR;
				}
			} else {
				data = getData(field, ((ReadResult) result).getData());
			}
		}
		return getReport(tag, resultState, data);
	}

	private String getVariableData(Tag tag, VariableField field, byte[] data) {
		// always decode current data here
		tag.resetItemData(field.getBank());
		tag.decodeItemData(field.getBank(), data, ISODecoder.getInstance());
		ItemData itemData = tag.getItemData(field.getBank());

		if (itemData != null) {
			OID oid = field.getOID();
			for (Entry<String, String> entry : itemData.getDataElements()) {
				if (oid.matches(entry.getKey())) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	private String getData(CommonField field, byte[] data) {
		try {
			return Fields.toString(field, data);
		} catch (Exception e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Core.Cycle.CommandCycle.Name, Exits.Core.Cycle.CommandCycle.Report,
					"Failed to convert result data: " + e.getMessage(), e);
		}
		return null;
	}
}