package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.trigger.Trigger.Callback;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class RtcTriggerTest {

	private boolean isDst() {
		return TimeZone.getDefault().inDaylightTime(new Date());
	}

	private long getPassedMsOfDay() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long now = c.getTimeInMillis();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return now - c.getTimeInMillis();
	}

	@Test
	public void rtcTrigger() throws ValidationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		// ^urn:epcglobal:ale:trigger:rtc:(?<period>\d+)\.(?<offset>\d+)(\.(?<timezone>(Z|([+-]\d{2}:\d{2}))))?$
		RtcTrigger rtcTrigger = new RtcTrigger("1", "urn:epcglobal:ale:trigger:rtc:1000.500.-08:00", callback);
		try {
			Assert.assertEquals(1000, rtcTrigger.period);
			Assert.assertEquals(500 - (-28800000), rtcTrigger.offset);
		} finally {
			rtcTrigger.dispose();
		}

		rtcTrigger = new RtcTrigger("2", "urn:epcglobal:ale:trigger:rtc:7000.0", callback);
		try {
			Assert.assertEquals(7000, rtcTrigger.period);
			Assert.assertEquals(isDst() ? -7200000 : -3600000, rtcTrigger.offset);
		} finally {
			rtcTrigger.dispose();
		}

		try {
			new RtcTrigger("3", "urn:epcglobal:ale:trigger:rtc:1000.1000.-08:00", callback);
			Assert.fail();
		} catch (ValidationException e) {
			Assert.assertEquals("Offset of rtc trigger less then zero or not less then period", e.getMessage());
		}
		try {
			new RtcTrigger("4", "urn:epcglobal:ale:trigger:rtc:0.1000.-08:00", callback);
			Assert.fail();
		} catch (ValidationException e) {
			Assert.assertEquals("Period of rtc trigger not greater then zero or greater then seconds of a day", e.getMessage());
		}
		try {
			new RtcTrigger("5", "urn:epcglobal:ale:trigger:rtc:..-08:00", callback);
			Assert.fail();
		} catch (ValidationException e) {
			Assert.assertEquals("No period or offset given in rtc trigger", e.getMessage());
		}
	}

	@Test
	public void rtcOffset() throws ValidationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		RtcTrigger rtc = new RtcTrigger("1", "urn:epcglobal:ale:trigger:rtc:1000.500.-08:00", callback);
		try {
			Assert.assertEquals(0, rtc.getOffsetFromUtc("Z"));
			Assert.assertEquals(isDst() ? 7200000 : 3600000, rtc.getOffsetFromUtc(""));
			Assert.assertEquals(isDst() ? 7200000 : 3600000, rtc.getOffsetFromUtc(null));
			Assert.assertEquals(3600000, rtc.getOffsetFromUtc("+01:00"));
			Assert.assertEquals(7200000, rtc.getOffsetFromUtc("+02:00"));
			Assert.assertEquals(0, rtc.getOffsetFromUtc("+00:00"));
			Assert.assertEquals(0, rtc.getOffsetFromUtc("-00:00"));
			Assert.assertEquals(-3600000, rtc.getOffsetFromUtc("-01:00"));
			Assert.assertEquals(-7200000, rtc.getOffsetFromUtc("-02:00"));
		} finally {
			rtc.dispose();
		}
	}

	@Test
	public void getNext() throws ValidationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		RtcTrigger rtc = new RtcTrigger("1", "urn:epcglobal:ale:trigger:rtc:1000.500.Z", callback);
		try {
			long passedMsOfDay = getPassedMsOfDay();
			long next = rtc.getNext(passedMsOfDay);
			long offset = (next + passedMsOfDay) % 1000;
			Assert.assertEquals(500, offset);
		} finally {
			rtc.dispose();
		}

		rtc = new RtcTrigger("2", "urn:epcglobal:ale:trigger:rtc:1000.0.Z", callback);
		try {
			long passedMsOfDay = getPassedMsOfDay();
			long next = rtc.getNext(passedMsOfDay);
			long offset = (next + passedMsOfDay) % 1000;
			Assert.assertEquals(0, offset);
		} finally {
			rtc.dispose();
		}

		rtc = new RtcTrigger("3", "urn:epcglobal:ale:trigger:rtc:777.333.Z", callback);
		try {
			long passedMsOfDay = getPassedMsOfDay();
			long next = rtc.getNext(passedMsOfDay);
			long offset = (next + passedMsOfDay) % 777;
			Assert.assertEquals(333, offset);
		} finally {
			rtc.dispose();
		}

		rtc = new RtcTrigger("4", "urn:epcglobal:ale:trigger:rtc:86400000.0.Z", callback);
		try {
			long passedMsOfDay = getPassedMsOfDay();
			long next = rtc.getNext(passedMsOfDay);
			Assert.assertEquals(passedMsOfDay + Trigger.DAY, next);
		} finally {
			rtc.dispose();
		}

		rtc = new RtcTrigger("5", "urn:epcglobal:ale:trigger:rtc:7000.0.Z", callback);
		try {
			long passedMsOfDay = getPassedMsOfDay();
			long next = rtc.getNext(passedMsOfDay);
			long offset = (next + passedMsOfDay) % 7000;
			Assert.assertEquals(0, offset);
		} finally {
			rtc.dispose();
		}
	}

	@Test
	public void rtcRun() throws ValidationException, InterruptedException {
		final AtomicInteger runs = new AtomicInteger(0);
		final CountDownLatch firstRun = new CountDownLatch(1);
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				runs.incrementAndGet();
				firstRun.countDown();
				return true;
			}
		};
		RtcTrigger rtc = new RtcTrigger("1", "urn:epcglobal:ale:trigger:rtc:20.10.Z", callback);
		try {
			firstRun.await();
			Assert.assertEquals(1, runs.get());
			Thread.sleep(22);
			Assert.assertEquals(2, runs.get());
			rtc.dispose();
			Thread.sleep(22);
			Assert.assertEquals(2, runs.get());
		} finally {
			rtc.dispose();
		}
	}

	@Test
	public void rtcLateAdd() throws ValidationException, InterruptedException {
		final AtomicInteger runs1 = new AtomicInteger(0);
		final AtomicInteger runs2 = new AtomicInteger(0);
		final CountDownLatch firstRun = new CountDownLatch(1);

		Callback callback1 = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				runs1.incrementAndGet();
				return true;
			}
		};

		Callback callback2 = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				runs2.incrementAndGet();
				firstRun.countDown();
				return true;
			}
		};
		RtcTrigger rtc1 = new RtcTrigger("1", "urn:epcglobal:ale:trigger:rtc:86400000.0.Z", callback1);
		try {
			Thread.sleep(100);
			// still a long time until this trigger will run
			Assert.assertEquals(0, runs1.get());

			// will wake the timer thread to process the new trigger
			RtcTrigger rtc2 = new RtcTrigger("1", "urn:epcglobal:ale:trigger:rtc:20.10.Z", callback2);
			try {
				firstRun.await();
				Assert.assertEquals(1, runs2.get());
				Assert.assertEquals(0, runs1.get());
				Thread.sleep(22);
				Assert.assertEquals(2, runs2.get());
				Assert.assertEquals(0, runs1.get());
			} finally {
				rtc2.dispose();
			}
		} finally {
			rtc1.dispose();
		}
	}

	@Test
	public void rtcToggle() throws ValidationException, InterruptedException {
		final AtomicBoolean running = new AtomicBoolean(false);
		final CountDownLatch invokedOnce = new CountDownLatch(1);

		Callback start = new Callback() {

			@Override
			public boolean invoke(Trigger trigger) {
				invokedOnce.countDown();
				if (!running.get()) {
					running.set(true);
					return true;
				}
				return false;
			}
		};

		Callback stop = new Callback() {

			@Override
			public boolean invoke(Trigger trigger) {
				invokedOnce.countDown();
				if (running.get()) {
					running.set(false);
					return true;
				}
				return false;
			}
		};
		RtcTrigger rtcStart = new RtcTrigger("1", "urn:epcglobal:ale:trigger:rtc:20.10.Z", start);
		try {
			RtcTrigger rtcStop = new RtcTrigger("1", "urn:epcglobal:ale:trigger:rtc:20.10.Z", stop);
			try {
				invokedOnce.await();
				Assert.assertTrue(running.get());
				Thread.sleep(22);
				Assert.assertFalse(running.get());
				Thread.sleep(22);
				Assert.assertTrue(running.get());
				Thread.sleep(22);
				Assert.assertFalse(running.get());
			} finally {
				rtcStop.dispose();
			}
		} finally {
			rtcStart.dispose();
		}
	}
}
