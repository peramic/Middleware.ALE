package havis.middleware.ale.core.reader;

import havis.middleware.ale.reader.Callback;

/**
 * Handler for reader connector callbacks
 */
public interface CallbackHandler extends Callback {

	/**
	 * Add a callback receiver
	 * 
	 * @param receiver
	 *            the receiver to add
	 */
	public void add(Receiver<?> receiver);

	/**
	 * Remove a callback receiver by ID
	 * 
	 * @param id
	 *            the ID of the receiver to remove
	 * @return true if a callback receiver was found and removed with the
	 *         specifed ID, false otherwise
	 */
	public boolean remove(long id);

	/**
	 * Get a callback receiver by ID
	 * 
	 * @param id
	 *            the ID of the callback receiver
	 * @return the callback receiver
	 */
	public Receiver<?> get(long id);

	/**
	 * Dispose the callback handler
	 */
	public void dispose();
}
