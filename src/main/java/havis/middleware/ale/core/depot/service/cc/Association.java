package havis.middleware.ale.core.depot.service.cc;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.config.AssociationType;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.Depot;
import havis.middleware.ale.core.report.cc.data.Associations;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.AssocTableEntry;
import havis.middleware.ale.service.cc.AssocTableEntryList;
import havis.middleware.ale.service.cc.AssocTableSpec;
import havis.middleware.ale.service.mc.MCAssociationSpec;

import java.util.List;
import java.util.UUID;

/**
 * Implements the association depot
 */
public class Association extends Depot<AssociationType, MCAssociationSpec> {

	private static Association instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new Association();
	}

	/**
	 * Retrieves the static instance
	 * 
	 * @return The static instance
	 */
	public static Association getInstance() {
		return instance;
	}

	/**
	 * Get the entries
	 *
	 * @return The entry list
	 */
	@Override
    public List<AssociationType> getList() {
		return Config.getInstance().getService().getCc().getAssociations()
				.getAssociation();
	}

	/**
	 * Returns the entry from specification
	 *
	 * @param spec
	 *            The specification
	 * @return The entry
	 */
	@Override
    protected AssociationType get(final MCAssociationSpec spec) {
		return new AssociationType(spec.getName(), Boolean.valueOf(spec.isEnable()),
				spec.getSpec(), spec.getEntries());
	}

	/**
	 * Returns the specification from entry
	 *
	 * @param entry
	 *            The entry
	 * @return The specification
	 */
	@Override
    protected MCAssociationSpec get(final AssociationType entry) {
		return new MCAssociationSpec(entry.getName(), entry.isEnable(),
				entry.getSpec(), entry.getEntries());
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
    protected void setEnable(AssociationType entry, boolean enable)
			throws ALEException {
		if (enable) {
			Associations.getInstance().define(entry.getName(), entry.getSpec(),
					entry.getEntries().getEntries().getEntry(), false);
		} else {
			try {
				Associations.getInstance().undefine(entry.getName(), false);
			} catch (NoSuchNameException e) {
				Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Association table not found: " + e.getMessage(), e);
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
	 * @param entries
	 *            The entries
	 */
	public void add(final String name, final AssocTableSpec spec,
			final List<AssocTableEntry> entries) {
		super.add(new AssociationType(name, Boolean.TRUE, spec,
				new AssocTableEntryList(entries)));
	}

	/**
	 * Updates the given specification
	 *
	 * @param name
	 *            The name
	 * @param entries
	 *            The entries
	 */
	public void update(String name, final List<AssocTableEntry> entries) {
		UUID id = names.get(name);
		if (id != null) {
			try {
				AssociationType association = getType(id);
				if (association != null) {
					association.setEntries(new AssocTableEntryList(entries));
					Config.serialize();
				}
			} catch (NoSuchIdException e) {
				Exits.Log.logp(Exits.Level.Warning, Exits.Common.Name, Exits.Common.Warning, "Association table not found: " + e.getMessage(), e);
			}
		}
	}
}