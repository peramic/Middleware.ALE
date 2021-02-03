package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.cc.AssocTableEntry;
import havis.middleware.ale.service.cc.AssocTableSpec;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class AssociationsTest {

	private String name = "ValidName";
	private String epc = "EPC";
	private String iso = "ISO";

	@Test
	public void defineAndGetAssociation(@Mocked final havis.middleware.ale.core.depot.service.cc.Association depot) throws Exception {
		Associations associations = new Associations();
		final AssocTableSpec spec = createSpec(epc);
		List<AssocTableEntry> entries = createEntryList(epc);
		associations.define(name, spec, entries, true);

		// Validate the defined Association
		Assert.assertNotNull(associations.get(name));
		final Association association = associations.get(name);
		Assert.assertTrue(association.equals(new Association(name, createSpec(epc), createEntryList(epc))));
		// Validate the associations Map
		Map<String, Association> associationsMap = Deencapsulation.getField(associations, "associations");
		Assert.assertEquals(1, associationsMap.size());
		// Validate the depot entry
		new Verifications() {
			{
				List<AssocTableEntry> list;
				depot.add(withEqual(name), withSameInstance(spec), list = withCapture());
				times = 1;

				Assert.assertNotNull(list);
				Assert.assertEquals(1, list.size());
			}
		};
	}

	@Test
	public void defineAssociationAlreadyExists() throws Exception {
		Associations associations = new Associations();
		associations.define(name, createSpec(epc), createEntryList(epc), false);
		try {
			associations.define(name, createSpec(iso), createEntryList(iso), false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Association table '" + name + "' already defined", e.getMessage());
		}
	}

	@Test
	public void undefineAndGetAssociation(@Mocked final havis.middleware.ale.core.depot.service.cc.Association depot) throws Exception {
		Associations associations = new Associations();
		final AssocTableSpec spec = createSpec(epc);
		associations.define(name, spec, createEntryList(epc), true);
		// Validate the associations Map
		Map<String, Association> associationsMap = Deencapsulation.getField(associations, "associations");
		Assert.assertEquals(1, associationsMap.size());

		associations.undefine(name, true);
		// Validate the defined Association is not there
		Assert.assertNull(associations.get(name));
		// Validate the associations Map
		associationsMap = Deencapsulation.getField(associations, "associations");
		Assert.assertEquals(0, associationsMap.size());

		// Validate the depot
		new Verifications() {
			{
				List<AssocTableEntry> list;
				depot.add(withEqual(name), withSameInstance(spec), list = withCapture());
				times = 1;

				Assert.assertNotNull(list);
				Assert.assertEquals(1, list.size());

				depot.remove(name);
				times = 1;
			}
		};
	}

	@Test
	public void undefineAssociationInUse() throws Exception {
		Associations associations = new Associations();
		associations.define(name, createSpec(epc), createEntryList(epc), false);
		Association assoc = associations.get(name);
		assoc.inc();

		try {
			associations.undefine(name, false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not undefine an : use association table '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void undefineAssociationDoesNotExist() throws Exception {
		Associations associations = new Associations();
		try {
			associations.undefine(name, false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not undefine an unknown association table '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void getNames() throws Exception {
		Associations associations = new Associations();
		List<String> nameList;
		nameList = associations.getNames();
		// Validate an empty list
		Assert.assertTrue(nameList.isEmpty());

		associations.define(name, createSpec(epc), createEntryList(epc), false);
		associations.define(name + "1", createSpec(iso), createEntryList(iso), false);
		associations.define(name + "2", createSpec(epc), createEntryList(epc), false);
		nameList = associations.getNames();
		// Validate all names are in the list
		Assert.assertEquals(3, nameList.size());
		Assert.assertTrue(nameList.contains(name + "2"));
		Assert.assertTrue(nameList.contains(name + "1"));
		Assert.assertTrue(nameList.contains(name));
	}

	@Test
	public void getSpec() throws Exception {
		Associations associations = new Associations();
		AssocTableSpec spec = createSpec(epc);
		associations.define(name, spec, createEntryList(epc), false);
		// Validate the specification
		Assert.assertEquals(spec, associations.getSpec(name));
	}

	@Test
	public void getSpecAssociationDoesNotExist() throws Exception {
		Associations associations = new Associations();
		try {
			associations.getSpec(name);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not get specification for an unknown association table '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void putEntries(@Mocked final havis.middleware.ale.core.depot.service.cc.Association depot) throws Exception {
		Associations associations = new Associations();
		List<AssocTableEntry> entries = createEntryList(iso);
		associations.define(name, createSpec(iso), entries, true);
		// Validate ISO entries
		Assert.assertEquals(entries.get(0).getKey(), associations.get(name).getEntries().get(0).getKey());
		associations.putEntries(name, createEntryList(epc), true);
		// Validate EPC entries
		Assert.assertEquals(createEntry(epc).getKey(), associations.get(name).getEntries().get(0).getKey());

		new Verifications() {
			{
				depot.update(name, this.<List<AssocTableEntry>> withNotNull());
				times = 1;
			}
		};
	}

	@Test
	public void putEntriesAssociationDoesNotExist() throws Exception {
		Associations associations = new Associations();
		try {
			associations.putEntries(name, createEntryList(epc), false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not put entries to an unknown association table '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void getValue() throws Exception {
		Associations associations = new Associations();
		associations.define(name, createSpec(epc), createEntryList(epc), false);
		associations.define(name + "1", createSpec(iso), createEntryList(iso), false);
		// Validate the values of an EPC entry and an ISO entry
		Assert.assertEquals("urn:epc:raw:96.x0103070F1F3F7F3F1F0F0703", associations.getValue(name, createEntry(epc).getKey()));
		Assert.assertEquals("0123456789", associations.getValue(name + "1", createEntry(iso).getKey()));
	}

	@Test
	public void getValueAssociationDoesNotExist() throws Exception {
		Associations associations = new Associations();
		try {
			associations.getValue(name, createEntry(iso).getKey());
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not get value from an unknown association table '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void getEntries(@Mocked final Patterns patterns) throws Exception {
		Associations associations = new Associations();
		associations.define(name, createSpec(epc), createEntryList(epc), false);
		final List<String> list = new ArrayList<String>();
		list.add("testString");

		new NonStrictExpectations() {
			{
				patterns.match(TagDecoder.getInstance().fromUrn(createEntry("EPC").getKey(), new byte[0]));
				result = Boolean.TRUE;
			}
		};
		List<AssocTableEntry> entryList;
		entryList = associations.getEntries(name, list);
		// Validate the list with the enties
		Assert.assertEquals(1, entryList.size());
		Assert.assertEquals("urn:epc:tag:sgtin-96:3.0614141.812345.6789", entryList.get(0).getKey());
		Assert.assertEquals("urn:epc:raw:96.x0103070F1F3F7F3F1F0F0703", entryList.get(0).getValue());
	}

	@Test
	public void getEntriesAssociationDoesNotExist() throws Exception {
		Associations associations = new Associations();
		final List<String> list = new ArrayList<String>();
		try {
			associations.getEntries(name, list);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not get entries from an unknown association table '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void removeEntry(@Mocked final havis.middleware.ale.core.depot.service.cc.Association depot) throws Exception {
		Associations associations = new Associations();
		associations.define(name, createSpec(epc), createEntryList(epc), true);
		associations.removeEntry(name, createEntry(epc).getKey(), true);
		// Validate the empty list for the Association
		Assert.assertTrue(associations.get(name) != null);
		Assert.assertTrue(associations.get(name).getEntries().isEmpty());

		new Verifications() {
			{
				depot.update(name, this.<List<AssocTableEntry>> withNotNull());
				times = 1;
			}
		};
	}

	@Test
	public void removeEntryAssociationDoesNotExist() throws Exception {
		Associations associations = new Associations();
		try {
			associations.removeEntry(name, createEntry(epc).getKey(), false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not remove entry from an unknown association table '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void removeEntries(@Mocked final Patterns patterns, @Mocked final havis.middleware.ale.core.depot.service.cc.Association depot) throws Exception {
		Associations associations = new Associations();
		associations.define(name, createSpec(epc), createEntryList(epc), true);
		List<String> list = new LinkedList<String>();
		list.add("urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		new NonStrictExpectations() {
			{
				patterns.match(TagDecoder.getInstance().fromUrn(createEntry("EPC").getKey(), new byte[0]));
				result = Boolean.TRUE;
			}
		};
		associations.removeEntries(name, list, true);
		// Validates the empty entry list of the Association
		Assert.assertTrue(associations.get(name).getEntries().isEmpty());

		new Verifications() {
			{
				depot.update(name, this.<List<AssocTableEntry>> withNotNull());
				times = 1;
			}
		};
	}

	@Test
	public void removeEntriesAssociationDoesNotExist() throws Exception {
		Associations associations = new Associations();
		List<String> list = new LinkedList<String>();
		try {
			associations.removeEntries(name, list, false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not remove entries from an unknown association table '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void dispose() throws Exception {
		Associations associations = new Associations();
		associations.define(name, createSpec(epc), createEntryList(epc), false);
		associations.dispose();
		Map<String, Association> associationMap = Deencapsulation.getField(associations, "associations");
		Assert.assertTrue(associationMap.isEmpty());
	}

	/*
	 * Methods to create Association elements
	 * (AssocTableSpec/List<AssocTableEntry>/AssocTableEntry)
	 */

	private AssocTableSpec createSpec(String type) {
		AssocTableSpec spec = new AssocTableSpec();
		spec.setDatatype((type.equals(epc) ? "epc" : "iso-15962-string"));
		spec.setFormat((type.equals(epc) ? "epc-tag" : null));
		return spec;
	}

	private List<AssocTableEntry> createEntryList(String type) {
		List<AssocTableEntry> entries = new LinkedList<AssocTableEntry>();
		entries.add(createEntry(type));
		return entries;

	}

	private AssocTableEntry createEntry(String type) {
		AssocTableEntry entry = new AssocTableEntry();
		entry.setKey((type.equals(epc) ? "urn:epc:tag:sgtin-96:3.0614141.812345.6789" : "urn:epc:tag:grai-96:1.00000050.2437.547365"));
		entry.setValue((type.equals(epc) ? "000000010000001100000111000011110001111100111111011111110011111100011111000011110000011100000011" : "0123456789"));
		return entry;
	}
}
