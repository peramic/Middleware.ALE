package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Event;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.report.Initiation;
import havis.middleware.ale.core.report.ReportsInfo;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.ECFilterListMember.PatList;
import havis.middleware.ale.service.ECTime;
import havis.middleware.ale.service.pc.PCEventReport;
import havis.middleware.ale.service.pc.PCFilterSpec;
import havis.middleware.ale.service.pc.PCFilterSpec.FilterList;
import havis.middleware.ale.service.pc.PCFilterSpecExtension;
import havis.middleware.ale.service.pc.PCOpSpec;
import havis.middleware.ale.service.pc.PCOpSpecs;
import havis.middleware.ale.service.pc.PCPortSpec;
import havis.middleware.ale.service.pc.PCReport;
import havis.middleware.ale.service.pc.PCReport.EventReports;
import havis.middleware.ale.service.pc.PCReportSpec;
import havis.middleware.ale.service.pc.PCReportSpec.TriggerList;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.service.pc.PCSpec;
import havis.middleware.ale.service.pc.PCSpec.ReportSpecs;
import havis.middleware.utils.threading.ThreadManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

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
    public void reportsWithFilters() throws ImplementationException, ValidationException {
        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());
        PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        PCFilterSpec filterSpec = new PCFilterSpec();
        filterSpec.setExtension(new PCFilterSpecExtension());
        filterSpec.setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        filterSpec.getFilterList().getFilter().add(member1);
        ECFilterListMember member2 = new ECFilterListMember();
        member2.setPatList(new PatList());
        member2.getPatList().getPat().add("X");
        member2.setIncludeExclude("EXCLUDE");
        member2.setFieldspec(new ECFieldSpec("killPwd"));
        filterSpec.getFilterList().getFilter().add(member2);
        reportSpec.setFilterSpec(filterSpec);
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        Reports reports = new Reports("name", spec, false, null);
        Assert.assertEquals("name", reports.getName());
        Assert.assertNotNull(reports.getTagOperation());
        Assert.assertEquals(2, reports.getTagOperation().getOperations().size());
        Assert.assertEquals(1, reports.getTagOperation().getOperations().get(0).getId());
        Assert.assertEquals(OperationType.READ, reports.getTagOperation().getOperations().get(0).getType());
        Assert.assertEquals("epc", reports.getTagOperation().getOperations().get(0).getField().getName());
        Assert.assertEquals(2, reports.getTagOperation().getOperations().get(1).getId());
        Assert.assertEquals(OperationType.READ, reports.getTagOperation().getOperations().get(1).getType());
        Assert.assertEquals("killPwd", reports.getTagOperation().getOperations().get(1).getField().getName());
    }

    @Test(expected = ValidationException.class)
    public void reportsNoReportSpecs1() throws ImplementationException, ValidationException {
        PCSpec spec = new PCSpec();
        spec.setReportSpecs(null);

        new Reports("name", spec, false, null);
    }

    @Test(expected = ValidationException.class)
    public void reportsNoReportSpecs2() throws ImplementationException, ValidationException {
        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());

        new Reports("name", spec, false, null);
    }

    @Test
    public void reportsDuplicateName(@Mocked final Report report1, @Mocked final Report report2) throws ImplementationException, ValidationException {
        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec1 = new PCReportSpec();
        reportSpec1.setName("duplicate");
        reportSpec1.setTriggerList(new TriggerList());
        reportSpec1.setOpSpecs(new PCOpSpecs());
        reportSpec1.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec1);
        final PCReportSpec reportSpec2 = new PCReportSpec();
        reportSpec2.setName("duplicate");
        reportSpec2.setTriggerList(new TriggerList());
        reportSpec2.setOpSpecs(new PCOpSpecs());
        reportSpec2.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec2);

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec1), false, null);
                result = report1;

                new Report(withEqual(reportSpec2), false, null);
                result = report2;
            }
        };

        try {
            new Reports("name", spec, false, null);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        new Verifications() {
            {
                report1.dispose();
                times = 1;

                report2.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void reportsWithTriggers(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader) throws ImplementationException,
            ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                Trigger.getInstance(this.<String> withNotNull(), withEqual("test"), this.<Trigger.Callback> withNotNull());
                result = trigger;

                LR.getInstance();
                result = lr;

                lr.lock("somereader");
                result = reader;
            }
        };

        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());
        PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.getTriggerList().getTrigger().add("test");
        reportSpec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        reportSpec.getOpSpecs().getOpSpec().add(spec1);
        PCOpSpec spec2 = new PCOpSpec();
        spec2.setOpName("spec2");
        spec2.setPortSpec(new PCPortSpec());
        spec2.getPortSpec().setReader("somereader");
        spec2.getPortSpec().setType("OUTPUT");
        spec2.setOpType("WRITE");
        spec2.setState(Boolean.TRUE);
        spec2.setDuration(new ECTime());
        spec2.getDuration().setUnit("MS");
        spec2.getDuration().setValue(1000);
        reportSpec.getOpSpecs().getOpSpec().add(spec2);
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        Reports reports = new Reports(null, spec, false, null);
        Assert.assertEquals("", reports.getName());
        Assert.assertNull(reports.getTagOperation());
    }

    @Test
    public void enqueue(@Mocked final Report report, @Mocked final ReportsInfo<PCReports, Events> info, @Mocked final SubscriberController subscriber,
            @Mocked ThreadManager manager) throws ValidationException, ImplementationException {
        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        final Reports reports = new Reports("name", spec, false, null);

        new NonStrictExpectations() {
            {
                info.getSubscribers();
                result = new SubscriberController[] { subscriber };
            }
        };

        reports.enqueue(info);

        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                ThreadManager.enqueue(withEqual(reports));
                times = 1;
            }
        };
    }

    @Test
    public void runUndefine(@Mocked final Report report, @Mocked final SubscriberController subscriber) throws ValidationException, ImplementationException,
            InterruptedException {
        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.getName();
                result = "spec";

                report.getFilter();
                result = null;

                report.getOperations();
                result = new ArrayList<Operation>();
            }
        };

        Reports reports = new Reports("name", spec, false, null);

        Events events = new Events();
        Event event1 = new Event("test");
        events.add(event1, event1);
        events.pulse();
        ReportsInfo<PCReports, Events> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, events, new Date(), 10, Initiation.UNDEFINE,
                "init", Termination.UNDEFINE, "term");
        reports.enqueue(info); // executes run

        Thread.sleep(70);

        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                PCReports pcReports = null;
                subscriber.enqueue(pcReports = withCapture());
                times = 1;

                Date now = new Date();
                Assert.assertNotNull(pcReports);
                Assert.assertEquals(new BigDecimal(1), pcReports.getSchemaVersion());
                Assert.assertTrue(pcReports.getCreationDate().getTime() >= now.getTime() - 100 && pcReports.getCreationDate().getTime() <= now.getTime());
                Assert.assertEquals("name", pcReports.getSpecName());
                Assert.assertTrue(pcReports.getDate().getTime() >= now.getTime() - 100 && pcReports.getDate().getTime() <= now.getTime());
                Assert.assertEquals("Ha-VIS Middleware", pcReports.getALEID());
                Assert.assertEquals(10, pcReports.getTotalMilliseconds());
                Assert.assertEquals("UNDEFINE", pcReports.getInitiationCondition());
                Assert.assertEquals("init", pcReports.getInitiationTrigger());
                Assert.assertEquals("UNDEFINE", pcReports.getTerminationCondition());
                Assert.assertEquals("term", pcReports.getTerminationTrigger());
                Assert.assertNull(pcReports.getReports());

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void runSingleResult(@Mocked final Report report, @Mocked final SubscriberController subscriber) throws ValidationException,
            ImplementationException, InterruptedException {
        final PCSpec spec = new PCSpec();
        spec.setIncludeSpecInReports(Boolean.TRUE);
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        final Events events = new Events();
        Event event1 = new Event("test");
        event1.setResult(new HashMap<Integer, Result>());
        event1.setCompleted(true);
        events.add(event1, event1);
        events.pulse();

        final PCReport pcReport = new PCReport();
        pcReport.setReportName("report");
        pcReport.setEventReports(new EventReports());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.getName();
                result = "spec";

                report.getFilter();
                result = null;

                report.getOperations();
                result = new ArrayList<Operation>();

                report.get(withEqual(events.toList()));
                result = pcReport;
            }
        };

        Reports reports = new Reports("name", spec, false, null);

        ReportsInfo<PCReports, Events> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, events, new Date(), 10, Initiation.NULL,
                "init", Termination.NULL, "term");
        reports.enqueue(info); // executes run

        Thread.sleep(70);

        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                PCReports pcReports = null;
                subscriber.enqueue(pcReports = withCapture());
                times = 1;

                Date now = new Date();
                Assert.assertNotNull(pcReports);
                Assert.assertEquals(new BigDecimal(1), pcReports.getSchemaVersion());
                Assert.assertTrue(pcReports.getCreationDate().getTime() >= now.getTime() - 100 && pcReports.getCreationDate().getTime() <= now.getTime());
                Assert.assertEquals("name", pcReports.getSpecName());
                Assert.assertTrue(pcReports.getDate().getTime() >= now.getTime() - 100 && pcReports.getDate().getTime() <= now.getTime());
                Assert.assertEquals("Ha-VIS Middleware", pcReports.getALEID());
                Assert.assertEquals(10, pcReports.getTotalMilliseconds());
                Assert.assertEquals("NULL", pcReports.getInitiationCondition());
                Assert.assertEquals("init", pcReports.getInitiationTrigger());
                Assert.assertEquals("NULL", pcReports.getTerminationCondition());
                Assert.assertEquals("term", pcReports.getTerminationTrigger());
                Assert.assertEquals(spec, pcReports.getPCSpec());
                Assert.assertNotNull(pcReports.getReports());
                Assert.assertEquals(1, pcReports.getReports().getReport().size());
                Assert.assertSame(pcReport, pcReports.getReports().getReport().get(0));

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 0;
            }
        };
    }

    @Test
    public void runRemoveEmpty(@Mocked final Report report, @Mocked final SubscriberController subscriber) throws ValidationException, ImplementationException,
            InterruptedException {
        final PCSpec spec = new PCSpec();
        spec.setIncludeSpecInReports(Boolean.TRUE);
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        Events events = new Events();
        final Event event1 = new Event("test1");
        event1.setResult(new HashMap<Integer, Result>());
        event1.setCompleted(true);
        events.add(event1, event1);
        events.pulse();

        Event event2 = new Event("test2");
        event2.setResult(null);
        event2.setCompleted(true);
        events.add(event2, event2);
        events.pulse();

        final Event event3 = new Event("test3");
        event3.setResult(new HashMap<Integer, Result>());
        event3.setCompleted(true);
        events.add(event3, event3);
        events.pulse();

        final PCReport pcReport = new PCReport();
        pcReport.setReportName("report");
        pcReport.setEventReports(new EventReports());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.getName();
                result = "spec";

                report.getFilter();
                result = null;

                report.getOperations();
                result = new ArrayList<Operation>();

                report.get(withEqual(new HashSet<Event>(Arrays.asList(event1, event3))));
                result = pcReport;
            }
        };

        Reports reports = new Reports("name", spec, false, null);

        ReportsInfo<PCReports, Events> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, events, new Date(), 10, Initiation.NULL,
                "init", Termination.NULL, "term");
        reports.enqueue(info); // executes run

        Thread.sleep(70);

        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                PCReports pcReports = null;
                subscriber.enqueue(pcReports = withCapture());
                times = 1;

                Date now = new Date();
                Assert.assertNotNull(pcReports);
                Assert.assertEquals(new BigDecimal(1), pcReports.getSchemaVersion());
                Assert.assertTrue(pcReports.getCreationDate().getTime() >= now.getTime() - 100 && pcReports.getCreationDate().getTime() <= now.getTime());
                Assert.assertEquals("name", pcReports.getSpecName());
                Assert.assertTrue(pcReports.getDate().getTime() >= now.getTime() - 100 && pcReports.getDate().getTime() <= now.getTime());
                Assert.assertEquals("Ha-VIS Middleware", pcReports.getALEID());
                Assert.assertEquals(10, pcReports.getTotalMilliseconds());
                Assert.assertEquals("NULL", pcReports.getInitiationCondition());
                Assert.assertEquals("init", pcReports.getInitiationTrigger());
                Assert.assertEquals("NULL", pcReports.getTerminationCondition());
                Assert.assertEquals("term", pcReports.getTerminationTrigger());
                Assert.assertEquals(spec, pcReports.getPCSpec());
                Assert.assertNotNull(pcReports.getReports());
                Assert.assertEquals(1, pcReports.getReports().getReport().size());
                Assert.assertSame(pcReport, pcReports.getReports().getReport().get(0));

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 0;
            }
        };
    }

    @Test
    public void runWaitForIncompleteEvent(@Mocked final Report report, @Mocked final SubscriberController subscriber) throws ValidationException,
            ImplementationException, InterruptedException {
        final PCSpec spec = new PCSpec();
        spec.setIncludeSpecInReports(Boolean.TRUE);
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        final Events events = new Events();
        Event event1 = new Event("test1");
        event1.setResult(new HashMap<Integer, Result>());
        event1.setCompleted(true);
        events.add(event1, event1);
        events.pulse();

        final Event event2 = new Event("test2");
        event2.setResult(new HashMap<Integer, Result>());
        event2.setCompleted(false);
        events.add(event2, null);
        events.pulse();

        Event event3 = new Event("test3");
        event3.setResult(new HashMap<Integer, Result>());
        event3.setCompleted(true);
        events.add(event3, event3);
        events.pulse();

        final PCReport pcReport = new PCReport();
        pcReport.setReportName("report");
        pcReport.setEventReports(new EventReports());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.getName();
                result = "spec";

                report.getFilter();
                result = null;

                report.getOperations();
                result = new ArrayList<Operation>();

                report.get(withEqual(events.toList()));
                result = pcReport;

                report.isCompleted(withEqual(event2));
                result = Boolean.FALSE;
            }
        };

        Reports reports = new Reports("name", spec, false, null);

        ReportsInfo<PCReports, Events> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, events, new Date(), 10, Initiation.NULL,
                "init", Termination.NULL, "term");
        reports.enqueue(info); // executes run

        Thread.sleep(70);

        // still waiting
        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                subscriber.enqueue(this.<PCReports> withNotNull());
                times = 0;

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 0;
            }
        };

        event2.setCompleted(true);
        events.set(event2, event2);
        events.pulse(); // stop waiting

        Thread.sleep(100);

        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                PCReports pcReports = null;
                subscriber.enqueue(pcReports = withCapture());
                times = 1;

                Date now = new Date();
                Assert.assertNotNull(pcReports);
                Assert.assertEquals(new BigDecimal(1), pcReports.getSchemaVersion());
                Assert.assertTrue(pcReports.getCreationDate().getTime() >= now.getTime() - 200 && pcReports.getCreationDate().getTime() <= now.getTime());
                Assert.assertEquals("name", pcReports.getSpecName());
                Assert.assertTrue(pcReports.getDate().getTime() >= now.getTime() - 200 && pcReports.getDate().getTime() <= now.getTime());
                Assert.assertEquals("Ha-VIS Middleware", pcReports.getALEID());
                Assert.assertEquals(10, pcReports.getTotalMilliseconds());
                Assert.assertEquals("NULL", pcReports.getInitiationCondition());
                Assert.assertEquals("init", pcReports.getInitiationTrigger());
                Assert.assertEquals("NULL", pcReports.getTerminationCondition());
                Assert.assertEquals("term", pcReports.getTerminationTrigger());
                Assert.assertEquals(spec, pcReports.getPCSpec());
                Assert.assertNotNull(pcReports.getReports());
                Assert.assertEquals(1, pcReports.getReports().getReport().size());
                Assert.assertSame(pcReport, pcReports.getReports().getReport().get(0));

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 0;
            }
        };
    }

    @Test
    public void runEmptyResult(@Mocked final Report report, @Mocked final SubscriberController subscriber) throws ValidationException, ImplementationException,
            InterruptedException {
        final PCSpec spec = new PCSpec();
        spec.setIncludeSpecInReports(Boolean.TRUE);
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        final Events events = new Events();

        final PCReport pcReport = new PCReport();
        pcReport.setReportName("report");
        pcReport.setEventReports(new EventReports());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.getName();
                result = "spec";

                report.getFilter();
                result = null;

                report.getOperations();
                result = new ArrayList<Operation>();

                report.get(withEqual(events.toList()));
                result = pcReport;
            }
        };

        Reports reports = new Reports("name", spec, false, null);

        ReportsInfo<PCReports, Events> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, events, new Date(), 10, Initiation.NULL,
                "init", Termination.NULL, "term");
        reports.enqueue(info); // executes run

        Thread.sleep(70);

        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                subscriber.enqueue(this.<PCReports> withNotNull());
                times = 0;

                subscriber.dec();
                times = 1;

                report.dispose();
                times = 0;
            }
        };
    }

    @Test
    public void runEmptyResultToSubscriberListener(@Mocked final Report report, @Mocked final SubscriberListener<PCReports> subscriber)
            throws ValidationException, ImplementationException, InterruptedException {
        final PCSpec spec = new PCSpec();
        spec.setIncludeSpecInReports(Boolean.TRUE);
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        final Events events = new Events();

        final PCReport pcReport = new PCReport();
        pcReport.setReportName("report");
        pcReport.setEventReports(new EventReports());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.getName();
                result = "spec";

                report.getFilter();
                result = null;

                report.getOperations();
                result = new ArrayList<Operation>();

                report.get(withEqual(events.toList()));
                result = pcReport;
            }
        };

        Reports reports = new Reports("name", spec, false, null);

        ReportsInfo<PCReports, Events> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, events, new Date(), 10, Initiation.NULL,
                "init", Termination.NULL, "term");
        reports.enqueue(info); // executes run

        Thread.sleep(70);

        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                PCReports pcReports = null;
                subscriber.enqueue(pcReports = withCapture());
                times = 1;

                Date now = new Date();
                Assert.assertNotNull(pcReports);
                Assert.assertEquals(new BigDecimal(1), pcReports.getSchemaVersion());
                Assert.assertTrue(pcReports.getCreationDate().getTime() >= now.getTime() - 100 && pcReports.getCreationDate().getTime() <= now.getTime());
                Assert.assertEquals("name", pcReports.getSpecName());
                Assert.assertTrue(pcReports.getDate().getTime() >= now.getTime() - 100 && pcReports.getDate().getTime() <= now.getTime());
                Assert.assertEquals("Ha-VIS Middleware", pcReports.getALEID());
                Assert.assertEquals(10, pcReports.getTotalMilliseconds());
                Assert.assertEquals("NULL", pcReports.getInitiationCondition());
                Assert.assertEquals("init", pcReports.getInitiationTrigger());
                Assert.assertEquals("NULL", pcReports.getTerminationCondition());
                Assert.assertEquals("term", pcReports.getTerminationTrigger());
                Assert.assertEquals(spec, pcReports.getPCSpec());
                Assert.assertNotNull(pcReports.getReports());
                Assert.assertEquals(0, pcReports.getReports().getReport().size());

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 0;
            }
        };
    }

    @Test
    public void runThrowingException(@Mocked final Report report, @Mocked final SubscriberListener<PCReports> subscriber) throws ValidationException,
            ImplementationException, InterruptedException {
        final PCSpec spec = new PCSpec();
        spec.setIncludeSpecInReports(Boolean.TRUE);
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        final Events events = new Events();

        final PCReport pcReport = new PCReport();
        pcReport.setReportName("report");
        pcReport.setEventReports(new EventReports());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.getName();
                result = "spec";

                report.getFilter();
                result = null;

                report.getOperations();
                result = new ArrayList<Operation>();

                report.get(withEqual(events.toList()));
                result = pcReport;
            }
        };

        Reports reports = new Reports("name", spec, false, null);
        Thread thread = new Thread(reports);
        thread.start(); // will wait on queue.take()

        Thread.sleep(70);

        thread.interrupt();

        Thread.sleep(200);

        new Verifications() {
            {
                subscriber.inc();
                times = 0; // no reports.enqueue() called in this test

                subscriber.enqueue(this.<PCReports> withNotNull());
                times = 0;

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 0;
            }
        };
    }

    @Test
    public void getPortOperation(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader) throws ImplementationException,
            ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                Trigger.getInstance(this.<String> withNotNull(), withEqual("test"), this.<Trigger.Callback> withNotNull());
                result = trigger;

                LR.getInstance();
                result = lr;

                lr.lock("somereader");
                result = reader;
            }
        };

        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());
        PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        reportSpec.getOpSpecs().getOpSpec().add(spec1);
        PCOpSpec spec2 = new PCOpSpec();
        spec2.setOpName("spec2");
        spec2.setPortSpec(new PCPortSpec());
        spec2.getPortSpec().setReader("somereader");
        spec2.getPortSpec().setType("OUTPUT");
        spec2.setOpType("WRITE");
        spec2.setState(Boolean.TRUE);
        spec2.setDuration(new ECTime());
        spec2.getDuration().setUnit("MS");
        spec2.getDuration().setValue(1000);
        reportSpec.getOpSpecs().getOpSpec().add(spec2);
        PCFilterSpec filterSpec = new PCFilterSpec();
        filterSpec.setExtension(new PCFilterSpecExtension());
        filterSpec.setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        filterSpec.getFilterList().getFilter().add(member1);
        reportSpec.setFilterSpec(filterSpec);
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        Reports reports = new Reports("name", spec, false, null);

        Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tag.setResult(new HashMap<Integer, havis.middleware.ale.base.operation.tag.result.Result>());
        tag.getResult().put(
                Integer.valueOf(1),
                new havis.middleware.ale.base.operation.tag.result.ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19,
                        0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));

        List<Operation> portOperations = reports.getPortOperation(tag);
        Assert.assertNotNull(portOperations);
        Assert.assertEquals(2, portOperations.size());
        Assert.assertEquals(2 /* filter has id = 1 */, portOperations.get(0).getPortOperation().getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.port.Operation.Type.READ, portOperations.get(0).getPortOperation().getType());
        Assert.assertEquals(3, portOperations.get(1).getPortOperation().getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.port.Operation.Type.WRITE, portOperations.get(1).getPortOperation().getType());

        tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
        tag.setResult(new HashMap<Integer, havis.middleware.ale.base.operation.tag.result.Result>());
        tag.getResult().put(
                Integer.valueOf(1),
                new havis.middleware.ale.base.operation.tag.result.ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19,
                        0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 }));

        Assert.assertNull(reports.getPortOperation(tag)); // no match
    }

    @Test
    public void isCompleted(@Mocked final Report report) throws ValidationException, ImplementationException {
        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        final Event event1 = new Event("1");
        event1.setCompleted(false);
        final Event event2 = new Event("2");
        event2.setCompleted(false);
        final Event event3 = new Event("3");
        event3.setCompleted(true);

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.isCompleted(withEqual(event1));
                result = Boolean.FALSE;

                report.isCompleted(withEqual(event2));
                result = Boolean.TRUE;

                report.isCompleted(withEqual(event3));
                result = Boolean.TRUE;
            }
        };

        Reports reports = new Reports("name", spec, false, null);

        Assert.assertFalse(event1.isCompleted());
        Assert.assertFalse(reports.isCompleted(event1));
        Assert.assertFalse(event1.isCompleted());

        Assert.assertFalse(event2.isCompleted());
        Assert.assertTrue(reports.isCompleted(event2));
        Assert.assertTrue(event2.isCompleted());

        Assert.assertTrue(event3.isCompleted());
        Assert.assertTrue(reports.isCompleted(event3));
        Assert.assertTrue(event3.isCompleted());
    }

    @Test
    public void dispose(@Mocked final Report report) throws ValidationException, ImplementationException {
        PCSpec spec = new PCSpec();
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;
            }
        };

        Reports reports = new Reports("name", spec, false, null);

        reports.dispose();

        new Verifications() {
            {
                report.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void disposeWaitForQueue(@Mocked final Report report, @Mocked final SubscriberController subscriber) throws ValidationException,
            InterruptedException, ImplementationException {
        final PCSpec spec = new PCSpec();
        spec.setIncludeSpecInReports(Boolean.TRUE);
        spec.setReportSpecs(new ReportSpecs());
        final PCReportSpec reportSpec = new PCReportSpec();
        reportSpec.setName("spec");
        reportSpec.setTriggerList(new TriggerList());
        reportSpec.setOpSpecs(new PCOpSpecs());
        reportSpec.setFilterSpec(new PCFilterSpec());
        spec.getReportSpecs().getReportSpec().add(reportSpec);

        final Events events = new Events();
        Event event1 = new Event("test1");
        event1.setResult(new HashMap<Integer, Result>());
        event1.setCompleted(true);
        events.add(event1, event1);
        events.pulse();

        final Event event2 = new Event("test2");
        event2.setResult(new HashMap<Integer, Result>());
        event2.setCompleted(false);
        events.add(event2, null);
        events.pulse();

        Event event3 = new Event("test3");
        event3.setResult(new HashMap<Integer, Result>());
        event3.setCompleted(true);
        events.add(event3, event3);
        events.pulse();

        final PCReport pcReport = new PCReport();
        pcReport.setReportName("report");
        pcReport.setEventReports(new EventReports());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());
        pcReport.getEventReports().getEventReport().add(new PCEventReport());

        new NonStrictExpectations() {
            {
                new Report(withEqual(reportSpec), false, null);
                result = report;

                report.getName();
                result = "spec";

                report.getFilter();
                result = null;

                report.getOperations();
                result = new ArrayList<Operation>();

                report.get(withEqual(events.toList()));
                result = pcReport;

                report.isCompleted(withEqual(event2));
                result = Boolean.FALSE;
            }
        };

        final Reports reports = new Reports("name", spec, false, null);

        ReportsInfo<PCReports, Events> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, events, new Date(), 10, Initiation.NULL,
                "init", Termination.NULL, "term");
        reports.enqueue(info); // executes run

        Thread.sleep(70);

        // still waiting
        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                subscriber.enqueue(this.<PCReports> withNotNull());
                times = 0;

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 0;
            }
        };

        ReportsInfo<PCReports, Events> info2 = new ReportsInfo<>(new SubscriberController[] { subscriber }, new Events(), new Date(), 10, Initiation.NULL,
                "init", Termination.NULL, "term");
        reports.enqueue(info2); // waits for first execution

        Thread.sleep(70);

        ReportsInfo<PCReports, Events> info3 = new ReportsInfo<>(new SubscriberController[] { subscriber }, new Events(), new Date(), 10, Initiation.NULL,
                "init", Termination.NULL, "term");
        reports.enqueue(info3); // waits for first execution

        Thread.sleep(70);

        Thread disposeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                reports.dispose(); // will wait for all executions
            }
        });
        disposeThread.start();

        Thread.sleep(100);

        Assert.assertTrue(disposeThread.isAlive());

        // verify again that nothing happened yet
        new Verifications() {
            {
                subscriber.inc();
                times = 3;

                subscriber.enqueue(this.<PCReports> withNotNull());
                times = 0;

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 0;
            }
        };

        event2.setCompleted(true);
        events.set(event2, event2);
        events.pulse(); // stop waiting

        Thread.sleep(100);

        new Verifications() {
            {
                subscriber.inc();
                times = 3;

                PCReports pcReports = null;
                subscriber.enqueue(pcReports = withCapture());
                times = 1;

                Date now = new Date();
                Assert.assertNotNull(pcReports);
                Assert.assertEquals(new BigDecimal(1), pcReports.getSchemaVersion());
                Assert.assertTrue(pcReports.getCreationDate().getTime() >= now.getTime() - 500 && pcReports.getCreationDate().getTime() <= now.getTime());
                Assert.assertEquals("name", pcReports.getSpecName());
                Assert.assertTrue(pcReports.getDate().getTime() >= now.getTime() - 500 && pcReports.getDate().getTime() <= now.getTime());
                Assert.assertEquals("Ha-VIS Middleware", pcReports.getALEID());
                Assert.assertEquals(10, pcReports.getTotalMilliseconds());
                Assert.assertEquals("NULL", pcReports.getInitiationCondition());
                Assert.assertEquals("init", pcReports.getInitiationTrigger());
                Assert.assertEquals("NULL", pcReports.getTerminationCondition());
                Assert.assertEquals("term", pcReports.getTerminationTrigger());
                Assert.assertEquals(spec, pcReports.getPCSpec());
                Assert.assertNotNull(pcReports.getReports());
                Assert.assertEquals(1, pcReports.getReports().getReport().size());
                Assert.assertSame(pcReport, pcReports.getReports().getReport().get(0));

                subscriber.dec();
                times = 2;

                report.dispose();
                times = 1;
            }
        };
    }
}
