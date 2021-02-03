package havis.middleware.ale.core;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.utils.generic.LinkedHashSet;
import havis.middleware.utils.generic.Predicate;

public class TagSmoothingHandler implements Predicate<TagSmoothingEntry> {

    /**
     * Tag smoothing glimpsed timeout defined for this logical reader
     */
    final Integer glimpsedTimeout;

    /**
     * Tag smoothing observed timeout defined for this logical reader
     */
    final Integer observedTimeThreshold;

    /**
     * Tag smoothing observed count defined for this logical reader
     */
    final Integer observedCountThreshold;

    /**
     * Tag smoothing lost timeout defined for this logical reader
     */
    final Integer lostTimeout;

    private LinkedHashSet<TagSmoothingEntry> observedEntries = new LinkedHashSet<>();

    private LinkedHashSet<TagSmoothingEntry> unobservedEntries = new LinkedHashSet<>();

    /**
     * Creates a new tag smoothing handler
     *
     * @param glimpsedTimeout
     *            the glimpsed timeout
     * @param observedTimeThreshold
     *            the observed timeout
     * @param observedCountThreshold
     *            the observed count
     * @param lostTimeout
     *            the lost timeout
     */
    public TagSmoothingHandler(Integer glimpsedTimeout, Integer observedTimeThreshold, Integer observedCountThreshold, Integer lostTimeout) {
        this.glimpsedTimeout = glimpsedTimeout;
        this.observedTimeThreshold = observedTimeThreshold;
        this.observedCountThreshold = observedCountThreshold;
        this.lostTimeout = lostTimeout;
    }

    /**
     * Process the specified tag
     *
     * @param tag
     *            the tag to process
     * @param callback
     *            the callback to use when the tag is observed
     * @param controller
     *            the controller to pass
     */
    public void process(Tag tag, Caller<Tag> callback, ReaderController controller) {
        this.unobservedEntries.remove(this);
        this.observedEntries.remove(this);

        TagSmoothingEntry entry = new TagSmoothingEntry(tag);
        entry = addOrMoveToEnd(entry);

        if (seen(entry)) {
            tag.setTimeout(this.lostTimeout != null ? this.lostTimeout.intValue() : 0);
            callback.invoke(tag, controller);
        }
    }

    private TagSmoothingEntry addOrMoveToEnd(TagSmoothingEntry entry) {
        if (this.observedEntries.containsKey(entry)) { // was it already observed
            entry = this.observedEntries.update(entry);
        } else {
            // might be an unobserved entry or a new one
            entry = this.unobservedEntries.update(entry);
        }
        return entry;
    }

    private void switchToObserved(TagSmoothingEntry entry) {
        unobservedEntries.remove(entry);
        entry.setObserved(true);
        observedEntries.add(entry, entry);
    }

    private boolean seen(TagSmoothingEntry entry) {
        entry.seen();

        if (entry.isObserved()) {
            return true;
        }

        if (((this.observedCountThreshold != null) && (entry.getSeenCount() >= this.observedCountThreshold.intValue()))
                || ((this.observedTimeThreshold != null) && ((entry.getLastSeen() - entry.getFirstSeen()) >= this.observedTimeThreshold.longValue()))) {
            switchToObserved(entry);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the remove predicate
     *
     * @param entry
     *            the tag to check for removal
     * @return true if the tag can be removed, false otherwise
     */
    @Override
    public boolean invoke(TagSmoothingEntry entry) {
        if (entry.isObserved()) {
            return (this.lostTimeout != null && (System.currentTimeMillis() - entry.getLastSeen()) > this.lostTimeout.longValue());
        } else {
            return (this.glimpsedTimeout != null && (System.currentTimeMillis() - entry.getLastSeen()) > this.glimpsedTimeout.longValue());
        }
    }
}
