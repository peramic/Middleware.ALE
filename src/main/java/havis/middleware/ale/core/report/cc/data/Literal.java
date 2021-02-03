package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;

/**
 * This class provides the literal data as specified in ALE 1.1.1 (9.3.6)
 */
public class Literal implements Data {

	private RawData data;

	private FieldDatatype datatype;

	private FieldFormat format;

	/**
	 * Creates a new instance
	 * 
	 * @param data
	 *            The data to provide
	 * @throws ValidationException
	 */
	public Literal(CommonField field, String data, RawData predefinedData) throws ValidationException {
		if (predefinedData != null) {
			this.data = predefinedData;
		} else if (field != null) {
			this.datatype = field.getFieldDatatype();
			this.format = field.getFieldFormat();
			if (this.format == FieldFormat.STRING) {
				this.data = new Characters(data);
			} else {
				this.data = Fields.toBytes(field, data);
			}
		}
	}

	/**
	 * Retrieves the data
	 * 
	 * @param tag
	 *            The tag
	 * @return The provided bytes
	 */
	@Override
	public Bytes getBytes(Tag tag) {
		if (this.data instanceof Bytes) {
			return (Bytes) this.data;
		}
		return new Bytes(ResultState.MISC_ERROR_TOTAL);
	}

	/**
	 * Retrieves the data
	 * 
	 * @param tag
	 *            The tag
	 * @return the provided characters
	 */
	@Override
	public Characters getCharacters(Tag tag) {
		if (this.data instanceof Characters) {
			return (Characters) this.data;
		}
		return new Characters(ResultState.MISC_ERROR_TOTAL);
	}

	/**
	 * Does nothing
	 */
	@Override
	public void inc() {
	}

	/**
	 * Does nothing
	 */
	@Override
	public void dec() {
	}

	/**
	 * Retrieves the use state
	 * 
	 * @return Always false
	 */
	@Override
	public boolean isUsed() {
		return false;
	}

	@Override
	public FieldDatatype getFieldDatatype() {
		return datatype;
	}

	@Override
	public FieldFormat getFieldFormat() {
		return format;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((datatype == null) ? 0 : datatype.hashCode());
		result = prime * result + ((format == null) ? 0 : format.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Literal))
			return false;
		Literal other = (Literal) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (datatype != other.datatype)
			return false;
		if (format != other.format)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Literal [data=" + data + ", datatype=" + datatype + ", format=" + format + "]";
	}
}