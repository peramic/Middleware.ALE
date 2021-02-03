package havis.middleware.ale.core.field;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.IFieldSpec;

public class ShadowField extends CommonField {

    private CommonField field;
    private FieldDatatype datatype;
    private FieldFormat format;

    /**
     * Provides the field name
     */
    @Override
    public String getName() {
        return field.getName();
    }

    /**
     * Provides the data type of the field
     */
    @Override
    public FieldDatatype getFieldDatatype() {
        return datatype;
    }

    /**
     * Provides the format of the field
     */
    @Override
    public FieldFormat getFieldFormat() {
        return format;
    }

    /**
     * Provides the tag bank
     */
    @Override
    public int getBank() {
        return field.getBank();
    }

    /**
     * Provides the offset in bits within the bank
     */
    @Override
    public int getOffset() {
        return field.getOffset();
    }

    /**
     * Provides the count of bits of data on bank to read or write
     */
    @Override
    public int getLength() {
        return field.getLength();
    }

    /**
     * Retrieves if operation needs advanced data
     */
    @Override
    public boolean isAdvanced() {
        return field.isAdvanced();
    }

    /**
     * Creates a new instance
     *
     * @param field
     *            The field
     * @param spec
     *            The field specification
     * @throws ValidationException
     */
    ShadowField(CommonField field, IFieldSpec spec) throws ValidationException {
        this.field = field;
        this.datatype = Fields.getDatatype(spec.getFieldname(), spec.getDatatype());
        if (this.datatype == null)
            this.datatype = field.getFieldDatatype();
        this.format = Fields.getFormat(spec, datatype);
        if (this.format == null)
            this.format = field.getFieldFormat();
    }

    /**
     * Increases the use count of field
     */
    @Override
    public void inc() {
        field.inc();
    }

    /**
     * Decreases the use count of field
     */
    @Override
    public void dec() {
        field.dec();
    }

    /**
     * Retrieves the using state of field
     */
    @Override
    public boolean isUsed() {
        return field.isUsed();
    }

	@Override
	public CommonField getBase() {
		return field;
	}
}