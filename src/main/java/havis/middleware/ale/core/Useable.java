package havis.middleware.ale.core;

/**
 * This interface provides methods to indicate that a resource is in used.
 */
public interface Useable {

	/**
	 * Increments the usage count.
	 */
	void inc();

	/**
	 * Decrements the usage count.
	 */
	void dec();

	/**
	 * Indicates if the resource is in use.
	 * 
	 * @return true if the usage count is greater then zero false otherwise
	 */
	boolean isUsed();
}