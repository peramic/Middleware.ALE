package havis.middleware.ale.core.report.cc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Counter {

	private long counter = 0;
	private Lock monitor = new ReentrantLock();
	private Condition condition = monitor.newCondition();

	/**
	 * Waits for next pulse if no changes occurred
	 *
	 * @param counter
	 *            The current counter
	 * @return The next counter
	 */
	long await(Long counter) {
        monitor.lock();
        try {
            if (counter != null && counter.longValue() == this.counter) {
                condition.awaitUninterruptibly();
            }
            return this.counter;
        } finally {
            monitor.unlock();
        }
	}

	/**
     * Pulses all waiting reports
     */
    void pulse() {
        monitor.lock();
        try {
            counter++;
            condition.signalAll();
        } finally {
            monitor.unlock();
        }
	}
}