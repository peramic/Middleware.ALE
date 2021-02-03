package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.RNGValidationException;
import havis.middleware.ale.config.RandomType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.Depot;
import havis.middleware.ale.core.report.cc.data.Randoms;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.RNGSpec;
import havis.middleware.ale.service.mc.MCRandomSpec;

import java.util.List;

/**
 * Implements the random data depot
 */
public class Random extends Depot<RandomType, MCRandomSpec> {

	private static Random instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new Random();
	}

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static Random getInstance() {
		return instance;
	}

	/**
	 * Retrieves the entries
	 *
	 * @return The list of random types from configuration
	 */
	@Override
    protected List<RandomType> getList() {
		return Config.getInstance().getService().getCc().getRandoms()
				.getRandom();
	}

	/**
	 * Returns the entry from specification
	 *
	 * @param spec
	 *            The specification
	 * @return The configuration entry
	 */
	@Override
    protected RandomType get(final MCRandomSpec spec) {
		return new RandomType(spec.getName(), spec.isEnable(), spec.getSpec());
	}

	/**
	 * Returns the specification from entry
	 *
	 * @param entry
	 *            The entry
	 * @return The specification
	 */
	@Override
    protected MCRandomSpec get(final RandomType entry) {
		return new MCRandomSpec(entry.getName(), entry.isEnable(),
				entry.getSpec());
	}

	/**
	 * Sets the enable state
	 *
	 * @param entry
	 *            The entry
	 * @throws InUseException
	 * @throws RNGValidationException
	 * @throws DuplicateNameException
	 */
	@Override
    protected void setEnable(RandomType entry, boolean enable)
			throws ALEException {
		if (enable) {
			Randoms.getInstance().define(entry.getName(), entry.getSpec(),
					false);
		} else {
			try {
				Randoms.getInstance().undefine(entry.getName(), false);
			} catch (NoSuchNameException e) {
				Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Random number generator not found: " + e.getMessage(), e);
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
	 */
	public void add(final String name, final RNGSpec spec) {
		super.add(new RandomType(name, true, spec));
	}
}