package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.core.trigger.Trigger;

/**
 * Provides the interface for a report trigger callback
 */
public interface Callback {

    /**
     * Invokes the callback
     * 
     * @param report
     *            The report
     * @param trigger
     *            The trigger
     */
    void invoke(Report report, Trigger trigger);
}