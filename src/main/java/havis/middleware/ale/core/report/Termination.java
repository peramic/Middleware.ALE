package havis.middleware.ale.core.report;

/**
 * Type of termination conditions
 */
public enum Termination {

	/**
	 * Unknown
	 */
	NULL,

	/**
	 * Cycle was undefined
	 */
	UNDEFINE,

	/**
	 * A trigger occurs
	 */
	TRIGGER,

	/**
	 * Duration time expired
	 */
	DURATION,

	/**
	 * No new data registered in given time
	 */
	STABLE_SET,

	/**
	 * New data available
	 */
	DATA_AVAILABLE,

	/**
	 * State of cycle changed to unrequested
	 */
	UNREQUESTED,

	/**
	 * No new tags between given interval
	 */
	NO_NEW_TAGS,

	/**
	 * Commands were execute on count tags
	 */
	COUNT,

	/**
	 * A command error occurs
	 */
	ERROR,

	/**
	 * No new events between given interval
	 */
	NO_NEW_EVENTS;

	/**
	 * Returns the termination condition as string
	 * 
	 * @param termination
	 *            The termination condition
	 * @return The string representation of the termination condition
	 */
	public static String toString(Termination termination) {
		return termination == null ? null : termination.name();
	}
}
