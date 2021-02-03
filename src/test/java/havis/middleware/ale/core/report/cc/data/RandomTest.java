package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.RNGValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.service.cc.RNGSpec;
import mockit.Deencapsulation;

import org.junit.Assert;
import org.junit.Test;

public class RandomTest {

	@Test
	public void newRandom() throws RNGValidationException {
		String name = "ValidName";
		RNGSpec spec = new RNGSpec();
		spec.setLength(10);
		Random random = new Random(name, spec);
		
		Assert.assertEquals(spec, random.getSpec());
		Assert.assertEquals(name, Deencapsulation.getField(random, "name"));
		Assert.assertNotNull(Deencapsulation.getField(random, "random")); 
		Assert.assertNull(random.getFieldDatatype());
		Assert.assertNull(random.getFieldFormat());
	}
	
	@Test
	public void newRandomNameNotValid() throws RNGValidationException {
		try {
			new Random("Non Valid Name", new RNGSpec());
		} catch (RNGValidationException e) {
			Assert.assertEquals("Name 'Non Valid Name' enthält ein UNICODE 'Pattern_White_Space' Zeichen", e.getMessage());
		}
	}
	
	@Test
	public void newRandonInvalidLength() throws RNGValidationException {
		try {
			new Random("name", new RNGSpec());
		} catch (RNGValidationException e) {
			Assert.assertEquals("Length of random number generator 'name' must be greater then zero", e.getMessage());
		}
	}
	
	@Test
	public void incDecInUse() throws RNGValidationException {
		String name = "ValidName";
		RNGSpec spec = new RNGSpec();
		spec.setLength(10);
		Random random = new Random(name, spec);
		int i = 0;
		while (i<2){
			random.inc();
			i++;
		}
		random.dec();
		Assert.assertTrue(random.isUsed());
	}
	
	@Test
	public void getBytes() throws RNGValidationException {
		String name = "ValidName";
		RNGSpec spec = new RNGSpec();
		spec.setLength(2);
		Random random = new Random(name, spec);
		Bytes bytes = random.getBytes(new Tag());
		Assert.assertEquals(2, bytes.getLength());
	}
	
	@Test (expected = UnsupportedOperationException.class)
	public void getCharacters() throws UnsupportedOperationException, RNGValidationException {
		String name = "ValidName";
		RNGSpec spec = new RNGSpec();
		spec.setLength(2);
		Random random = new Random(name, spec);
		random.getCharacters(new Tag());
	}
	
	@Test
	public void hashCodeTest() throws RNGValidationException {
		String name = "ValidName";
		RNGSpec spec = new RNGSpec();
		spec.setLength(2);
		Random random1 = new Random(name, spec);
		Random random2 = new Random(name, spec);
		Assert.assertEquals(random1.hashCode(), random2.hashCode());		
	}
	
	@Test
	public void equals() throws RNGValidationException {
		String name = "ValidName";
		RNGSpec spec = new RNGSpec();
		spec.setLength(2);
		
		Random rnd1;
		Random rnd2;
		
		rnd1 = new Random(name, spec);
		rnd2 = new Random(name, spec);
		Assert.assertTrue(rnd1.equals(rnd2));
		Assert.assertTrue(rnd1.equals(rnd1));
		
		rnd2 = null;
		Assert.assertFalse(rnd1.equals(rnd2));
		Assert.assertFalse(rnd1.equals("test"));
		
		spec.setLength(3);
		rnd2 = new Random(name, spec);
		Assert.assertFalse(rnd1.equals(rnd2));
		
		Deencapsulation.setField(rnd1, "datatype", FieldDatatype.EPC);
		Assert.assertFalse(rnd1.equals(rnd2));
		
		Deencapsulation.setField(rnd1, "datatype", null);
		Deencapsulation.setField(rnd1, "format", FieldFormat.EPC_TAG);
		Assert.assertFalse(rnd1.equals(rnd2));
	}
}
