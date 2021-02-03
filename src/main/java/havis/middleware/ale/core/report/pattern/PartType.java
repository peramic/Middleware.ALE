package havis.middleware.ale.core.report.pattern;

/**
 * Defines the part types
 */
public enum PartType {

	/**
	 * Indicates a grouping pattern to create a single group for each differnt
	 * value
	 */
	X,

	/**
	 * Includes each value
	 */
	ASTERISK,

	/**
	 * Includes only value with exact match in length and content
	 */
	VALUE,

	/**
	 * Includes only numbers which are is in range
	 */
	RANGE
}