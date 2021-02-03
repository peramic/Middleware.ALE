package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.report.pattern.PatternType;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.ec.ECFilterSpec;
import havis.middleware.tdt.TdtTranslationException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to make the filters usable for the common cycle
 */
public class Filter extends
		havis.middleware.ale.core.report.Filter<ECFilterSpec> {

	/**
	 * Creates a new instance. Keeps specification. It also provides the include
	 * and exclude patterns from ALE 1.0 for back-compatibility
	 * 
	 * @param spec
	 *            >The event cycle filter specification
	 * @throws ValidationException
	 * @throws TdtTranslationException
	 */
	public Filter(ECFilterSpec spec) throws ValidationException {
		super(spec);
		if (spec != null) {
			// for back-compatibility with ALE 1.0
			if (spec.getIncludePatterns() != null
					&& spec.getIncludePatterns().getIncludePattern().size() > 0) {
				// add pattern to includes with default fieldname
				include.add(new Patterns(PatternType.FILTER, spec
						.getIncludePatterns().getIncludePattern(),
						new ECFieldSpec("epc")));
			}
			if (spec.getExcludePatterns() != null
					&& spec.getExcludePatterns().getExcludePattern().size() > 0) {
				// add pattern to excludes with default fieldname
				exclude.add(new Patterns(PatternType.FILTER, spec
						.getExcludePatterns().getExcludePattern(),
						new ECFieldSpec("epc")));
			}
		}
	}

	/**
	 * Retrieves a array of filter members
	 */
	@Override
    protected List<ECFilterListMember> getList() {
		if (spec.getExtension() != null && spec.getExtension().getFilterList() != null) {
			return spec.getExtension().getFilterList().getFilter();
		} else {
			return new ArrayList<ECFilterListMember>();
		}
	}
}
