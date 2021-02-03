package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Event;
import havis.middleware.ale.base.operation.Statistics;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.report.ReportConstants;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.stat.Count;
import havis.middleware.ale.core.stat.Reader;
import havis.middleware.ale.core.stat.ReaderNames;
import havis.middleware.ale.core.stat.SightingSignal;
import havis.middleware.ale.core.stat.Timestamps;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.service.pc.PCEventCountStat;
import havis.middleware.ale.service.pc.PCEventReport;
import havis.middleware.ale.service.pc.PCEventStat;
import havis.middleware.ale.service.pc.PCEventTimestampStat;
import havis.middleware.ale.service.pc.PCOpReport;
import havis.middleware.ale.service.pc.PCReport;
import havis.middleware.ale.service.pc.PCReport.EventReports;
import havis.middleware.ale.service.pc.PCReportSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This class is used to generate a report. It validates the name, filters,
 * groups and output specification. It provides the reader operation to get data
 * from tag
 */
public class Report {

	private PCReportSpec spec;
	private Filter filter;
	private List<Trigger> trigger;
	private List<Operation> operations;
	private Reader<PCEventStat, Statistics>[] stats;

	/**
	 * Retrieves the unique ID
	 */
	protected String guid = UUID.randomUUID().toString();

	/**
	 * Creates a report instance. Validates report name, filters, groups and
	 * output specification.
	 * 
	 * @param spec
	 *            The event cycle report specification
	 * @param flat
	 *            True if at least one logical reader is specified, false
	 *            otherwise
	 * @param callback
	 *            The callback
	 * @throws ValidationException
	 *             if name is not given, report set type is invalid or unknown,
	 *             filters, groups or output specification are invalid or if the
	 *             output specification contains unknown or invalid field
	 *             specifications
	 * @throws ImplementationException
	 */
	public Report(PCReportSpec spec, boolean flat, final Callback callback) throws ValidationException, ImplementationException {
		if (Name.isValid(spec.getName())) {
			try {
				// keep specification
				this.spec = spec;

				if ((spec.getTriggerList() == null) || (spec.getTriggerList().getTrigger().size() == 0)) {
					if (flat) {
						throw new ValidationException("Neither a logical reader nor a trigger specified");
					} else {
						try {
							// validate and keep filter
							this.filter = new Filter(spec.getFilterSpec());
						} catch (ValidationException e) {
							throw new ValidationException("Filter is invalid. " + e.getReason());
						}
					}
				} else {
					if (spec.getFilterSpec() == null) {
						this.trigger = new ArrayList<Trigger>();
						for (String uri : spec.getTriggerList().getTrigger()) {
							this.trigger.add(Trigger.getInstance(this.guid, uri, new Trigger.Callback() {
								@Override
								public boolean invoke(Trigger trigger) {
									callback.invoke(Report.this, trigger);
									return true;
								}
							}));
						}
					} else {
						throw new ValidationException("Filter must be omitted if triggers are specified");
					}
				}
				operations = spec.getOpSpecs() != null ? Operation.get(spec.getOpSpecs().getOpSpec()) : new ArrayList<Operation>();

				if (spec.getStatProfileNames() != null && spec.getStatProfileNames().getStatProfileName().size() > 0) {
					List<Reader<? extends PCEventStat, ? extends Statistics>> statList = new ArrayList<>();
					for (String profile : spec.getStatProfileNames().getStatProfileName()) {
						switch (profile) {
						case ReportConstants.EventTimestampsProfileName:
							statList.add(new Timestamps<PCEventTimestampStat>(profile, PCEventTimestampStat.class));
							break;
						case ReportConstants.EventCountProfileName:
							statList.add(new Count<PCEventCountStat>(profile, PCEventCountStat.class));
							break;
						case ReportConstants.ReaderNamesProfileName:
							if (this.filter != null) {
								statList.add(new ReaderNames<PCEventStat>(profile, PCEventStat.class));
							} else {
								throw new ValidationException("Unsupported statistic profile '" + profile + "' for trigger based port cycle.");
							}
							break;
						case ReportConstants.ReaderSightingSignalsProfileName:
							if (this.filter != null) {
								statList.add(new SightingSignal<PCEventStat>(profile, PCEventStat.class));
							} else {
								throw new ValidationException("Unsupported statistic profile '" + profile + "' for trigger based port cycle.");
							}
							break;
						default:
							throw new ValidationException("Unknown statistic profile name '" + profile + "'.");
						}
					}
					@SuppressWarnings("unchecked")
					Reader<PCEventStat, Statistics>[] statsArray = (Reader<PCEventStat, Statistics>[]) statList.toArray(new Reader<?, ?>[0]);
					this.stats = statsArray;
				}
			} catch (ValidationException | ImplementationException e) {
				dispose();
				e.setReason("Report '" + spec.getName() + "' is invalid. " + e.getReason());
				throw e;
			}
		}
	}

	/**
	 * Gets the report name
	 * 
	 * @return The specification name
	 */
	public String getName() {
		return spec.getName();
	}

	/**
	 * Gets the filter
	 */
	public Filter getFilter() {
		return filter;
	}

	/**
	 * Gets the event cycle report specification
	 */
	public PCReportSpec getSpec() {
		return spec;
	}

	/**
	 * Gets the operations
	 */
	public List<Operation> getOperations() {
		return operations;
	}

	/**
	 * Gets the stats
	 */
	public Reader<PCEventStat, Statistics>[] getStats() {
		return stats;
	}

	private boolean match(Event event) {
		if (filter instanceof Filter) {
			if (event.getTag() instanceof Tag) {
				return Boolean.TRUE.equals(filter.match(event.getTag()));
			}
		} else {
			if (event.getTag() == null) {
				for (Trigger trigger : this.trigger) {
					if (event.getUri().startsWith(trigger.getUri()))
						return true;
				}
			}
		}
		return false;
	}

	private List<PCOpReport> createReportsFor(Event event) {
		List<PCOpReport> list = new ArrayList<PCOpReport>();
		for (Operation operation : operations) {
			Result result = event.getResult().get(Integer.valueOf(operation.getPortOperation().getId()));
			list.add(operation.getReport(result));
		}
		return list;
	}

	boolean isCompleted(Event event) {
		if (match(event)) {
			for (Operation operation : operations) {
				if ((event.getResult() == null) || (!event.getResult().containsKey(Integer.valueOf(operation.getPortOperation().getId())))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns the complete report depending on given tags and report
	 * specification. Each tag has to pass the filters before it could be part
	 * of report. Tags are grouped and therefore could be reported twice If
	 * report specification request for additional fields, results will be
	 * decoded and include : report
	 * 
	 * @param set
	 *            The reader operation results
	 * @return The port cycle report
	 */
	public PCReport get(Set<Event> set) {
		// create new report
		PCReport report = new PCReport();
		// assign report name
		report.setReportName(spec.getName());

		if (set.size() > 0) {
			// create list container
			report.setEventReports(new EventReports());

			for (final Event event : set) {
				if (match(event)) {
					final List<PCEventStat> stats = new ArrayList<PCEventStat>();
					if (this.stats != null) {
						for (Reader<PCEventStat, Statistics> stat : this.stats) {
							switch (stat.getProfile()) {
							case ReportConstants.EventTimestampsProfileName:
							case ReportConstants.EventCountProfileName:
							case ReportConstants.ReaderNamesProfileName:
							case ReportConstants.ReaderSightingSignalsProfileName:
								stats.add(stat.getStat(event.getTag()));
								break;
							}
						}
					}
					report.getEventReports().getEventReport().add(new PCEventReport(event.getUri(), createReportsFor(event), stats));
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
		if (this.trigger != null) {
			for (Trigger trigger : this.trigger) {
				trigger.dispose();
			}
			this.trigger = null;
		}
		if (operations != null) {
			for (Operation operation : operations) {
				operation.dispose();
			}
		}
	}
}