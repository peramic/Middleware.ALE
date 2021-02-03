package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.tdt.FieldX;
import havis.middleware.tdt.LevelTypeList;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;
import havis.middleware.utils.data.Converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to bundle a set of pattern position to a coherent unit.
 * Here each position of pattern will be validate. This class can handle
 * EPC-pure and EPC-tag pattern, integer pattern will be implemented soon
 */
public class Pattern implements IPattern {

	Logger log = Logger.getLogger(Pattern.class.getName());

	LevelTypeList level;
	String prefix;
	List<Part> parts;
	boolean init;

	/**
	 * Creates pattern instance. Validate each part within pattern.
	 *
	 * @param type
	 *            The pattern type
	 * @param level
	 *            The level within the TDT
	 * @param prefix
	 *            The pattern prefix i.e 'urn:epc:tag:sgtin-96:'
	 * @param suffixes
	 *            Each other position data as specified in part type
	 * @param fields
	 *            The TDT field definition to validate each position
	 * @throws ValidationException
	 *             if a specific value attached to a wildcard, group is given in
	 *             filter pattern type, a range is given by a pure id pattern or
	 *             the part value is invalid by TDT field definition.
	 */
	public Pattern(PatternType type, LevelTypeList level, String prefix,
			String[] suffixes, Collection<FieldX> fields)
			throws ValidationException {
		// keep type
		this.level = level;
		// keep encoding
		this.prefix = prefix;
		// validate parts
		parts = createParts(type, suffixes, fields);
	}

	/**
	 * Creates part list
	 *
	 * @param type
	 *            The pattern type
	 * @param suffixes
	 *            each other position data
	 * @param fields
	 *            The TDT fields
	 * @return A array of part instances
	 * @throws ValidationException
	 */
	List<Part> createParts(PatternType type, String[] suffixes,
			Collection<FieldX> fields) throws ValidationException {
		if (suffixes.length == fields.size()) {
			List<Part> list = new ArrayList<Part>();
			boolean isRange = false;
			Iterator<FieldX> i = fields.iterator();
			for (String suffix : suffixes) {
				FieldX field = i.next();
				Part part;
				if ((type == PatternType.CACHE) && (suffix.equals("*"))) {
					if ((field.getDecimalMinimum() != null)
							&& (field.getDecimalMaximum() != null)) {
						part = new Part(
								"["
										+ String.format(
												"%"
														+ (field.getLength() == null ? ""
																: field.getLength())
														+ "d",
												Long.valueOf(Long.parseLong(field.getDecimalMinimum())))
												.replace(' ',
														field.getPadChar())
										+ "-" + field.getDecimalMaximum() + "]",
								field);
					} else {
						throw new ValidationException(
								"Range not supported for field '"
										+ field.getName() + "'");
					}
				} else {
					try {
						part = new Part(suffix, field);
					} catch (ValidationException e) {
						// TODO: really ignore the message?
						throw new ValidationException(null);
					}
				}
				if ((type == PatternType.CACHE) && part.isRange() && isRange) {
					throw new ValidationException(
							"EPC cache pattern already contains a range or wildcard part");
				}
				if ((list.size() > 0)
						&& (list.get(list.size() - 1).isWildcard() || list.get(
								list.size() - 1).isRange())
						&& !(part.isWildcard() || part.isRange())) {
					throw new ValidationException(
							"Specific values are not allowed after a wildcard or range");
				} else if (((type == PatternType.FILTER) || (type == PatternType.CACHE))
						&& (part.isGroup())) {
					throw new ValidationException(
							"Can not group in filter or cache pattern");
				} else if ((level == LevelTypeList.PURE_IDENTITY)
						&& (part.isRange())) {
					throw new ValidationException(
							"A range is forbidden for pure identity pattern");
				} else {
					if (part.isRange())
						isRange = true;
					list.add(part);
				}
			}
			return list;
		} else {
			throw new ValidationException("Pattern has wrong number of fields");
		}
	}

	/**
	 * Indicates if {@link TdtTagInfo} object match this pattern.
	 *
	 * @param info
	 *            The TDT tag info object which contains the URIid or URItag
	 * @param result
	 *            The read result
	 * @return Returns true if pattern match otherwise false
	 * @throws TdtTranslationException
	 */
	@Override
	public boolean match(TdtTagInfo info, Result result) {
		try {
			if (info != null) {
				switch (level) {
				case PURE_IDENTITY: {
					String uri = info.getUriId();
					int index = uri.lastIndexOf(':');
					return match(uri.substring(0, index),
							uri.substring(index + 1).split("\\."));
				}
				case TAG_ENCODING: {
					String uri = info.getUriTag();
					int index = uri.lastIndexOf(':');
					return match(uri.substring(0, index),
							uri.substring(index + 1).split("\\."));
				}
				default:
					break;
				}
			}
		} catch (TdtTranslationException e) {
			log.log(Level.WARNING, "Failed to decode tag", e);
		}
		return false;
	}

	/**
	 * Indicates if the prefix and parts of a tag match on pattern
	 *
	 * @param prefix
	 *            The tag prefix
	 * @param parts
	 *            The other position values
	 * @return Returns true if prefix and all parts matched
	 */
	public boolean match(String prefix, String[] parts) {
		// compare type
		if (prefix.equals(this.prefix)) {
			// compare parts count
			if (this.parts.size() == parts.length) {
				// test match of each part
				int i = 0;
				for (Part part : this.parts) {
					if (!part.match(parts[i]))
						return false;
					i++;
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the group name depending on pattern an TDT tag info value.
	 *
	 * @param info
	 *            The TDT tag info value
	 * @param result
	 *            The read result
	 * @return Returns the group name or null if pattern does not match
	 * @throws TdtTranslationException
	 */
	@Override
	public String name(TdtTagInfo info, Result result) {
		try {
			switch (level) {
			case PURE_IDENTITY: {
				String uri = info.getUriId();
				int index = uri.lastIndexOf(':');
				String name = name(uri.substring(0, index),
						uri.substring(index + 1).split("\\."));
				if (name == null)
					return null;
				else
					return this.prefix.replace(":id:", ":idpat:") + ":" + name;
			}
			case TAG_ENCODING: {
				String uri = info.getUriTag();
				int index = uri.lastIndexOf(':');
				String name = name(uri.substring(0, index),
						uri.substring(index + 1).split("\\."));
				if (name == null)
					return null;
				else
					return this.prefix.replace(":tag:", ":pat:") + ":" + name;
			}
			default:
				break;
			}
		} catch (TdtTranslationException e) {
			log.log(Level.WARNING, "Failed to decode tag", e);
		}
		return null;
	}

	/**
	 * Returns the group name depending on encoding type and other positions.
	 *
	 * @param enc
	 *            The tag encoding type i.e. sgtin-96
	 * @param parts
	 *            The other positions
	 * @return Returns the group name or null if pattern does not match
	 */
	String name(String enc, String[] parts) {
		// compare type
		if (this.prefix != null && this.prefix.equals(enc)) {
			// compare parts count
			if (parts.length == this.parts.size()) {
				// prepare name array
				String[] name = new String[this.parts.size()];
				// test match of each part, keep part name
				int i = 0;
				for (Part part : this.parts) {
					// keep part name
					name[i] = part.name(parts[i]);
					// abort if one part does not match
					if (name[i] == null)
						return null;
					i++;
				}
				// join part names
				return Converter.join(name, '.');
			}
		}
		return null;
	}

	/**
	 * Returns the next EPC tag from cache or null if there is no more tag in
	 * cache
	 *
	 * @return The EPC tag
	 */
	@Override
	public String next() {
		if (!init) {
			// initialize parts (except the last one)
			for (int i = 0; i < parts.size() - 1; i++) {
				parts.get(i).next();
			}
			init = true;
		}
		// determine current value for each part
		String[] name = new String[this.parts.size()];
		for (int i = 0; i < parts.size(); i++) {
			name[i] = parts.get(i).getCurrent();
		}
		// start at the end
		for (int i = parts.size() - 1; i < parts.size(); i++) {
			if (parts.get(i).next())
				name[i] = parts.get(i).getCurrent();
			else {
				// reset part
				parts.get(i).reset();
				// decrease index
				if (i > 0) {
					i -= 2;
				}
				else {
					return null;
				}
			}
		}
		return prefix + ":" + Converter.join(name, '.');
	}

	/**
	 * Returns string representation of pattern
	 *
	 * @return The string representation of pattern
	 */
	@Override
	public String toString() {
		String[] s = new String[this.parts.size()];
		boolean next = false;
		for (int i = parts.size() - 1; i > -1; i--) {
			s[i] = parts.get(i).toString();
			if (s[i] == null)
				return null;
			next |= parts.get(i).hasNext();
		}
		if (next) {
			return prefix.replace(":tag:", ":pat:") + ":"
					+ Converter.join(s, '.');
		} else {
			return null;
		}
	}

	/**
	 * Returns if prefix and parts are disjoint from local values
	 *
	 * @param prefix
	 *            The prefix
	 * @param parts
	 *            The parts
	 * @return True if prefix and parts are disjoint, false otherwise
	 */
	boolean disjoint(String prefix, List<Part> parts) {
		if (prefix.equals(this.prefix)) {
			if (this.parts.size() == parts.size()) {
				Iterator<Part> i = parts.iterator();
				for (Part part : this.parts) {
					if (part.disjoint(i.next()))
						return true;
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns if patterns are disjoint from local values
	 *
	 * @param patterns
	 *            The patterns
	 * @return Returns true if all patterns are dis-join, false otherwise
	 */
	@Override
	public boolean disjoint(Iterable<IPattern> patterns) {
		for (IPattern pattern : patterns) {
			if (pattern instanceof Pattern) {
				if (!((Pattern) pattern).disjoint(prefix, parts))
					return false;
			}
		}
		return true;
	}
}
