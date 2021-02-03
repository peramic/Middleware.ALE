package havis.middleware.ale.core.report;

/**
 * Type of initiation events
 */
public enum Initiation {

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
	 * Repeat period expired
	 */
	REPEAT_PERIOD,

	/**
	 * State of cycle changed to requested
	 */
	REQUESTED;

	/**
	 * Returns the initiation condition as string
	 * 
	 * @param initiation
	 *            The initiation condition
	 * @return The string representation of the initiation condition
	 */
	public static String toString(Initiation initiation) {
		return initiation == null ? null : initiation.name();
	}
}
