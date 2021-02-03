package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.ISightings;
import havis.middleware.ale.base.operation.tag.Sighting;
import havis.middleware.ale.service.ECReaderStat;
import havis.middleware.ale.service.IStat;
import havis.middleware.ale.service.ec.ECSightingSignalStat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;


public class SightingSignalTest {


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
        sightings.put("reader1", new ArrayList<Sighting>(Arrays.asList(new Sighting("host1", (short) 1, 2), new Sighting("host1", (short) 3, 4))));
        sightings.put("reader2", new ArrayList<Sighting>(Arrays.asList(new Sighting("host2", (short) 5, 6))));
        sightings.put("reader3", new ArrayList<Sighting>());

        new NonStrictExpectations() {
            {
                argument.getSightings();
                result = sightings;
            }
        };

        SightingSignal<Dummy> sightingSignal = new SightingSignal<>("test", Dummy.class);
        IStat result = sightingSignal.getStat(argument);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Dummy);
        List<ECReaderStat> statBlockList = result.getStatBlockList();
        Assert.assertNotNull(statBlockList);
        Assert.assertEquals(3, statBlockList.size());
        Assert.assertNotNull(statBlockList.get(0));
        Assert.assertEquals("reader1", statBlockList.get(0).getReaderName());
        Assert.assertNotNull(statBlockList.get(0).getSightings());
        Assert.assertEquals(2, statBlockList.get(0).getSightings().getSighting().size());
        Assert.assertTrue(statBlockList.get(0).getSightings().getSighting().get(0) instanceof ECSightingSignalStat);
        Assert.assertEquals("host1", ((ECSightingSignalStat) statBlockList.get(0).getSightings().getSighting().get(0)).getHost());
        Assert.assertEquals(1, ((ECSightingSignalStat) statBlockList.get(0).getSightings().getSighting().get(0)).getAntenna());
        Assert.assertEquals(2, ((ECSightingSignalStat) statBlockList.get(0).getSightings().getSighting().get(0)).getStrength());
        Assert.assertTrue(statBlockList.get(0).getSightings().getSighting().get(1) instanceof ECSightingSignalStat);
        Assert.assertEquals("host1", ((ECSightingSignalStat) statBlockList.get(0).getSightings().getSighting().get(1)).getHost());
        Assert.assertEquals(3, ((ECSightingSignalStat) statBlockList.get(0).getSightings().getSighting().get(1)).getAntenna());
        Assert.assertEquals(4, ((ECSightingSignalStat) statBlockList.get(0).getSightings().getSighting().get(1)).getStrength());

        Assert.assertNotNull(statBlockList.get(1));
        Assert.assertEquals("reader2", statBlockList.get(1).getReaderName());
        Assert.assertNotNull(statBlockList.get(1).getSightings());
        Assert.assertEquals(1, statBlockList.get(1).getSightings().getSighting().size());
        Assert.assertTrue(statBlockList.get(1).getSightings().getSighting().get(0) instanceof ECSightingSignalStat);
        Assert.assertEquals("host2", ((ECSightingSignalStat) statBlockList.get(1).getSightings().getSighting().get(0)).getHost());
        Assert.assertEquals(5, ((ECSightingSignalStat) statBlockList.get(1).getSightings().getSighting().get(0)).getAntenna());
        Assert.assertEquals(6, ((ECSightingSignalStat) statBlockList.get(1).getSightings().getSighting().get(0)).getStrength());

        Assert.assertNotNull(statBlockList.get(2));
        Assert.assertEquals("reader3", statBlockList.get(2).getReaderName());
        Assert.assertNotNull(statBlockList.get(2).getSightings());
        Assert.assertEquals(0, statBlockList.get(2).getSightings().getSighting().size());

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

        SightingSignal<BadDummy> sightingSignal = new SightingSignal<>("test", BadDummy.class);
        Assert.assertNull(sightingSignal.getStat(argument));
    }
}
