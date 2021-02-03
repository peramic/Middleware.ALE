package havis.middleware.ale.core.depot;

import havis.middleware.ale.config.Property;
import havis.middleware.ale.service.mc.MCProperty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class DepotTest {

    @Test
    public void getProperty1() {
        List<Property> properties = new ArrayList<>();
        properties.add(new Property("a", "1"));
        properties.add(new Property("b", "2"));
        properties.add(new Property("c", "3"));
        List<MCProperty> actual = Depot.getProperty(properties);

        Assert.assertNotNull(actual);
        Assert.assertEquals(3, actual.size());
        Assert.assertEquals("a", actual.get(0).getName());
        Assert.assertEquals("1", actual.get(0).getValue());
        Assert.assertEquals("b", actual.get(1).getName());
        Assert.assertEquals("2", actual.get(1).getValue());
        Assert.assertEquals("c", actual.get(2).getName());
        Assert.assertEquals("3", actual.get(2).getValue());

        Assert.assertNull(Depot.getProperty(null));
    }

    @Test
    public void getProperty2() {
        List<MCProperty> properties = new ArrayList<>();
        properties.add(new MCProperty());
        properties.get(0).setName("a");
        properties.get(0).setValue("1");
        properties.add(new MCProperty());
        properties.get(1).setName("b");
        properties.get(1).setValue("2");
        properties.add(new MCProperty());
        properties.get(2).setName("c");
        properties.get(2).setValue("3");
        List<Property> actual = Depot.getMCProperty(properties);

        Assert.assertNotNull(actual);
        Assert.assertEquals(3, actual.size());
        Assert.assertEquals("a", actual.get(0).getName());
        Assert.assertEquals("1", actual.get(0).getValue());
        Assert.assertEquals("b", actual.get(1).getName());
        Assert.assertEquals("2", actual.get(1).getValue());
        Assert.assertEquals("c", actual.get(2).getName());
        Assert.assertEquals("3", actual.get(2).getValue());

        Assert.assertNull(Depot.getMCProperty(null));
    }
}