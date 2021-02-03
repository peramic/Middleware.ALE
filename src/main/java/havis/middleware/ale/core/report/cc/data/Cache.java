package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.EPCCacheSpecValidationException;
import havis.middleware.ale.base.exception.InvalidPatternException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.report.pattern.IPattern;
import havis.middleware.ale.core.report.pattern.PatternType;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.EPCCacheSpec;
import havis.middleware.tdt.TdtTranslationException;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a implementation of the epc cache as specified in ALE 1.1.1 (9.5)
 */
public class Cache implements Data {

	String name;
	EPCCacheSpec spec;
	List<String> value;
	Patterns patterns;
	IPattern pattern;
	int count;

	/**
	 * Creates a new instance. Keeps parameters.
	 *
	 * @param name
	 *            The name
	 * @param spec
	 *            The specification
	 * @param patterns
	 *            The pattern list
	 * @throws InvalidPatternException
	 * @throws EPCCacheSpecValidationException
	 */
	public Cache(String name, EPCCacheSpec spec, List<String> patterns)
			throws InvalidPatternException, EPCCacheSpecValidationException {
		try {
			if (Name.isValid(name)) {
				this.name = name;
				this.spec = spec;
				this.value = patterns;
				try {
					this.patterns = new Patterns(PatternType.CACHE, patterns);
				} catch (ValidationException e) {
					throw new InvalidPatternException("EPC cache '" + name
							+ "' validation failed. " + e.getReason());
				}
				pattern = this.patterns.next();
			}
		} catch (ValidationException e) {
			throw new EPCCacheSpecValidationException(e.getReason());
		}
	}

	/**
	 * Retrieves specification
	 *
	 * @return The specification
	 */
	public EPCCacheSpec getSpec() {
		return spec;
	}

	/**
	 * Retrieves the byte array for tag
	 *
	 * @param tag
	 *            The tag
	 */
	@Override
	public Bytes getBytes(Tag tag) {
		synchronized (patterns) {
			try {
				String next = null;
				while (next == null) {
					if ((pattern == null) || ((next = pattern.next()) == null)
							&& ((pattern = patterns.next()) == null)) {
						return new Bytes(ResultState.EPC_CACHE_DEPLETED);
					}
				}
				byte[] bytes;
				try {
					bytes = TagDecoder.getInstance().decodeUrn(next);
				} catch (TdtTranslationException e) {
					Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "TDT translation failed: " + e.getMessage(), e);
					return null;
				}
				return new Bytes(FieldDatatype.EPC, bytes, bytes.length * 8);
			} finally {
				havis.middleware.ale.core.depot.service.cc.Cache.getInstance().update(name, getPatterns());
			}
		}
	}

    @Override
	public Characters getCharacters(Tag tag) {
		throw new UnsupportedOperationException();
	}

	void replenish(List<String> list) throws InvalidPatternException {
		synchronized (patterns) {
			try {
				patterns.append(list);
				if (pattern == null)
					pattern = patterns.next();
			} catch (ValidationException e) {
				throw new InvalidPatternException(e.getReason());
			}
		}
	}

	List<String> deplete() {
		synchronized (patterns) {
			List<String> list = getPatterns();
			patterns.clear();
			pattern = null;
			return list;
		}
	}

	/**
	 * Retrieves the patterns
	 *
	 * @return The pattern string list
	 */
	public List<String> getPatterns() {
		synchronized (patterns) {
			List<String> list = new ArrayList<String>();
			if (pattern != null) {
				String s = pattern.toString();
				if (s != null)
					list.add(s);
				list.addAll(patterns.toList());
			}
			return list;
		}
	}

	/**
	 * Retrieves the data type
	 *
	 * @return The field data type
	 */
	@Override
	public FieldDatatype getFieldDatatype() {
		return FieldDatatype.EPC;
	}

	/**
	 * Retrieves the format
	 *
	 * @return The field format
	 */
	@Override
	public FieldFormat getFieldFormat() {
		return FieldFormat.EPC_TAG;
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
	 * @return True if use count is greater then zero, false
	 */
	@Override
	public boolean isUsed() {
		synchronized (this) {
			return count > 0;
		}
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Cache))
            return false;
        Cache other = (Cache) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    /**
	 * Disposes cache
	 */
	public void dispose() {
		synchronized (patterns) {
			patterns.dispose();
		}
	}
}