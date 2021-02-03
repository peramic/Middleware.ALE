package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.ISightings;
import havis.middleware.ale.base.operation.tag.Sighting;
import havis.middleware.ale.service.ECReaderStat;
import havis.middleware.ale.service.IStat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class ReaderNamesTest {

    static class Dummy implements IStat {

        private String profile;
        private List<ECReaderStat> statBlockList;

        @Override
        public String getProfile() {
            return profile;
        }

        @Override
        public void setProfile(String profile) {
            this.profile = profile;
        }

        @Override
        public List<ECReaderStat> getStatBlockList() {
            return this.statBlockList;
        }

        @Override
        public void setStatBlockList(List<ECReaderStat> list) {
            this.statBlockList = list;
        }
    }

    static class BadDummy implements IStat {

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
        public List<ECReaderStat> getStatBlockList() {
            return null;
        }

        @Override
        public void setStatBlockList(List<ECReaderStat> list) {
        }
    }

    @Test
    public void getStat(@Mocked final ISightings argument) throws InstantiationException, IllegalAccessException {
        final Map<String, List<Sighting>> sightings = new LinkedHashMap<>();
        sightings.put("reader1", new ArrayList<Sighting>());
        sightings.put("reader2", new ArrayList<Sighting>());

        new NonStrictExpectations() {
            {
                argument.getSightings();
                result = sightings;
            }
        };

        ReaderNames<Dummy> readerNames = new ReaderNames<>("test", Dummy.class);
        IStat result = readerNames.getStat(argument);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Dummy);
        List<ECReaderStat> statBlockList = result.getStatBlockList();
        Assert.assertNotNull(statBlockList);
        Assert.assertEquals(2, statBlockList.size());
        Assert.assertNotNull(statBlockList.get(0));
        Assert.assertEquals("reader1", statBlockList.get(0).getReaderName());
        Assert.assertNotNull(statBlockList.get(1));
        Assert.assertEquals("reader2", statBlockList.get(1).getReaderName());
        Assert.assertEquals("test", result.getProfile());
    }

    @Test
    public void getStatException(@Mocked final ISightings argument) throws InstantiationException, IllegalAccessException {
        new NonStrictExpectations() {
            {
                argument.getSightings();
                result = null;
            }
        };

        ReaderNames<BadDummy> readerNames = new ReaderNames<>("test", BadDummy.class);
        Assert.assertNull(readerNames.getStat(argument));
    }
}
