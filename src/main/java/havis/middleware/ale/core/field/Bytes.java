package havis.middleware.ale.core.field;

import havis.middleware.ale.base.operation.tag.result.ResultState;

import java.util.Arrays;

/**
 * Provides a set of bytes
 */
public class Bytes extends RawData {

	/**
	 * Retrieves the field data type
	 */
	private FieldDatatype datatype;

	/**
	 * The bytes
	 */
	private byte[] value;

	/**
	 * The bit length
	 */
	private int length;

	/**
	 * Creates new instance
	 * 
	 * @param datatype
	 *            The data type
	 * @param value
	 *            The value
	 * @param length
	 *            The length
	 * @param state
	 *            the result state
	 */
	public Bytes(FieldDatatype datatype, byte[] value, int length, ResultState state) {
		super(state);
		this.datatype = datatype;
		this.value = value;
		this.length = length;
	}

	/**
	 * Creates new instance. Sets result state to {@link ResultState#SUCCESS}
	 * 
	 * @param datatype
	 *            The data type
	 * @param value
	 *            The value
	 * @param length
	 *            The length
	 */
	public Bytes(FieldDatatype datatype, byte[] value, int length) {
		this(datatype, value, length, ResultState.SUCCESS);
	}

	/**
	 * Creates new instance. Sets data type to {@link FieldDatatype#EPC}, value
	 * to empty byte array and length to zero
	 * 
	 * @param state
	 *            the result state
	 */
	public Bytes(ResultState state) {
		this(FieldDatatype.EPC, new byte[0], 0, state);
	}

	/**
	 * Creates new instance. Sets data type to {@link FieldDatatype#EPC}, length
	 * to zero and result state to {@link ResultState#SUCCESS}
	 * 
	 * @param value
	 *            the value
	 */
	public Bytes(byte[] value) {
		this(FieldDatatype.EPC, value, 0, ResultState.SUCCESS);
	}

	/**
	 * Creates new instance. Sets data type to {@link FieldDatatype#EPC}, value
	 * to empty byte array, length to zero and result state to
	 * {@link ResultState#SUCCESS}
	 */
	public Bytes() {
		this(FieldDatatype.EPC, new byte[0], 0, ResultState.SUCCESS);
	}

	/**
	 * Gets the data type
	 * 
	 * @return The data type
	 */
	public FieldDatatype getDatatype() {
		return datatype;
	}

	/**
	 * Gets the value
	 * 
	 * @return The value
	 */
	public byte[] getValue() {
		return value;
	}

	/**
	 * Gets the length
	 * 
	 * @return The length
	 */
	public int getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((datatype == null) ? 0 : datatype.hashCode());
		result = prime * result + length;
		result = prime * result + Arrays.hashCode(value);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Bytes))
			return false;
		Bytes other = (Bytes) obj;
		if (datatype != other.datatype)
			return false;
		if (length != other.length)
			return false;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Bytes [datatype=" + datatype + ", value=" + Arrays.toString(value) + ", length=" + length + ", state=" + state + "]";
	}
}