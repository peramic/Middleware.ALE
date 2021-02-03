package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.field.VariableField;
import havis.middleware.ale.service.cc.CCOpDataSpec;

/**
 * This class is used to validate and keep a {@link CCOpDataSpec} as specified
 * in ALE 1.1.1 (9.3.6).
 */
public class Common {

	/**
	 * Type of data
	 */
	public enum DataType {

		/**
		 * Data contains a literal
		 */
		LITERAL,

		/**
		 * Data contains a parameter
		 */
		PARAMETER,

		/**
		 * Data contains a EPC cache
		 */
		CACHE,

		/**
		 * Data contains the name of the association table
		 */
		ASSOCIATION,

		/**
		 * Data contains the name of a random number generator
		 */
		RANDOM
	}

	private int hashCode = -1;

	private CCOpDataSpec spec;
	private DataType type;
	private Data data;

	/**
	 * Creates a new instance. Keeps the specification. Validates the data type.
	 *
	 * @param spec
	 *            The data specification
	 * @param parameters
	 *            The parameters
	 * @throws ValidationException
	 *             if data type is not specified or unknown.
	 */
	public Common(CCOpDataSpec spec, Parameters parameters)
			throws ValidationException {
		this(spec, parameters, null, null);
	}

	/**
	 * Creates a new instance. Keeps the specification. Validates the datatype.
	 *
	 * @param spec
	 *            The data specification
	 * @param parameters
	 *            The parameters
	 * @param field
	 *            A optional field for validation
	 * @throws ValidationException
	 *             if data type is not specified or unknown.
	 */
	public Common(CCOpDataSpec spec, Parameters parameters, CommonField field)
			throws ValidationException {
		this(spec, parameters, field, null);
	}

	/**
	 * Creates a new instance. Keeps the specification. Validates the datatype.
	 * 
	 * @param spec
	 *            The data specification
	 * @param parameters
	 *            The parameters
	 * @param field
	 *            A optional field for validation
	 * @param predefinedData
	 *            Predefined data
	 * @throws ValidationException
	 *             if data type is not specified or unknown.
	 */
	public Common(CCOpDataSpec spec, Parameters parameters, CommonField field, RawData predefinedData) throws ValidationException {
		if (spec == null) {
			throw new ValidationException("Command cycle operation data specification could not be null");
		} else {
			this.spec = spec;
			if ((spec.getSpecType() == null) || (spec.getSpecType().length() == 0)) {
				throw new ValidationException("Command cycle operation data type could not be null or empty");
			} else {
				try {
					type = DataType.valueOf(spec.getSpecType());
				} catch (IllegalArgumentException e) {
				}
				if (type != null) {
					switch (type) {
					case LITERAL:
						data = new Literal(field, spec.getData(), predefinedData);
						break;
					case PARAMETER:
						if (field != null) {
							data = parameters.get(spec.getData(), field.getFieldDatatype(), field.getFieldFormat());
						}
						break;
					case CACHE:
						if (field != null) {
							if (field.getBase() instanceof VariableField) {
								throw new ValidationException("CACHE data type not allowed for variable fields");
							}
							data = Caches.getInstance().get(spec.getData());
							if (data == null) {
								throw new ValidationException("Unknown epc cache '" + spec.getData() + "'");
							} else if (field.getFieldDatatype() != FieldDatatype.EPC) {
								throw new ValidationException("Field datatype for epc cache data source must be epc");
							} else if (field.getFieldFormat() != FieldFormat.EPC_TAG) {
								throw new ValidationException("Field format for epc cache data source must be epc tag");
							} else {
								data.inc();
							}
						}
						break;
					case ASSOCIATION:
						if (field != null) {
							data = Associations.getInstance().get(spec.getData());
							if (data == null) {
								throw new ValidationException("Unknown association table '" + spec.getData() + "'");
							} else if (field.getFieldDatatype() != data.getFieldDatatype()) {
								throw new ValidationException("Field '" + field.getName() + "' datatype deviates from datatype of association table '"
										+ spec.getData() + "'");
							} /*else if (field.getFieldFormat() != data.getFieldFormat()) {
								throw new ValidationException("Field '" + field.getName() + "' format deviates from format of association table '"
										+ spec.getData() + "'");
							}*/ else {
								data.inc();
							}
						}
						break;
					case RANDOM:
						if (field != null) {
							if (field.getBase() instanceof VariableField) {
								throw new ValidationException("RANDOM data type not allowed for variable fields");
							}
							data = Randoms.getInstance().get(spec.getData());
							if (data == null) {
								throw new ValidationException("Unknown random number generator '" + spec.getData() + "'");
							} else if (field.getFieldDatatype() != FieldDatatype.UINT) {
								throw new ValidationException("Field datatype for random data source must be uint");
							} else if (field.getFieldFormat() != FieldFormat.HEX) {
								throw new ValidationException("Field format for random data source must be hex");
							} else {
								data.inc();
							}
						}
						break;
					}
				} else {
					throw new ValidationException("Unknown command cycle operation data type '" + spec.getSpecType() + "'");
				}
			}
		}
	}

	/**
	 * Retrieves the original specification
	 *
	 * @return The command cycle specification
	 */
	public CCOpDataSpec getSpec() {
		return spec;
	}

	/**
	 * Retrieves the data type
	 */
	public DataType getType() {
		return type;
	}

	public String getValue() {
		return spec.getData();
	}

	public Bytes getBytes(Tag tag) {
        return data.getBytes(tag);
    }

	public Characters getCharacters(Tag tag) {
        return data.getCharacters(tag);
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((data == null) ? 0 : data.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            hashCode = result;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Common))
            return false;
        Common other = (Common) obj;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    /**
	 * Decreases data use count if specified
	 */
	public void dispose() {
		if (data != null) {
			data.dec();
			data = null;
		}
	}
}