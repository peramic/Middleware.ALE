package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.ISightings;
import havis.middleware.ale.base.operation.tag.Sighting;
import havis.middleware.ale.service.ECReaderStat;
import havis.middleware.ale.service.IStat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Implements the reader names stat
 *
 * @param <Stat>
 *            The report stat type
 */
public class ReaderNames<Stat extends IStat> extends Reader<Stat, ISightings> {

    private Class<Stat> clazz;

    /**
     * Creates a new instance
     *
     * @param profile
     *            The profile name
     * @param clazz
     *            The class
     */
    public ReaderNames(String profile, Class<Stat> clazz) {
        super(profile);
        this.clazz = clazz;
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
            ECReaderStat readerStat = new ECReaderStat();
            readerStat.setReaderName(entry.getKey());
            stat.add(readerStat);
        }
        return stat;
    }

    /**
     * Returns the report stat sightings
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