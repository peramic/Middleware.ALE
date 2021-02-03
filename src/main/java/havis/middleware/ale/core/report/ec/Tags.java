package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.report.IDatas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implements the tags container
 */
public class Tags implements IDatas, Cloneable {

	/**
	 * Tags of the previous cycle
	 */
	private Map<PrimaryKey, Tag> past;

	/**
	 * tags of the current cycle
	 */
	private Map<PrimaryKey, Tag> present;

	/**
	 * Creates a new instance
	 */
	public Tags() {
		init();
	}

	public Map<PrimaryKey, Tag> getPast() {
		return past;
	}

	public Map<PrimaryKey, Tag> getPresent() {
		return present;
	}

	private void init() {
		past = new LinkedHashMap<PrimaryKey, Tag>();
		present = new LinkedHashMap<PrimaryKey, Tag>();
	}

	/**
	 * Clears the present container and puts all tags from the past container,
	 * which have not yet timed out, to the present container
	 */
	@Override
    public void clear() {
		long now = System.currentTimeMillis();
		present = new LinkedHashMap<PrimaryKey, Tag>();
		for (Entry<PrimaryKey, Tag> pair : past.entrySet()) {
			if (now - pair.getValue().getLastTime().getTime() < pair.getValue()
					.getTimeout()) {
				Tag t = pair.getValue().clone();
				t.clear();
				present.put(pair.getKey(), t);
			}
		}
	}

	/**
	 * Clears the past container and copies all tags
	 * from the preset container to the past container
	 */
	@Override
    public void rotate() {
		past = new LinkedHashMap<PrimaryKey, Tag>(present);
	}

	/**
	 * Resets the past and present container
	 */
	@Override
    public void reset() {
		init();
	}

	/**
	 * Tries to get a value
	 *
	 * @param key
	 *            The key
	 * @return The output value
	 */
	public Tag get(PrimaryKey key) {
		return present.get(key);
	}

	/**
	 * Returns whether the key is in the list
	 * @param key the key
	 * @return true if the key is in the list, false otherwise
	 */
	public boolean contains(PrimaryKey key) {
		return present.containsKey(key);
	}

	/**
	 * Adds a new tag to present container
	 *
	 * @param key
	 *            The key
	 * @param tag
	 *            The tag
	 */
	public void add(PrimaryKey key, Tag tag) {
		present.put(key, tag);
	}

	/**
	 * Clones the object
	 */
	@Override
	public Tags clone() {
		try {
			return (Tags) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return "Tags [past=" + past + ", present=" + present + "]";
	}

	@Override
	public void dispose() {
		// nothing to do
	}
}