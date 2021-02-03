package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.po.OID;
import havis.middleware.ale.service.tm.TMVariableFieldSpec;

/**
 * This class represents the {@link TMVariableFieldSpec} from ALE 1.1 (7.6). It
 * inherits from {@link CommonField} and override name, bank, offset and length
 */
public class VariableField extends CommonField {

	private OID oid;

	private TMVariableFieldSpec spec;

	/**
	 * Creates a new instance. Validates fieldname, length and oid syntax
	 * 
	 * @param spec
	 *            The spec
	 * @throws ValidationException
	 */
	public VariableField(TMVariableFieldSpec spec) throws ValidationException {
		super(spec.getFieldname());
		this.spec = spec;
		if (spec.getBank() < 0) {
			throw new ValidationException("Bank of field '" + spec.getFieldname() + "' is less then zero");
		} else {
			if (Fields.OID_VALUE.matcher(spec.getOid()).matches()) {
				this.oid = new OID(spec.getOid());
			} else {
				throw new ValidationException("OID syntax of field '" + spec.getFieldname() + "' does not match [rfc3061]");
			}
		}
	}

	/**
	 * Create a new instance without a spec
	 * 
	 * @param name
	 *            the name of the field
	 * @param bank
	 *            the bank
	 * @param oid
	 *            the oid
	 * @throws ValidationException
	 */
	public VariableField(String name, int bank, String oid) throws ValidationException {
		super(name, bank);
		this.spec = null;
		if (bank < 0) {
			throw new ValidationException("Bank of field '" + name + "' is less then zero");
		} else {
			if (Fields.OID_VALUE.matcher(oid).matches()) {
				this.oid = new OID(oid);
			} else {
				throw new ValidationException("OID syntax of field '" + name + "' does not match [rfc3061]");
			}
		}
	}

	/**
	 * Retrieves the OID
	 * 
	 * @return The OID
	 */
	public OID getOID() {
		return oid;
	}

	/**
	 * Retrieves the field name
	 * 
	 * @return The field name
	 */
	@Override
	public String getName() {
		return spec != null ? spec.getFieldname() : super.getName();
	}

	/**
	 * Retrieves {@link FieldDatatype#ISO} as the field datatype
	 * 
	 * @return The datatype
	 */
	@Override
	public FieldDatatype getFieldDatatype() {
		return FieldDatatype.ISO;
	}

	/**
	 * Retrieves {@link FieldFormat#STRING} as the field format
	 */
	@Override
	public FieldFormat getFieldFormat() {
		return FieldFormat.STRING;
	}

	/**
	 * Retrieves the tag bank
	 */
	@Override
	public int getBank() {
		return spec != null ? spec.getBank() : super.getBank();
	}

	/**
	 * Retrieves zero for offset because the whole bank is used for oids
	 */
	@Override
	public int getOffset() {
		return 0;
	}

	/**
	 * Retrieves zero for length because the whole bank is used for oids
	 */
	@Override
	public int getLength() {
		return 0;
	}

	/**
	 * Retrieves if operation needs advanced data
	 */
	@Override
	public boolean isAdvanced() {
		return true;
	}
}
