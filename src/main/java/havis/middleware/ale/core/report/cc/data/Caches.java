package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.EPCCacheSpecValidationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.InvalidPatternException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.EPCCacheSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements the EPC cache as specified:ALE 1.1.1 (9.5)
 */
public class Caches {

	private Lock lock = new ReentrantLock();

	private havis.middleware.ale.core.depot.service.cc.Cache depot = havis.middleware.ale.core.depot.service.cc.Cache.getInstance();

	private static Caches instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new Caches();
	}

	private Map<String, Cache> caches;

	Caches() {
		caches = new HashMap<String, Cache>();
	}

	/**
	 * Gets the static instance
	 */
	public static Caches getInstance() {
		return instance;
	}

	/**
	 * Retrieves the EPC cache by name
	 * 
	 * @param name
	 *            The name of the EPC cache
	 * @return The EPC cache
	 */
	public Cache get(String name) {
		lock.lock();
		try {
			Cache cache = caches.get(name);
			if (cache != null) {
				return cache;
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Defines a EPC cache.
	 * 
	 * @param name
	 *            The name of the EPC cache
	 * @param spec
	 *            The specification of the EPC cache
	 * @param patterns
	 *            List of patterns
	 * @param persist
	 *            Persist changes
	 * @throws DuplicateNameException
	 *             if named EPC cache already exists.
	 * @throws InvalidPatternException
	 * @throws EPCCacheSpecValidationException
	 */
	public void define(String name, EPCCacheSpec spec, List<String> patterns,
			boolean persist) throws DuplicateNameException,
			InvalidPatternException, EPCCacheSpecValidationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.Define, "Define {0} {1} {2}", new Object[] { name, spec, patterns });
		try {
			lock.lock();
			try {
				if (caches.containsKey(name)) {
					throw new DuplicateNameException("EPC cache '" + name
							+ "' already defined");
				} else {
					caches.put(name, new Cache(name, spec, patterns));
					if (persist)
						depot.add(name, spec, patterns);
				}
			} finally {
				lock.unlock();
			}
		} catch (DuplicateNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.DefineFailed, "Define failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Un-defines EPC cache.
	 * 
	 * @param name
	 *            The name of the EPC cache
	 * @param persist
	 *            Persist changes
	 * @return the patterns removed from the cache
	 * @throws InUseException
	 * @throws NoSuchNameException
	 *             if no EPC cache with that name exists.
	 */
	public List<String> undefine(String name, boolean persist)
			throws InUseException, NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.Undefine, "Undefine {0}", name);
		try {
			lock.lock();
			try {
				Cache cache = caches.get(name);
				if (cache != null) {
					if (cache.isUsed()) {
						throw new InUseException("EPC cache '" + name
								+ "' is:use");
					} else {
						caches.remove(name);
						if (persist)
							depot.remove(name);
						return cache.getPatterns();
					}
				} else {
					throw new NoSuchNameException(
							"Could not undefine a unknown epc cache '" + name
									+ "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (InUseException | NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.UndefineFailed, "Undefine failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves the specification of a named EPC cache.
	 * 
	 * @param name
	 *            The name of the EPC cache
	 * @return The EPC cache specification
	 * @throws NoSuchNameException
	 *             if no EPC cache with that name exists.
	 */
	public EPCCacheSpec getSpec(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.Get, "Get {0}", name);
		try {
			lock.lock();
			try {
				Cache cache = caches.get(name);
				if (cache != null) {
					return cache.getSpec();
				} else {
					throw new NoSuchNameException(
							"Could not get specification for unknown epc cache '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.GetFailed, "Get failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves the list of all EPC cache names.
	 * 
	 * @return The list of EPC cache names
	 */
	public List<String> getNames() {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.GetNames, "Get names");
		lock.lock();
		try {
			return new ArrayList<String>(caches.keySet());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Replenishes the list of patterns of a named EPC cache.
	 * 
	 * @param name
	 *            The name of the EPC cache
	 * @param patterns
	 *            The pattern list
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 *             if no EPC cache with that name exists.
	 * @throws InvalidPatternException
	 */
	public void replenish(String name, List<String> patterns, boolean persist)
			throws NoSuchNameException, InvalidPatternException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.Replenish, "Replenish {0} {1}", new Object[] { name, patterns });
		try {
			lock.lock();
			try {
				Cache cache = caches.get(name);
				if (cache != null) {
					cache.replenish(patterns);
					if (persist)
						depot.update(name, cache.getPatterns());
				} else {
					throw new NoSuchNameException(
							"Could not replenish a unknown epc cache '" + name
									+ "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.ReplenishFailed, "Replenish failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Depletes the patterns of the named EPC cache
	 * 
	 * @param name
	 *            The name of the EPC cache
	 * @param persist
	 *            Persist changes
	 * @return the removed list of patterns
	 * @throws NoSuchNameException
	 *             if no EPC cache with that name exists.
	 */
	public List<String> deplete(String name, boolean persist)
			throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.Deplete, "Deplete {0}", name);
		try {
			lock.lock();
			try {
				Cache cache = caches.get(name);
				if (cache != null) {
					List<String> list = cache.deplete();
					if (persist)
						depot.update(name, null);
					return list;
				} else {
					throw new NoSuchNameException(
							"Could not deplete a unknown epc cache '" + name
									+ "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.DepleteFailed, "Deplete failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retries the pattern list of a named EPC cache
	 * 
	 * @param name
	 *            The name of the EPC cache
	 * @return The list of patterns
	 * @throws NoSuchNameException
	 *             if no EPC cache with that name exists.
	 */
	public List<String> getContents(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.GetContents, "Get contents {0}", name);
		try {
			lock.lock();
			try {
				Cache cache = caches.get(name);
				if (cache != null) {
					return cache.getPatterns();
				} else {
					throw new NoSuchNameException(
							"Could not get contents for a unknown epc cache '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Cache.Name, Exits.Service.CC.Cache.GetContentsFailed, "Get contents failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Disposes instance
	 */
	public void dispose() {
		lock.lock();
		try {
			for (Cache cache : caches.values()) {
				cache.dispose();
			}
			caches.clear();
		} finally {
			lock.unlock();
		}
	}
}