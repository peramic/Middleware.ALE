package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.cc.CCFilterSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to make filters usable for the common cycle
 */
public class Filter extends
		havis.middleware.ale.core.report.Filter<CCFilterSpec> {

	/**
	 * Creates a new instance. Keeps the specification
	 * 
	 * @param spec
	 *            The filter specification
	 * @throws ValidationException
	 *             If filter specification is invalid
	 */
	public Filter(CCFilterSpec spec) throws ValidationException {
		super(spec);
	}

	/**
	 * Retrieves a array of filter members
	 */
	@Override
	protected List<ECFilterListMember> getList() {
		if (spec.getFilterList() != null) {
			return spec.getFilterList().getFilter();
		} else {
			return new ArrayList<ECFilterListMember>();
		}
	}
}