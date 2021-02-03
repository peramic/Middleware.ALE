package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.Tag.Property;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.misc.TdtWrapper;
import havis.middleware.tdt.EpcTagDataTranslationX;
import havis.middleware.tdt.LevelTypeList;
import havis.middleware.tdt.LevelX;
import havis.middleware.tdt.OptionX;
import havis.middleware.tdt.SchemeX;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;

/**
 * This class is used as a collection for a {@link Pattern} list. It provides a
 * method to find out if any pattern in list match or a method to get all group
 * names of matching patterns. It also provides method to got fields need for
 * filtering and grouping.
 */
public class Patterns {

	/**
	 * Retrieves the regular expression for pattern
	 */
	public static java.util.regex.Pattern PATTERN;
	static {
		reset();
	}

	public static void reset() {
		PATTERN = java.util.regex.Pattern.compile(Config.getInstance().getGlobal().getUrn().getPattern().trim());
	}

	private CommonField field; // field
	private Queue<IPattern> patterns; // pattern list

	/**
	 * Creates new patterns instance. Validates and keeps each pattern in list.
	 * Validates field specification.
	 *
	 * @param type
	 *            The patterns type
	 * @param list
	 *            A list of patterns
	 * @throws ValidationException
	 *             if pattern list is empty, any pattern is invalid or field
	 *             specification is invalid.
	 * @throws TdtTranslationException
	 */
	public Patterns(PatternType type, List<String> list)
			throws ValidationException {
		this(type, list, null);
	}

	/**
	 * Creates new patterns instance. Validates and keeps each pattern in list.
	 * Validates field specification.
	 *
	 * @param type
	 *            The patterns type
	 * @param list
	 *            A list of patterns
	 * @param spec
	 *            The optional field specification i.e. epc
	 * @throws ValidationException
	 *             if pattern list is empty, any pattern is invalid or field
	 *             specification is invalid.
	 * @throws TdtTranslationException
	 */
	public Patterns(PatternType type, List<String> list, ECFieldSpec spec)
			throws ValidationException {
		try {
			if ((list != null) && (list.size() > 0)) {
				// validate and keep pattern list
				if (spec != null) {
					try {
						Fields.lock();
						field = Fields.getInstance().get(spec);
						field.inc();
						operation = new Operation(0, OperationType.READ,
								field.getField());
					} finally {
						Fields.unlock();
					}
				}
				patterns = pattern(type, list, field);
			} else {
				throw new ValidationException(
						"Pattern list could not be null or empty");
			}
		} catch (ValidationException e) {
			dispose();
			throw e;
		}
	}

	/**
	 * Creates patterns by parsing list of strings
	 *
	 * @param type
	 *            The pattern type
	 * @param list
	 *            The pattern list
	 * @return Return array of pattern
	 * @throws TdtTranslationException
	 * @throws ValidationException
	 */
	public static Queue<IPattern> pattern(PatternType type, List<String> list)
			throws ValidationException {
		return pattern(type, list, null);
	}

	/**
	 * Creates patterns by parsing list of strings
	 *
	 * @param type
	 *            The pattern type
	 * @param list
	 *            The pattern list
	 * @param field
	 *            The field
	 * @return Return array of pattern
	 * @throws TdtTranslationException
	 * @throws ValidationException
	 */
	public static Queue<IPattern> pattern(PatternType type, List<String> list,
			CommonField field) throws ValidationException {
		Queue<IPattern> patterns = new ArrayDeque<IPattern>();
		if ((field == null) || (field.getFieldDatatype() == FieldDatatype.EPC)) {
			if (list != null) {
				for (String entry : list) {
					try {
						IPattern pattern = pattern(type, entry);
						if (pattern.disjoint(patterns)) {
							patterns.add(pattern);
						} else {
							throw new ValidationException("Pattern is not disjoint");
						}
					} catch (ValidationException e) {
						e.setReason("Pattern '" + entry + "' is invalid. " + e.getReason());
						throw e;
					}
				}
			}
		} else if (field.getFieldDatatype() == FieldDatatype.UINT) {
			if (field.getLength() > 64) {
				if (list != null) {
					for (String entry : list) {
						try {
							IPattern pattern = new BigIntPattern(type, entry, field);
							if (pattern.disjoint(patterns)) {
								patterns.add(pattern);
							} else {
								throw new ValidationException("Pattern is not disjoint");
							}
						} catch (ValidationException e) {
							e.setReason("Pattern '" + entry + "' is invalid. " + e.getReason());
							throw e;
						}
					}
				}
			} else {
				if (list != null) {
					for (String entry : list) {
						try {
							IPattern pattern = new UIntPattern(type, entry, field);
							if (pattern.disjoint(patterns)) {
								patterns.add(pattern);
							} else {
								throw new ValidationException("Pattern is not disjoint");
							}
						} catch (ALEException e) {
							e.setReason("Pattern '" + entry + "' is invalid. " + e.getReason());
							throw e;
						}
					}
				}
			}
		} else {
			throw new ValidationException(
					"No pattern is defined for datatype of field '"
							+ field.getName() + "'");
		}
		return patterns;
	}

	/**
	 * Returns a new pattern instance
	 *
	 * @param type
	 *            The pattern type
	 * @param urn
	 *            The pattern urn
	 * @return The pattern instance
	 * @throws ValidationException
	 * @throws TdtTranslationException
	 */
	public static Pattern pattern(PatternType type, String urn)
			throws ValidationException {
		Matcher match = PATTERN.matcher(urn);
		if (match.matches()) {
			// determine pattern type support
			switch (match.group("type")) {
			case "pat":
				return pattern(type, LevelTypeList.TAG_ENCODING, "urn:epc:tag:"
						+ match.group("scheme"), match.group("data"));
			case "idpat":
				return pattern(type, LevelTypeList.PURE_IDENTITY, "urn:epc:id:"
						+ match.group("scheme"), match.group("data"));
			default:
				return null;
			}
		} else {
			throw new ValidationException("Pattern type not supported");
		}
	}

	/**
	 * Creates a array of pattern by type, encoding, prefix and suffix
	 *
	 * @param type
	 *            The pattern type
	 * @param encoding
	 *            The TDT level type encoding
	 * @param prefix
	 *            The pattern prefix i.e. urn:epc:tag:sgtin-96
	 * @param suffix
	 *            The other position values
	 * @return Returns a array of patterns
	 * @throws ValidationException
	 * @throws TdtTranslationException
	 */
	public static Pattern pattern(PatternType type, LevelTypeList encoding,
			String prefix, String suffix) throws ValidationException {
		for (EpcTagDataTranslationX translation : TdtWrapper.getTdt().getTdtDefinitions().getDefinitions()) {
			for (SchemeX scheme : translation.getSchemes()) {
				for (LevelX level : scheme.getLevels()) {
					if (encoding == level.getType()) {
						if (prefix.equals(level.getPrefixMatch())) {
							if ((type == PatternType.CACHE)
									&& (level.getType() != LevelTypeList.TAG_ENCODING)) {
								throw new ValidationException(
										"Only tag pattern supported for cache pattern");
							}
							for (OptionX option : level.getOptions()) {
								// validate pattern and add to pattern list
								try {
									return new Pattern(type, encoding, prefix,
											suffix.split("\\."),
											option.getFields());
								} catch (ValidationException e) {
									if (e.getReason() != null)
										throw e;
								}
							}
							throw new ValidationException(
									"No matching pattern found");
						}
					}
				}
			}
		}
		throw new ValidationException("Wrong pattern format");
	}

	/**
	 * Retrieves the read field operation
	 */
	Operation operation;

	Result getResult(Map<Integer, Result> results) {
		if (operation == null) {
			return null;
		} else {
			Result result = results.get(Integer.valueOf(operation.getId()));
			return result;
		}
	}

	/**
	 * Indicates if urn match to one pattern
	 *
	 * @param urn
	 *            The tag urn
	 * @return True if one pattern match otherwise false
	 * @throws TdtTranslationException
	 */
	public Boolean match(String urn) {
		return match(TagDecoder.getInstance().fromUrn(urn));
	}

	/**
	 * Indicates if tag match to one pattern. use this for filtering.
	 *
	 * @param tag
	 *            The tag
	 * @return Returns true if tag match to one pattern in list or null if data
	 *         is incomplete
	 * @throws TdtTranslationException
	 */
	public Boolean match(Tag tag) {
		// call match for each pattern
		for (IPattern pattern : patterns) {
			Result result = getResult(tag.getResult());
			// check if all needed data for matching are available
			if ((result != null) && (result.getState() != ResultState.SUCCESS)) {
				return null;
			} else {
				// abort if pattern match
				if (pattern.match(tag.<TdtTagInfo>getProperty(Property.TAG_INFO), result))
					return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * Returns a list of group names for all matching pattern
	 *
	 * @param urn
	 *            The tag urn
	 * @return A list of group names
	 * @throws TdtTranslationException
	 */
	public List<String> name(String urn) {
		return name(TagDecoder.getInstance().fromUrn(urn));
	}

	/**
	 * Returns a list of group names for all matching pattern. Use this for
	 * grouping.
	 *
	 * @param tag
	 *            The tag
	 * @return A list of group names
	 * @throws TdtTranslationException
	 */
	public List<String> name(Tag tag) {
		// create name list instance
		List<String> name = new ArrayList<String>();
		// check if tag was translated
		if (tag.<TdtTagInfo>getProperty(Property.TAG_INFO) != null) {
			for (IPattern pattern : patterns) {
				// add name to name list if pattern match
				String n = pattern.name(tag.<TdtTagInfo>getProperty(Property.TAG_INFO),
						getResult(tag.getResult()));
				if (n != null)
					name.add(n);
			}
		}
		// return name list
		return name;
	}

	public IPattern next() {
		synchronized (this) {
			if (patterns.size() > 0) {
				return patterns.poll();
			} else {
				return null;
			}
		}
	}

	public void append(List<String> list) throws ValidationException {
		List<IPattern> patterns = new ArrayList<IPattern>();
		for (IPattern pattern : pattern(PatternType.CACHE, list)) {
			patterns.add(pattern);
		}
		synchronized (this) {
			for (IPattern pattern : patterns) {
				this.patterns.add(pattern);
			}
		}
	}

	public List<String> toList() {
		List<String> list = new ArrayList<String>();
		synchronized (this) {
			for (IPattern pattern : patterns) {
				String s = pattern.toString();
				if (s != null)
					list.add(s);
			}
		}
		return list;
	}

	public void clear() {
		patterns.clear();
	}

	/**
	 * Disposes instance
	 */
	public void dispose() {
		synchronized (this) {
			if (patterns != null) {
				patterns.clear();
			}
			if (field != null) {
				field.dec();
				field = null;
			}
		}
	}

	public Operation getOperation() {
		return operation;
	}
}
