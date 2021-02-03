package havis.middleware.ale.core;

import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;

import org.junit.Assert;
import org.junit.Test;

public class LogicalReaderTest {

	@Test
	public void isEqualSpec() throws Exception {
		LRSpec spec1 = new LRSpec();
		LRSpec spec2 = new LRSpec();

		Assert.assertTrue(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setIsComposite(Boolean.FALSE);
		spec2 = new LRSpec();
		spec2.setIsComposite(Boolean.FALSE);

		Assert.assertTrue(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setIsComposite(Boolean.TRUE);
		spec2 = new LRSpec();
		spec2.setIsComposite(Boolean.TRUE);

		Assert.assertTrue(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setIsComposite(Boolean.FALSE);
		spec2 = new LRSpec();
		spec2.setIsComposite(Boolean.TRUE);

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setIsComposite(null);
		spec2 = new LRSpec();
		spec2.setIsComposite(Boolean.TRUE);

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setIsComposite(Boolean.FALSE);
		spec2 = new LRSpec();
		spec2.setIsComposite(null);

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());

		Assert.assertTrue(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec2 = new LRSpec();
		spec2.setProperties(null);

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(null);
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec1.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());
		spec2.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});

		Assert.assertTrue(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec1.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());
		spec2.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});
		spec2.getProperties().getProperty().add(new LRProperty() {
			{
				setName("c");
				setValue("d");
			}
		});

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec1.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});
		spec1.getProperties().getProperty().add(new LRProperty() {
			{
				setName("c");
				setValue("d");
			}
		});
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());
		spec2.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec1.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());
		spec2.getProperties().getProperty().add(new LRProperty() {
			{
				setName("x");
				setValue("b");
			}
		});

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec1.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());
		spec2.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("x");
			}
		});

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec1.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());
		spec2.getProperties().getProperty().add(new LRProperty() {
			{
				setName(null);
				setValue("b");
			}
		});

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setProperties(new LRSpec.Properties());
		spec1.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue("b");
			}
		});
		spec2 = new LRSpec();
		spec2.setProperties(new LRSpec.Properties());
		spec2.getProperties().getProperty().add(new LRProperty() {
			{
				setName("a");
				setValue(null);
			}
		});

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec2 = new LRSpec();
		spec2.setReaders(null);

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(null);
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());

		Assert.assertTrue(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec1.getReaders().getReader().add("a");
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());
		spec2.getReaders().getReader().add("a");

		Assert.assertTrue(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec1.getReaders().getReader().add("a");
		spec1.getReaders().getReader().add("b");
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());
		spec2.getReaders().getReader().add("a");

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec1.getReaders().getReader().add("a");
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());
		spec2.getReaders().getReader().add("a");
		spec2.getReaders().getReader().add("b");

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec1.getReaders().getReader().add("a");
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());
		spec2.getReaders().getReader().add("x");

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec1.getReaders().getReader().add("a");
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());
		spec2.getReaders().getReader().add(null);

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec1.getReaders().getReader().add(null);
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());
		spec2.getReaders().getReader().add("a");

		Assert.assertFalse(LogicalReader.isEqualSpec(spec1, spec2));

		spec1 = new LRSpec();
		spec1.setReaders(new LRSpec.Readers());
		spec1.getReaders().getReader().add(null);
		spec2 = new LRSpec();
		spec2.setReaders(new LRSpec.Readers());
		spec2.getReaders().getReader().add(null);

		Assert.assertTrue(LogicalReader.isEqualSpec(spec1, spec2));
	}
}
