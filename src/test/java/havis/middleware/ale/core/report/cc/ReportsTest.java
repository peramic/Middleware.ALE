package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Filter;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.report.Initiation;
import havis.middleware.ale.core.report.ReportsInfo;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.ECFilterListMember.PatList;
import havis.middleware.ale.service.cc.CCCmdReport;
import havis.middleware.ale.service.cc.CCCmdReport.TagReports;
import havis.middleware.ale.service.cc.CCCmdSpec;
import havis.middleware.ale.service.cc.CCCmdSpec.OpSpecs;
import havis.middleware.ale.service.cc.CCFilterSpec;
import havis.middleware.ale.service.cc.CCFilterSpec.FilterList;
import havis.middleware.ale.service.cc.CCFilterSpecExtension;
import havis.middleware.ale.service.cc.CCOpDataSpec;
import havis.middleware.ale.service.cc.CCOpReport;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.ale.service.cc.CCSpec.CmdSpecs;
import havis.middleware.ale.service.cc.CCTagReport;
import havis.middleware.ale.service.cc.CCTagStat;
import havis.middleware.utils.threading.ThreadManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

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
    public void reports() throws ValidationException {
        CCSpec spec = new CCSpec();
        spec.setCmdSpecs(new CmdSpecs());
        CCCmdSpec cmdSpec = new CCCmdSpec();
        cmdSpec.setName("specName");

        cmdSpec.setFilterSpec(new CCFilterSpec());
        cmdSpec.getFilterSpec().setExtension(new CCFilterSpecExtension());
        cmdSpec.getFilterSpec().setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        cmdSpec.getFilterSpec().getFilterList().getFilter().add(member1);
        ECFilterListMember member2 = new ECFilterListMember();
        member2.setPatList(new PatList());
        member2.getPatList().getPat().add("X");
        member2.setIncludeExclude("EXCLUDE");
        member2.setFieldspec(new ECFieldSpec("killPwd"));
        cmdSpec.getFilterSpec().getFilterList().getFilter().add(member2);

        cmdSpec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("checkEpcBank");
        opSpec.setOpType("CHECK");
        opSpec.setDataSpec(new CCOpDataSpec());
        opSpec.getDataSpec().setSpecType("LITERAL");
        opSpec.getDataSpec().setData("urn:epcglobal:ale:check:iso15962");
        opSpec.setFieldspec(new ECFieldSpec("epcBank"));
        cmdSpec.getOpSpecs().getOpSpec().add(opSpec);

        spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

        Reports reports = new Reports("whatever", spec);

        Assert.assertEquals("whatever", reports.getName());
        Assert.assertEquals(new Parameters(), reports.getParameters());
        TagOperation readerOperation = reports.getReaderOperation();
        Assert.assertNotNull(readerOperation);
        Assert.assertNull(readerOperation.getFilter());
        Assert.assertNotNull(readerOperation.getOperations());
        Assert.assertEquals(3, readerOperation.getOperations().size());

        Iterator<havis.middleware.ale.base.operation.tag.Operation> iterator = readerOperation.getOperations().iterator();
        havis.middleware.ale.base.operation.tag.Operation op1 = iterator.next();
        Assert.assertEquals(1, op1.getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.READ, op1.getType());
        Assert.assertEquals("epc", op1.getField().getName());
        havis.middleware.ale.base.operation.tag.Operation op2 = iterator.next();
        Assert.assertEquals(2, op2.getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.READ, op2.getType());
        Assert.assertEquals("killPwd", op2.getField().getName());
        havis.middleware.ale.base.operation.tag.Operation op3 = iterator.next();
        Assert.assertEquals(3, op3.getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.READ, op3.getType());
        Assert.assertEquals("epcBank", op3.getField().getName());

        Reports reportsNoName = new Reports(null, spec);
        Assert.assertEquals("", reportsNoName.getName());
    }

    @Test
    public void reportsParameters(@Mocked final Report report1, @Mocked final Report report2, @Mocked final havis.middleware.ale.core.report.cc.Filter filter)
            throws ValidationException {
        CCSpec spec = new CCSpec();
        spec.setCmdSpecs(new CmdSpecs());
        final CCCmdSpec cmdSpec1 = new CCCmdSpec();
        cmdSpec1.setName("specName1");
        spec.getCmdSpecs().getCmdSpec().add(cmdSpec1);
        final CCCmdSpec cmdSpec2 = new CCCmdSpec();
        cmdSpec2.setName("specName2");
        spec.getCmdSpecs().getCmdSpec().add(cmdSpec2);

        new NonStrictExpectations() {
            {
                new Report(withEqual(cmdSpec1), this.<Parameters> withNotNull());
                result = report1;

                new Report(withEqual(cmdSpec2), this.<Parameters> withNotNull());
                result = report2;

                report1.getName();
                result = cmdSpec1.getName();

                report1.getFilter();
                result = filter;

                report1.getOperations();
                result = new CCOperation[0];

                report2.getName();
                result = cmdSpec2.getName();

                report2.getFilter();
                result = filter;

                report2.getOperations();
                result = new CCOperation[0];

                filter.getOperations();
                result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();
            }
        };

        Reports reports = new Reports("whatever", spec);

        Assert.assertEquals(new Parameters(), reports.getParameters());
        final Parameters parameters = reports.getParameters();

        new Verifications() {
            {
                new Report(withEqual(cmdSpec1), withSameInstance(parameters));
                times = 1;

                new Report(withEqual(cmdSpec2), withSameInstance(parameters));
                times = 1;
            }
        };
    }

    @Test(expected = ValidationException.class)
    public void reportsNoCmdSpec1() throws ValidationException {
        CCSpec spec = new CCSpec();
        new Reports("whatever", spec);
    }

    @Test(expected = ValidationException.class)
    public void reportsNoCmdSpec2() throws ValidationException {
        CCSpec spec = new CCSpec();
        spec.setCmdSpecs(new CmdSpecs());
        new Reports("whatever", spec);
    }

    @Test(expected = ValidationException.class)
    public void reportsDuplicateName(@Mocked CCOperation operation) throws ValidationException {
        CCSpec spec = new CCSpec();
        spec.setCmdSpecs(new CmdSpecs());
        CCCmdSpec cmdSpec = new CCCmdSpec();
        cmdSpec.setName("specName");
        cmdSpec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc1");
        cmdSpec.getOpSpecs().getOpSpec().add(opSpec);
        spec.getCmdSpecs().getCmdSpec().add(cmdSpec);
        cmdSpec = new CCCmdSpec();
        cmdSpec.setName("specName");
        cmdSpec.setOpSpecs(new OpSpecs());
        opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc2");
        cmdSpec.getOpSpecs().getOpSpec().add(opSpec);
        spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

        new Reports("whatever", spec);
    }

    @Test
    public void enqueue(@Mocked CCOperation operation, @Mocked final ReportsInfo<CCReports, Tags> info, @Mocked final SubscriberController subscriber,
            @Mocked ThreadManager manager) throws ValidationException {
        CCSpec spec = new CCSpec();
        spec.setCmdSpecs(new CmdSpecs());
        CCCmdSpec cmdSpec = new CCCmdSpec();
        cmdSpec.setName("specName");
        cmdSpec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc1");
        cmdSpec.getOpSpecs().getOpSpec().add(opSpec);
        spec.getCmdSpecs().getCmdSpec().add(cmdSpec);
        final Reports reports = new Reports("whatever", spec);

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
    public void runWithUndefine(@Mocked final SubscriberController subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        CCSpec spec = new CCSpec();
        spec.setCmdSpecs(new CmdSpecs());
        final CCCmdSpec cmdSpec = new CCCmdSpec();
        cmdSpec.setName("specName");
        spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

        new NonStrictExpectations() {
            {
                report.getSpec();
                result = cmdSpec;

                report.getFilter();
                result = filter;

                filter.getOperations();
                result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                report.getOperations();
                result = new CCOperation[0];
            }
        };

        final Reports reports = new Reports("whatever", spec);

        Tags tags = new Tags();

        ReportsInfo<CCReports, Tags> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, tags, new Date(), 10, Initiation.UNDEFINE,
                "init", Termination.UNDEFINE, "term");
        reports.enqueue(info); // executes run

        Thread.sleep(70);

        new Verifications() {
            {
                subscriber.inc();
                times = 1;

                CCReports ccReports = null;
                subscriber.enqueue(ccReports = withCapture());
                times = 1;

                Date now = new Date();
                Assert.assertNotNull(ccReports);
                Assert.assertEquals(new BigDecimal(1), ccReports.getSchemaVersion());
                Assert.assertTrue(ccReports.getCreationDate().getTime() >= now.getTime() - 100 && ccReports.getCreationDate().getTime() <= now.getTime());
                Assert.assertEquals("whatever", ccReports.getSpecName());
                Assert.assertTrue(ccReports.getDate().getTime() >= now.getTime() - 100 && ccReports.getDate().getTime() <= now.getTime());
                Assert.assertEquals("Ha-VIS Middleware", ccReports.getALEID());
                Assert.assertEquals(10, ccReports.getTotalMilliseconds());
                Assert.assertEquals("UNDEFINE", ccReports.getInitiationCondition());
                Assert.assertEquals("init", ccReports.getInitiationTrigger());
                Assert.assertEquals("UNDEFINE", ccReports.getTerminationCondition());
                Assert.assertEquals("term", ccReports.getTerminationTrigger());
                Assert.assertNull(ccReports.getCmdReports());
                Assert.assertNull(ccReports.getCCSpec());

                subscriber.dec();
                times = 0;

                report.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void runWithSingleResult(@Mocked final SubscriberController subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            final CCSpec spec = new CCSpec();
            spec.setIncludeSpecInReports(Boolean.TRUE);
            spec.setCmdSpecs(new CmdSpecs());
            final CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");
            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
            tag.setCompleted(true);

            final Tags tags = new Tags();
            tags.add(tag, tag);
            tags.pulse();

            final CCCmdReport cmdReport = new CCCmdReport();
            cmdReport.setTagReports(new TagReports());
            cmdReport.setCmdSpecName("specName");
            cmdReport.getTagReports().getTagReport().add(new CCTagReport(tag.toString(), new ArrayList<CCOpReport>(), new ArrayList<CCTagStat>()));

            new NonStrictExpectations() {
                {
                    report.getSpec();
                    result = cmdSpec;

                    report.getFilter();
                    result = filter;

                    filter.getOperations();
                    result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                    report.getOperations();
                    result = new CCOperation[0];

                    report.get(withEqual(tags.toList()));
                    result = cmdReport;
                }
            };

            final Reports reports = new Reports("whatever", spec);

            ReportsInfo<CCReports, Tags> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, tags, new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info); // executes run

            Thread.sleep(70);

            new Verifications() {
                {
                    subscriber.inc();
                    times = 1;

                    CCReports ccReports = null;
                    subscriber.enqueue(ccReports = withCapture());
                    times = 1;

                    Date now = new Date();
                    Assert.assertNotNull(ccReports);
                    Assert.assertEquals(new BigDecimal(1), ccReports.getSchemaVersion());
                    Assert.assertTrue(ccReports.getCreationDate().getTime() >= now.getTime() - 100 && ccReports.getCreationDate().getTime() <= now.getTime());
                    Assert.assertEquals("whatever", ccReports.getSpecName());
                    Assert.assertTrue(ccReports.getDate().getTime() >= now.getTime() - 100 && ccReports.getDate().getTime() <= now.getTime());
                    Assert.assertEquals("Ha-VIS Middleware", ccReports.getALEID());
                    Assert.assertEquals(10, ccReports.getTotalMilliseconds());
                    Assert.assertEquals("NULL", ccReports.getInitiationCondition());
                    Assert.assertEquals("init", ccReports.getInitiationTrigger());
                    Assert.assertEquals("NULL", ccReports.getTerminationCondition());
                    Assert.assertEquals("term", ccReports.getTerminationTrigger());
                    Assert.assertNotNull(ccReports.getCmdReports());
                    Assert.assertEquals(1, ccReports.getCmdReports().getCmdReport().size());
                    Assert.assertEquals(cmdReport, ccReports.getCmdReports().getCmdReport().get(0));
                    Assert.assertEquals(spec, ccReports.getCCSpec());

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void runWithSingleResultAndRemovableTags(@Mocked final SubscriberController subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            final CCSpec spec = new CCSpec();
            spec.setIncludeSpecInReports(Boolean.TRUE);
            spec.setCmdSpecs(new CmdSpecs());
            final CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");
            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            final Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
            tag.setCompleted(true);

            Tag tagWillBeRemovedNoResultMap = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
            tagWillBeRemovedNoResultMap.setResult(null);

            Tag tagWillBeRemovedNoResultValue = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6791");
            tagWillBeRemovedNoResultValue.setResult(new HashMap<Integer, Result>());
            tagWillBeRemovedNoResultValue.getResult().put(Integer.valueOf(1), null);

            final Tags tags = new Tags();
            tags.add(tag, tag);
            tags.pulse();
            tags.add(tagWillBeRemovedNoResultMap, tagWillBeRemovedNoResultMap);
            tags.pulse();
            tags.add(tagWillBeRemovedNoResultValue, tagWillBeRemovedNoResultValue);
            tags.pulse();

            final CCCmdReport cmdReport = new CCCmdReport();
            cmdReport.setTagReports(new TagReports());
            cmdReport.setCmdSpecName("specName");
            cmdReport.getTagReports().getTagReport().add(new CCTagReport(tag.toString(), new ArrayList<CCOpReport>(), new ArrayList<CCTagStat>()));

            new NonStrictExpectations() {
                {
                    report.getSpec();
                    result = cmdSpec;

                    report.getFilter();
                    result = filter;

                    filter.getOperations();
                    result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                    report.getOperations();
                    result = new CCOperation[0];

                    report.get(withEqual(Arrays.asList(tag)));
                    result = cmdReport;
                }
            };

            final Reports reports = new Reports("whatever", spec);

            ReportsInfo<CCReports, Tags> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, tags, new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info); // executes run

            Thread.sleep(70);

            new Verifications() {
                {
                    subscriber.inc();
                    times = 1;

                    CCReports ccReports = null;
                    subscriber.enqueue(ccReports = withCapture());
                    times = 1;

                    Date now = new Date();
                    Assert.assertNotNull(ccReports);
                    Assert.assertEquals(new BigDecimal(1), ccReports.getSchemaVersion());
                    Assert.assertTrue(ccReports.getCreationDate().getTime() >= now.getTime() - 100 && ccReports.getCreationDate().getTime() <= now.getTime());
                    Assert.assertEquals("whatever", ccReports.getSpecName());
                    Assert.assertTrue(ccReports.getDate().getTime() >= now.getTime() - 100 && ccReports.getDate().getTime() <= now.getTime());
                    Assert.assertEquals("Ha-VIS Middleware", ccReports.getALEID());
                    Assert.assertEquals(10, ccReports.getTotalMilliseconds());
                    Assert.assertEquals("NULL", ccReports.getInitiationCondition());
                    Assert.assertEquals("init", ccReports.getInitiationTrigger());
                    Assert.assertEquals("NULL", ccReports.getTerminationCondition());
                    Assert.assertEquals("term", ccReports.getTerminationTrigger());
                    Assert.assertNotNull(ccReports.getCmdReports());
                    Assert.assertEquals(1, ccReports.getCmdReports().getCmdReport().size());
                    Assert.assertEquals(cmdReport, ccReports.getCmdReports().getCmdReport().get(0));
                    Assert.assertEquals(spec, ccReports.getCCSpec());

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void runWithNoTags(@Mocked final SubscriberController subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            CCSpec spec = new CCSpec();
            spec.setIncludeSpecInReports(Boolean.TRUE);
            spec.setCmdSpecs(new CmdSpecs());
            final CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");
            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            final Tags tags = new Tags();

            new NonStrictExpectations() {
                {
                    report.getSpec();
                    result = cmdSpec;

                    report.getFilter();
                    result = filter;

                    filter.getOperations();
                    result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                    report.getOperations();
                    result = new CCOperation[0];
                }
            };

            final Reports reports = new Reports("whatever", spec);

            ReportsInfo<CCReports, Tags> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, tags, new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info); // executes run

            Thread.sleep(70);

            new Verifications() {
                {
                    subscriber.inc();
                    times = 1;

                    subscriber.enqueue(this.<CCReports> withNotNull());
                    times = 0;

                    subscriber.dec();
                    times = 1;

                    report.dispose();
                    times = 0;
                }
            };
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void runWithNoTagsAndEmptyReport(@Mocked final SubscriberController subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            final CCSpec spec = new CCSpec();
            spec.setIncludeSpecInReports(Boolean.TRUE);
            spec.setCmdSpecs(new CmdSpecs());
            final CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setReportIfEmpty(Boolean.TRUE); // report empty
            cmdSpec.setName("specName");
            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            final Tags tags = new Tags();

            final CCCmdReport cmdReport = new CCCmdReport();
            cmdReport.setTagReports(new TagReports());
            cmdReport.setCmdSpecName("specName");

            new NonStrictExpectations() {
                {
                    report.getSpec();
                    result = cmdSpec;

                    report.getFilter();
                    result = filter;

                    filter.getOperations();
                    result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                    report.getOperations();
                    result = new CCOperation[0];

                    report.get(withEqual(new ArrayList<Tag>()));
                    result = cmdReport;
                }
            };

            final Reports reports = new Reports("whatever", spec);

            ReportsInfo<CCReports, Tags> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, tags, new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info); // executes run

            Thread.sleep(70);

            new Verifications() {
                {
                    subscriber.inc();
                    times = 1;

                    CCReports ccReports = null;
                    subscriber.enqueue(ccReports = withCapture());
                    times = 1;

                    Date now = new Date();
                    Assert.assertNotNull(ccReports);
                    Assert.assertEquals(new BigDecimal(1), ccReports.getSchemaVersion());
                    Assert.assertTrue(ccReports.getCreationDate().getTime() >= now.getTime() - 100 && ccReports.getCreationDate().getTime() <= now.getTime());
                    Assert.assertEquals("whatever", ccReports.getSpecName());
                    Assert.assertTrue(ccReports.getDate().getTime() >= now.getTime() - 100 && ccReports.getDate().getTime() <= now.getTime());
                    Assert.assertEquals("Ha-VIS Middleware", ccReports.getALEID());
                    Assert.assertEquals(10, ccReports.getTotalMilliseconds());
                    Assert.assertEquals("NULL", ccReports.getInitiationCondition());
                    Assert.assertEquals("init", ccReports.getInitiationTrigger());
                    Assert.assertEquals("NULL", ccReports.getTerminationCondition());
                    Assert.assertEquals("term", ccReports.getTerminationTrigger());
                    Assert.assertNotNull(ccReports.getCmdReports());
                    Assert.assertEquals(1, ccReports.getCmdReports().getCmdReport().size());
                    Assert.assertEquals(cmdReport, ccReports.getCmdReports().getCmdReport().get(0));
                    Assert.assertEquals(spec, ccReports.getCCSpec());

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void runWithNoTagsAndSubscriberListener(@Mocked final SubscriberListener<CCReports> subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            final CCSpec spec = new CCSpec();
            spec.setIncludeSpecInReports(Boolean.TRUE);
            spec.setCmdSpecs(new CmdSpecs());
            final CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");
            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            final Tags tags = new Tags();

            new NonStrictExpectations() {
                {
                    report.getSpec();
                    result = cmdSpec;

                    report.getFilter();
                    result = filter;

                    filter.getOperations();
                    result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                    report.getOperations();
                    result = new CCOperation[0];
                }
            };

            final Reports reports = new Reports("whatever", spec);

            ReportsInfo<CCReports, Tags> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, tags, new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info); // executes run

            Thread.sleep(70);

            new Verifications() {
                {
                    subscriber.inc();
                    times = 1;

                    CCReports ccReports = null;
                    subscriber.enqueue(ccReports = withCapture());
                    times = 1;

                    Date now = new Date();
                    Assert.assertNotNull(ccReports);
                    Assert.assertEquals(new BigDecimal(1), ccReports.getSchemaVersion());
                    Assert.assertTrue(ccReports.getCreationDate().getTime() >= now.getTime() - 100 && ccReports.getCreationDate().getTime() <= now.getTime());
                    Assert.assertEquals("whatever", ccReports.getSpecName());
                    Assert.assertTrue(ccReports.getDate().getTime() >= now.getTime() - 100 && ccReports.getDate().getTime() <= now.getTime());
                    Assert.assertEquals("Ha-VIS Middleware", ccReports.getALEID());
                    Assert.assertEquals(10, ccReports.getTotalMilliseconds());
                    Assert.assertEquals("NULL", ccReports.getInitiationCondition());
                    Assert.assertEquals("init", ccReports.getInitiationTrigger());
                    Assert.assertEquals("NULL", ccReports.getTerminationCondition());
                    Assert.assertEquals("term", ccReports.getTerminationTrigger());
                    Assert.assertNotNull(ccReports.getCmdReports());
                    Assert.assertEquals(0, ccReports.getCmdReports().getCmdReport().size());
                    Assert.assertEquals(spec, ccReports.getCCSpec());

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void runWithSingleResultAndIncompleteTags(@Mocked final SubscriberController subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            final CCSpec spec = new CCSpec();
            spec.setIncludeSpecInReports(Boolean.TRUE);
            spec.setCmdSpecs(new CmdSpecs());
            final CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");
            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            final Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
            tag.setCompleted(true);

            final Tag tagIncomplete = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
            tagIncomplete.setResult(null);
            tagIncomplete.setCompleted(false);

            final Tags tags = new Tags();
            tags.add(tag, tag);
            tags.pulse();
            tags.add(tagIncomplete, null);

            final CCCmdReport cmdReport = new CCCmdReport();
            cmdReport.setTagReports(new TagReports());
            cmdReport.setCmdSpecName("specName");
            cmdReport.getTagReports().getTagReport().add(new CCTagReport(tag.toString(), new ArrayList<CCOpReport>(), new ArrayList<CCTagStat>()));
            cmdReport.getTagReports().getTagReport().add(new CCTagReport(tagIncomplete.toString(), new ArrayList<CCOpReport>(), new ArrayList<CCTagStat>()));

            new NonStrictExpectations() {
                {
                    report.getSpec();
                    result = cmdSpec;

                    report.getFilter();
                    result = filter;

                    filter.getOperations();
                    result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                    filter.match(withEqual(tagIncomplete));
                    result = Boolean.TRUE;

                    report.isCompleted(withEqual(tagIncomplete));
                    result = Boolean.FALSE;

                    report.getOperations();
                    result = new CCOperation[0];

                    report.get(withEqual(Arrays.asList(tag, tagIncomplete)));
                    result = cmdReport;
                }
            };

            final Reports reports = new Reports("whatever", spec);

            ReportsInfo<CCReports, Tags> info = new ReportsInfo<>(new SubscriberController[] { subscriber }, tags, new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info); // executes run

            Thread.sleep(70);

            // still waiting for completion
            new Verifications() {
                {
                    subscriber.inc();
                    times = 1;

                    subscriber.enqueue(this.<CCReports> withNotNull());
                    times = 0;

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };

            // complete the tag
            tagIncomplete.setResult(new HashMap<Integer, Result>());
            tagIncomplete.getResult().put(Integer.valueOf(1),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, 0x7C }));
            tagIncomplete.setCompleted(true);

            tags.add(tagIncomplete, tagIncomplete);
            tags.pulse(); // stop waiting

            Thread.sleep(100);

            new Verifications() {
                {
                    subscriber.inc();
                    times = 1;

                    CCReports ccReports = null;
                    subscriber.enqueue(ccReports = withCapture());
                    times = 1;

                    Date now = new Date();
                    Assert.assertNotNull(ccReports);
                    Assert.assertEquals(new BigDecimal(1), ccReports.getSchemaVersion());
                    Assert.assertTrue(ccReports.getCreationDate().getTime() >= now.getTime() - 200 && ccReports.getCreationDate().getTime() <= now.getTime());
                    Assert.assertEquals("whatever", ccReports.getSpecName());
                    Assert.assertTrue(ccReports.getDate().getTime() >= now.getTime() - 200 && ccReports.getDate().getTime() <= now.getTime());
                    Assert.assertEquals("Ha-VIS Middleware", ccReports.getALEID());
                    Assert.assertEquals(10, ccReports.getTotalMilliseconds());
                    Assert.assertEquals("NULL", ccReports.getInitiationCondition());
                    Assert.assertEquals("init", ccReports.getInitiationTrigger());
                    Assert.assertEquals("NULL", ccReports.getTerminationCondition());
                    Assert.assertEquals("term", ccReports.getTerminationTrigger());
                    Assert.assertNotNull(ccReports.getCmdReports());
                    Assert.assertEquals(1, ccReports.getCmdReports().getCmdReport().size());
                    Assert.assertEquals(cmdReport, ccReports.getCmdReports().getCmdReport().get(0));
                    Assert.assertEquals(spec, ccReports.getCCSpec());

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void runWithSingleResultThrowingException(@Mocked final SubscriberController subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            CCSpec spec = new CCSpec();
            spec.setIncludeSpecInReports(Boolean.TRUE);
            spec.setCmdSpecs(new CmdSpecs());
            final CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");
            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            new NonStrictExpectations() {
                {
                    report.getSpec();
                    result = cmdSpec;

                    report.getFilter();
                    result = filter;

                    filter.getOperations();
                    result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                    report.getOperations();
                    result = new CCOperation[0];
                }
            };

            final Reports reports = new Reports("whatever", spec);
            Thread thread = new Thread(reports);
            thread.start(); // will wait on queue.take()

            Thread.sleep(70);

            thread.interrupt();

            Thread.sleep(200);

            new Verifications() {
                {
                    subscriber.inc();
                    times = 0; // no reports.enqueue() called in this test

                    subscriber.enqueue(this.<CCReports> withNotNull());
                    times = 0;

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void getTagOperation() throws ValidationException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            CCSpec spec = new CCSpec();
            spec.setCmdSpecs(new CmdSpecs());
            CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");

            cmdSpec.setFilterSpec(new CCFilterSpec());
            cmdSpec.getFilterSpec().setExtension(new CCFilterSpecExtension());
            cmdSpec.getFilterSpec().setFilterList(new FilterList());
            ECFilterListMember member1 = new ECFilterListMember();
            member1.setPatList(new PatList());
            member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
            member1.setIncludeExclude("INCLUDE");
            member1.setFieldspec(new ECFieldSpec("epc"));
            cmdSpec.getFilterSpec().getFilterList().getFilter().add(member1);

            cmdSpec.setOpSpecs(new OpSpecs());
            CCOpSpec opSpec = new CCOpSpec();
            opSpec.setOpName("readEpc");
            opSpec.setOpType("READ");
            opSpec.setFieldspec(new ECFieldSpec("epc"));
            cmdSpec.getOpSpecs().getOpSpec().add(opSpec);

            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            final Reports reports = new Reports("whatever", spec);

            Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
            TagOperation tagOperation = reports.getTagOperation(tag);

            Assert.assertNotNull(tagOperation);
            Assert.assertNotNull(tagOperation.getFilter());
            Assert.assertEquals(1, tagOperation.getFilter().size());
            Assert.assertEquals(new Filter(1, 96, 32, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }),
                    tagOperation.getFilter().get(0));
            Assert.assertNotNull(tagOperation.getOperations());
            Assert.assertEquals(1, tagOperation.getOperations().size());
            Assert.assertEquals(2 /* filter is 1 */, tagOperation.getOperations().get(0).getId());
            Assert.assertEquals(OperationType.READ, tagOperation.getOperations().get(0).getType());
            Assert.assertEquals("epc", tagOperation.getOperations().get(0).getField().getName());

            tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.MISC_ERROR_TOTAL));
            tagOperation = reports.getTagOperation(tag);
            Assert.assertNull(tagOperation);
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void isCompleted() throws ValidationException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            CCSpec spec = new CCSpec();
            spec.setCmdSpecs(new CmdSpecs());
            CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");

            cmdSpec.setFilterSpec(new CCFilterSpec());
            cmdSpec.getFilterSpec().setExtension(new CCFilterSpecExtension());
            cmdSpec.getFilterSpec().setFilterList(new FilterList());
            ECFilterListMember member1 = new ECFilterListMember();
            member1.setPatList(new PatList());
            member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
            member1.setIncludeExclude("INCLUDE");
            member1.setFieldspec(new ECFieldSpec("epc"));
            cmdSpec.getFilterSpec().getFilterList().getFilter().add(member1);

            cmdSpec.setOpSpecs(new OpSpecs());
            CCOpSpec opSpec = new CCOpSpec();
            opSpec.setOpName("readEpc");
            opSpec.setOpType("READ");
            opSpec.setFieldspec(new ECFieldSpec("epc"));
            cmdSpec.getOpSpecs().getOpSpec().add(opSpec);

            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            final Reports reports = new Reports("whatever", spec);

            Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
            tag.getResult().put(Integer.valueOf(2),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
            tag.setCompleted(false);
            Assert.assertTrue(reports.isCompleted(tag));
            Assert.assertTrue(tag.isCompleted());

            tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
            tag.getResult().put(Integer.valueOf(2),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
            tag.setCompleted(true);
            Assert.assertTrue(reports.isCompleted(tag));
            Assert.assertTrue(tag.isCompleted());

            tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));
            tag.getResult().put(Integer.valueOf(2), null); // no result
            tag.setCompleted(false);
            Assert.assertFalse(reports.isCompleted(tag));
            Assert.assertFalse(tag.isCompleted());
        } finally {
            Tag.setExtended(extended);
        }
    }

    @Test
    public void dispose(@Mocked final Report report, @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException {
        CCSpec spec = new CCSpec();
        spec.setCmdSpecs(new CmdSpecs());
        CCCmdSpec cmdSpec = new CCCmdSpec();
        cmdSpec.setName("specName");
        spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

        new NonStrictExpectations() {
            {
                report.getFilter();
                result = filter;

                filter.getOperations();
                result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                report.getOperations();
                result = new CCOperation[0];
            }
        };

        final Reports reports = new Reports("whatever", spec);
        reports.dispose();

        new Verifications() {
            {
                report.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void disposeWaitForQueue(@Mocked final SubscriberController subscriber, @Mocked final Report report,
            @Mocked final havis.middleware.ale.core.report.cc.Filter filter) throws ValidationException, InterruptedException {
        boolean extended = Tag.isExtended();
        Tag.setExtended(false);
        try {
            final CCSpec spec = new CCSpec();
            spec.setIncludeSpecInReports(Boolean.TRUE);
            spec.setCmdSpecs(new CmdSpecs());
            final CCCmdSpec cmdSpec = new CCCmdSpec();
            cmdSpec.setName("specName");
            spec.getCmdSpecs().getCmdSpec().add(cmdSpec);

            final Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
            tag.setResult(new HashMap<Integer, Result>());
            tag.getResult().put(Integer.valueOf(1),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
            tag.setCompleted(true);

            final Tag tagIncomplete = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
            tagIncomplete.setResult(null);
            tagIncomplete.setCompleted(false);

            final Tags tags = new Tags();
            tags.add(tag, tag);
            tags.pulse();
            tags.add(tagIncomplete, null);

            final CCCmdReport cmdReport = new CCCmdReport();
            cmdReport.setTagReports(new TagReports());
            cmdReport.setCmdSpecName("specName");
            cmdReport.getTagReports().getTagReport().add(new CCTagReport(tag.toString(), new ArrayList<CCOpReport>(), new ArrayList<CCTagStat>()));
            cmdReport.getTagReports().getTagReport().add(new CCTagReport(tagIncomplete.toString(), new ArrayList<CCOpReport>(), new ArrayList<CCTagStat>()));

            new NonStrictExpectations() {
                {
                    report.getSpec();
                    result = cmdSpec;

                    report.getFilter();
                    result = filter;

                    filter.getOperations();
                    result = new ArrayList<havis.middleware.ale.base.operation.tag.Operation>();

                    filter.match(withEqual(tagIncomplete));
                    result = Boolean.TRUE;

                    report.isCompleted(withEqual(tagIncomplete));
                    result = Boolean.FALSE;

                    report.getOperations();
                    result = new CCOperation[0];

                    report.get(withEqual(Arrays.asList(tag, tagIncomplete)));
                    result = cmdReport;
                }
            };

            final Reports reports = new Reports("whatever", spec);

            ReportsInfo<CCReports, Tags> info1 = new ReportsInfo<>(new SubscriberController[] { subscriber }, tags, new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info1); // executes run

            Thread.sleep(50);

            // still waiting for completion
            new Verifications() {
                {
                    subscriber.inc();
                    times = 1;

                    subscriber.enqueue(this.<CCReports> withNotNull());
                    times = 0;

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };

            ReportsInfo<CCReports, Tags> info2 = new ReportsInfo<>(new SubscriberController[] { subscriber }, new Tags(), new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info2); // waits for first execution

            Thread.sleep(50);

            ReportsInfo<CCReports, Tags> info3 = new ReportsInfo<>(new SubscriberController[] { subscriber }, new Tags(), new Date(), 10, Initiation.NULL,
                    "init", Termination.NULL, "term");
            reports.enqueue(info3); // waits for first execution

            Thread.sleep(50);

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

                    subscriber.enqueue(this.<CCReports> withNotNull());
                    times = 0;

                    subscriber.dec();
                    times = 0;

                    report.dispose();
                    times = 0;
                }
            };

            // complete the tag
            tagIncomplete.setResult(new HashMap<Integer, Result>());
            tagIncomplete.getResult().put(Integer.valueOf(1),
                    new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, 0x7C }));
            tagIncomplete.setCompleted(true);

            tags.add(tagIncomplete, tagIncomplete);
            tags.pulse(); // stop waiting

            Thread.sleep(200);

            Assert.assertFalse(disposeThread.isAlive());

            new Verifications() {
                {
                    subscriber.inc();
                    times = 3;

                    CCReports ccReports = null;
                    subscriber.enqueue(ccReports = withCapture());
                    times = 1;

                    Date now = new Date();
                    Assert.assertNotNull(ccReports);
                    Assert.assertEquals(new BigDecimal(1), ccReports.getSchemaVersion());
                    Assert.assertTrue(ccReports.getCreationDate().getTime() >= now.getTime() - 500 && ccReports.getCreationDate().getTime() <= now.getTime());
                    Assert.assertEquals("whatever", ccReports.getSpecName());
                    Assert.assertTrue(ccReports.getDate().getTime() >= now.getTime() - 500 && ccReports.getDate().getTime() <= now.getTime());
                    Assert.assertEquals("Ha-VIS Middleware", ccReports.getALEID());
                    Assert.assertEquals(10, ccReports.getTotalMilliseconds());
                    Assert.assertEquals("NULL", ccReports.getInitiationCondition());
                    Assert.assertEquals("init", ccReports.getInitiationTrigger());
                    Assert.assertEquals("NULL", ccReports.getTerminationCondition());
                    Assert.assertEquals("term", ccReports.getTerminationTrigger());
                    Assert.assertNotNull(ccReports.getCmdReports());
                    Assert.assertEquals(1, ccReports.getCmdReports().getCmdReport().size());
                    Assert.assertEquals(cmdReport, ccReports.getCmdReports().getCmdReport().get(0));
                    Assert.assertEquals(spec, ccReports.getCCSpec());

                    subscriber.dec();
                    times = 2;

                    report.dispose();
                    times = 1;
                }
            };
        } finally {
            Tag.setExtended(extended);
        }
    }
}
