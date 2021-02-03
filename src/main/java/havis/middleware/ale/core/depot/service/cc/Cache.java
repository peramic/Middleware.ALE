package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.config.CacheType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.Depot;
import havis.middleware.ale.core.report.cc.data.Caches;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.EPCCacheSpec;
import havis.middleware.ale.service.cc.EPCPatternList;
import havis.middleware.ale.service.mc.MCCacheSpec;

import java.util.List;
import java.util.UUID;

/**
 * Implements the EPC cache data depot
 */
public class Cache extends Depot<CacheType, MCCacheSpec> {

	private static Cache instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new Cache();
	}

	/**
	 * Retrieves the static instance
	 *
	 * @return The static instance
	 */
	public static Cache getInstance() {
		return instance;
	}

	/**
	 * Retrieves the configured entries
	 *
	 * @return The cache entry list
	 */
	@Override
    protected List<CacheType> getList() {
		return Config.getInstance().getService().getCc().getCaches().getCache();
	}

	/**
	 * Returns the entry from specification
	 *
	 * @param spec
	 *            The specification
	 * @return The entry
	 */
	@Override
    protected CacheType get(MCCacheSpec spec) {
		return new CacheType(spec.getName(), spec.isEnable(), spec.getSpec(),
				spec.getPatterns());
	}

	/**
	 * Returns the specification from entry
	 *
	 * @param entry
	 *            The entry
	 * @return The specification
	 */
	@Override
    protected MCCacheSpec get(CacheType entry) {
		return new MCCacheSpec(entry.getName(), entry.isEnable(),
				entry.getSpec(), entry.getPatterns());
	}

	/**
	 * Sets the enable state
	 *
	 * @param entry
	 *            The entry
	 * @param enable
	 *            The enable state
	 */
	@Override
    protected void setEnable(CacheType entry, boolean enable)
			throws ALEException {
		if (enable) {
			Caches.getInstance().define(entry.getName(), entry.getSpec(), entry
					.getPatterns().getPatterns().getPattern(), false);
		} else {
			try {
				Caches.getInstance().undefine(entry.getName(), false);
			} catch (NoSuchNameException e) {
				Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "EPC cache not found: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Adds a new entry
	 *
	 * @param name
	 *            The name
	 * @param spec
	 *            The specification
	 * @param patterns
	 *            The patterns
	 */
	public void add(String name, EPCCacheSpec spec, final List<String> patterns) {
		CacheType type = new CacheType(name, true, spec, new EPCPatternList(
				patterns));
		super.add(type);
	}

	/**
	 * Updates the given specification
	 *
	 * @param name
	 *            The name
	 * @param patterns
	 *            The patterns
	 */
	public void update(String name, final List<String> patterns) {
		UUID id = names.get(name);
		if (id != null) {
			try {
				CacheType cache = getType(id);
				if (cache != null) {
					cache.setPatterns(new EPCPatternList(patterns));
					Config.serialize();
				}
			} catch (NoSuchIdException e) {
				Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "EPC cache not found: " + e.getMessage(), e);
			}
		}
	}
}