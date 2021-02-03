package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.tdt.FieldX;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used for a single position within a filter or group pattern. It
 * allows a position to be a {@link PartType#X}, {@link PartType#ASTERISK},
 * {@link PartType#VALUE} or {@link PartType#RANGE}. It validates the position
 * depending on field definition from the TDT package.
 */
class Part {

	static Pattern RANGE = Pattern
			.compile("^\\[(?<min>\\d+)\\-(?<max>\\d+)\\]$");

	PartType type;
	String value;
	long min, max;
	long current;
	Boolean active;
	char pad;
	int length;

	/**
	 * Creates a instance. Validates if value is a part type. If value is a
	 * value or a range this constructor checks if the value or the minimum or
	 * maximum are between the allowed interval
	 *
	 * @param value
	 *            The type part as string
	 * @param field
	 *            the TDT field description
	 * @throws ValidationException
	 *             if pattern type is invalid, values not permit or outside the
	 *             interval
	 */
	Part(String value, FieldX field) throws ValidationException {
		switch (value) {
		case "X":
			type = PartType.X;
			break;
		case "*":
			type = PartType.ASTERISK;
			break;
		default:
			Matcher match = RANGE.matcher(value);
			if (match.matches()) {
				type = PartType.RANGE;
				if ((field.getDecimalMinimum() == null)
						|| (field.getDecimalMaximum() == null)) {
					throw new ValidationException(
							"Part '"
									+ value
									+ "' could not have a range on field without decimal minumum or maximum");
				} else {
					String min = match.group("min"), max = match.group("max");
					if (field.getLength() != null) {
						length = field.getLength().intValue();
						if ((min.length() != length)
								|| (max.length() != length)) {
							throw new ValidationException("Part '" + value
									+ "' has an inapplicable length '" + length
									+ "'");
						}
						pad = field.getPadChar();
					}
					this.min = Long.parseLong(min);
					this.max = Long.parseLong(max);
					if ((this.min < Long.parseLong(field.getDecimalMinimum()))
							|| (this.min > this.max)
							|| (this.max > field.getMaximum().longValue())) {
						throw new ValidationException("Part '" + value
								+ "' is outside the decimal minumum '"
								+ this.min + "' or maximum '" + this.max + "'");
					}
				}
			} else {
				type = PartType.VALUE;
				if (field.getLength() != null) {
					int length = field.getLength().intValue();
					if (value.length() != length) {
						throw new ValidationException("Part '" + value
								+ "' has an inapplicable length '" + length
								+ "'");
					}
				}
				if ((field.getDecimalMinimum() == null)
						|| (field.getDecimalMaximum() == null)) {
					if (!Pattern.matches("^" + field.getCharacterSet() + "$",
							value)) {
						throw new ValidationException("Part '" + value
								+ "'  does not match character set '"
								+ field.getCharacterSet() + "'");
					}
				} else {
					String set = field.getCharacterSet();
                    if (Pattern.matches("^" + set + (set.charAt(set.length() - 1) == '*' ? "" : "+") + "$", value)) {
						long num = Long.parseLong(value), min = field
								.getMinimum().longValue(), max = field.getMaximum().longValue();
						if ((num < min) || (num > max)) {
							throw new ValidationException("Part '" + value
									+ "' is outside the decimal minimum '"
									+ min + "' or maximum '" + max + "'");
						}
					} else {
						throw new ValidationException("Part '" + value
								+ "'  does not match character set '" + set
								+ "'");
					}
				}
				this.value = value;
			}
			break;
		}
	}

	/**
	 * Indicates if part type is a wild card like {@link PartType#X} or
	 * {@link PartType#ASTERISK}.
	 *
	 * @return True, if is wild card
	 */
	boolean isWildcard() {
		return (type == PartType.X) || (type == PartType.ASTERISK);
	}

	/**
	 * Indicates if part type is a range like {@link PartType#RANGE}.
	 *
	 * @return True, if is range
	 */
	boolean isRange() {
		return type == PartType.RANGE;
	}

	/**
	 * Indicates if part type is a group like {@link PartType#X}.
	 *
	 * @return True, if is group
	 */
	boolean isGroup() {
		return type == PartType.X;
	}

	/**
	 * Tests if given value match to type an the internal characteristics
	 *
	 * @param value
	 *            The testing value
	 * @return Returns true if part match otherwise false
	 */
	boolean match(String value) {
		switch (type) {
		case X:
			return true;
		case ASTERISK:
			return true;
		case VALUE:
			return this.value.equals(value);
		case RANGE:
			long num = Long.parseLong(value);
			return ((num >= min) && (num <= max));
		default:
			return false;
		}
	}

	/**
	 * Returns the group name for this position depending an part type
	 *
	 * @param value
	 *            The position value
	 * @return Returns the name of the position
	 */
	String name(String value) {
		switch (type) {
		case X:
			return value;
		case ASTERISK:
			return "*";
		case VALUE:
			return this.value.equals(value) ? value : null;
		case RANGE:
			long num = Long.parseLong(value);
			return ((num >= min) && (num <= max)) ? "[" + min + "-" + max + "]"
					: null;
		default:
			return null;
		}
	}

	/**
	 * Increases current within min and max interval. Sets current to min if
	 * undefined or to undefined if greater then max.
	 *
	 * @return True, if next exists
	 */
	boolean next() {
		if (active == null) {
			active = Boolean.TRUE;
			return (type == PartType.VALUE) || ((current = min) <= max);
		} else {
			if (active.booleanValue()) {
				return ((type == PartType.VALUE) && ((active = Boolean.FALSE).booleanValue() == true) /* sets false and returns false (will never be true) */)
						|| (current++ < max) || ((active = Boolean.FALSE).booleanValue() == true) /* sets false and returns false (will never be true) */;
			} else {
				return false;
			}
		}
	}

	/**
	 * Retrieves the current value. Make sure that next is true before using
	 * current.
	 *
	 * @return The current string or null if not active
	 */
	String getCurrent() {
		if (Boolean.TRUE.equals(active)) {
			if (type == PartType.VALUE) {
				return value;
			} else {
				return String.format("%" + (length > 0 ? Integer.toString(length) : "") + "d", Long.valueOf(current)).replace(' ',
						pad);
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns if part has next value
	 *
	 * @return True if part has next value, false otherwise
	 */
	boolean hasNext() {
		switch (type) {
		case VALUE:
			return active == null;
		default:
			return (active == null) || (active.booleanValue() == true) && (current < max);
		}
	}

	/**
	 * Resets enumerator
	 */
	void reset() {
		active = null;
	}

	/**
	 * Returns current part as string
	 *
	 * @return Current part as string
	 */
	@Override
    public String toString() {
		switch (type) {
		case VALUE:
			return active == null || active.booleanValue() == true ? value : null;
		case RANGE:
			if (active != null) {
				if (active.booleanValue()) {
					return current + 1 < max ? "["
							+ String.format("%" + (length > 0 ? Integer.toString(length) : "") + "d", Long.valueOf(current + 1))
									.replace(' ', pad)
							+ "-"
							+ String.format("%" + (length > 0 ? Integer.toString(length) : "") + "d", Long.valueOf(max)).replace(
									' ', pad) + "]" : String.format(
							"%" + (length > 0 ? Integer.toString(length) : "") + "d", Long.valueOf(max)).replace(' ', pad);
				} else {
					return null;
				}
			} else {
				return "["
						+ String.format("%" + (length > 0 ? Integer.toString(length) : "") + "d", Long.valueOf(min)).replace(' ',
								pad)
						+ "-"
						+ String.format("%" + (length > 0 ? Integer.toString(length) : "") + "d", Long.valueOf(max)).replace(' ',
								pad) + "]";
			}
		default:
			return null;
		}
	}

	/**
	 * Returns if part is disjoint
	 *
	 * @param part
	 *            The part
	 * @return True if part is disjoint, false otherwise
	 */
	boolean disjoint(Part part) {
		if (!isWildcard() && !part.isWildcard()) {
			if (isRange()) {
				if (part.isRange()) {
					if (min >= part.min) {
						if (min > part.max) {
							return true;
						}
					} else {
						if (max < part.min) {
							return true;
						}
					}
				} else {
					long value = Long.parseLong(part.value);
					if ((value < min) || (value > max))
						return true;
				}
			} else {
				if (part.isRange()) {
					long value = Long.parseLong(this.value);
					if ((value < part.min) || (value > part.max))
						return true;
				} else {
					return !value.equals(part.value);
				}
			}
		}
		return false;
	}
}
