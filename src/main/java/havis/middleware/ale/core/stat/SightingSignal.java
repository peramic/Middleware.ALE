package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.ISightings;
import havis.middleware.ale.base.operation.tag.Sighting;
import havis.middleware.ale.service.ECReaderStat;
import havis.middleware.ale.service.ECSightingStat;
import havis.middleware.ale.service.IStat;
import havis.middleware.ale.service.ec.ECSightingSignalStat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Implements the sighting signal stat
 *
 * @param <Stat>
 *            The report stat type
 */
public class SightingSignal<Stat extends IStat> extends Reader<Stat, ISightings> {
	private Class<Stat> clazz;

	/**
	 * Creates a new instance
	 *
	 * @param profile
	 *            The profile name
	 * @param clazz
	 *            The class
	 */
	public SightingSignal(String profile, Class<Stat> clazz) {
		super(profile);
		this.clazz = clazz;
	}

	/**
	 * Returns a list of report sighting stat
	 *
	 * @param sightings
	 *            The sightings
	 * @return The list of sighting stats
	 */
    private List<ECSightingStat> getSightings(List<Sighting> sightings) {
        List<ECSightingStat> stat = new ArrayList<ECSightingStat>();
        for (Sighting sighting : sightings) {
            stat.add(new ECSightingSignalStat(sighting.getHost(), sighting.getAntenna(), sighting.getStrength(), sighting.getTimestamp()));
        }
        return stat;
    }

	/**
	 * Returns a list of report reader stat
	 *
	 * @param sightings
	 *            The sightings
	 * @return The list of reader stats
	 */
    private List<ECReaderStat> getBlocks(ISightings sightings) {
		List<ECReaderStat> stat = new ArrayList<ECReaderStat>();
        for (Entry<String, List<Sighting>> entry : sightings.getSightings().entrySet()) {
            stat.add(new ECReaderStat(entry.getKey(), getSightings(entry.getValue())));
        }
		return stat;
	}

	/**
	 * Returns the report stat from sightings
	 *
	 * @param sightings
	 *            The sightings
	 * @return The report stat
	 */
	@Override
    public Stat getStat(ISightings sightings) {
		try {
			Stat stat = clazz.newInstance();
			stat.setProfile(profile);
			stat.setStatBlockList(getBlocks(sightings));
			return stat;
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
}