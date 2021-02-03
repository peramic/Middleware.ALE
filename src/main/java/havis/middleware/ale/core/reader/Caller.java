package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.operation.Data;

/**
 * Delegate for reporting reader results.
 * 
 * @author abrams
 * 
 * @param <T>
 *            The data type
 */
public interface Caller<T extends Data> {

	/**
	 * Invokes the caller
	 * 
	 * @param t
	 *            The data
	 * @param controller
	 *            The controller
	 */
	void invoke(T t, ReaderController controller);
}