package havis.middleware.ale.core.config;

import havis.middleware.ale.config.Property;

import org.junit.Assert;

import org.junit.Test;

public class PropertyTest {
	@Test
	public void propertyTest(){
		Property actual = new Property("test", "value");
		Assert.assertEquals("test", actual.getName());
		Assert.assertEquals("value", actual.getValue());
		actual.setName("value");
		actual.setValue("test");
		Assert.assertEquals("value", actual.getName());
		Assert.assertEquals("test", actual.getValue());
	}
}
