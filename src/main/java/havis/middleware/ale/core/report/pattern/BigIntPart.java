package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.field.CommonField;
import havis.middleware.utils.data.Calculator;
import havis.middleware.utils.data.Comparison;
import havis.middleware.utils.data.Converter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

/**
 * This class is used for a single position within a filter or group pattern. It
 * allows a position to be a X, ASTERISK, VALUE or RANGE. It validates the
 * position depending on field definition from the TDT package.
 */
class BigIntPart {

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

	private PartType type;
	private CommonField field;

	/**
	 * The left bound of the interval
	 */
	private byte[] min;

	/**
	 * The right bound of the interval
	 */
	private byte[] max;

	/**
	 * The compare value
	 */
	private byte[] value;

	/**
	 * The bit mask
	 */
	private byte[] mask;

	/**
	 * Creates a instance. Validates if value is a part type. If value is a
	 * value or a range this constructor checks if the value or the minimum or
	 * maximum are between the allowed interval
	 *
	 * @param field
	 *            The field
	 * @param value
	 *            The type part as string
	 * @throws ValidationException
	 *             if pattern type is invalid, values not permit or outside the
	 *             interval
	 */
	BigIntPart(CommonField field, String value) throws ValidationException {
		this.field = field;

		switch (value) {
		case "X":
			type = PartType.X;
			break;
		case "*":
			type = PartType.ASTERISK;
			break;
		default:
			switch (field.getFieldFormat()) {
			case HEX: {
				if (value.startsWith("x")) {
					try {
						type = PartType.VALUE;
						this.value = Converter.hexToBytes(value.substring(1),
								field.getLength());
					} catch (Exception e) {
						throw new ValidationException(
								"Could not parse pattern. " + e.getMessage());
					}
				} else {
					if (value.startsWith("&")) {
						Matcher match = HEX_MASK.matcher(value);
						if (match.matches()) {
							type = PartType.MASK;
							this.value = Converter.hexToBytes(
									match.group("value"), field.getLength());
							mask = Converter.hexToBytes(match.group("mask"),
									field.getLength());
						} else {
							throw new ValidationException(
									"Could not parse pattern");
						}
					} else {
						Matcher match = HEX_RANGE.matcher(value);
						if (match.matches()) {
							type = PartType.RANGE;
							min = Converter.hexToBytes(match.group("min"),
									field.getLength());
							max = Converter.hexToBytes(match.group("max"),
									field.getLength());
						} else {
							throw new ValidationException(
									"Could not parse pattern");
						}
					}
				}
			}
				break;
			case DECIMAL: {
				AtomicInteger length = new AtomicInteger(0);
				Matcher match = DEC_RANGE.matcher(value);
				if (match.matches()) {
					type = PartType.RANGE;
					min = Converter.decToBytes(match.group("min"), length);
					max = Converter.decToBytes(match.group("max"), length);
				} else {
					try {
						type = PartType.VALUE;
						this.value = Converter.decToBytes(value, length);
					} catch (Exception e) {
						throw new ValidationException(
								"Could not parse pattern. " + e.getMessage());
					}
				}
			}
				break;
			default:
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
	 * Indicates if part type is a group like <see cref="PartType.X"/>.
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
	 * @return True, if part match otherwise false
	 */
	boolean match(byte[] value) {
		switch (type) {
		case X:
			return true;
		case ASTERISK:
			return true;
		case VALUE:
			return Comparison.equal(this.value, value, field.getLength());
		case MASK:
			return Comparison.equal(this.value, value, field.getLength(), mask,
					0);
		case RANGE:
			return !(Comparison.lower(value, min, field.getLength()) || Comparison
					.greater(value, max, field.getLength()));
		default:
			return false;
		}
	}

	/**
	 * Returns the group name for this position depending an part type
	 *
	 * @param value
	 *            The position value
	 * @return The name of the position
	 */
	String name(byte[] value) {
		switch (type) {
		case X:
			return Converter.toString(value, field.getLength(), 4);
		case ASTERISK:
			return "*";
		case VALUE:
			return Comparison.equal(this.value, value, field.getLength()) ? Converter
					.toString(value, field.getLength(), 4) : null;
		case MASK:
			return Comparison.equal(this.value, value, field.getLength(), mask,
					0) ? Converter.toString(value, field.getLength(), 4) : null;
		case RANGE:
			return !(Comparison.lower(value, min, field.getLength()) || Comparison
					.greater(value, max, field.getLength())) ? "["
					+ Converter.toString(min, field.getLength(), 4) + "-"
					+ Converter.toString(max, field.getLength(), 4) + "]"
					: null;
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
	 *            The bit mask
	 * @param value
	 *            The compare value
	 * @param length
	 *            The bit length
	 * @return True, if interval is disjoint, false otherwise
	 */
	boolean disjoint(byte[] min, byte[] max, byte[] mask, byte[] value,
			int length) {
		int size = (length + (8 - length % 8) % 8) / 8;
		if (Comparison.greater(mask, new byte[size], length)) {
			// calculate the pow and gap
			// pow is one bit higher than the highest bit of mask
			// gap is sum of the values for each bit of mask which is set to zero
			byte[] gap = new byte[size];
			int pow = 0;

			// TODO: pow calculation is likely wrong
			int p = (8 - length % 8) % 8;
			for (int i = size; i > 0; i--) {
				if ((mask[i - 1] & 0xFF) > 0) {
					do {
						// add value of pow to gap if bit in mask is set to zero
						if (((mask[i - 1] & 0xFF) & (1 << p % 8)) == 0) {
							gap[i - 1] = (byte) ((gap[i - 1] & 0xFF) + (1 << p % 8));
						}
					} while (1 < (mask[i - 1] & 0xFF) >> (++p % 8));
					pow = p - (8 - length % 8) % 8;
				}
			}

			// calculate pos relative to max value
			// where mask & value is the offset
			// TODO: this is not working for min = 1, max = 2, mask = 3, value = 3
			byte[] pos = Calculator.diff(max, Calculator.cut(Calculator.diff(max, Calculator.and(mask, value, length), length), pow, length), length);

			// pos is between the interval if it is higher then min
			if (!Comparison.lower(pos, min, length)) {
				return false;
			} else {
				if (Comparison.greater(Calculator.sum(pos, gap, length), max,
						length)) {
					// decrease gap if it is higher then max relative to pos
					p = 0;
					do {
						byte[] i = new byte[size];
						i[size - 1 - p / 8] = (byte) (1 << (p + (8 - length % 8) % 8) % 8);
						byte[] x = Calculator.and(mask, i, length);
						if (Comparison.equal(x, new byte[size], length)) {
							byte[] d = Calculator
									.diff(Calculator.sum(pos, gap, length), i,
											length);
							// gap is lower or eqaul to max
							if (!Comparison.greater(d, max, length)) {
								// gap is lower than min
								if (Comparison.lower(d, min, length)) {
									return true;
								} else {
									return false;
								}
							}
						}
					} while (++p < pow);
					// there is no match between min and max relative to pos + gap
					return true;
				} else {
					// pos + gap is higher or equal to min
					if (!Comparison.lower(Calculator.sum(pos, gap, length),
							min, length)) {
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
	boolean disjoint(BigIntPart part) {
		if (!isWildcard() && !part.isWildcard()) {
			if (isRange()) {
				if (part.isRange()) {
					if (Comparison.lower(min, part.min, field.getLength())) {
						if (Comparison.lower(max, part.min, field.getLength())) {
							return true;
						}
					} else {
						if (Comparison
								.greater(min, part.max, field.getLength())) {
							return true;
						}
					}
				} else {
					if (part.type == PartType.VALUE) {
						if (Comparison
								.lower(part.value, min, field.getLength())
								|| Comparison.greater(part.value, max,
										field.getLength()))
							return true;
					} else {
						return disjoint(min, max, part.mask, part.value,
								field.getLength());
					}
				}
			} else {
				if (part.isRange()) {
					if (type == PartType.VALUE) {
						if (Comparison.lower(this.value, part.min,
								field.getLength())
								|| Comparison.greater(this.value, part.max,
										field.getLength()))
							return true;
					} else {
						return disjoint(part.min, part.max, mask, this.value,
								field.getLength());
					}
				} else {
					if (type == PartType.VALUE) {
						if (part.type == PartType.VALUE) {
							return !Comparison.equal(value, part.value,
									field.getLength());
						} else {
							return !Comparison.equal(value, part.value,
									field.getLength(), part.mask, 1);
						}
					} else {
						if (part.type == PartType.VALUE) {
							return !Comparison.equal(value, part.value,
									field.getLength(), mask, -1);
						} else {
                            return !Comparison.equal(Calculator.and(value, mask, field.getLength()),
                                    Calculator.and(part.value, part.mask, field.getLength()), field.getLength());
						}
					}
				}
			}
		}
		return false;
	}
}
