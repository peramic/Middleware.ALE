package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.pc.PCFilterSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to make filters usable for the port cycle
 */
public class Filter extends
		havis.middleware.ale.core.report.Filter<PCFilterSpec> {

	/**
	 * Creates a new instance. Keeps the specification
	 *
	 * @param spec
	 *            The specification
	 * @throws ValidationException
	 *             If validation failed
	 */
	public Filter(PCFilterSpec spec) throws ValidationException {
		super(spec);
	}

	/**
	 * Gets an array of filter members
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