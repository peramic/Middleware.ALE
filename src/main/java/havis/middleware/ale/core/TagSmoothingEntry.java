package havis.middleware.ale.core;

import havis.middleware.ale.base.operation.tag.Tag;

/**
 * Entry for {@link TagSmoothingHandler}
 */
public class TagSmoothingEntry {

    private boolean isObserved;
    private int seenCount;
    private long firstSeen;
    private long lastSeen;
    private Tag tag;

    /**
     * Initializes a instance
     *
     * @param tag The tag object to perform tag smoothing on
     */
    public TagSmoothingEntry(Tag tag) {
        seenCount = 0;
        firstSeen = System.currentTimeMillis();
        lastSeen = firstSeen;

        this.tag = tag;

        isObserved = false;
    }

	/**
	 * @return the tag object an which smoothing is performed
	 */
    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

	/**
	 * @return the last seen time stamp of the tag
	 */
    public long getLastSeen() {
        return lastSeen;
    }

	/**
	 * @return the first seen time stamp of the tag
	 */
    public long getFirstSeen() {
        return firstSeen;
    }

	/**
	 * @return the seen quantity of the tag
	 */
    public int getSeenCount() {
        return seenCount;
    }

	/**
	 * @return an indicator if the tag is in the observed state or not.
	 */
    public boolean isObserved() {
        return isObserved;
    }

    /**
     * Sets an indicator if the tag is in the observed state or not.
     *
     * @param observed
     */
    public void setObserved(boolean observed) {
        isObserved = observed;
    }

    /**
     * Mark this tag as seen, increases the seen count and sets the last seen time
     */
    public void seen() {
        seenCount++;
        lastSeen = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        TagSmoothingEntry t = (TagSmoothingEntry) obj;
        return (tag.equals(t.getTag()));
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public String toString() {
        return tag.toString();
    }
}