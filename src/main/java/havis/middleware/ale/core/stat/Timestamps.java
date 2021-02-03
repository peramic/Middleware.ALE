package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.ITimestamps;
import havis.middleware.ale.service.ITimestampStat;

/**
 * Implements the time stamp statistic
 *
 * @param <TimestampStat>
 *            The report time stamp statistic type
 */
public class Timestamps<TimestampStat extends ITimestampStat>
		extends Reader<TimestampStat, ITimestamps> {

	private Class<TimestampStat> type;

	/**
	 * Creates a new instance
	 *
	 * @param profile
	 *            The profile name
	 * @param type
	 *            The statistic report type
	 */
	public Timestamps(String profile, Class<TimestampStat> type) {
		super(profile);
		this.type = type;
	}

	/**
	 * Returns the report statistic from tag
	 *
	 * @param timestamps
	 *            The time stamps
	 * @return The statistic report instance
	 */
	@Override
    public TimestampStat getStat(ITimestamps timestamps) {
		try {
		    TimestampStat stat = type.newInstance();
			stat.setProfile(profile);
            stat.setFirstSightingTime(timestamps.getFirstTime());
            stat.setLastSightingTime(timestamps.getFirstTime() == null ? timestamps.getFirstTime() : timestamps.getLastTime());
			return stat;
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
}