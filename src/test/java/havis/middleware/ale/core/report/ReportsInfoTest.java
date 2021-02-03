package havis.middleware.ale.core.report;

import havis.middleware.ale.core.report.ec.Tags;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.service.ec.ECReports;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class ReportsInfoTest {

	@Test
	public void reportsInfoTest1() {
		Date start = new Date();
		SubscriberController[] subscribers = new SubscriberController[0];
		ReportsInfo<ECReports, Tags> reportsInfo = new ReportsInfo<ECReports, Tags>(
				subscribers, Initiation.TRIGGER, Termination.STABLE_SET);
		Assert.assertSame(subscribers, reportsInfo.getSubscribers());
		Assert.assertEquals(Initiation.TRIGGER, reportsInfo.getInitiation());
		Assert.assertEquals(Termination.STABLE_SET,
				reportsInfo.getTermination());

		Assert.assertNotNull(reportsInfo.getDate());
		Assert.assertTrue(start.getTime() <= reportsInfo.getDate().getTime());
		Assert.assertTrue(new Date().getTime() >= reportsInfo.getDate()
				.getTime());
	}

	@Test
	public void reportsInfoTest2() {
		Date creationDate = new Date();
		SubscriberController[] subscribers = new SubscriberController[0];
		Tags datas = new Tags();
		ReportsInfo<ECReports, Tags> reportsInfo = new ReportsInfo<ECReports, Tags>(
				subscribers, datas, creationDate, 1000, Initiation.TRIGGER,
				"init", Termination.STABLE_SET, "term");
		Assert.assertSame(subscribers, reportsInfo.getSubscribers());
		Assert.assertSame(datas, reportsInfo.getDatas());
		Assert.assertEquals(creationDate, reportsInfo.getDate());
		Assert.assertEquals(1000, reportsInfo.getTotalMilliseconds());
		Assert.assertEquals(Initiation.TRIGGER, reportsInfo.getInitiation());
		Assert.assertEquals("init", reportsInfo.getInitiator());
		Assert.assertEquals(Termination.STABLE_SET,
				reportsInfo.getTermination());
		Assert.assertEquals("term", reportsInfo.getTerminator());

		Assert.assertNotNull(reportsInfo.getDate());
	}
}
