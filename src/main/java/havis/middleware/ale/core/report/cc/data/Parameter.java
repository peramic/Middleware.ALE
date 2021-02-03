package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;

/**
 * Implements the parameter as described in ALE 1.1.1 (9.3.6)
 */
public class Parameter implements Data {

    /**
     * Provides the parameter callback. It is used do get the value from
     * parameters bytes
     */
    interface Callback {

        /**
         * @return the bytes
         */
        Bytes getBytes();
        
        /**
         * @return the characters
         */
        Characters getCharacters();
    }

    Callback callback;

    /**
     * Returns the bytes
     *
     * @param tag
     *            The tag
     * @return The bytes
     */
    @Override
    public Bytes getBytes(Tag tag) {
        return callback.getBytes();
    }

    @Override
    public Characters getCharacters(Tag tag) {
        return callback.getCharacters();
    }

    /**
     * Retrieves the data type
     */
    public FieldDatatype datatype;

    /**
     * Retrieves the format
     */
    public FieldFormat format;

    /**
     * Creates a instance. Callback will invoke if bytes requested
     *
     * @param callback
     *            The callback
     */
    Parameter(Callback callback, FieldDatatype datatype, FieldFormat format) {
        this.callback = callback;
        this.datatype = datatype;
        this.format = format;
    }

    /**
     * Should increase the use count. Does nothing
     */
    @Override
    public void inc() {
    }

    /**
     * Should decrease the use count. Does nothing
     */
    @Override
    public void dec() {
    }

    /**
     * Should return the use state. Returns false
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
        if (!(obj instanceof Parameter))
            return false;
        Parameter other = (Parameter) obj;
        if (datatype != other.datatype)
            return false;
        if (format != other.format)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Parameter [datatype=" + datatype + ", format=" + format + "]";
    }
}