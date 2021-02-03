package havis.middleware.ale.core.report.pattern;

import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.tdt.TdtTagInfo;
import havis.middleware.tdt.TdtTranslationException;

/**
 * Provides the interface for a pattern
 */
public interface IPattern {
	/**
	 * Returns if tag info and result match to the pattern
	 * 
	 * @param info
	 *            The tag info
	 * @param result
	 *            The result
	 * @return True if tag info and result match to the pattern, false otherwise
	 * @throws TdtTranslationException
	 */
	boolean match(TdtTagInfo info, Result result);

	/**
	 * Returns the group name based on tag info and result if pattern match
	 * 
	 * @param info
	 *            The tag info
	 * @param result
	 *            The result
	 * @return Returns the group name if pattern match, false otherwise
	 * @throws TdtTranslationException
	 */
	String name(TdtTagInfo info, Result result);

	/**
	 * Returns the next value
	 * 
	 * @return The next value, or null if no more values exists
	 */
	String next();

	/**
	 * Returns if patterns are disjoint
	 * 
	 * @param patterns
	 *            The patterns
	 * @return True if patterns are disjoint
	 */
	boolean disjoint(Iterable<IPattern> patterns);

	/**
	 * Returns the string representation
	 * @return The string representation of the pattern
	 */
	String toString();
}
