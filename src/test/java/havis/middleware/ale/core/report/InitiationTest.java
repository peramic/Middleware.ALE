package havis.middleware.ale.core.report;

import org.junit.Assert;
import org.junit.Test;

public class InitiationTest {

    @Test
    public void toStringTest() {
        Assert.assertNull(Initiation.toString(null));
        Assert.assertEquals("NULL", Initiation.toString(Initiation.NULL));
        Assert.assertEquals("REPEAT_PERIOD", Initiation.toString(Initiation.REPEAT_PERIOD));
        Assert.assertEquals("REQUESTED", Initiation.toString(Initiation.REQUESTED));
        Assert.assertEquals("TRIGGER", Initiation.toString(Initiation.TRIGGER));
        Assert.assertEquals("UNDEFINE", Initiation.toString(Initiation.UNDEFINE));
    }
}
