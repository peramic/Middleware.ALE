package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.AssocTableValidationException;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.InvalidAssocTableEntryException;
import havis.middleware.ale.base.exception.InvalidEPCException;
import havis.middleware.ale.base.exception.InvalidPatternException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.cc.AssocTableEntry;
import havis.middleware.ale.service.cc.AssocTableSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements the association table as specified : ALE 1.1.1 (9.6)
 */
public class Associations {

	private Lock lock = new ReentrantLock();

	private havis.middleware.ale.core.depot.service.cc.Association depot = havis.middleware.ale.core.depot.service.cc.Association.getInstance();

	private static Associations instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new Associations();
	}

	/**
	 * Gets the static instance
	 */
	public static Associations getInstance() {
		return instance;
	}

	private Map<String, Association> associations;

	/**
	 * Creates a new instance
	 */
	Associations() {
		associations = new HashMap<String, Association>();
	}

	/**
	 * Gets the association
	 * 
	 * @param name
	 *            The name of the association
	 * @return The association
	 */
	Association get(String name) {
		lock.lock();
		try {
			Association association = associations.get(name);
			if (association != null) {
				return association;
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Defines a named association table with initial entries.
	 * 
	 * @param name
	 *            The name of the association table
	 * @param spec
	 *            The specification of the association table
	 * @param entries
	 *            The entry list
	 * @param persist
	 *            Persist changes
	 * @throws InvalidAssocTableEntryException
	 * @throws InvalidEPCException
	 * @throws AssocTableValidationException
	 * @throws DuplicateNameException
	 *             if association table with that name already exists.
	 */
	public void define(String name, AssocTableSpec spec,
			List<AssocTableEntry> entries, boolean persist)
			throws InvalidAssocTableEntryException,
			AssocTableValidationException, DuplicateNameException, ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.Define, "Define {0} {1} {2}", new Object[] { name, spec, entries });
		try {
			lock.lock();
			try {
				if (associations.containsKey(name)) {
					throw new DuplicateNameException("Association table '"
							+ name + "' already defined");
				} else {
					Association association = new Association(name, spec, entries);
					associations.put(name, association);
					if (persist)
						depot.add(name, spec, association.getEntries());
				}
			} finally {
				lock.unlock();
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.DefineFailed, "Define failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Undefines association table
	 * 
	 * @param name
	 *            The name of the association table
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 *             if association table is in use.
	 * @throws InUseException
	 *             if no association table with that name exists.
	 */
	public void undefine(String name, boolean persist)
			throws NoSuchNameException, InUseException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.Undefine, "Undefine {0}", name);
		try {
			lock.lock();
			try {
				Association association = associations.get(name);
				if (association != null) {
					if (association.isUsed()) {
						throw new InUseException(
								"Could not undefine an : use association table '"
										+ name + "'");
					} else {
						associations.remove(name);
						association.dispose();
						if (persist)
							depot.remove(name);
					}
				} else {
					throw new NoSuchNameException(
							"Could not undefine an unknown association table '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.UndefineFailed, "Undefine failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves names of all association tables.
	 * 
	 * @return List of association table names
	 */
	public List<String> getNames() {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.GetNames, "Get names");
		lock.lock();
		try {
			return new ArrayList<String>(associations.keySet());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Retrieves the specification of the association table
	 * 
	 * @param name
	 *            The name of the association table
	 * @return The specification of the association table
	 * @throws NoSuchNameException
	 *             if no association table with that name exists.
	 */
	public AssocTableSpec getSpec(String name) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.Get, "Get {0}", name);
		try {
			lock.lock();
			try {
				Association association = associations.get(name);
				if (association != null) {
					return association.getSpec();
				} else {
					throw new NoSuchNameException(
							"Could not get specification for an unknown association table '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.GetFailed, "Get failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Puts entries to association table.
	 * 
	 * @param name
	 *            The name of the association table
	 * @param entries
	 *            The entries to put
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 *             if no association table with that name exists.
	 * @throws InvalidAssocTableEntryException
	 * @throws ImplementationException
	 * @throws InvalidEPCException
	 */
	public void putEntries(String name, List<AssocTableEntry> entries,
			boolean persist) throws NoSuchNameException,
			InvalidAssocTableEntryException, ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.PutEntries, "Put entries {0} {1}", new Object[] { name, entries });
		try {
			lock.lock();
			try {
				Association association = associations.get(name);
				if (association != null) {
					association.putEntries(entries);
					if (persist)
						depot.update(name, association.getEntries());
				} else {
					throw new NoSuchNameException(
							"Could not put entries to an unknown association table '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.PutEntriesFailed, "Put entries failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves the value of a association table by EPC
	 * 
	 * @param name
	 *            The name of the association table
	 * @param epc
	 *            The EPC of the entry
	 * @return The value of the entry
	 * @throws NoSuchNameException
	 *             if no association table with that name exists.
	 */
	public String getValue(String name, String epc) throws NoSuchNameException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.GetValue, "Get value {0} {1}", new Object[] { name, epc });
		try {
			lock.lock();
			try {
				Association association = associations.get(name);
				if (association != null) {
					return association.getValue(epc);
				} else {
					throw new NoSuchNameException(
							"Could not get value from an unknown association table '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.GetValueFailed, "Get value failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Retrieves all association table entries where a pattern matches.
	 * 
	 * @param name
	 *            The name of the association table
	 * @param patterns
	 *            The pattern list
	 * @return The association table entries where a pattern matches
	 * @throws NoSuchNameException
	 *             if no association table with that name exists.
	 * @throws InvalidPatternException
	 */
	public List<AssocTableEntry> getEntries(String name, List<String> patterns)
			throws NoSuchNameException, InvalidPatternException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.GetEntries, "Get entries {0} {1}", new Object[] { name, patterns });
		try {
			lock.lock();
			try {
				Association association = associations.get(name);
				if (association != null) {
					return association.getEntries(patterns);
				} else {
					throw new NoSuchNameException(
							"Could not get entries from an unknown association table '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.GetEntriesFailed, "Get entries failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Removes a entry from association table by EPC.
	 * 
	 * @param name
	 *            The name of the association table
	 * @param epc
	 *            The EPC of the entry
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 *             if no association table with that name exists.
	 * @throws InvalidEPCException
	 * @throws ImplementationException
	 * @throws InvalidAssocTableEntryException
	 */
	public void removeEntry(String name, String epc, boolean persist)
			throws NoSuchNameException, InvalidEPCException,
			ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.RemoveEntry, "Remove entry {0} {1}", new Object[] { name, epc });
		try {
			lock.lock();
			try {
				Association association = associations.get(name);
				if (association != null) {
					association.removeEntry(epc);
					if (persist)
						depot.update(name, association.getEntries());
				} else {
					throw new NoSuchNameException(
							"Could not remove entry from an unknown association table '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.RemoveEntryFailed, "Remove entry failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Removes all entries from association table by pattern list.
	 * 
	 * @param name
	 *            The name of the association table
	 * @param patterns
	 *            The pattern list
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 *             if no association table with that name exists.
	 * @throws InvalidPatternException
	 * @throws InvalidAssocTableEntryException
	 * @throws ImplementationException
	 */
	public void removeEntries(String name, List<String> patterns,
			boolean persist) throws NoSuchNameException,
			InvalidPatternException, ImplementationException {
		Exits.Log.logp(Exits.Level.Detail, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.RemoveEntries, "Remove entries {0} {1}", new Object[] { name, patterns });
		try {
			lock.lock();
			try {
				Association association = associations.get(name);
				if (association != null) {
					association.removeEntries(patterns);
					if (persist)
						depot.update(name, association.getEntries());
				} else {
					throw new NoSuchNameException(
							"Could not remove entries from an unknown association table '"
									+ name + "'");
				}
			} finally {
				lock.unlock();
			}
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Service.CC.Association.Name, Exits.Service.CC.Association.RemoveEntriesFailed, "Remove entries failed: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Disposes instance
	 */
	public void dispose() {
		lock.lock();
		try {
			for (Association association : associations.values()) {
				association.dispose();
			}
			associations.clear();
		} finally {
			lock.unlock();
		}
	}
}