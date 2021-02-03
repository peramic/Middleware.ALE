package havis.middleware.ale.core.stat;

import havis.middleware.ale.base.operation.ITimestamps;
import havis.middleware.ale.service.ECReaderStat;
import havis.middleware.ale.service.ITimestampStat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class TimestampsTest {

    static class Dummy implements ITimestampStat {

        private String profile;
        private List<ECReaderStat> statBlockList;
        private Date firstSightingTime;
        private Date lastSightingTime;

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

        public Date getFirstSightingTime() {
            return this.firstSightingTime;
        }

        @Override
        public void setFirstSightingTime(Date date) {
            this.firstSightingTime = date;
        }

        public Date getLastSightingTime() {
            return this.lastSightingTime;
        }

        @Override
        public void setLastSightingTime(Date date) {
            this.lastSightingTime = date;
        }
    }

    static class BadDummy implements ITimestampStat {

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

        @Override
        public void setFirstSightingTime(Date date) {
        }

        @Override
        public void setLastSightingTime(Date date) {
        }
    }

    public Date getDate(int day, int month, int year) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month);
        date.set(Calendar.DAY_OF_MONTH, day);

        return date.getTime();
    }

    public Date getDate(int day, int month, int year, int hour, int minute, int second) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month);
        date.set(Calendar.DAY_OF_MONTH, day);
        date.set(Calendar.HOUR_OF_DAY, hour);
        date.set(Calendar.MINUTE, minute);
        date.set(Calendar.SECOND, second);

        return date.getTime();
    }

    @Test
    public void getStat(@Mocked final ITimestamps argument) throws InstantiationException, IllegalAccessException {
        final Date firstTime = getDate(10, 9 + 1, 2015);
        final Date lastTime = getDate(11, 9 + 1, 2015);

        new NonStrictExpectations() {
            {
                argument.getFirstTime();
                result = firstTime;

                argument.getLastTime();
                result = lastTime;
            }
        };

        Timestamps<Dummy> timestamps = new Timestamps<>("test", Dummy.class);
        ITimestampStat result = timestamps.getStat(argument);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Dummy);

        Dummy dummyResult = (Dummy) result;
        Assert.assertEquals(firstTime, dummyResult.getFirstSightingTime());
        Assert.assertEquals(lastTime, dummyResult.getLastSightingTime());

        Assert.assertEquals("test", dummyResult.getProfile());
    }

    @Test
    public void getStatNoFirstTime(@Mocked final ITimestamps parameter) throws InstantiationException, IllegalAccessException {
        final Date firstTime = null;
        final Date lastTime = getDate(2015, 9 + 1, 10, 10, 10, 10);

        new NonStrictExpectations() {
            {
                parameter.getFirstTime();
                result = firstTime;

                parameter.getLastTime();
                result = lastTime;
            }
        };

        Timestamps<Dummy> timestamps = new Timestamps<>("test", Dummy.class);
        ITimestampStat result = timestamps.getStat(parameter);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Dummy);

        Dummy dummyResult = (Dummy) result;
        Assert.assertNull(dummyResult.getFirstSightingTime());
        Assert.assertNull(dummyResult.getLastSightingTime());

        Assert.assertEquals("test", dummyResult.getProfile());
    }

    @Test
    public void getStatException(@Mocked final ITimestamps argument) {
        Timestamps<BadDummy> timestamps = new Timestamps<>("test", BadDummy.class);
        Assert.assertNull(timestamps.getStat(argument));
    }
}
