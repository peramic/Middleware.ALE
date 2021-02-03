package havis.middleware.ale.core.report;

import havis.middleware.ale.core.subscriber.SubscriberController;

import java.util.Date;

/**
 * This class is a wrapper to keep additional reports details and implements
 * static condition methods for initiation and termination
 * 
 * @param <T>
 *            Reports type
 * @param <S>
 *            Data type
 */
public class ReportsInfo<T, S> {

	private Date date;

	private S datas;

	private long totalMilliseconds;

	private Initiation initiation;

	private String initiator;

	private Termination termination;

	private String terminator;

	/**
	 * Array of subscribers
	 */
	private SubscriberController[] subscribers;

	public ReportsInfo(SubscriberController[] subscribers,
			Initiation initiation, Termination termination) {
		this.date = new Date();
		this.subscribers = subscribers;
		this.initiation = initiation;
		this.termination = termination;
	}

	public ReportsInfo(SubscriberController[] subscribers, S datas,
			Date date, long totalMilliseconds, Initiation initiation,
			String initiator, Termination termination, String terminator) {
		this(subscribers, initiation, termination);
		this.date = date;
		this.datas = datas;
		this.totalMilliseconds = totalMilliseconds;
		this.initiator = initiator;
		this.terminator = terminator;
	}

	public SubscriberController[] getSubscribers() {
		return subscribers;
	}

	/**
	 * Gets the datas
	 */
	public S getDatas() {
		return datas;
	}

	/**
	 * Gets the cycle end time stamp
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Gets the effective duration of the cycle
	 */
	public long getTotalMilliseconds() {
		return totalMilliseconds;
	}

	/**
	 * Gets the initiation condition
	 */
	public Initiation getInitiation() {
		return initiation;
	}

	/**
	 * Gets the name of the trigger if initiation condition is trigger
	 */
	public String getInitiator() {
		return initiator;
	}

	/**
	 * Gets the termination condition
	 */
	public Termination getTermination() {
		return termination;
	}

	/**
	 * Gets the name of the trigger if termination condition is trigger
	 */
	public String getTerminator() {
		return terminator;
	}
}
