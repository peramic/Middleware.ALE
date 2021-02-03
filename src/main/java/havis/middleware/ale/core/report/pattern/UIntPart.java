package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.field.FieldFormat;

import java.util.regex.Matcher;

/**
 * This class is used for a single position within a filter or group pattern. It
 * allows a position to be a X, ASTERISK, VALUE or RANGE. It validates the
 * position depending on field definition from the TDT package.
 */
public class UIntPart {

	/**
	 * Defines the part types
	 */
	enum PartType {

		/**
		 * Indicates a grouping pattern to create a single group for each
		 * different value
		 */
		X,

		/**
		 * Includes each value
		 */
		ASTERISK,

		/**
		 * Includes only values
		 */
		VALUE,

		/**
		 * Includes only numbers which are is in range
		 */
		RANGE,

		/**
		 * Includes only values that bitwise match to mask
		 */
		MASK
	}

	static java.util.regex.Pattern HEX_RANGE = java.util.regex.Pattern
			.compile("^\\[x(?<min>[\\da-fA-F]+)\\-x(?<max>[\\da-fA-F]+)\\]$");
	static java.util.regex.Pattern DEC_RANGE = java.util.regex.Pattern
			.compile("^\\[(?<min>\\d+)\\-(?<max>\\d+)\\]$");
	static java.util.regex.Pattern HEX_MASK = java.util.regex.Pattern
			.compile("^&x(?<mask>[\\da-fA-F]+)=x(?<value>[\\da-fA-F]+)$");

	PartType type;
	FieldFormat format;

	/**
	 * The left bound of the interval
	 */
	long min;

	/**
	 * The right bound of the interval
	 */
	long max;

	/**
	 * The compare value
	 */
	long value;

	/**
	 * The bitmask
	 */
	long mask;

	/**
	 * Creates a instance. Validates if value is a part type. If value is a
	 * value or a range this constructor checks if the value or the minimum or
	 * maximum are between the allowed interval
	 *
	 * @param format
	 *            The field format
	 * @param value
	 *            The type part as string
	 * @throws ValidationException
	 *             if pattern type is invalid, values not permit or outside the
	 *             interval
	 */
	UIntPart(FieldFormat format, String value) throws ValidationException {
		this.format = format;

		switch (value) {
		case "X":
			type = PartType.X;
			break;
		case "*":
			type = PartType.ASTERISK;
			break;
		default:
			if (format == FieldFormat.HEX) {
				if (value.startsWith("x")) {
					try {
						type = PartType.VALUE;
						this.value = Long.parseLong(value.substring(1), 16);
					} catch (Exception e) {
						throw new ValidationException(
								"Could not parse pattern. " + e.getMessage());
					}
				} else {
					if (value.startsWith("&")) {
						Matcher match = HEX_MASK.matcher(value);
						if (match.matches()) {
							type = PartType.MASK;
							this.value = Long.parseLong(match.group("value"),
									16);
							mask = Long.parseLong(match.group("mask"), 16);

						} else {
							throw new ValidationException(
									"Could not parse pattern");
						}
					} else {
						Matcher match = HEX_RANGE.matcher(value);
						if (match.matches()) {
							type = PartType.RANGE;
							min = Long.parseLong(match.group("min"), 16);
							max = Long.parseLong(match.group("max"), 16);
						} else {
							throw new ValidationException(
									"Could not parse pattern");
						}
					}
				}
			} else if (format == FieldFormat.DECIMAL) {
				Matcher match = DEC_RANGE.matcher(value);
				if (match.matches()) {
					type = PartType.RANGE;
					min = Long.parseLong(match.group("min"));
					max = Long.parseLong(match.group("max"));
				} else {
					try {
						type = PartType.VALUE;
						this.value = Long.parseLong(value);
					} catch (Exception e) {
						throw new ValidationException(
								"Could not parse pattern. " + e.getMessage());
					}
				}
			} else {
				throw new ValidationException("Unknown format for pattern");
			}
			break;
		}
	}

	/**
	 * Indicates if part type is a wildcard like {@link PartType.X} or
	 * {@link PartType.Asterisk}.
	 *
	 * @return True, if is wildcard
	 */
	boolean isWildcard() {
		return (type == PartType.X) || (type == PartType.ASTERISK);
	}

	/**
	 * Indicates if part type is a range like {@link PartType.Range}.
	 *
	 * @return True, if is range
	 */
	boolean isRange() {
		return type == PartType.RANGE;
	}

	/**
	 * Indicates if part type is a group like {@link PartType.X}.
	 *
	 * @return True, if is group
	 */
	boolean isGroup() {
		return type == PartType.X;
	}

	/**
	 * Tests if given value match to type an the internal characterisics
	 *
	 * @param value
	 *            The testing value
	 * @return True, if part match otherwise false
	 */
	boolean match(long value) {
		switch (type) {
		case X:
			return true;
		case ASTERISK:
			return true;
		case VALUE:
			return this.value == value;
		case MASK:
			return (this.value & mask) == (value & mask);
		case RANGE:
			return ((value >= min) && (value <= max));
		default:
			return false;
		}
	}

	/**
	 * Returns the group name for this position depending an part type
	 *
	 * @param value
	 *            The postion value
	 * @return The name of the position
	 */
	String name(long value) {
		switch (type) {
		case X:
			return String.format("x%x", Long.valueOf(value));
		case ASTERISK:
			return "*";
		case VALUE:
			return this.value == value ? String.format("x%x", Long.valueOf(value)) : null;
		case MASK:
			return (this.value & mask) == (value & mask) ? String.format("x%x",
			        Long.valueOf(value)) : null;
		case RANGE:
			return ((value >= min) && (value <= max)) ? "["
					+ String.format("x%x", Long.valueOf(min)) + "-"
					+ String.format("x%x", Long.valueOf(max)) + "]" : null;
		default:
			return null;
		}
	}

	/**
	 * Checks if there exists a value between interval of minimum and maximum
	 * where the combine of mask and value matches
	 *
	 * @param min
	 *            The minimum value
	 * @param max
	 *            The maximum value
	 * @param mask
	 *            The bitmask
	 * @param value
	 *            The compare value
	 * @return True, if interval is disjoint, false otherwise
	 */
	boolean disjoint(long min, long max, long mask, long value) {
		if (mask > 0) {
			// calculate the pow and gap
			// pow is one bit higher than the highest bit of mask
			// gap is sum of the values for each bit of mask which is set to
			// zero
			long pow = 1, gap = 0;
			do {
				// add value of pow to gap if bit in mask is set to zero
				if ((mask & pow) == 0) {
					gap += pow;
				}
			} while ((pow <<= 1) < mask);

			// calculate pos relative to max value
			// where mask & value is the offset
			long pos = max - Math.floorMod(max - (mask & value), pow);

			// pos is between the interval if it is higher then min
			if (pos >= min) {
				return false;
			} else {
				if (pos + gap > max) {
					// decrease gap if it is higher then max relative to pos
					long i = 1;
					do {
						if ((mask & i) == 0) {
							// gap is lower or eqaul to max
							if (pos + gap - i <= max) {
								// gap is lower than min
								if (pos + gap - i < min) {
									return true;
								} else {
									return false;
								}
							}
						}
					} while ((i <<= 1) < pow);
					// there is no match between min and max relative to pos +
					// gap
					return true;
				} else {
					// pos + gap is higher or equal to min
					if (pos + gap >= min) {
						return false;
					} else {
						return true;
					}
				}
			}
		} else {
			return false;
		}
	}

	/**
	 * Returns if part is disjoint
	 *
	 * @param part
	 *            The part
	 * @return True, if part is disjoint, false otherwise
	 */
	boolean disjoint(UIntPart part) {
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
					if (part.type == PartType.VALUE) {
						if ((part.value < min) || (part.value > max))
							return true;
					} else {
						return disjoint(min, max, part.mask, part.value);
					}
				}
			} else {
				if (part.isRange()) {
					if (type == PartType.VALUE) {
						if ((this.value < part.min) || (this.value > part.max))
							return true;
					} else {
						return disjoint(part.min, part.max, mask, this.value);
					}
				} else {
					if (type == PartType.VALUE) {
						if (part.type == PartType.VALUE) {
							return value != part.value;
						} else {
							return value != (part.value & part.mask);
						}
					} else {
						if (part.type == PartType.VALUE) {
							return (value & mask) != part.value;
						} else {
							return (value & mask) != (part.value & part.mask);
						}
					}
				}
			}
		}
		return false;
	}
}
