package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.service.cc.EPCCacheSpec;
import havis.middleware.ale.service.cc.EPCCacheSpecExtension;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class CachesTest {

	private String name = "ValidName";
	private String patternURN = "urn:epc:pat:sgtin-96:3.0614141.812345.6790";
	private String patternURN1 = "urn:epc:pat:sgtin-96:3.0614141.812345.6791";

	@Test
	public void defineAndGetCache(@Mocked final havis.middleware.ale.core.depot.service.cc.Cache depot) throws Exception {
		Caches caches = new Caches();
		final EPCCacheSpec spec = createSpec();
		final List<String> patterns = createPattern(patternURN);
		caches.define(name, spec, patterns, true);

		// Validate the defined Cache
		Assert.assertNotNull(caches.get(name));
		Cache cache = caches.get(name);
		Assert.assertTrue(cache.equals(new Cache(name, createSpec(), createPattern(patternURN))));
		// Validate the caches Map
		Map<String, Cache> cachesMap = Deencapsulation.getField(caches, "caches");
		Assert.assertEquals(1, cachesMap.size());
		// Valifate the depot entry
		new Verifications() {
			{
				depot.add(withEqual(name), withSameInstance(spec), withEqual(patterns));
				times = 1;
			}
		};
	}

	@Test
	public void defineCacheAlreadyExits() throws Exception {
		Caches caches = new Caches();
		caches.define(name, createSpec(), createPattern(patternURN), false);
		try {
			caches.define(name, createSpec(), createPattern(patternURN1), false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("EPC cache '" + name + "' already defined", e.getMessage());
		}
	}

	// depot doesnt change after dispose?
	@Test
	public void undefineAndGetCache(@Mocked final havis.middleware.ale.core.depot.service.cc.Cache depot) throws Exception {
		Caches caches = new Caches();
		caches.define(name, createSpec(), createPattern(patternURN), true);

		List<String> removedPattern = caches.undefine(name, true);
		// Validate the defined Association is not there
		Assert.assertNull(caches.get(name));
		// Validate the associations Map
		Map<String, Cache> cachesMap = Deencapsulation.getField(caches, "caches");
		Assert.assertEquals(0, cachesMap.size());

		Assert.assertEquals(1, removedPattern.size());
		Assert.assertEquals(patternURN, removedPattern.get(0));

		new Verifications() {
			{
				depot.remove(name);
				times = 1;
			}
		};
	}

	// Exception message???
	@Test
	public void undefineCacheInUse() throws Exception {
		Caches caches = new Caches();
		caches.define(name, createSpec(), createPattern(patternURN), false);
		Cache cache = caches.get(name);
		cache.inc();

		try {
			caches.undefine(name, false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("EPC cache '" + name + "' is:use", e.getMessage());
		}
	}

	@Test
	public void undefineCacheDoesNotExist() throws Exception {
		Caches caches = new Caches();
		try {
			caches.undefine(name, false);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not undefine a unknown epc cache '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void getSpec() throws Exception {
		Caches caches = new Caches();
		EPCCacheSpec spec = createSpec();
		caches.define(name, spec, createPattern(patternURN), false);
		// Validate the specification
		Assert.assertEquals(spec, caches.getSpec(name));
	}

	@Test
	public void getSpecCacheDoesNotExist() throws Exception {
		Caches caches = new Caches();
		try {
			caches.getSpec(name);
			Assert.fail("ALEException expected");
		} catch (ALEException e) {
			Assert.assertEquals("Could not get specification for unknown epc cache '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void getNames() throws Exception {
		Caches caches = new Caches();
		List<String> nameList;
		nameList = caches.getNames();
		// Validate an empty list
		Assert.assertTrue(nameList.isEmpty());

		caches.define(name, createSpec(), createPattern(patternURN), false);
		caches.define(name + "1", createSpec(), createPattern(patternURN), false);
		caches.define(name + "2", createSpec(), createPattern(patternURN1), false);
		nameList = caches.getNames();
		// Valiedate all names are in the list
		Assert.assertEquals(3, nameList.size());
		Assert.assertTrue(nameList.contains(name + "2"));
		Assert.assertTrue(nameList.contains(name + "1"));
		Assert.assertTrue(nameList.contains(name));
	}

	@Test
	public void replenish(@Mocked final havis.middleware.ale.core.depot.service.cc.Cache depot) throws Exception {
		Caches caches = new Caches();
		caches.define(name, createSpec(), createPattern(patternURN), true);
		caches.replenish(name, createPattern(patternURN1), true);
		Cache cache = caches.get(name);
		List<String> patternList = cache.getPatterns();
		// Validates that the pattern from the 'patterns' list are added to the
		// cache with the name 'name'
		Assert.assertEquals(2, patternList.size());
		Assert.assertEquals(patternURN1, patternList.get(1));
	}

	@Test
	public void replenishCacheDoesNotExist() throws Exception {
		Caches caches = new Caches();
		try {
			caches.replenish(name, createPattern(patternURN1), false);
			Assert.fail("NoSuchNameException expected");
		} catch (NoSuchNameException e) {
			Assert.assertEquals("Could not replenish a unknown epc cache '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void deplete(@Mocked final havis.middleware.ale.core.depot.service.cc.Cache depot) throws Exception {
		Caches caches = new Caches();
		caches.define(name, createSpec(), createPattern(patternURN), true);
		List<String> patternList = caches.deplete(name, true);

		// Validates that the correct pattern were depleted
		Assert.assertEquals(1, patternList.size());
		Assert.assertEquals(patternURN, patternList.get(0));

		new Verifications() {
			{
				depot.update(name, null);
				times = 1;
			}
		};
	}

	@Test
	public void depleteCacheDoesNotExist() throws Exception {
		Caches caches = new Caches();
		try {
			caches.deplete(name, false);
			Assert.fail("NoSuchNameException expected");
		} catch (NoSuchNameException e) {
			Assert.assertEquals("Could not deplete a unknown epc cache '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void getContents() throws Exception {
		Caches caches = new Caches();
		caches.define(name, createSpec(), createPattern(patternURN), false);
		caches.replenish(name, createPattern(patternURN1), false);
		List<String> cachePattern = caches.getContents(name);
		// Validates that the cache with the name 'name' has two pattern
		Assert.assertEquals(2, cachePattern.size());
		Assert.assertEquals(patternURN, cachePattern.get(0));
		Assert.assertEquals(patternURN1, cachePattern.get(1));
	}

	@Test
	public void getContentsCacheDoesNotExist() throws Exception {
		Caches caches = new Caches();
		try {
			caches.getContents(name);
			Assert.fail("NoSuchNameException expected");
		} catch (NoSuchNameException e) {
			Assert.assertEquals("Could not get contents for a unknown epc cache '" + name + "'", e.getMessage());
		}
	}

	@Test
	public void dispose() throws Exception {
		Caches caches = new Caches();
		caches.define(name, createSpec(), createPattern(patternURN), false);
		caches.dispose();
		Map<String, Cache> cachesMap = Deencapsulation.getField(caches, "caches");
		Assert.assertTrue(cachesMap.values().isEmpty());
	}

	private EPCCacheSpec createSpec() {
		EPCCacheSpec spec = new EPCCacheSpec();
		spec.setExtension(new EPCCacheSpecExtension());
		return spec;
	}

	private List<String> createPattern(String urn) {
		List<String> patternList = new LinkedList<String>();
		patternList.add(urn);
		return patternList;
	}
}
