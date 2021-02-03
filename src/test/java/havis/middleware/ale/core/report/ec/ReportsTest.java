package havis.middleware.ale.core.report.ec;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.report.Initiation;
import havis.middleware.ale.core.report.ReportsInfo;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.service.ECTime;
import havis.middleware.ale.service.ec.ECBoundarySpec;
import havis.middleware.ale.service.ec.ECReportOutputSpec;
import havis.middleware.ale.service.ec.ECReportSetSpec;
import havis.middleware.ale.service.ec.ECReportSpec;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.ale.service.ec.ECSpec.ReportSpecs;
import havis.middleware.utils.threading.ThreadManager;

import java.util.Date;

import mockit.Mocked;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReportsTest {

	@BeforeClass
	public static void init() {
		ConfigResetter.reset();
		ConfigResetter.disablePersistence();
	}

	@Test
	public void runEmptyChange(@Mocked ThreadManager manager) throws Exception {
		ECSpec spec = new ECSpec();
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setDuration(new ECTime());
		spec.getBoundarySpec().getDuration().setUnit("MS");
		spec.getBoundarySpec().getDuration().setValue(1000);
		spec.getBoundarySpec().setRepeatPeriod(new ECTime());
		spec.getBoundarySpec().getRepeatPeriod().setUnit("MS");
		spec.getBoundarySpec().getRepeatPeriod().setValue(0);
		spec.setReportSpecs(new ReportSpecs());
		spec.getReportSpecs().getReportSpec().add(new ECReportSpec());
		spec.getReportSpecs().getReportSpec().get(spec.getReportSpecs().getReportSpec().size() - 1).setReportIfEmpty(Boolean.FALSE);
		spec.getReportSpecs().getReportSpec().get(spec.getReportSpecs().getReportSpec().size() - 1).setReportName("R1");
		spec.getReportSpecs().getReportSpec().get(spec.getReportSpecs().getReportSpec().size() - 1).setReportOnlyOnChange(Boolean.TRUE);
		spec.getReportSpecs().getReportSpec().get(spec.getReportSpecs().getReportSpec().size() - 1).setReportSet(new ECReportSetSpec());
		spec.getReportSpecs().getReportSpec().get(spec.getReportSpecs().getReportSpec().size() - 1).getReportSet().setSet("CURRENT");
		spec.getReportSpecs().getReportSpec().get(spec.getReportSpecs().getReportSpec().size() - 1).setOutput(new ECReportOutputSpec());
		spec.getReportSpecs().getReportSpec().get(spec.getReportSpecs().getReportSpec().size() - 1).getOutput().setIncludeEPC(Boolean.TRUE);
		Reports reports = new Reports("test", spec);

		// tag in the field
		SubscriberListener<ECReports> listener = new SubscriberListener<ECReports>(1);
		reports.enqueue(new ReportsInfo<ECReports, Tags>(new SubscriberController[] { listener }, createData("urn:epc:tag:sgtin-96:3.0614141.812345.6789"),
				new Date(), 1000, Initiation.REQUESTED, null, null, null));
		reports.run();
		ECReports result = listener.dequeue();
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getReports().getReport().size());
		Assert.assertEquals("urn:epc:id:sgtin:0614141.812345.6789", result.getReports().getReport().get(0).getGroup().get(0).getGroupList().getMember().get(0)
				.getEpc().getValue());

		// tag removed from the field
		listener = new SubscriberListener<ECReports>(1);
		reports.enqueue(new ReportsInfo<ECReports, Tags>(new SubscriberController[] { listener }, createData(), new Date(), 1000, Initiation.REPEAT_PERIOD,
				null, null, null));
		reports.run();
		result = listener.dequeue();
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getReports().getReport().size());

		// tag back in the field
		listener = new SubscriberListener<ECReports>(1);
		reports.enqueue(new ReportsInfo<ECReports, Tags>(new SubscriberController[] { listener }, createData("urn:epc:tag:sgtin-96:3.0614141.812345.6789"),
				new Date(), 1000, Initiation.REPEAT_PERIOD, null, null, null));
		reports.run();
		result = listener.dequeue();
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getReports().getReport().size());
		Assert.assertEquals("urn:epc:id:sgtin:0614141.812345.6789", result.getReports().getReport().get(0).getGroup().get(0).getGroupList().getMember().get(0)
				.getEpc().getValue());
	}

	private Tags createData(String... tags) {
		Tags data = new Tags();
		for (String urn : tags) {
			Tag tag = TagDecoder.getInstance().fromUrn(urn, new byte[] { 0x01 });
			data.add(new PrimaryKey(tag, null), tag);
		}
		return data;
	}
}
