package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.report.IDatas;
import havis.middleware.utils.generic.LinkedHashSet;
import havis.middleware.utils.generic.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements {@link IDatas} interface for command cycle
 */
public class Tags implements IDatas {

	private LinkedHashSet<Tag> tags;

	private List<Tag> list;

	private Counter counter = new Counter();

	private AtomicBoolean disposed = new AtomicBoolean(false);

	/**
	 * Creates new instance.
	 */
	public Tags() {
		init();
	}

	Tags(LinkedHashSet<Tag> tags, List<Tag> list, Counter counter, AtomicBoolean disposed) {
		this();
		this.tags = tags;
		this.list = list;
		this.counter = counter;
		this.disposed = disposed;
	}

	private void init() {
		tags = new LinkedHashSet<Tag>();
		list = new ArrayList<Tag>();
	}

	/**
	 * Clears tag data
	 */
	@Override
	public void clear() {
		while ((tags.getCount() > Config.getInstance().getGlobal()
				.getReaderCycle().getCount())
				&& tags.removeFirst()) {
		}
		tags.remove(new Predicate<Tag>() {
			@Override
			public boolean invoke(Tag tag) {
				return System.currentTimeMillis() - tag.getFirstTime().getTime() > Config.getInstance().getGlobal().getReaderCycle().getLifetime();
			}
		});
	}

	/**
	 * Rotates data
	 */
	@Override
	public void rotate() {
		list = new ArrayList<Tag>();
	}

	/**
	 * Resets tag data and tag container
	 */
	@Override
	public void reset() {
		if (!disposed.get()) {
			long counter = this.counter.await(null);
			while (tags.find(new Predicate<Tag>() {
				@Override
				public boolean invoke(Tag tag) {
					return tag == null;
				}
			})) {
				counter = this.counter.await(Long.valueOf(counter));
				// TODO: add timeout
			}
		}
		init();
	}

	/**
	 * Returns a clone
	 *
	 * @return The object clone
	 */
	@Override
	public IDatas clone() {
		return new Tags(tags, list, counter, disposed);
	}

	/**
	 * Retrieves the count of processed tags
	 *
	 * @return The processed count
	 */
	public int getCount() {
		return list.size();
	}

	/**
	 * Returns the tags as list
	 *
	 * @return The tag list
	 */
	List<Tag> toList() {
		return new ArrayList<>(list);
	}

	/**
	 * Adds tag to container
	 *
	 * @param tag
	 *            The tag
	 */
	public void add(Tag tag) {
		synchronized (this) {
			tags.add(tag, tag);
		}
	}

	/**
	 * Waits for next pulse if no changes occurred
	 *
	 * @param counter
	 *            The current counter
	 * @return The next counter
	 */
	long await(Long counter) {
		if (disposed.get()) {
			return 0;
		}
		return this.counter.await(counter);
	}

	/**
	 * Adds a new entry to container
	 *
	 * @param key
	 *            The key tag
	 * @param value
	 *            The value tag
	 */
	public void add(Tag key, Tag value) {
		tags.add(key, value);
		list.add(key);
	}

	/**
	 * Removes tag
	 *
	 * @param key
	 *            The key tag
	 */
	public void remove(Tag key) {
		tags.remove(key);
	}

	/**
	 * Pulses all waiting reports
	 */
	public void pulse() {
		counter.pulse();
	}

	/**
	 * Gets the tag by tag as key
	 *
	 * @param tag
	 *            The key tag
	 * @return The value tag
	 */
	public Tag get(Tag tag) {
		return tags.get(tag);
	}

	/**
	 * Returns whether the tag is in the list
	 * @param tag the tag
	 * @return true if the tag is in the list, false otherwise
	 */
	public boolean contains(Tag tag) {
		return tags.containsKey(tag);
	}
	
	/**
	 * Checks whether the tag has been seen and removes the entry if the command has been executed and the lifetime exceeded
	 * @param tag the tag to check
	 * @return true if the tag was seen and hasn't exceeded its lifetime, false if it wasn't seen or the lifetime has exceeded
	 */
	public boolean removeLifetimeExceededAndCheckWhetherSeen(Tag tag) {
		boolean result = tags.containsKey(tag);
		Tag value;
		if (result && (value = tags.get(tag)) != null) {
			if (System.currentTimeMillis() - value.getFirstTime().getTime() > Config.getInstance().getGlobal().getReaderCycle().getLifetime()) {
				// lifetime exceeded, remove and return false
				tags.remove(tag);
				return false;
			}
		}
		return result;
	}

	/**
	 * Puts the tag value by tag as key
	 *
	 * @param tag
	 *            The key tag
	 * @param value
	 *            The value tag
	 */
	public void put(Tag tag, Tag value) {
		tags.set(tag, value);
	}

	/**
	 * Returns whether the specified tags have the same underlying list as this instance
	 * 
	 * @param tags the tags to check
	 * @return true if the specified tags have the same underlying list as this instance
	 */
	public boolean hasSameData(Tags tags) {
		return tags != null && tags.list == list;
	}

	public boolean isDisposed() {
		return disposed.get();
	}

	@Override
	public void dispose() {
		if (disposed.compareAndSet(false, true)) {
			if (list != null) {
				list.clear();
			}
			if (tags != null) {
				tags.clear();
			}
			if (counter != null) {
				counter.pulse();
			}
		}
	}
}