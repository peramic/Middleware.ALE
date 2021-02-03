package havis.middleware.ale.core.report;

import org.junit.Assert;
import org.junit.Test;

public class TerminationTest {

    @Test
    public void toStringTest() {
        Assert.assertNull(Termination.toString(null));
        Assert.assertEquals("NULL", Termination.toString(Termination.NULL));
        Assert.assertEquals("COUNT", Termination.toString(Termination.COUNT));
        Assert.assertEquals("DATA_AVAILABLE", Termination.toString(Termination.DATA_AVAILABLE));
        Assert.assertEquals("DURATION", Termination.toString(Termination.DURATION));
        Assert.assertEquals("ERROR", Termination.toString(Termination.ERROR));
        Assert.assertEquals("NO_NEW_EVENTS", Termination.toString(Termination.NO_NEW_EVENTS));
        Assert.assertEquals("NO_NEW_TAGS", Termination.toString(Termination.NO_NEW_TAGS));
        Assert.assertEquals("STABLE_SET", Termination.toString(Termination.STABLE_SET));
        Assert.assertEquals("TRIGGER", Termination.toString(Termination.TRIGGER));
        Assert.assertEquals("UNDEFINE", Termination.toString(Termination.UNDEFINE));
        Assert.assertEquals("UNREQUESTED", Termination.toString(Termination.UNREQUESTED));
    }
}
