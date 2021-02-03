package havis.middleware.ale.core.report;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.report.pattern.PatternType;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.IFilterSpec;
import havis.middleware.tdt.TdtTranslationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is used in common cycle to make Filters available in event and
 * command cycle.
 *
 * @param <FilterSpec>
 *            The filter specification type
 */
public abstract class Filter<FilterSpec extends IFilterSpec> {

	/**
	 * Retrieves the include pattern list
	 */
	protected List<Patterns> include; // include pattern

	/**
	 * Retrieves the exclude pattern list
	 */
	protected List<Patterns> exclude; // exclude pattern

	/**
	 * Retrieves the filter specification
	 */
	protected FilterSpec spec;

	/**
	 * Retrieves an array of filter list members
	 *
	 * @return The filter list members
	 */
	protected abstract List<ECFilterListMember> getList();

	/**
	 * Creates new instance. Groups pattern by includes and excludes. Validates
	 * each pattern list
	 *
	 * @param spec
	 *            Filter specification
	 * @throws ValidationException
	 *             if a filter pattern is invalid
	 * @throws TdtTranslationException
	 */
	public Filter(FilterSpec spec) throws ValidationException {
		// keep specification
		this.spec = spec;
		if (spec != null) {
			include = new ArrayList<Patterns>();
			exclude = new ArrayList<Patterns>();

			for (ECFilterListMember member : getList()) {
				// validate and group each pattern list
				Patterns patterns = new Patterns(PatternType.FILTER, member.getPatList() != null ? member.getPatList().getPat() : null, member.getFieldspec());
				switch (member.getIncludeExclude()) {
				// add patterns to includes
				case "INCLUDE":
					include.add(patterns);
					break;
				// add patterns to excludes
				case "EXCLUDE":
					exclude.add(patterns);
					break;
				}
			}
		}
	}

	/**
	 * Retrieves the read operations which are necessary for extended filter
	 * operations i.e. filter by alternative fields or data types
	 *
	 * @return The tag operations
	 */
	public Collection<Operation> getOperations() {
		List<Operation> list = new ArrayList<Operation>();
		if (spec != null) {
			for (Patterns patterns : include) {
				Operation operation = patterns.getOperation();
				if (operation != null)
					list.add(operation);
			}
			for (Patterns patterns : exclude) {
				Operation operation = patterns.getOperation();
				if (operation != null)
					list.add(operation);
			}
		}
		return list;
	}

	/**
	 * Returns true if tag match to each include and does not match to any
	 * exclude pattern
	 *
	 * @param tag
	 *            The tag
	 * @return True if match
	 */
	public Boolean match(Tag tag) {
		Boolean match = Boolean.TRUE;
		if (spec != null) {
			for (Patterns patterns : include) {
				// abort if one include patterns does not match
				Boolean m = patterns.match(tag);
				if (Boolean.FALSE.equals(m)) {
					return Boolean.FALSE;
				}
				if (m == null) {
					match = null;
					break;
				}
			}
			for (Patterns patterns : exclude) {
				// abort if one exclude patterns match
				Boolean m = patterns.match(tag);
				if (Boolean.TRUE.equals(m)) {
					return Boolean.FALSE;
				}
				if (m == null) {
					match = null;
					break;
				}
			}
		}
		return match;
	}

	/**
	 * Disposes the filter
	 */
	public void dispose() {
		if (include != null) {
			for (Patterns patterns : include) {
				patterns.dispose();
			}
			include = null;
		}
		if (exclude != null) {
			for (Patterns patterns : exclude) {
				patterns.dispose();
			}
			exclude = null;
		}
	}
}
