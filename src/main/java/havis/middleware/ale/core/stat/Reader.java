package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.Statistics;
import havis.middleware.ale.service.IStat;

/**
 * This class is used to provide the statistic information as specified in ALE
 * 1.1.1 (8.3.9..12)
 *
 * @param <Stat>
 *            The report stat type
 * @param <Source>
 *            The statistics source type
 */
public abstract class Reader<Stat extends IStat, Source extends Statistics> {

	/**
	 * Retrieves the profile name
	 */
	protected String profile;

	/**
	 * Creates a new instance
	 *
	 * @param profile
	 *            The profile name
	 */
	public Reader(String profile) {
		this.profile = profile;
	}

	public String getProfile() {
		return profile;
	}

	/**
	 * @param source the source for the stat
	 * @return the stat object
	 */
	public abstract Stat getStat(Source source);
}