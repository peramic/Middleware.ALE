package havis.middleware.ale.core.report.cc;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Statistics;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.operation.tag.result.VirtualReadResult;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.report.cc.data.Common;
import havis.middleware.ale.core.report.cc.data.Parameters;
import havis.middleware.ale.core.stat.Count;
import havis.middleware.ale.core.stat.Reader;
import havis.middleware.ale.core.stat.ReaderNames;
import havis.middleware.ale.core.stat.SightingSignal;
import havis.middleware.ale.core.stat.Timestamps;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.ECFilterListMember.PatList;
import havis.middleware.ale.service.cc.CCCmdReport;
import havis.middleware.ale.service.cc.CCCmdSpec;
import havis.middleware.ale.service.cc.CCCmdSpec.OpSpecs;
import havis.middleware.ale.service.cc.CCCmdSpec.StatProfileNames;
import havis.middleware.ale.service.cc.CCFilterSpec;
import havis.middleware.ale.service.cc.CCFilterSpec.FilterList;
import havis.middleware.ale.service.cc.CCFilterSpecExtension;
import havis.middleware.ale.service.cc.CCOpDataSpec;
import havis.middleware.ale.service.cc.CCOpReport;
import havis.middleware.ale.service.cc.CCOpSpec;
import havis.middleware.ale.service.cc.CCTagCountStat;
import havis.middleware.ale.service.cc.CCTagStat;
import havis.middleware.ale.service.cc.CCTagTimestampStat;
import havis.middleware.tdt.TdtTranslationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReportTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void report() throws ValidationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setFilterSpec(new CCFilterSpec());
        spec.getFilterSpec().setExtension(new CCFilterSpecExtension());
        spec.getFilterSpec().setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        spec.getFilterSpec().getFilterList().getFilter().add(member1);
        ECFilterListMember member2 = new ECFilterListMember();
        member2.setPatList(new PatList());
        member2.getPatList().getPat().add("X");
        member2.setIncludeExclude("EXCLUDE");
        member2.setFieldspec(new ECFieldSpec("killPwd"));
        spec.getFilterSpec().getFilterList().getFilter().add(member2);

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        opSpec.setOpType("READ");
        opSpec.setFieldspec(new ECFieldSpec("epc"));
        spec.getOpSpecs().getOpSpec().add(opSpec);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("TagTimestamps");
        spec.getStatProfileNames().getStatProfileName().add("TagCount");
        spec.getStatProfileNames().getStatProfileName().add("ReaderNames");
        spec.getStatProfileNames().getStatProfileName().add("ReaderSightingSignals");

        Report report = new Report(spec, new Parameters());

        Assert.assertSame(spec, report.getSpec());
        Assert.assertEquals("specName", report.getName());

        Filter filter = report.getFilter();
        Assert.assertNotNull(filter);
        Collection<havis.middleware.ale.base.operation.tag.Operation> filterOperations = filter.getOperations();
        Assert.assertNotNull(filterOperations);
        Assert.assertEquals(2, filterOperations.size());
        Iterator<havis.middleware.ale.base.operation.tag.Operation> iterator = filterOperations.iterator();
        havis.middleware.ale.base.operation.tag.Operation op1 = iterator.next();
        Assert.assertEquals(0, op1.getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.READ, op1.getType());
        Assert.assertEquals("epc", op1.getField().getName());
        havis.middleware.ale.base.operation.tag.Operation op2 = iterator.next();
        Assert.assertEquals(0, op2.getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.READ, op2.getType());
        Assert.assertEquals("killPwd", op2.getField().getName());

        CCOperation[] operations = report.getOperations();
        Assert.assertNotNull(operations);
        Assert.assertEquals(1, operations.length);
        Assert.assertTrue(operations[0] instanceof ReadOperation);
        Assert.assertEquals("epc", operations[0].getField().getName());

        Reader<CCTagStat, Statistics>[] stats = report.getStats();
        Assert.assertNotNull(operations);
        Assert.assertEquals(4, stats.length);
        Assert.assertEquals("TagTimestamps", stats[0].getProfile());
        Assert.assertTrue(stats[0] instanceof Timestamps);
        Assert.assertEquals("TagCount", stats[1].getProfile());
        Assert.assertTrue(stats[1] instanceof Count);
        Assert.assertEquals("ReaderNames", stats[2].getProfile());
        Assert.assertTrue(stats[2] instanceof ReaderNames);
        Assert.assertEquals("ReaderSightingSignals", stats[3].getProfile());
        Assert.assertTrue(stats[3] instanceof SightingSignal);

        spec.setOpSpecs(null);
        spec.setStatProfileNames(new StatProfileNames());
        spec.setFilterSpec(new CCFilterSpec());
        spec.getFilterSpec().setExtension(new CCFilterSpecExtension());
        spec.getFilterSpec().setFilterList(new FilterList());
        report = new Report(spec, new Parameters());

        Assert.assertSame(spec, report.getSpec());
        Assert.assertEquals("specName", report.getName());

        filter = report.getFilter();
        Assert.assertNotNull(filter);
        filterOperations = filter.getOperations();
        Assert.assertNotNull(filterOperations);
        Assert.assertEquals(0, filterOperations.size());

        operations = report.getOperations();
        Assert.assertNotNull(operations);
        Assert.assertEquals(0, operations.length);

        stats = report.getStats();
        Assert.assertNull(stats);
    }

    @Test(expected = ValidationException.class)
    public void reportInvalidFilter(@Mocked final Filter filter) throws ValidationException {
        final CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        new NonStrictExpectations() {
            {
                new Filter(this.<CCFilterSpec> withNull());
                result = new ValidationException();
            }
        };

        new Report(spec, new Parameters());
    }

    @Test(expected = ValidationException.class)
    public void reportInvalidStatProfileName() throws ValidationException {
        final CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");
        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("whatever");

        new Report(spec, new Parameters());
    }

    @Test
    public void reportDuplicateOperationName(@Mocked final CCOperation operation) throws ValidationException {
        final CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");
        spec.setOpSpecs(new OpSpecs());
        final CCOpSpec opSpec1 = new CCOpSpec();
        opSpec1.setOpName("readEpc");
        spec.getOpSpecs().getOpSpec().add(opSpec1);
        final CCOpSpec opSpec2 = new CCOpSpec();
        opSpec2.setOpName("readEpc");
        spec.getOpSpecs().getOpSpec().add(opSpec2);

        final Parameters parameters = new Parameters();

        try {
            new Report(spec, parameters);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        new Verifications() {
            {
                CCOperation operation1 = CCOperation.get(withEqual(opSpec1), withEqual(parameters));
                times = 1;

                CCOperation.get(withEqual(opSpec2), withEqual(parameters));
                times = 0;

                operation1.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void getOperations() throws ValidationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        opSpec.setOpType("READ");
        opSpec.setFieldspec(new ECFieldSpec("epc"));
        spec.getOpSpecs().getOpSpec().add(opSpec);

        Report report = new Report(spec, new Parameters());
        report.getOperations()[0].setId(0);

        Tag tag = new Tag(new byte[] { 0x01 });
        tag.setResult(new HashMap<Integer, Result>());
        tag.getResult().put(Integer.valueOf(0), new ReadResult(ResultState.SUCCESS));

        List<havis.middleware.ale.base.operation.tag.Operation> tagOperations = new ArrayList<>();
        report.getOperations(tag, tagOperations);

        Assert.assertEquals(1, tagOperations.size());
        Assert.assertEquals(0, tagOperations.get(0).getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.READ, tagOperations.get(0).getType());
        Assert.assertEquals("epc", tagOperations.get(0).getField().getName());

        Assert.assertEquals(1, tag.getResult().size());
        Assert.assertEquals(ResultState.SUCCESS, tag.getResult().get(Integer.valueOf(0)).getState());
        Assert.assertTrue(tag.getResult().get(Integer.valueOf(0)) instanceof ReadResult);
    }

    @Test
    public void getOperationsWithIntermediateResult() throws ValidationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec1 = new CCOpSpec();
        opSpec1.setOpName("add1");
        opSpec1.setOpType("ADD");
        opSpec1.setFieldspec(new ECFieldSpec("@3.urn:oid:1.0.15961.9.21"));
        CCOpDataSpec data1 = new CCOpDataSpec();
        data1.setSpecType("LITERAL");
        data1.setData("CID");
		opSpec1.setDataSpec(data1);
        spec.getOpSpecs().getOpSpec().add(opSpec1);
        CCOpSpec opSpec2 = new CCOpSpec();
        opSpec2.setOpName("add2");
        opSpec2.setOpType("ADD");
        opSpec2.setFieldspec(new ECFieldSpec("@3.urn:oid:1.0.15961.9.22"));
        CCOpDataSpec data2 = new CCOpDataSpec();
        data2.setSpecType("LITERAL");
        data2.setData("CIDDFLWAX");
		opSpec2.setDataSpec(data2);
        spec.getOpSpecs().getOpSpec().add(opSpec2);

        Report report = new Report(spec, new Parameters());
        report.getOperations()[0].setId(0);
        report.getOperations()[1].setId(1);

        Tag tag = new Tag(new byte[] { 0x01 });
        tag.setResult(new HashMap<Integer, Result>());
        tag.getResult().put(Integer.valueOf(0), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0x89, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }));
        tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { (byte) 0x89, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }));

        List<havis.middleware.ale.base.operation.tag.Operation> tagOperations = new ArrayList<>();
        report.getOperations(tag, tagOperations);

        Assert.assertEquals(2, tagOperations.size());
        Assert.assertEquals(0, tagOperations.get(0).getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.WRITE, tagOperations.get(0).getType());
        Assert.assertArrayEquals(new byte[] { (byte) 0x89, 0x16, 0x05, (byte) 0xC7, 0x17, 0x3D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, tagOperations.get(0).getData());
        Assert.assertEquals(128, tagOperations.get(0).getBitLength());
        Assert.assertEquals(3, tagOperations.get(0).getField().getBank());
        Assert.assertEquals(0, tagOperations.get(0).getField().getOffset());
        Assert.assertEquals(0, tagOperations.get(0).getField().getLength());
        
        Assert.assertEquals(1, tagOperations.get(1).getId());
        Assert.assertEquals(havis.middleware.ale.base.operation.tag.OperationType.WRITE, tagOperations.get(1).getType());
        Assert.assertArrayEquals(new byte[] {(byte) 0x89, 0x36, 0x47, (byte) 0xD5, 0x21, (byte) 0xFF, (byte) 0xE3, 0x3F, (byte) 0xE5, 0x22, 0x2D, 0x06, (byte) 0xB8, 0x4A, 0x00, 0x00 }, tagOperations.get(1).getData());
        Assert.assertEquals(128, tagOperations.get(1).getBitLength());
        Assert.assertEquals(3, tagOperations.get(1).getField().getBank());
        Assert.assertEquals(0, tagOperations.get(1).getField().getOffset());
        Assert.assertEquals(0, tagOperations.get(1).getField().getLength());

        Assert.assertEquals(2, tag.getResult().size());
        Assert.assertEquals(ResultState.SUCCESS, tag.getResult().get(Integer.valueOf(0)).getState());
        Assert.assertTrue(tag.getResult().get(Integer.valueOf(0)) instanceof ReadResult);
        Assert.assertFalse(tag.getResult().get(Integer.valueOf(0)) instanceof VirtualReadResult);
        Assert.assertArrayEquals(new byte[] { (byte) 0x89, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, ((ReadResult) tag.getResult().get(Integer.valueOf(0))).getData());
        Assert.assertEquals(ResultState.SUCCESS, tag.getResult().get(Integer.valueOf(1)).getState());
        Assert.assertTrue(tag.getResult().get(Integer.valueOf(1)) instanceof VirtualReadResult);
        Assert.assertArrayEquals(new byte[] { (byte) 0x89, 0x16, 0x05, (byte) 0xC7, 0x17, 0x3D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, ((VirtualReadResult) tag.getResult().get(Integer.valueOf(1))).getData());
    }

    @Test
    public void getOperationsError() throws ValidationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("lockUserBank");
        opSpec.setOpType("LOCK");
        opSpec.setDataSpec(new CCOpDataSpec());
        opSpec.getDataSpec().setData("LOCK");
        opSpec.getDataSpec().setSpecType("LITERAL");
        opSpec.setFieldspec(new ECFieldSpec("userBank"));
        spec.getOpSpecs().getOpSpec().add(opSpec);
        opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        opSpec.setOpType("READ");
        opSpec.setFieldspec(new ECFieldSpec("epc"));
        spec.getOpSpecs().getOpSpec().add(opSpec);

        Parameters parameters = new Parameters();

        Report report = new Report(spec, parameters);
        report.getOperations()[0].setId(0);
        // replace the created operation with one that surely fails
        Common current = report.getOperations()[0].getData();
        report.getOperations()[0].setData(new Common(current.getSpec(), parameters, null, new Bytes(ResultState.MISC_ERROR_TOTAL)));

        report.getOperations()[1].setId(1);

        Tag tag = new Tag(new byte[] { 0x01 });
        tag.setResult(new HashMap<Integer, Result>());
        tag.getResult().put(Integer.valueOf(0), new ReadResult(ResultState.SUCCESS));
        tag.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS));

        List<havis.middleware.ale.base.operation.tag.Operation> tagOperations = new ArrayList<>();
        report.getOperations(tag, tagOperations);

        Assert.assertEquals(0, tagOperations.size());

        Assert.assertEquals(2, tag.getResult().size());
        Assert.assertEquals(ResultState.MISC_ERROR_TOTAL, tag.getResult().get(Integer.valueOf(0)).getState());
        Assert.assertTrue(tag.getResult().get(Integer.valueOf(0)) instanceof FaultResult);
        Assert.assertEquals(ResultState.MISC_ERROR_TOTAL, tag.getResult().get(Integer.valueOf(1)).getState());
        Assert.assertTrue(tag.getResult().get(Integer.valueOf(1)) instanceof FaultResult);
    }

    @Test
    public void isComplete(@Mocked final CCOperation operation) throws ValidationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        spec.getOpSpecs().getOpSpec().add(opSpec);

        Report report = new Report(spec, new Parameters());

        final Tag tag1 = new Tag(new byte[] { 0x01 });
        tag1.setResult(null);
        Assert.assertFalse(report.isCompleted(tag1));

        tag1.setResult(new HashMap<Integer, Result>());
        Assert.assertFalse(report.isCompleted(tag1));

        tag1.setResult(new HashMap<Integer, Result>());
        tag1.getResult().put(Integer.valueOf(0), new ReadResult(ResultState.SUCCESS));
        new NonStrictExpectations() {
            {
                operation.getId();
                result = Integer.valueOf(0);

                operation.isCompleted(withEqual(tag1), this.<Result>withNotNull());
                result = Boolean.FALSE;
            }
        };
        Assert.assertFalse(report.isCompleted(tag1));

        final Tag tag2 = new Tag(new byte[] { 0x02 });
        tag2.setResult(new HashMap<Integer, Result>());
        tag2.getResult().put(Integer.valueOf(0), new ReadResult(ResultState.SUCCESS));
        new NonStrictExpectations() {
            {
                operation.getId();
                result = Integer.valueOf(0);

                operation.isCompleted(withEqual(tag2), this.<Result>withNotNull());
                result = Boolean.TRUE;
            }
        };
        Assert.assertTrue(report.isCompleted(tag2));
    }

    @Test
    public void getOperationReport(@Mocked final CCOperation operation) throws ValidationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        spec.getOpSpecs().getOpSpec().add(opSpec);

        Report report = new Report(spec, new Parameters());

        final Tag tag = new Tag(new byte[] { 0x01 });
        tag.setResult(new HashMap<Integer, Result>());
        final Result tagResult = new Result(ResultState.SUCCESS);
        tag.getResult().put(Integer.valueOf(0), tagResult);
        final CCOpReport opReport = new CCOpReport();

        new NonStrictExpectations() {
            {
                operation.getId();
                result = Integer.valueOf(0);

                operation.getReport(withEqual(tag), withEqual(tagResult));
                result = opReport;
            }
        };

        List<CCOpReport> operationReport = report.getOperationReport(tag);
        Assert.assertNotNull(operationReport);
        Assert.assertEquals(1, operationReport.size());
        Assert.assertSame(opReport, operationReport.get(0));
    }

    @Test
    public void get(@Mocked final CCOperation operation) throws ValidationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        spec.getOpSpecs().getOpSpec().add(opSpec);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("TagTimestamps");
        spec.getStatProfileNames().getStatProfileName().add("TagCount");
        spec.getStatProfileNames().getStatProfileName().add("ReaderNames");
        spec.getStatProfileNames().getStatProfileName().add("ReaderSightingSignals");

        Report report = new Report(spec, new Parameters());

        List<Tag> tags = new ArrayList<Tag>();
        final Tag tag1 = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tag1.setResult(new HashMap<Integer, Result>());
        tag1.getResult().put(Integer.valueOf(0), new Result(ResultState.SUCCESS));
        tags.add(tag1);
        final Tag tag2 = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
        tag2.setResult(new HashMap<Integer, Result>());
        tag2.getResult().put(Integer.valueOf(0), new Result(ResultState.SUCCESS));
        tags.add(tag2);

        final CCOpReport opReport1 = new CCOpReport();
        final CCOpReport opReport2 = new CCOpReport();

        new NonStrictExpectations() {
            {
                operation.getId();
                result = Integer.valueOf(0);

                operation.getReport(withEqual(tag1), this.<Result>withNotNull());
                result = opReport1;

                operation.getReport(withEqual(tag2), this.<Result>withNotNull());
                result = opReport2;
            }
        };

        CCCmdReport ccCmdReport = report.get(tags);

        Assert.assertNotNull(ccCmdReport);
        Assert.assertEquals("specName", ccCmdReport.getCmdSpecName());
        Assert.assertNotNull(ccCmdReport.getTagReports());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport());
        Assert.assertEquals(2, ccCmdReport.getTagReports().getTagReport().size());
        Assert.assertEquals("urn:epc:tag:sgtin-96:3.0614141.812345.6789", ccCmdReport.getTagReports().getTagReport().get(0).getId());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport().get(0).getStats());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat());
        Assert.assertEquals(4, ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().size());
        Assert.assertEquals("TagTimestamps", ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().get(0).getProfile());
        Assert.assertTrue(ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().get(0) instanceof CCTagTimestampStat);
        Assert.assertEquals("TagCount", ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().get(1).getProfile());
        Assert.assertTrue(ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().get(1) instanceof CCTagCountStat);
        Assert.assertEquals("ReaderNames", ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().get(2).getProfile());
        Assert.assertTrue(ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().get(2) instanceof CCTagStat);
        Assert.assertEquals("ReaderSightingSignals", ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().get(3).getProfile());
        Assert.assertTrue(ccCmdReport.getTagReports().getTagReport().get(0).getStats().getStat().get(3) instanceof CCTagStat);
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport().get(0).getOpReports());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport().get(0).getOpReports().getOpReport());
        Assert.assertEquals(1, ccCmdReport.getTagReports().getTagReport().get(0).getOpReports().getOpReport().size());
        Assert.assertSame(opReport1, ccCmdReport.getTagReports().getTagReport().get(0).getOpReports().getOpReport().get(0));
        Assert.assertEquals("urn:epc:tag:sgtin-96:3.0614141.812345.6790", ccCmdReport.getTagReports().getTagReport().get(1).getId());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport().get(1).getStats());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat());
        Assert.assertEquals(4, ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().size());
        Assert.assertEquals("TagTimestamps", ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().get(0).getProfile());
        Assert.assertTrue(ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().get(0) instanceof CCTagTimestampStat);
        Assert.assertEquals("TagCount", ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().get(1).getProfile());
        Assert.assertTrue(ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().get(1) instanceof CCTagCountStat);
        Assert.assertEquals("ReaderNames", ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().get(2).getProfile());
        Assert.assertTrue(ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().get(2) instanceof CCTagStat);
        Assert.assertEquals("ReaderSightingSignals", ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().get(3).getProfile());
        Assert.assertTrue(ccCmdReport.getTagReports().getTagReport().get(1).getStats().getStat().get(3) instanceof CCTagStat);
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport().get(1).getOpReports());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport().get(1).getOpReports().getOpReport());
        Assert.assertEquals(1, ccCmdReport.getTagReports().getTagReport().get(1).getOpReports().getOpReport().size());
        Assert.assertSame(opReport2, ccCmdReport.getTagReports().getTagReport().get(1).getOpReports().getOpReport().get(0));
    }

    @Test
    public void getNullTag(@Mocked final CCOperation operation) throws ValidationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        spec.getOpSpecs().getOpSpec().add(opSpec);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("TagTimestamps");
        spec.getStatProfileNames().getStatProfileName().add("TagCount");
        spec.getStatProfileNames().getStatProfileName().add("ReaderNames");
        spec.getStatProfileNames().getStatProfileName().add("ReaderSightingSignals");

        Report report = new Report(spec, new Parameters());

        List<Tag> tags = new ArrayList<Tag>();
        final Tag tag1 = null;
        tags.add(tag1);

        CCCmdReport ccCmdReport = report.get(tags);

        Assert.assertNotNull(ccCmdReport);
        Assert.assertEquals("specName", ccCmdReport.getCmdSpecName());
        Assert.assertNotNull(ccCmdReport.getTagReports());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport());
        Assert.assertEquals(0, ccCmdReport.getTagReports().getTagReport().size());
    }

    @Test
    public void getNoMatch(@Mocked final CCOperation operation) throws ValidationException, TdtTranslationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        spec.getOpSpecs().getOpSpec().add(opSpec);

        spec.setFilterSpec(new CCFilterSpec());
        spec.getFilterSpec().setExtension(new CCFilterSpecExtension());
        spec.getFilterSpec().setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("EXCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        spec.getFilterSpec().getFilterList().getFilter().add(member1);

        Report report = new Report(spec, new Parameters());

        List<Tag> tags = new ArrayList<Tag>();
        final Tag tag1 = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tag1.setResult(new HashMap<Integer, Result>());
        tag1.getResult().put(Integer.valueOf(1), new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tags.add(tag1);

        new NonStrictExpectations() {
            {
                operation.getId();
                result = Integer.valueOf(1);
            }
        };

        CCCmdReport ccCmdReport = report.get(tags);

        Assert.assertNotNull(ccCmdReport);
        Assert.assertEquals("specName", ccCmdReport.getCmdSpecName());
        Assert.assertNotNull(ccCmdReport.getTagReports());
        Assert.assertNotNull(ccCmdReport.getTagReports().getTagReport());
        Assert.assertEquals(0, ccCmdReport.getTagReports().getTagReport().size());
    }

    @Test
    public void dispose(@Mocked final CCOperation operation, @Mocked final Filter filter) throws ValidationException, TdtTranslationException {
        CCCmdSpec spec = new CCCmdSpec();
        spec.setName("specName");

        spec.setOpSpecs(new OpSpecs());
        CCOpSpec opSpec = new CCOpSpec();
        opSpec.setOpName("readEpc");
        spec.getOpSpecs().getOpSpec().add(opSpec);

        spec.setFilterSpec(new CCFilterSpec());
        spec.getFilterSpec().setExtension(new CCFilterSpecExtension());
        spec.getFilterSpec().setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("EXCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        spec.getFilterSpec().getFilterList().getFilter().add(member1);

        Report report = new Report(spec, new Parameters());
        report.dispose();

        new Verifications() {
            {
                operation.dispose();
                times = 1;

                filter.dispose();
                times = 1;
            }
        };
    }
}
