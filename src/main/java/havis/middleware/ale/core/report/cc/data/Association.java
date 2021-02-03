package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.AssocTableValidationException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidAssocTableEntryException;
import havis.middleware.ale.base.exception.InvalidEPCException;
import havis.middleware.ale.base.exception.InvalidPatternException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.Tag.Property;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.report.pattern.PatternType;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.IFieldSpec;
import havis.middleware.ale.service.cc.AssocTableEntry;
import havis.middleware.ale.service.cc.AssocTableSpec;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This is a implementation of association table as specified in ALE 1.1.1 (9.6)
 */
public class Association implements Data, IFieldSpec {

	private String name;
	private AssocTableSpec spec;
	private Map<Tag, RawData> entries;
	private int count;

	private FieldDatatype datatype;
	private FieldFormat format;

	/**
	 * Creates a new instance. Validates name. Keeps parameters. Transfers
	 *
	 * @param name
	 *            The name
	 * @param spec
	 *            The specification
	 * @param entries
	 *            The entries list
	 * @throws InvalidAssocTableEntryException
	 * @throws InvalidEPCException
	 * @throws AssocTableValidationException
	 */
	Association(String name, AssocTableSpec spec, List<AssocTableEntry> entries) throws InvalidAssocTableEntryException, AssocTableValidationException {
		try {
			if (Name.isValid(name)) {
				this.name = name;
				this.spec = spec;
				try {
					FieldDatatype datatype = Fields.getDatatype(null, spec.getDatatype());
					if (datatype == null) {
						throw new ValidationException("Datatype could not be null");
					} else {
						this.datatype = datatype;
						FieldFormat format = Fields.getFormat(this, datatype);
						if (format == null) {
							throw new ValidationException("Format could not be null");
						}
						this.format = format;
					}
					try {
						this.entries = toMap(entries);
					} catch (InvalidEPCException e) {
						throw new InvalidAssocTableEntryException(e.getReason());
					}
				} catch (ValidationException e) {
					throw new AssocTableValidationException("The specification of association '" + name + "' is invalid. " + e.getReason());
				}
			}
		} catch (ValidationException e) {
			throw new AssocTableValidationException(e.getReason());
		}
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
	 * Retrieve the entries
	 * 
	 * @return the entries
	 * @throws ImplementationException
	 * @throws InvalidAssocTableEntryException
	 * @throws TdtTranslationException
	 */
	public List<AssocTableEntry> getEntries() throws ImplementationException {
		List<AssocTableEntry> entries = new ArrayList<AssocTableEntry>();
		for (final Entry<Tag, RawData> entry : this.entries.entrySet()) {
			try {
				AssocTableEntry e = new AssocTableEntry();
				e.setKey(entry.getKey().<TdtTagInfo>getProperty(Property.TAG_INFO).getUriTag());
				if (entry.getValue() instanceof Bytes) {
					e.setValue(Fields.toString(datatype, format, (Bytes) entry.getValue()));
				} else if (entry.getValue() instanceof Characters) {
					e.setValue(((Characters) entry.getValue()).getValue());
				}
				entries.add(e);
			} catch (TdtTranslationException e) {
				throw new ImplementationException("Invalid association table entry '" + entry.getKey() + "'");
			}
		}
		return entries;
	}

	/**
	 * Retrieves the specification
	 * 
	 * @return the specification
	 */
	public AssocTableSpec getSpec() {
		return spec;
	}

	/**
	 * Puts entries
	 *
	 * @param entries
	 *            The entries
	 * @throws InvalidAssocTableEntryException
	 *             if list contains entries with same keys
	 * @throws InvalidEPCException
	 */
	public void putEntries(List<AssocTableEntry> entries)
			throws InvalidAssocTableEntryException {
		synchronized (this.entries) {
			try {
				for (Entry<Tag, RawData> pair : toMap(entries).entrySet()) {
					this.entries.put(pair.getKey(), pair.getValue());
				}
			} catch (InvalidEPCException e) {
				throw new InvalidAssocTableEntryException(e.getReason());
			}
		}
	}

	/**
	 * Gets value of EPC
	 *
	 * @param epc the EPC
	 * @return the EPC value
	 */
	public String getValue(String epc) {
		synchronized (entries) {
			RawData data = entries.get(TagDecoder.getInstance().fromUrn(epc, new byte[0]));
			if (data instanceof Bytes) {
				return Fields.toString(datatype, format, (Bytes) data);
			} else if (data instanceof Characters) {
				return ((Characters) data).getValue();
			}
		}
		return null;
	}

	/**
	 * Gets list of entries filtered by pattern list
	 * 
	 * @param list
	 *            The pattern list
	 * @return Filtered list of entries
	 * @throws InvalidPatternException
	 * @throws TdtTranslationException
	 */
	public List<AssocTableEntry> getEntries(List<String> list) throws InvalidPatternException {
		Patterns patterns;
		try {
			patterns = new Patterns(PatternType.FILTER, list);
		} catch (ValidationException e) {
			throw new InvalidPatternException(e.getReason());
		}
		List<AssocTableEntry> entries = new ArrayList<AssocTableEntry>();
		synchronized (this.entries) {
			for (final Entry<Tag, RawData> pair : this.entries.entrySet()) {
				if (Boolean.TRUE.equals(patterns.match(pair.getKey()))) {
					try {
						AssocTableEntry e = new AssocTableEntry();
						e.setKey(pair.getKey().<TdtTagInfo>getProperty(Property.TAG_INFO).getUriTag());
						if (pair.getValue() instanceof Bytes) {
							e.setValue(Fields.toString(datatype, format, (Bytes) pair.getValue()));
						} else if (pair.getValue() instanceof Characters) {
							e.setValue(((Characters) pair.getValue()).getValue());
						}
						entries.add(e);
					} catch (TdtTranslationException e) {
						throw new InvalidPatternException("Invalid pattern list entry '" + pair.getKey() + "'");
					}
				}
			}
		}
		return entries;
	}

	/**
	 * Removes entry by EPC
	 *
	 * @param urn
	 *            The EPC
	 * @throws InvalidEPCException
	 */
	public void removeEntry(String urn) throws InvalidEPCException {
		synchronized (entries) {
			Tag tag = TagDecoder.getInstance().fromUrn(urn, new byte[0]);
			if (tag.<TdtTagInfo>getProperty(Property.TAG_INFO) == null) {
				throw new InvalidEPCException("Could not parse urn '" + urn + "'");
			} else {
				if (tag.<TdtTagInfo>getProperty(Property.TAG_INFO).isEpcGlobal()) {
					entries.remove(tag);
				} else {
					throw new InvalidEPCException("Tag '" + urn + "' is not conform to EPCglobal");
				}
			}
		}
	}

	/**
	 * Removes entries where pattern from list matches
	 * 
	 * @param list
	 *            The pattern list
	 * @throws InvalidPatternException
	 */
	public void removeEntries(List<String> list) throws InvalidPatternException {
		Patterns patterns;
		try {
			patterns = new Patterns(PatternType.FILTER, list);
		} catch (ValidationException e) {
			throw new InvalidPatternException(e.getReason());
		}
		synchronized (entries) {
			List<Tag> tags = new ArrayList<Tag>();
			for (Tag tag : entries.keySet()) {
				if (Boolean.TRUE.equals(patterns.match(tag))) {
					tags.add(tag);
				}
			}
			for (Tag tag : tags) {
				entries.remove(tag);
			}
		}
	}

	/**
	 * Transfers entries dictionary to list
	 *
	 * @param entries
	 *            The entries
	 * @return Entries as list
	 */
	static List<AssocTableEntry> toList(Map<String, String> entries) {
		List<AssocTableEntry> list = new ArrayList<AssocTableEntry>();
		if (entries != null) {
			for (final Entry<String, String> pair : entries.entrySet()) {
				AssocTableEntry e = new AssocTableEntry();
				e.setKey(pair.getKey());
				e.setValue(pair.getValue());
				list.add(e);
			}
		}
		return list;
	}

	/**
	 * Transfers entries list to dictionary
	 * 
	 * @param entries
	 *            The entries
	 * @return Entries as dictionary
	 * @throws InvalidAssocTableEntryException
	 * @throws InvalidEPCException
	 */
	public Map<Tag, RawData> toMap(List<AssocTableEntry> entries) throws InvalidAssocTableEntryException, InvalidEPCException {
		Map<Tag, RawData> map;
		if (entries == null) {
			map = new HashMap<Tag, RawData>();
		} else {
			map = new HashMap<Tag, RawData>(entries.size());
			for (AssocTableEntry entry : entries) {
				Tag tag = TagDecoder.getInstance().fromUrn(entry.getKey(), new byte[0] /* ignore TID */);
				if (tag.<TdtTagInfo>getProperty(Property.TAG_INFO) == null) {
					throw new InvalidAssocTableEntryException("Could not decode urn '" + entry.getKey() + "'");
				} else {
					if (tag.<TdtTagInfo>getProperty(Property.TAG_INFO).isEpcGlobal()) {
						if (map.containsKey(tag)) {
							throw new InvalidAssocTableEntryException("Duplicate key '" + entry.getKey() + "' not allowed");
						} else {
							try {
								if (format == FieldFormat.STRING) {
									map.put(tag, new Characters(entry.getValue()));
								} else {
									map.put(tag, Fields.toBytes(datatype, format, entry.getValue()));
								}
							} catch (ValidationException e) {
								throw new InvalidAssocTableEntryException(e.getReason());
							}
						}
					} else {
						throw new InvalidEPCException("Tag '" + entry.getKey() + "' is not conform to EPCglobal");
					}
				}
			}
		}
		return map;
	}

	/**
	 * Increases the use count
	 */
	@Override
	public void inc() {
		synchronized (this) {
			count++;
		}
	}

	/**
	 * Decreases the use count
	 */
	@Override
	public void dec() {
		synchronized (this) {
			count--;
		}
	}

	/**
	 * Retrieves the use state
	 *
	 * @return Returns true if use count is greater then zero, false otherwise
	 */
	@Override
	public boolean isUsed() {
		synchronized (this) {
			return count > 0;
		}
	}

	/**
	 * Retrieves the byte array for the specified tag
	 *
	 * @param tag
	 *            The tag
	 * @return The byte array
	 */
	@Override
	public Bytes getBytes(Tag tag) {
		synchronized (entries) {
			Tag matchTag = new Tag(tag.getEpc());
			matchTag.setTid(new byte[0]);
			Object data;
			if (!entries.containsKey(matchTag) || !((data = entries.get(matchTag)) instanceof Bytes)) {
				return new Bytes(ResultState.ASSOCIATION_TABLE_VALUE_MISSING);
			}
			return (Bytes) data;
		}
	}

	/**
	 * Retrieves the characters for the specified tag
	 * 
	 * @param tag
	 *            The tag
	 * @return The characters
	 */
	@Override
	public Characters getCharacters(Tag tag) {
		synchronized (entries) {
			Tag matchTag = new Tag(tag.getEpc());
			matchTag.setTid(new byte[0]);
			Object data;
			if (!entries.containsKey(matchTag) || !((data = entries.get(matchTag)) instanceof Characters)) {
				return new Characters(ResultState.ASSOCIATION_TABLE_VALUE_MISSING);
			}
			return (Characters) data;
		}
	}

	/**
	 * Retrieves the field name
	 */
	@Override
	public String getFieldname() {
		return null;
	}

	/**
	 * Retrieves the data type as String
	 */
	@Override
	public String getDatatype() {
		return spec.getDatatype();
	}

	/**
	 * Retrieves the format as String
	 */
	@Override
	public String getFormat() {
		return spec.getFormat();
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((datatype == null) ? 0 : datatype.hashCode());
        result = prime * result + ((entries == null) ? 0 : entries.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Association))
            return false;
        Association other = (Association) obj;
        if (datatype != other.datatype)
            return false;
        if (entries == null) {
            if (other.entries != null)
                return false;
        } else if (!entries.equals(other.entries))
            return false;
        if (format != other.format)
            return false;
        return true;
	}

	@Override
	public String toString() {
		return "Association [name=" + name + ", datatype=" + datatype + ", format=" + format + ", entries=" + entries + "]";
	}

	/**
	 * Disposes the association table
	 */
	public void dispose() {
		entries.clear();
	}
}