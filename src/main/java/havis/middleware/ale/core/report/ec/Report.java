package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Statistics;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.Tag.Property;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.po.OID;
import havis.middleware.ale.base.report.ReportConstants;
import havis.middleware.ale.core.ISODecoder;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.VariableField;
import havis.middleware.ale.core.report.Group;
import havis.middleware.ale.core.stat.Count;
import havis.middleware.ale.core.stat.Reader;
import havis.middleware.ale.core.stat.ReaderNames;
import havis.middleware.ale.core.stat.SightingSignal;
import havis.middleware.ale.core.stat.Timestamps;
import havis.middleware.ale.service.EPC;
import havis.middleware.ale.service.ec.ECReport;
import havis.middleware.ale.service.ec.ECReportGroup;
import havis.middleware.ale.service.ec.ECReportGroupCount;
import havis.middleware.ale.service.ec.ECReportGroupList;
import havis.middleware.ale.service.ec.ECReportGroupListMember;
import havis.middleware.ale.service.ec.ECReportGroupListMemberExtension;
import havis.middleware.ale.service.ec.ECReportMemberField;
import havis.middleware.ale.service.ec.ECReportOutputFieldSpec;
import havis.middleware.ale.service.ec.ECReportOutputSpec;
import havis.middleware.ale.service.ec.ECReportSpec;
import havis.middleware.ale.service.ec.ECTagCountStat;
import havis.middleware.ale.service.ec.ECTagStat;
import havis.middleware.ale.service.ec.ECTagTimestampStat;
import havis.middleware.tdt.ItemData;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;
import havis.middleware.utils.data.Calculator;
import havis.middleware.utils.data.Converter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is used to generate a report as specified in ALE 1.1.1 (8.3.3). It
 * validates the name, filters, groups and output specification. It provides the
 * reader operation to get data from tag
 */
class Report {

	/**
	 * This enumeration specified the different report set types
	 */
	public enum Set {

		/**
		 * Report all filtered tags which was seen in the current and previous
		 * cycle
		 */
		CURRENT,

		/**
		 * Report all filtered tags which was seen in the current and not seen
		 * in the previous cycle
		 */
		ADDITIONS,

		/**
		 * Report all filtered tags which was not seen in the currnet but in the
		 * previous cycle
		 */
		DELETIONS
	}

	/**
	 * Retrieves the report set type
	 */
	public Set set;

	/**
	 * Retrieves the event cycle report specification
	 */
	ECReportSpec spec;

	Filter filter; // filter
	Group group; // group
	List<CommonField> fields;
	Reader<ECTagStat, Statistics>[] stats;

	/**
	 * Global hashCode for tag set comparison between event cycles
	 */
	int hashCode;

	/**
	 * Retrieves reader operations to request additional data for filtering,
	 * grouping and report output.
	 */
	List<Operation> operations;

	/**
	 * Creates a report instance. Validates report name, filters, groups and
	 * output specification.
	 *
	 * @param spec
	 *            The event cycle report specification
	 * @throws ValidationException
	 *             if name is not given, report set type is invalid or unknown,
	 *             filters, groups or output specification are invalid or if the
	 *             output specification contains unknown or invalid field
	 *             specifications
	 */
	Report(ECReportSpec spec) throws ValidationException {
		if (Name.isValid(spec.getReportName())) {
			// keep specification
			this.spec = spec;
			try {
				try {
					// validate and keep filter
					filter = new Filter(spec.getFilterSpec());
				} catch (ValidationException e) {
					e.setReason("Filter is invalid. " + e.getReason());
					throw e;
				}
				// validate and keep groups if group spec exist
				if (spec.getGroupSpec() != null) {
					try {
						group = new Group(spec.getGroupSpec());
					} catch (ValidationException e) {
						e.setReason("Group is invalid. " + e.getReason());
						throw e;
					}
				}
				if (spec.getOutput() == null) {
					throw new ValidationException("No output specified");
				} else {
					if (!spec.getOutput().isIncludeEPC()
							&& !spec.getOutput().isIncludeTag()
							&& !spec.getOutput().isIncludeRawDecimal()
							&& !spec.getOutput().isIncludeRawHex()
							&& !spec.getOutput().isIncludeCount()
							&& (spec.getOutput().getExtension() == null
									|| spec.getOutput().getExtension()
											.getFieldList() == null || spec
									.getOutput().getExtension().getFieldList()
									.getField().size() == 0)) {
						throw new ValidationException(
								"No output values specified");
					}

					operations = new ArrayList<Operation>();
					if ((spec.getOutput().getExtension() != null)
							&& (spec.getOutput().getExtension().getFieldList() != null)) {
						try {
							Fields.lock();
							fields = new ArrayList<CommonField>();
							List<String> names = new ArrayList<String>();
							for (ECReportOutputFieldSpec fieldSpec : spec
									.getOutput().getExtension().getFieldList()
									.getField()) {
								CommonField field;
								if ((field = Fields.getInstance().get(
										fieldSpec.getFieldspec())) == null) {
									throw new ValidationException("Field '"
											+ fieldSpec.getFieldspec()
													.getFieldname()
											+ "' is not defined");
								} else {
									String name = fieldSpec.getName();
									if (name == null)
										name = field.getName();
									if (names.contains(name)) {
										throw new ValidationException(
												"The output already contains the field '"
														+ name + "'");
									} else {
										names.add(name);
										field.inc();
										fields.add(field);
										operations.add(new Operation(0,
												OperationType.READ, field
														.getField()));
									}
								}
							}
						} finally {
							Fields.unlock();
						}
					}
					if (group != null) {
						Operation operation = group.getOperation();
						if (operation instanceof Operation)
							operations.add(operation);
					}
					operations.addAll(filter.getOperations());
				}
				if (spec.getReportSet() == null) {
					throw new ValidationException("No report set defined");
				} else {
					try {
						// determine type of report set
						set = Set.valueOf(spec.getReportSet().getSet());
					} catch (Exception e) {
						throw new ValidationException("Unknown report set '" + spec.getReportSet().getSet() + "'");
					}
				}
				if (spec.getExtension() != null) {
					if (spec.getExtension().getStatProfileNames() != null && spec.getExtension().getStatProfileNames().getStatProfileName().size() > 0) {
						List<Reader<? extends ECTagStat, ? extends Statistics>> statList = new ArrayList<>();
						for (String profile : spec.getExtension()
								.getStatProfileNames().getStatProfileName()) {
							switch (profile) {
							case ReportConstants.TagTimestampsProfileName:
							    statList.add(new Timestamps<ECTagTimestampStat>(
										profile, ECTagTimestampStat.class));
								break;
							case ReportConstants.TagCountProfileName:
							    statList.add(new Count<ECTagCountStat>(
										profile, ECTagCountStat.class));
								break;
							case ReportConstants.ReaderNamesProfileName:
							    statList.add(new ReaderNames<ECTagStat>(profile,
										ECTagStat.class));
								break;
							case ReportConstants.ReaderSightingSignalsProfileName:
							    statList.add(new SightingSignal<ECTagStat>(
										profile, ECTagStat.class));
								break;
							default:
								throw new ValidationException(
										"Unknown profile name '" + profile
												+ "'");
							}
						}
	                    @SuppressWarnings("unchecked")
                        Reader<ECTagStat, Statistics>[] statsArray = (Reader<ECTagStat, Statistics>[]) statList.toArray(new Reader<?, ?>[0]);
	                    this.stats = statsArray;
					}

				}
			} catch (ValidationException e) {
				dispose();
				e.setReason("Report specification '" + spec.getReportName()
						+ "' is invalid. " + e.getReason());
				throw e;
			}
		}
	}

	public List<Operation> getOperations() {
		return operations;
	}

	public Set getSet() {
		return set;
	}

	public ECReportSpec getSpec() {
		return spec;
	}

	public Filter getFilter() {
		return filter;
	}

	/**
	 * Retrieves the report name
	 *
	 * @return The report name
	 */
	public String getName() {
		return spec.getReportName();
	}

	/**
	 * Retrieves the fields within this report
	 *
	 * @return The fields
	 */
	public List<CommonField> getFields() {
		return fields;
	}

	ECReportGroup getGroup() {
		return getGroup(null);
	}

	/**
	 * Creates a new named group. Assigns group name. Assigns instance of group
	 * list depends on output specification. Assigns group member list instance.
	 *
	 * @param name
	 *            The group name or null for unnamed group
	 * @return The report group
	 */
	ECReportGroup getGroup(String name) {
		return new ECReportGroup(name,
				spec.getOutput().isIncludeCount() ? new ECReportGroupCount()
						: null, new ECReportGroupList());
	}

	/**
	 * Gets the complete report depending on given tags and report
	 * specification. Each tag has to pass the filters before it could be part
	 * of report. Tags are grouped and therefore could be reported twice If
	 * report specification request for additional fields, results will be
	 * decoded and include in report
	 *
	 * @param tags
	 *            The list of unfiltered tags to report
	 * @return The report
	 */
	ECReport get(List<Tag> tags) {
		// create new report
		ECReport report = new ECReport();
		// assign report name
		report.setReportName(spec.getReportName());

		// instantiate default group
		ECReportGroup ecReportGroup = getGroup();

		// create map for named groups if necessary
		Map<String, ECReportGroup> ecReportGroups = null;
		if (group != null)
			ecReportGroups = new LinkedHashMap<String, ECReportGroup>();

		ECReportOutputSpec output = spec.getOutput();

		if (tags.size() > 0) {
			for (Tag tag : tags) {
				if (Boolean.TRUE.equals(filter.match(tag))) {

					ECReportGroupListMember member = null;

					if (output.isIncludeEPC()
							|| output.isIncludeTag()
							|| output.isIncludeRawDecimal()
							|| output.isIncludeRawHex()
							|| (spec.getExtension() != null || output
									.getExtension() != null)) {
						member = new ECReportGroupListMember();

						// look for epc infos to include

						if (output.isIncludeEPC()) {
							String value = null;
							try {
								value = tag.<TdtTagInfo> getProperty(Property.TAG_INFO).getUriId();
							} catch (TdtTranslationException e) {
							}
							member.setEpc(new EPC(value));
						}
						if (output.isIncludeTag()) {
							String value = null;
							try {
								value = tag.<TdtTagInfo> getProperty(Property.TAG_INFO).getUriTag();
							} catch (TdtTranslationException e) {
							}
							member.setTag(new EPC(value));
						}
						if (output.isIncludeRawHex()) {
							String value = null;
							try {
								value = tag.<TdtTagInfo> getProperty(Property.TAG_INFO).getUriRawHex();
							} catch (TdtTranslationException e) {
							}
							member.setRawHex(new EPC(value));
						}
						if (output.isIncludeRawDecimal()) {
							int length = tag.<TdtTagInfo> getProperty(Property.TAG_INFO).isEpcGlobal() ? tag.<TdtTagInfo> getProperty(Property.TAG_INFO)
									.getLength() : tag.getLength() * 16;
							member.setRawDecimal(new EPC("urn:epc:raw:" + length + "." + Converter.toDecimalString(Calculator.strip(tag.getEpc(), 0, length))));
						}

						// member extensions
						if ((fields != null) || (this.stats != null)) {
							member.setExtension(new ECReportGroupListMemberExtension());

							// append field list
							if (fields != null) {
								List<ECReportMemberField> list = new ArrayList<ECReportMemberField>();
								if ((tag.getResult() != null) && (tag.getResult().size() > 0)) {
									int i = -1;
									for (final ECReportOutputFieldSpec fieldSpec : output.getExtension().getFieldList().getField()) {
										final CommonField field = fields.get(++i);
										final Result result = tag.getResult().get(Integer.valueOf(operations.get(i).getId()));
										if (result != null) {
    										if (field.getBase() instanceof VariableField) {
    											// variable field
    											boolean foundField = false;
    											if (result instanceof ReadResult && result.getState() == ResultState.SUCCESS && ((ReadResult) result).getData() != null) {
    												// decode item data
    												if (!tag.hasItemData(field.getBank())) {
    													tag.decodeItemData(field.getBank(), ((ReadResult) result).getData(), ISODecoder.getInstance());
    												}
    												ItemData itemData = tag.getItemData(field.getBank());

    												if (itemData != null) {
    													OID oid = ((VariableField) field.getBase()).getOID();
        												for (Entry<String, String> entry : itemData.getDataElements()) {
															if (oid.matches(entry.getKey())) {
																foundField = true;
																list.add(createMemberField(fieldSpec, field, "@" + field.getBank() + "." + entry.getKey(), entry.getValue()));
        													}
        												}
    												}
    											}

												if (!foundField) {
													// field was not found
													list.add(createMemberField(fieldSpec, field, null, null));
												}
    										} else {
    											// fixed field
    											list.add(createMemberField(fieldSpec, field, null, getMemberFieldValue(field, result)));
    										}
										}
									}
								}
                                member.getExtension().setECReportMemberFieldList(list);
							}

							// append statistic data
							if (this.stats != null) {
								List<ECTagStat> stats = new ArrayList<ECTagStat>();
								for (Reader<ECTagStat, Statistics> stat : this.stats) {
									switch (stat.getProfile()) {
									case ReportConstants.TagTimestampsProfileName:
									case ReportConstants.TagCountProfileName:
									case ReportConstants.ReaderNamesProfileName:
									case ReportConstants.ReaderSightingSignalsProfileName:
										stats.add(stat.getStat(tag));
										break;
									}
								}
								member.getExtension().setECTagStatList(stats);
							}
						}
					}

					List<String> name;
					// determine report group names if group definition is null
					// or returned count of group names is null add member to
					// default group, add member to named group otherwise
					if ((group == null) || (name = group.name(tag)).size() == 0) {
						// Add member to default group
						if (member instanceof ECReportGroupListMember)
							ecReportGroup.getGroupList().getMember()
									.add(member);
						// include count if requested
						if (output.isIncludeCount())
							ecReportGroup.getGroupCount()
									.setCount(
											ecReportGroup.getGroupCount()
													.getCount() + 1);
					} else if (ecReportGroups != null /*
													 * just to satisfy the
													 * compiler, won't ever be
													 * null here
													 */) {
						ECReportGroup g;
						// Add member to each matching group
						for (String n : name) {
							// create group if not exist
							if ((g = ecReportGroups.get(n)) == null) {
								g = getGroup(n);
								ecReportGroups.put(n, g);
							}
							// add member to named group
							if (member instanceof ECReportGroupListMember)
								g.getGroupList().getMember().add(member);
							// include count if requested
							if (output.isIncludeCount())
								g.getGroupCount().setCount(
										g.getGroupCount().getCount() + 1);
						}
					}
				}
			}
		}

        // Add default group if not empty
        if ((ecReportGroup.getGroupList().getMember().size() > 0) || (output.isIncludeCount() && (ecReportGroup.getGroupCount().getCount() > 0))) {
            if (ecReportGroup.getGroupList().getMember().size() == 0)
                ecReportGroup.setGroupList(null);
            report.getGroup().add(ecReportGroup);
        }

        // Add other groups if exists
        if (group != null && ecReportGroups != null /*
                                                     * just to satisfy the
                                                     * compiler, won't ever be
                                                     * null here
                                                     */) {
            // add each named group
            for (ECReportGroup g : ecReportGroups.values()) {
                if ((g.getGroupList().getMember().size() > 0) || (output.isIncludeCount() && (g.getGroupCount().getCount() > 0))) {
                    if (g.getGroupList().getMember().size() == 0)
                        g.setGroupList(null);
                    report.getGroup().add(g);
                }
            }
        }
		return report;
	}

	private String getMemberFieldValue(CommonField field, Result result) {
		if (result instanceof ReadResult && result.getState() == ResultState.SUCCESS && ((ReadResult) result).getData() != null) {
			return Fields.toString(field, ((ReadResult) result).getData());
		}
		return "";
	}

	private ECReportMemberField createMemberField(ECReportOutputFieldSpec fieldSpec, CommonField field, String name, String value) {
		ECReportMemberField memberField = new ECReportMemberField();
		memberField.setName(fieldSpec.getName() != null ? fieldSpec.getName() : (name != null ? name : field.getName()));
		memberField.setValue(value);
		memberField.setFieldspec(Boolean.TRUE.equals(fieldSpec.isIncludeFieldSpecInReport()) ? fieldSpec.getFieldspec() : null);
		return memberField;
	}

	/**
	 * Process whether the report has changed. Should only be called if reportOnlyOnChanged is set.
	 * @param tags the tags
	 * @return true if the report has changed, false otherwise
	 */
    public boolean processChanged(List<Tag> tags) {
        // reset local hashCode
        int hashCode = 1;
        for (Tag tag : tags) {
            if (Boolean.TRUE.equals(filter.match(tag))) {
                // building hashCode for comparison
                if (spec.isReportOnlyOnChange()) {
                    hashCode *= 31 + tag.hashCode();
                }
            }
        }
        // determine changes
        boolean changed = hashCode != this.hashCode;
        // set current hashCode global
        this.hashCode = hashCode;
        return changed;
    }

	boolean isCompleted(Tag tag) {
		for (Operation operation : operations) {
			Result result;
			// FIXME dead end?
			if ((tag.getResult() == null)
					|| ((result = tag.getResult().get(
							Integer.valueOf(operation.getId()))) == null)
					|| (result.getState() == ResultState.MISC_ERROR_TOTAL)
					|| (result.getState() == ResultState.MISC_ERROR_PARTIAL)) {
				return false;
			}
		}
		return true;
	}

	void dispose() {
		if (fields != null) {
			for (CommonField field : fields) {
				field.dec();
			}
			fields = null;
		}
		if (filter != null) {
			filter.dispose();
			filter = null;
		}
		if (group != null) {
			group.dispose();
			group = null;
		}
	}
}
