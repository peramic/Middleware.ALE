package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Statistics;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.Tag.Property;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.operation.tag.result.VirtualReadResult;
import havis.middleware.ale.base.report.ReportConstants;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.core.stat.Count;
import havis.middleware.ale.core.stat.Reader;
import havis.middleware.ale.core.stat.ReaderNames;
import havis.middleware.ale.core.stat.SightingSignal;
import havis.middleware.ale.core.stat.Timestamps;
import havis.middleware.ale.service.cc.CCCmdReport;
import havis.middleware.ale.service.cc.CCCmdReport.TagReports;
import havis.middleware.ale.service.cc.CCCmdSpec;
import havis.middleware.ale.service.cc.CCOpReport;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.ale.service.cc.CCTagCountStat;
import havis.middleware.ale.service.cc.CCTagReport;
import havis.middleware.ale.service.cc.CCTagStat;
import havis.middleware.ale.service.cc.CCTagTimestampStat;
import havis.middleware.tdt.TdtTagInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will be used to generate a single report as specified:ALE 1.1.1
 * (9.4.3). Currently it only validates the name, filters and operations.
 */
public class Report {

	/**
	 * Retrieves the event cycle report specification
	 */
	private CCCmdSpec spec;

	/**
	 * Retrieves the filter
	 */
	private Filter filter;

	/**
	 * Retrieves the operations
	 */
	private CCOperation[] operations;

	/**
	 * Retrieves the stats
	 */
	private Reader<CCTagStat, Statistics>[] stats;

	/**
	 * Creates a new instance. Validates the name. Keeps the specification.
	 * Keeps a newly created instance of <see cref="Filter"/> and <see
	 * cref="Operations"/>
	 * 
	 * @param spec
	 *            The command cycle command specification
	 * @param parameters
	 *            The parameters
	 * @throws ValidationException
	 *             if name is not specified or if validation of filter or
	 *             operation failed.
	 */
	public Report(CCCmdSpec spec, Parameters parameters) throws ValidationException {
		if (Name.isValid(spec.getName())) {
			// keep spec
			this.spec = spec;
			try {
				try {
					// validate and keep filter
					filter = new Filter(spec.getFilterSpec());
				} catch (ValidationException e) {
					e.setReason("Filter is invalid. " + e.getReason());
					throw e;
				}
				operations = getOperations(spec.getOpSpecs() != null ? spec.getOpSpecs().getOpSpec() : new ArrayList<CCOpSpec>(), parameters);
				if (spec.getStatProfileNames() != null && spec.getStatProfileNames().getStatProfileName().size() > 0) {
					List<Reader<? extends CCTagStat, ? extends Statistics>> statList = new ArrayList<>();
					for (String profile : spec.getStatProfileNames().getStatProfileName()) {
						switch (profile) {
						case ReportConstants.TagTimestampsProfileName:
							statList.add(new Timestamps<CCTagTimestampStat>(profile, CCTagTimestampStat.class));
							break;
						case ReportConstants.TagCountProfileName:
							statList.add(new Count<CCTagCountStat>(profile, CCTagCountStat.class));
							break;
						case ReportConstants.ReaderNamesProfileName:
							statList.add(new ReaderNames<CCTagStat>(profile, CCTagStat.class));
							break;
						case ReportConstants.ReaderSightingSignalsProfileName:
							statList.add(new SightingSignal<CCTagStat>(profile, CCTagStat.class));
							break;
						default:
							throw new ValidationException("Unknown statistic profile name '" + profile + "'.");
						}
					}
					@SuppressWarnings("unchecked")
					Reader<CCTagStat, Statistics>[] statsArray = (Reader<CCTagStat, Statistics>[]) statList.toArray(new Reader<?, ?>[0]);
					this.stats = statsArray;
				}
			} catch (ValidationException e) {
				dispose();
				e.setReason("Command specification '" + spec.getName() + "' is invalid. " + e.getReason());
				throw e;
			}
		}
	}

	/**
	 * @return the spec
	 */
	public CCCmdSpec getSpec() {
		return spec;
	}

	/**
	 * @return the filter
	 */
	public Filter getFilter() {
		return filter;
	}

	/**
	 * @return the operations
	 */
	public CCOperation[] getOperations() {
		return operations;
	}

	/**
	 * @return the stats
	 */
	public Reader<CCTagStat, Statistics>[] getStats() {
		return stats;
	}

	/**
	 * @return the name of the specification
	 */
	public String getName() {
		return spec.getName();
	}

	CCOperation[] getOperations(List<CCOpSpec> specs, Parameters parameters) throws ValidationException {
		List<CCOperation> operations = new ArrayList<CCOperation>();
		try {
			List<String> names = new ArrayList<String>();
			for (CCOpSpec spec : specs) {
				String name = spec.getOpName();
				if (name != null) {
					if (names.contains(name)) {
						throw new ValidationException("Command specification already defines a operation named '" + name + "'");
					}
					names.add(name);
				}
				operations.add(CCOperation.get(spec, parameters));
			}
			return operations.toArray(new CCOperation[0]);
		} catch (ValidationException e) {
			for (CCOperation operation : operations) {
				operation.dispose();
			}
			throw e;
		}
	}

	void getOperations(Tag tag, List<havis.middleware.ale.base.operation.tag.Operation> tagOperations) {
		boolean error = false;
		for (int i = 0; i < this.operations.length; i++) {
			CCOperation operation = this.operations[i];
			if (error) {
				tag.getResult().put(Integer.valueOf(operation.getId()), new FaultResult(ResultState.MISC_ERROR_TOTAL));
			} else {
				ByRef<Result> result = new ByRef<Result>(tag.getResult().get(Integer.valueOf(operation.getId())));
				havis.middleware.ale.base.operation.tag.Operation o = operation.get(tag, result, tagOperations);
				if (result.getValue() instanceof FaultResult) {
					// an error occurs
					error = true;
					tag.getResult().put(Integer.valueOf(operation.getId()), result.getValue());
				} else if (result.getValue() instanceof VirtualReadResult) {
					// set intermediate read result for all following equal read operations
					Operation current = operation.getBase();
					for (int j = i + 1; j < this.operations.length; j++) {
						CCOperation op = this.operations[j];
						if (op.isAdvanced()) {
							Operation readOp = op.getBase();
							if (readOp != null && readOp.getType() == OperationType.READ
									&& readOp.getField() != null && current.getField() != null
									&& readOp.getField().getBank() == current.getField().getBank()
									&& readOp.getField().getOffset() == current.getField().getOffset()
									&& readOp.getField().getLength() == current.getField().getLength()) {
								tag.getResult().put(Integer.valueOf(op.getId()), result.getValue());
							}
						}
					}
				}
				if (o != null) {
					tagOperations.add(o);
				}
			}
		}
	}

	boolean isCompleted(Tag tag) {
		for (CCOperation operation : operations) {
			Result result;
			if ((tag.getResult() == null) || ((result = tag.getResult().get(Integer.valueOf(operation.getId()))) == null)
					|| (!operation.isCompleted(tag, result))) {
				return false;
			}
		}
		return true;
	}

	List<CCOpReport> getOperationReport(Tag tag) {
		List<CCOpReport> list = new ArrayList<CCOpReport>();
		for (CCOperation operation : operations) {
			Result result = tag.getResult().get(Integer.valueOf(operation.getId()));
			list.add(operation.getReport(tag, result));
		}
		return list;
	}

	/**
	 * Returns the complete report depending on given tags and report
	 * specification. Each tag has to pass the filters before it could be part
	 * of report. Tags are grouped and therefore could be reported twice If
	 * report specification request for additional fields, results will be
	 * decoded and include:report
	 * 
	 * @param tags
	 *            The list of unfiltered tags to report
	 * @return the report
	 * @throws ValidationException
	 */
	public CCCmdReport get(List<Tag> tags) {
		// create new report
		CCCmdReport report = new CCCmdReport();
		report.setTagReports(new TagReports());
		// assign report name
		report.setCmdSpecName(spec.getName());

		for (final Tag tag : tags) {
			if (tag != null) {
				if (tag.<TdtTagInfo>getProperty(Property.TAG_INFO) != null) {
					if (Boolean.TRUE.equals(filter.match(tag))) {
						final List<CCTagStat> stats = new ArrayList<CCTagStat>();
						if (this.stats != null) {
							for (Reader<CCTagStat, Statistics> stat : this.stats) {
								switch (stat.getProfile()) {
								case ReportConstants.TagTimestampsProfileName:
								case ReportConstants.TagCountProfileName:
								case ReportConstants.ReaderNamesProfileName:
								case ReportConstants.ReaderSightingSignalsProfileName:
									stats.add(stat.getStat(tag));
									break;
								}
							}
						}

						report.getTagReports().getTagReport().add(new CCTagReport(tag.toString(), getOperationReport(tag), stats));
					}
				}
			}
		}
		return report;
	}

	void dispose() {
		if (filter != null) {
			filter.dispose();
			filter = null;
		}
		if (operations != null) {
			for (CCOperation operation : operations) {
				operation.dispose();
			}
			operations = null;
		}
	}
}