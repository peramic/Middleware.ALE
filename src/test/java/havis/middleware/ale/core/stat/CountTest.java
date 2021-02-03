package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.ICount;
import havis.middleware.ale.service.ECReaderStat;
import havis.middleware.ale.service.ICountStat;

import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class CountTest {

    static class Dummy implements ICountStat {

        private int count;
        private String profile;

        @Override
        public String getProfile() {
            return profile;
        }

        @Override
        public void setProfile(String profile) {
            this.profile = profile;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public List<ECReaderStat> getStatBlockList() {
            return null;
        }

        @Override
        public void setStatBlockList(List<ECReaderStat> list) {
        }
    }

    static class BadDummy implements ICountStat {

        public BadDummy(boolean noDefaultConstructor) {
        }

        @Override
        public String getProfile() {
            return null;
        }

        @Override
        public void setProfile(String profile) {
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public void setCount(int count) {
        }

        @Override
        public List<ECReaderStat> getStatBlockList() {
            return null;
        }

        @Override
        public void setStatBlockList(List<ECReaderStat> list) {
        }
    }

    @Test
    public void getStat(@Mocked final ICount argument) throws InstantiationException, IllegalAccessException {
        new NonStrictExpectations() {
            {
                argument.getCount();
                result = Integer.valueOf(5);
            }
        };

        Count<Dummy> count = new Count<>("test", Dummy.class);
        ICountStat result = count.getStat(argument);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Dummy);
        Assert.assertEquals(5, result.getCount());
        Assert.assertEquals("test", result.getProfile());
    }

    @Test
    public void getStatException(@Mocked final ICount argument) throws InstantiationException, IllegalAccessException {
        new NonStrictExpectations() {
            {
                argument.getCount();
                result = Integer.valueOf(5);
            }
        };

        Count<BadDummy> count = new Count<>("test", BadDummy.class);
        Assert.assertNull(count.getStat(argument));
    }
}