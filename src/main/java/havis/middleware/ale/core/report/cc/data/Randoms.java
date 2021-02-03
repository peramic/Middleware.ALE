package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.RNGValidationException;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.RNGSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements the random number generator as specified in ALE 1.1.1 (9.7)
 */
public class Randoms {

	private Lock lock = new ReentrantLock();

	private havis.middleware.ale.core.depot.service.cc.Random depot = havis.middleware.ale.core.depot.service.cc.Random.getInstance();

	private static Randoms instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new Randoms();
	}

	/**
	 * Gets the static instance
	 */
	public static Randoms getInstance() {
		return instance;
	}

	private Map<String, Random> randoms;

	Randoms() {
		randoms = new HashMap<String, Random>();
	}

	/**
	 * Retrieves the random number generator by name
	 * 
	 * @param name
	 *            The name of the random number generator
	 * @return The random number generator
	 */
	Random get(String name) {
		lock.lock();
		try {
			Random random = randoms.get(name);
			if (random != null) {
				return random;
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Defines a random number generator.
	 * 
	 * @param name
	 *            The name of the random number generator
	 * @param spec
	 *            The specification of the random number generator
	 * @param persist
	 *            Persist changes
	 * @throws DuplicateNameException
	 *             If a random number generator with that name already exists.
	 * @throws RNGValidationException
	 */
	public void define(String name, RNGSpec spec, boolean persist)
			throws DuplicateNameException, RNGValidationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Random.Name, Exits.Service.CC.Random.Define, "Define {0} {1}", new Object[] { name, spec });
		try {
			lock.lock();
			try {
				if (randoms.containsKey(name)) {
					throw new DuplicateNameException(
							"Random number generator '" + name
									+ "' already defined");
				} else {
					randoms.put(name, new Random(name, spec));
					if (persist)
						depot.add(name, spec);
				}
			} finally {
				lock.unlock();
			}
		} catch (DuplicateNameException | RNGValidationException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Random.Name, Exits.Service.CC.Random.DefineFailed, "Define failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Un-defines a random number generator.
	 * 
	 * @param name
	 *            The name of the random number generator
	 * @param persist
	 *            Persist changes
	 * @throws InUseException
	 *             If the random number generator is in use.
	 * @throws NoSuchNameException
	 *             if no random number generator with that name exists.
	 */
	public void undefine(String name, boolean persist) throws InUseException,
			NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Random.Name, Exits.Service.CC.Random.Undefine, "Undefine {0}", name);
		try {
			lock.lock();
			try {
				Random random = randoms.get(name);
				if (random != null) {
					if (random.isUsed()) {
						throw new InUseException(
								"Could not undefine the in use random number generator '"
										+ name + "'");
					} else {
						randoms.remove(name);
						if (persist)
							depot.remove(name);
					}
				} else {
					throw new NoSuchNameException(
							"Could not undefine a unknown random number generator '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (InUseException | NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Random.Name, Exits.Service.CC.Random.UndefineFailed, "Undefine failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves the specification of the random number generator
	 * 
	 * @param name
	 *            The name of the random number generator
	 * @return The random generator specification
	 * @throws NoSuchNameException
	 *             If no random number generator with that name exists.
	 */
	public RNGSpec getSpec(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Random.Name, Exits.Service.CC.Random.Get, "Get {0}", name);
		try {
			lock.lock();
			try {
				Random random = randoms.get(name);
				if (random != null) {
					return random.getSpec();
				} else {
					throw new NoSuchNameException(
							"Could not get specification for unknown random number generator '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (NoSuchNameException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Random.Name, Exits.Service.CC.Random.GetFailed, "Get failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves the names of all random number generators.
	 * 
	 * @return List of random number generator names
	 */
	public List<String> getNames() {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Random.Name, Exits.Service.CC.Random.GetNames, "Get names");
		lock.lock();
		try {
			return new ArrayList<String>(randoms.keySet());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Disposes instance
	 */
	public void dispose() {
		lock.lock();
		try {
			randoms.clear();
		} finally {
			lock.unlock();
		}
	}
}