package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.RNGValidationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.service.cc.RNGSpec;

/**
 * This is a implementation of the random number generator as specified in ALE
 * 1.1.1 (9.7)
 */
public class Random implements Data {

	private int count;
	private int length;
	private java.util.Random random;

	@SuppressWarnings("unused")
    private String name; // TODO: check usage
	private RNGSpec spec;

	private FieldDatatype datatype;
	private FieldFormat format;

	/**
	 * Creates a new instance. Keeps parameters.
	 *
	 * @param name
	 *            The name
	 * @param spec
	 *            The specification
	 * @throws RNGValidationException
	 *             if name is invalid or length of specification is zero.
	 */
	public Random(String name, RNGSpec spec) throws RNGValidationException {
		try {
			if (Name.isValid(name)) {
				random = new java.util.Random();
				this.name = name;
				this.spec = spec;

				if (spec.getLength() < 1) {
					throw new RNGValidationException(
							"Length of random number generator '" + name
									+ "' must be greater then zero");
				} else {
					this.length = spec.getLength();
				}
			}
		} catch (ValidationException e) {
			throw new RNGValidationException(e.getReason());
		}
	}

	/**
	 * Retrieves the specification
	 *
	 * @return The specification
	 */
	public RNGSpec getSpec() {
		return spec;
	}

	/**
	 * Retrieves the data type
	 */
	@Override
	public FieldDatatype getFieldDatatype() {
		return datatype;
	}

	/**
	 * Retrieves the format
	 */
	@Override
	public FieldFormat getFieldFormat() {
		return format;
	}

	/**
	 * Increases the use count.
	 */
	@Override
	public void inc() {
		synchronized (this) {
			count++;
		}
	}

	/**
	 * Decreases the use count.
	 */
	@Override
	public void dec() {
		synchronized (this) {
			count--;
		}
	}

	/**
	 * Retrieves the use state.
	 *
	 * @return true if the use count is greater then zero, false otherwise
	 */
	@Override
	public boolean isUsed() {
		synchronized (this) {
			return count > 0;
		}
	}

	/**
	 * Provides bytes by tag
	 *
	 * @param tag
	 *            The tag
	 * @return The random bytes
	 */
	@Override
	public Bytes getBytes(Tag tag) {
		byte[] bytes = new byte[(length + (8 - length % 8) % 8) / 8];
		for (int i = 0, len = bytes.length; i < len;)
			bytes[i++] = (byte) random.nextInt();
		return new Bytes(FieldDatatype.UINT, bytes, length);
	}

    @Override
	public Characters getCharacters(Tag tag) {
		throw new UnsupportedOperationException();
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((datatype == null) ? 0 : datatype.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + length;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Random))
            return false;
        Random other = (Random) obj;
        if (datatype != other.datatype)
            return false;
        if (format != other.format)
            return false;
        if (length != other.length)
            return false;
        return true;
    }
}