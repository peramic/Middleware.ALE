package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.ECTime;

/**
 * This call is used to {@link ECTime} defined in ALE 1.1.1 (8.2.2). It has a
 * static method to get time value as long in milliseconds. This method only
 * knows the time unit MS (milliseconds).
 */
public class Time {

	/**
	 * Time units
	 */
	enum UNIT {
		/**
		 * Milliseconds
		 */
		MS
	}

	/**
	 * Returns the time in milliseconds
	 * 
	 * @param time
	 *            The time
	 * @return the time in milliseconds
	 * @throws ValidationException
	 */
	public static long getValue(ECTime time) throws ValidationException {
		if (time == null) {
			return -1;
		} else {
			String unit = time.getUnit();
			if (unit != null) {
				try {
					switch (UNIT.valueOf(unit)) {
					case MS:
						if (time.getValue() > -1) {
							return time.getValue();
						} else {
							throw new ValidationException(
									"Time value must not be negative");
						}
					}
				} catch (IllegalArgumentException e) {
				}
			}
			throw new ValidationException("Time unit must be MS");
		}
	}
}