package havis.middleware.ale.core.report.cc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Field;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.operation.tag.result.VirtualReadResult;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.service.cc.CCOpReport;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.utils.data.Calculator;
import havis.middleware.utils.data.Converter;

/**
 * This class instance of used to makes the {@link CCOpSpec} usable for the ALE
 * implementation as specified in ALE 1.1.1 (9.3.4). It validates the operation
 * itself, the contains field specification and data.
 */
public abstract class CCOperation {

	final static String ISO = "^urn:epcglobal:ale:(?<operation>(check|init)):iso15962";
	static Pattern URN = Pattern.compile(ISO);
	static Pattern URN_EPC = Pattern.compile(ISO + ":x(?<afi>[0-9A-F]{2})(.x(?<dsfid>[0-9A-F]{2,}))?(.(?<force>force))?$");
	static Pattern URN_USR = Pattern.compile(ISO + ":(x(?<dsfid>[0-9A-F]{2,}))?(.(?<force>force))?$");

	private int hashCode = -1;

	protected CCOpSpec spec;
	protected CommonField field;
	protected Common data;

	protected Map<Integer, RawData> tagData = new HashMap<>();

	/**
	 * Retrieves if this operation needs data in advance
	 */
	protected boolean advance;

	/**
	 * Retrieves the Id
	 */
	protected int id;

	/**
	 * Retrieves the base reader tag operation
	 */
	Operation getBase() {
		return new Operation(id, OperationType.READ, field.getField());
	}

	protected CCOperation(CCOpSpec spec) {
		this.spec = spec;
	}

	/**
	 * Gets a new instance. Keeps the specification. Parses the operation type
	 * and validates the contains field specification and data
	 * 
	 * @param spec
	 *            The operation specification
	 * @param parameters
	 *            The parameters
	 * @throws ValidationException
	 *             If operation type is not specified or unknown or if field
	 *             specification or data not valid for this kind of operation.
	 */
	public static CCOperation get(CCOpSpec spec, Parameters parameters) throws ValidationException {
		if ((spec.getOpType() == null) || spec.getOpType().isEmpty()) {
			throw new ValidationException("Operation type could not be null or empty");
		} else {
			Fields.lock();
			try {
				switch (spec.getOpType()) {
				case "READ":
					return new ReadOperation(spec);
				case "CHECK":
					return new CheckOperation(spec, parameters);
				case "INITIALIZE":
					return new InitializeOperation(spec, parameters);
				case "ADD":
					return new AddOperation(spec, parameters);
				case "WRITE":
					return new WriteOperation(spec, parameters);
				case "DELETE":
					return new DeleteOperation(spec);
				case "PASSWORD":
					return new PasswordOperation(spec, parameters);
				case "KILL":
					return new KillOperation(spec, parameters);
				case "LOCK":
					return new LockOperation(spec, parameters);
				case "CUSTOM":
					return new CustomOperation(spec, parameters);
				default:
					throw new ValidationException("Unknown operation type '" + spec.getOpType() + "'");
				}
			} catch (ValidationException e) {
				e.setReason("Operation '" + spec.getOpName() + "' is invalid. " + e.getReason());
				throw e;
			} finally {
				Fields.unlock();
			}
		}
	}

	/**
	 * @return the field of this operation
	 */
	CommonField getField() {
		return field;
	}

	/**
	 * @return the spec
	 */
	CCOpSpec getSpec() {
		return spec;
	}

	/**
	 * @return the data
	 */
	Common getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data
	 */
	void setData(Common data) {
		this.data = data;
	}

	/**
	 * @return whether this operation is advanced
	 */
	public boolean isAdvanced() {
		return advance;
	}

	/**
	 * Gets the id of operation
	 * 
	 * @return The operation id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the id of operation
	 * 
	 * @param id
	 *            The operation id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Retrieve the operation name
	 * 
	 * @return The operation name
	 */
	String getName() {
		return spec.getOpType();
	}

	abstract protected boolean isCompleted(Result result);

	/**
	 * Retrieves the real operation type
	 * 
	 * @param tag
	 *            The tag
	 * @param result
	 *            The result
	 * @return True if result type is as expected
	 */
	boolean isCompleted(Tag tag, Result result) {
		if (result == null)
			return false;

		if (result instanceof FaultResult)
			return true;
		else
			return isCompleted(result);
	}

	void apply(byte[] data, Tag tag, Iterable<Operation> operations) {
		for (Operation operation : operations) {
			if (operation.getId() < id) {
				if (operation.getType() == OperationType.WRITE) {
					Field field;
					if ((field = operation.getField()).getBank() == this.field.getBank()) {
						if (((field.getLength() == 0) && (this.field.getLength() == 0))
								|| ((field.getLength() == 0) && (this.field.getOffset() + this.field.getLength() > field.getOffset() / 16 * 16))
								|| ((this.field.getLength() == 0) && (field.getOffset() + field.getLength() > this.field.getOffset() / 16 * 16))
								|| ((field.getOffset() / 16 * 16 >= this.field.getOffset() / 16 * 16) && (field.getOffset() / 16 * 16 < this.field.getLength()
										+ this.field.getOffset()))
								|| ((this.field.getOffset() / 16 * 16 >= field.getOffset() / 16 * 16) && (this.field.getOffset() / 16 * 16 < field.getLength()
										+ field.getOffset()))
								|| ((field.getOffset() / 16 * 16 < this.field.getOffset()) && (field.getOffset() + field.getLength() > (this.field.getOffset() + this.field
										.getLength()) / 16 * 16))) {
							int offset = (this.field.getField().getOffset() - field.getOffset()) / 8;
							int length = operation.getData().length;
							Calculator.apply(data,
									Converter.toList(operation.getData()).subList((offset > 0 ? offset : 0), (offset > 0 ? offset : 0) + length),
									offset < 0 ? -offset : 0, data.length);
						}
					}
				}
			} else {
				break;
			}
		}
	}

	abstract Operation get(Tag tag, ByRef<Result> result, List<Operation> operations);

	protected CCOpReport getReport(Tag tag, ResultState resultState, String data) {
		CCOpReport report = new CCOpReport();

		report.setData(data);
		report.setOpStatus(resultState == null ? null : resultState.name());
		report.setOpName(spec.getOpName());

		return report;
	}

	/**
	 * Returns the tag report
	 * 
	 * @param tag
	 *            The tag
	 * @param result
	 *            The tag result
	 * @return The tag report
	 * @throws ValidationException
	 */
	CCOpReport getReport(Tag tag, Result result) {
		return getReport(tag, result != null ? result.getState() : null, null);
	}

	/**
	 * Create a write operation for the specified encoded data while not
	 * changing the AFI
	 * 
	 * @param readData
	 *            the data previously read
	 * @param encodedData
	 *            the encoded data
	 * @param result
	 *            the original read result
	 * @return the write operation, or null if not all conditions are met
	 */
	protected Operation createWriteOperation(byte[] readData, byte[] encodedData, ByRef<Result> result) {
		return createWriteOperation(readData, null, encodedData, result);
	}

	/**
	 * Create a write operation for the specified encoded data
	 * 
	 * @param readData
	 *            the data previously read
	 * @param afi
	 *            the AFI byte or null to not change the current AFI
	 * @param encodedData
	 *            the encoded data
	 * @param result
	 *            the original read result
	 * @return the write operation, or null if not all conditions are met
	 */
	protected Operation createWriteOperation(byte[] readData, Byte afi, byte[] encodedData, ByRef<Result> result) {
		if (encodedData != null && readData != null) {
			// if EPC, length of encoded data + CRC + PC <= length of read data
			if (field.getBank() == 1) {
				byte[] data = new byte[Math.max(readData.length - 2 /** without CRC **/, encodedData.length + 2 /** add PC **/)];
				System.arraycopy(readData, 2, data, 0, readData.length - 2);
				
				// check length in PC
				int currentBitSize = ((data[0] & 0xFF) >> 3) * 16;
				int updatedBitSize = ((int) Math.ceil(encodedData.length * 8 / 16.0)) * 16;
				if (updatedBitSize != currentBitSize) {
					// fix length in PC
					int newPC = (updatedBitSize / 16) << 3;
					// copy all other bits
					for (int i = 5; i <= 7; i++) {
						if (((data[0] & 0xFF) & (1 << (7 - i))) != 0) {
							newPC = newPC |= 1 << (7 - i);
						}
					}
					data[0] = (byte) newPC;
				}

				// set toggle bit
				data[0] |= 0x01;

				if (afi != null) {
					// set AFI
					data[1] = afi.byteValue();
				}

				// set encoded data
				System.arraycopy(encodedData, 0, data, 2, encodedData.length);

				// zero all remaining data
				int indexEncodedData = 2 + encodedData.length;
				for (int i = indexEncodedData; i < data.length; i++) {
					data[i] = 0x00;
				}

				Field epc = new Field("epcBank", 1, 16 /* no CRC */, data.length * 8);
				if (result != null && result.getValue() != null) {
					// simulate an intermediate read result for all following operations
					byte[] dataWithCrc = new byte[data.length + 2];
					System.arraycopy(data, 0, dataWithCrc, 2, data.length);
					result.setValue(new VirtualReadResult(result.getValue().getState(), dataWithCrc));
				}
				return new Operation(id, OperationType.WRITE, epc, data);
			} else if (field.getBank() == 3 && encodedData.length <= readData.length) {
				byte[] data = Arrays.copyOf(encodedData, readData.length);
				if (result != null && result.getValue() != null) {
					// simulate an intermediate read result for all following operations
					result.setValue(new VirtualReadResult(result.getValue().getState(), data));
				}
				return new Operation(id, OperationType.WRITE, field.getField(), data);
			}
		}
		return null;
	}

	/**
	 * Returns if the given instance instance of equal
	 * 
	 * @param obj
	 *            The Object
	 * @return True if Object instance of equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this) || (obj instanceof CCOperation) && (obj.hashCode() == hashCode())
				&& ((CCOperation) obj).compare(obj.getClass(), spec.getOpName(), field, data);
	}

	/**
	 * Compares to operations by parameters
	 * 
	 * @param clazz
	 *            The operation class
	 * @param name
	 *            The name
	 * @param field
	 *            The field
	 * @param data
	 *            The data
	 * @return the compare result
	 */
	protected boolean compare(Class<?> clazz, String name, CommonField field, Common data) {
		if (!this.getClass().equals(clazz))
			return false;
		if ((spec.getOpName() != null) && (!spec.getOpName().equals(name)))
			return false;
		if ((this.field != null) && (!this.field.equals(field)))
			return false;
		if ((this.data != null) && (!this.data.equals(data)))
			return false;
		return true;
	}

	/**
	 * Returns the hash code
	 * 
	 * @return The hash code
	 */
	@Override
	public int hashCode() {
		if (hashCode == -1) {
			hashCode = this.getClass().hashCode();
			if (spec.getOpName() != null)
				hashCode = hashCode * 41 + spec.getOpName().hashCode();
			if (field != null)
				hashCode = hashCode * 41 + field.getField().hashCode();
			if (data != null)
				hashCode = hashCode * 41 + data.hashCode();
		}
		return hashCode;
	}

	/**
	 * Decreases field use count and disposes data if specified
	 */
	public void dispose() {
		if (field != null) {
			field.dec();
			field = null;
		}
		if (data != null) {
			data.dispose();
			data = null;
		}
	}
}