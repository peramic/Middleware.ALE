package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.IFieldSpec;
import havis.middleware.ale.service.tm.TMFixedFieldSpec;

/**
 * This class represents a {@link TMFixedFieldSpec} from ALE 1.1 (7.4). It
 * inherits from {@link CommonField} and override name, bank, offset and length.
 * It implements {@link IFieldSpec} so that data type and format can be
 * determined by {@link Fields}.
 */
public class FixedField extends CommonField implements IFieldSpec {

	private TMFixedFieldSpec spec;

	/**
	 * Creates a new instance. Validates field name, bank, length, offset, field
	 * data type and format.
	 * 
	 * @param spec
	 *            The specification
	 * @throws ValidationException
	 *             if validation failed
	 */
	public FixedField(TMFixedFieldSpec spec) throws ValidationException {
		super(spec.getFieldname());
		this.spec = spec;
		if (spec.getBank() < 0) {
			throw new ValidationException("Bank of field '"
					+ spec.getFieldname() + "' is less then zero");
		} else if (spec.getLength() < 1) {
			throw new ValidationException("Length of field '"
					+ spec.getFieldname() + "' is less then one");
		} else if (spec.getOffset() < 0) {
			throw new ValidationException("Offset of field '"
					+ spec.getFieldname() + "' is less then zero");
		}

		FieldDatatype datatype = Fields.getDatatype(spec.getFieldname(),
				spec.getDefaultDatatype());
		if (datatype == null) {
			throw new ValidationException(
					"Unknown default datatype for field '"
							+ spec.getFieldname() + "'");
		} else {
			this.datatype = datatype;
			FieldFormat format = Fields.getFormat(this, datatype);
			if (format == null) {
				throw new ValidationException(
						"Unknown default format for field '"
								+ spec.getFieldname() + "'");
			}
			this.format = format;
		}

		// TODO: validation 7.4
		// defaultDatatype i.e. requires more bits then length
		// defaultFormat
	}

	/**
	 * Retrieves the field name
	 */
	@Override
    public String getName() {
		return spec.getFieldname();
	}

	/**
	 * Retrieves the tag bank
	 */
	@Override
    public int getBank() {
		return spec.getBank();
	}

	/**
	 * Retrieves the offset in bits within the bank
	 */
	@Override
    public int getOffset() {
		return spec.getOffset();
	}

	/**
	 * Retrieves the length in bits of bank data
	 */
	@Override
    public int getLength() {
		return spec.getLength();
	}

	/**
	 * Retrieves the field name
	 */
	public String getFieldname() {
		return spec.getFieldname();
	}

	/**
	 * Retrieves the field data type from specification as string
	 */
	public String getDatatype() {
		return spec.getDefaultDatatype();
	}

	/**
	 * Retrieves the field format from specification as string
	 */
	public String getFormat() {
		return spec.getDefaultFormat();
	}
}
