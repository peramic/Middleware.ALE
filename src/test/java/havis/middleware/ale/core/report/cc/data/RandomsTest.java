package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.service.cc.RNGSpec;

import java.util.List;

import mockit.Mocked;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;


public class RandomsTest {
	private String name = "Name";
	
	@Test
	public void defineAndget(@Mocked final havis.middleware.ale.core.depot.service.cc.Random depot) throws Exception {
		Randoms randoms = new Randoms();
		final RNGSpec spec = createSpec(2);
		randoms.define(name, spec, true);
		Assert.assertEquals(new Random(name, spec), randoms.get(name));
		
		new Verifications() {{
			depot.add(name, spec);
			times = 1;
		}};
	}
	
	@Test
	public void defineDuplicate() throws Exception {
		Randoms randoms = new Randoms();
		final RNGSpec spec = createSpec(2);
		randoms.define(name, spec, false);
		try {
			randoms.define(name, spec, false);
			Assert.fail("DuplicateNameException expected");
		} catch (DuplicateNameException e) {
			Assert.assertEquals("Random number generator '" + name + "' already defined", e.getMessage());
		}
	}
	
	@Test
	public void undefineAndget(@Mocked final havis.middleware.ale.core.depot.service.cc.Random depot)throws Exception {
		Randoms randoms = new Randoms();
		final RNGSpec spec = createSpec(2);
		randoms.define(name, spec, true);
		Assert.assertEquals(spec, randoms.getSpec(name));
		randoms.undefine(name, true);
		
		Assert.assertNull(randoms.get(name));		
		new Verifications() {{
			depot.remove(name);
			times = 1;
		}};
	}
	
	@Test
	public void undefineInUse() throws Exception {
		Randoms randoms = new Randoms();
		final RNGSpec spec = createSpec(2);
		randoms.define(name, spec, false);
		Random random = randoms.get(name);
		random.inc();
		
		try {
			randoms.undefine(name, false);
		} catch (InUseException e) {
			Assert.assertEquals("Could not undefine the in use random number generator '" + name + "'", e.getMessage());
		}
	}
	
	@Test
	public void undefineUnknownRandom() throws Exception {
		Randoms randoms = new Randoms();
		try {
			randoms.undefine(name, false);
		} catch (NoSuchNameException e) {
			Assert.assertEquals("Could not undefine a unknown random number generator '" + name + "'", e.getMessage());
		}
	}
	
	@Test
	public void getSpecUnknownRandom() throws Exception {
		Randoms randoms = new Randoms();
		try {
			randoms.getSpec(name);
		} catch (NoSuchNameException e) {
			Assert.assertEquals("Could not get specification for unknown random number generator '" + name + "'", e.getMessage());
		}
	}
	
	@Test
	public void getNames() throws Exception {
		Randoms randoms = new Randoms();
		randoms.define(name, createSpec(2), false);
		randoms.define(name + "1", createSpec(2), false);
		randoms.define(name + "2", createSpec(2), false);
		
		List<String> randomList = randoms.getNames();
		Assert.assertEquals(3, randomList.size());
		Assert.assertTrue(randomList.contains(name));
		Assert.assertTrue(randomList.contains(name + "1"));
		Assert.assertTrue(randomList.contains(name + "2"));
	}
	
	@Test
	public void dispose() throws Exception {
		Randoms randoms = new Randoms();
		randoms.define(name, createSpec(2), false);
		randoms.dispose();
		Assert.assertNull(randoms.get(name));
	}
	
	private RNGSpec createSpec(int length) {
		RNGSpec spec = new RNGSpec();
		spec.setLength(length);
		return spec;
	}
}
