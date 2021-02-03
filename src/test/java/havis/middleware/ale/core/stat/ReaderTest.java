package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.Statistics;
import havis.middleware.ale.service.IStat;

import org.junit.Assert;
import org.junit.Test;

public class ReaderTest {

    @Test
    public void reader() {
        Reader<IStat, Statistics> reader = new Reader<IStat, Statistics>("test") {
            @Override
            public IStat getStat(Statistics source) {
                return null;
            }
        };
        Assert.assertEquals("test", reader.getProfile());
    }
}
