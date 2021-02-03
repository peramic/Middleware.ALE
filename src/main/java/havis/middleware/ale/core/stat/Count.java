package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.ICount;
import havis.middleware.ale.service.ICountStat;

/**
 * Implements the count stat
 *
 * @author abrams
 *
 * @param <CountStat>
 *            The report count stat type
 */
public class Count<CountStat extends ICountStat> extends
		Reader<CountStat, ICount> {

	private Class<CountStat> clazz;

	/**
	 * Creates a new instance
	 *
	 * @param profile
	 *            The profile name
	 * @param clazz
	 *            The class
	 */
	public Count(String profile, Class<CountStat> clazz) {
		super(profile);
		this.clazz = clazz;
	}

	/**
	 * Returns the report stat
	 *
	 * @param count
	 *            The count
	 * @return The report stat
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@Override
    public CountStat getStat(ICount count) {
		try {
			CountStat stat = clazz.newInstance();
			stat.setProfile(profile);
			stat.setCount(count.getCount());
			return stat;
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
}