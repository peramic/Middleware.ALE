package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Field;
import havis.middleware.ale.core.Name;
import havis.middleware.utils.data.Calculator;

/**
 * This class is used by {@link Fields} to permanently holds all informations
 * for reading, writing, encoding and decoding tag data.
 */
public class CommonField {

	/**
	 * Provides the field name
	 */
	private String name;

	/**
	 * Provides the data type of the field
	 */
	protected FieldDatatype datatype;

	/**
	 * Provides the format of the field
	 */
	protected FieldFormat format;

	/**
	 * Provides the tag bank
	 */
	private int bank;

	/**
	 * Provides the offset in bits within the bank
	 */
	private int offset;

	/**
	 * Provides the count of bits of data on bank to read or write
	 */
	private int length;

	/**
	 * Retrieves if operation needs advanced data
	 *
	 * @return True, if common field is advanced
	 */
	public boolean isAdvanced() {
		return getOffset() % 16 > 0 || getLength() % 16 > 0 || getLength() == 0;
	}

	/**
	 * Retrieves if field is EPC
	 */
	private boolean isEpc;

	private int hashCode = -1;
	private int count;

	/**
	 * Returns a new instance.
	 */
	public CommonField() {
		this.count = 0;
	}

	/**
	 * Creates new instance of common field.
	 *
	 * @param datatype
	 *            The field data type
	 * @param format
	 *            The field format
	 * @param bank
	 *            The bank
	 * @param offset
	 *            The bit offset
	 * @param length
	 *            The bit length
	 */
	public CommonField(FieldDatatype datatype, FieldFormat format, int bank,
			int offset, int length) {
		this.datatype = datatype;
		this.format = format;
		this.bank = bank;
		this.offset = offset;
		this.length = length;
	}

	/**
	 * Creates new instance of common field with zero as default for bank,
	 * offset and length.
	 *
	 * @param datatype
	 *            The field data type
	 * @param format
	 *            The field format
	 */
	public CommonField(FieldDatatype datatype, FieldFormat format) {
		this(datatype, format, 0, 0, 0);
	}

	/**
	 * Creates new instance of common field with default data type
	 * {@link FieldDatatype#EPC} and default format {@link FieldFormat#EPC_TAG}.
	 *
	 * @param bank
	 *            The bank
	 * @param offset
	 *            The bit offset
	 * @param length
	 *            The bit length
	 */
	public CommonField(int bank, int offset, int length) {
		this(FieldDatatype.EPC, FieldFormat.EPC_TAG, bank, offset, length);
	}

	/**
	 * Validates field name
	 *
	 * @param name
	 *            The field name
	 * @throws ValidationException
	 *             If field name is null or empty, starts with character '@' or
	 *             name is reserved
	 */
	protected CommonField(String name) throws ValidationException {
		this();
		if (Name.isValid(name)) {
			if (name.startsWith("@")) {
				throw new ValidationException("Fieldname '" + name
						+ "' starts with @ character");
			} else if (Fields.RESERVED.contains(name)) {
				throw new ValidationException("Fieldname '" + name
						+ "' is reserved");
			}
		}
	}

	/**
	 * Creates a common field with name and bank
	 * 
	 * @param name
	 *            The name
	 * @param bank
	 *            The bank
	 */
	protected CommonField(String name, int bank) {
		this();
		this.name = name;
		this.bank = bank;
	}

	CommonField(String name, FieldDatatype datatype, FieldFormat format) {
		this.name = name;
		this.datatype = datatype;
		this.format = format;
	}

	CommonField(String name, FieldDatatype datatype, FieldFormat format,
			int bank, int length, int offset, boolean isEpc) {
		this.name = name;
		this.datatype = datatype;
		this.format = format;
		this.bank = bank;
		this.length = length;
		this.offset = offset;
		this.isEpc = isEpc;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public FieldDatatype getFieldDatatype() {
		return this.datatype;
	}

	public void setFieldDatatype(FieldDatatype datatype) {
		this.datatype = datatype;
	}

	public FieldFormat getFieldFormat() {
		return this.format;
	}

	public void setFieldFormat(FieldFormat format) {
		this.format = format;
	}

	public int getBank() {
		return this.bank;
	}

	public void setBank(int bank) {
		this.bank = bank;
	}

	public int getOffset() {
		return this.offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getLength() {
		return this.length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public boolean isEpc() {
		return this.isEpc;
	}

	public void setEpc(boolean isEpc) {
		this.isEpc = isEpc;
	}

	/**
	 * Increases the use count of field
	 */
	public void inc() {
		synchronized (this) {
			this.count++;
		}
	}

	/**
	 * Decreases the use count of field
	 */
	public void dec() {
		synchronized (this) {
			if (this.count == 0) throw new UnsupportedOperationException("Invalid dec() call, count is already 0");
				this.count--;
		}
	}

	/**
	 * @return the using state of field
	 */
	public boolean isUsed() {

		synchronized (this) {
			return (this.count > 0);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CommonField && equals((CommonField) obj);
	}

	/**
	 * Validates if bank, offset and length are equal
	 *
	 * @param field
	 *            The field
	 * @return Returns true if bank, offset and length are equal, false
	 *         otherwise
	 */
	private boolean equals(CommonField field) {
		if ((getBank() == field.getBank()) && (getOffset() == field.getOffset())
				&& (getLength() == field.getLength())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Retrieves the reader operation field
	 *
	 * @return The reader operation field
	 */
	public Field getField() {
		return new Field(getName(), getBank(), isAdvanced() ? getOffset() / 16 * 16 : getOffset(),
				isAdvanced() && getLength() > 0 ? (Calculator.size(getOffset() + getLength(),
						16) - getOffset() / 16) * 16 : getLength());
	}

	/**
	 * @return the base field
	 */
	public CommonField getBase() {
		return this;
	}

	/**
	 * Retrieves the hash code
	 */
	@Override
	public int hashCode() {
		if (hashCode == -1) {
			hashCode = getField().hashCode();
		}
		return hashCode;
	}
}
