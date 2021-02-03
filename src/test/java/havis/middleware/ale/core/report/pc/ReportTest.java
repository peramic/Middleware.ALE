package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Event;
import havis.middleware.ale.base.operation.port.Operation.Type;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.base.operation.port.result.ReadResult;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.port.result.Result.State;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.service.ECFieldSpec;
import havis.middleware.ale.service.ECFilterListMember;
import havis.middleware.ale.service.ECFilterListMember.PatList;
import havis.middleware.ale.service.ECTime;
import havis.middleware.ale.service.pc.PCFilterSpec;
import havis.middleware.ale.service.pc.PCFilterSpec.FilterList;
import havis.middleware.ale.service.pc.PCFilterSpecExtension;
import havis.middleware.ale.service.pc.PCOpSpec;
import havis.middleware.ale.service.pc.PCOpSpecs;
import havis.middleware.ale.service.pc.PCPortSpec;
import havis.middleware.ale.service.pc.PCReport;
import havis.middleware.ale.service.pc.PCReportSpec;
import havis.middleware.ale.service.pc.PCReportSpec.StatProfileNames;
import havis.middleware.ale.service.pc.PCReportSpec.TriggerList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void reportWithFilter() throws ValidationException, ImplementationException, NoSuchNameException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.setOpSpecs(null);
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
        spec.setFilterSpec(filterSpec);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("ReaderNames");
        spec.getStatProfileNames().getStatProfileName().add("ReaderSightingSignals");

        boolean flat = false;
        Report report = new Report(spec, flat, null);

        Assert.assertNotNull(report.getFilter());
        Collection<havis.middleware.ale.base.operation.tag.Operation> filterOperations = report.getFilter().getOperations();
        Assert.assertNotNull(filterOperations);
        Assert.assertEquals(2, filterOperations.size());
        Iterator<havis.middleware.ale.base.operation.tag.Operation> iterator = filterOperations.iterator();
        havis.middleware.ale.base.operation.tag.Operation op1 = iterator.next();
        Assert.assertEquals(0, op1.getId());
        Assert.assertEquals(OperationType.READ, op1.getType());
        Assert.assertEquals("epc", op1.getField().getName());
        havis.middleware.ale.base.operation.tag.Operation op2 = iterator.next();
        Assert.assertEquals(0, op2.getId());
        Assert.assertEquals(OperationType.READ, op2.getType());
        Assert.assertEquals("killPwd", op2.getField().getName());

        Assert.assertEquals("spec", report.getName());
        Assert.assertSame(spec, report.getSpec());
        List<Operation> operations = report.getOperations();
        Assert.assertNotNull(operations);
        Assert.assertEquals(0, operations.size());
    }

    @Test
    public void reportWithoutStats() throws ValidationException, ImplementationException, NoSuchNameException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.setOpSpecs(new PCOpSpecs());
        PCFilterSpec filterSpec = new PCFilterSpec();
        filterSpec.setExtension(new PCFilterSpecExtension());
        filterSpec.setFilterList(new FilterList());
        spec.setFilterSpec(filterSpec);

        spec.setStatProfileNames(new StatProfileNames());

        boolean flat = false;
        Report report = new Report(spec, flat, null);

        Assert.assertNull(report.getStats());
    }

    @Test(expected = ValidationException.class)
    public void reportWithoutTriggersFlat() throws ValidationException, ImplementationException, NoSuchNameException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.setOpSpecs(null);

        boolean flat = true;
        new Report(spec, flat, null);
    }

    @Test(expected = ValidationException.class)
    public void reportWithInvalidFilter() throws ValidationException, ImplementationException, NoSuchNameException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.setOpSpecs(new PCOpSpecs());
        PCFilterSpec filterSpec = new PCFilterSpec();
        filterSpec.setExtension(new PCFilterSpecExtension());
        filterSpec.setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList()); // empty list
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        filterSpec.getFilterList().getFilter().add(member1);
        spec.setFilterSpec(filterSpec);

        boolean flat = false;
        new Report(spec, flat, null);
    }

    @Test
    public void reportWithTriggers(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException,
            ImplementationException, NoSuchNameException {
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

        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.getTriggerList().getTrigger().add("test");
        spec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        spec.getOpSpecs().getOpSpec().add(spec1);
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
        spec.getOpSpecs().getOpSpec().add(spec2);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("EventTimestamps");
        spec.getStatProfileNames().getStatProfileName().add("EventCount");

        boolean flat = true;
        final AtomicInteger count = new AtomicInteger(0);
        Callback callback = new Callback() {
            @Override
            public void invoke(Report report, Trigger trigger) {
                Assert.assertNotNull(report);
                Assert.assertNotNull(trigger);
                count.incrementAndGet();
            }
        };

        final Report report = new Report(spec, flat, callback);

        Assert.assertNull(report.getFilter());
        Assert.assertEquals("spec", report.getName());
        Assert.assertSame(spec, report.getSpec());
        List<Operation> operations = report.getOperations();
        Assert.assertNotNull(operations);
        Assert.assertEquals(2, operations.size());
        Assert.assertEquals("spec1", operations.get(0).getPortOperation().getName());
        Assert.assertEquals("spec2", operations.get(1).getPortOperation().getName());

        new Verifications() {
            {
                Trigger.Callback callback = null;
                Trigger.getInstance(report.guid, withEqual("test"), callback = withCapture());
                times = 1;

                Assert.assertNotNull(callback);
                callback.invoke(trigger);
            }
        };

        Assert.assertEquals(1, count.get());
    }

    @Test(expected = ValidationException.class)
    public void reportWithTriggerAndFilter() throws ValidationException, ImplementationException, NoSuchNameException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.getTriggerList().getTrigger().add("test");
        spec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        spec.getOpSpecs().getOpSpec().add(spec1);
        PCFilterSpec filterSpec = new PCFilterSpec();
        filterSpec.setExtension(new PCFilterSpecExtension());
        filterSpec.setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        filterSpec.getFilterList().getFilter().add(member1);
        spec.setFilterSpec(filterSpec);

        boolean flat = false;
        new Report(spec, flat, null);
    }

    @Test(expected = ValidationException.class)
    public void reportWithTriggersAndInvalidProfile1(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader)
            throws ValidationException, ImplementationException, NoSuchNameException {
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

        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.getTriggerList().getTrigger().add("test");
        spec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        spec.getOpSpecs().getOpSpec().add(spec1);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("ReaderNames");

        boolean flat = true;
        new Report(spec, flat, null);
    }

    @Test(expected = ValidationException.class)
    public void reportWithTriggersAndInvalidProfile2(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader)
            throws ValidationException, ImplementationException, NoSuchNameException {
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

        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.getTriggerList().getTrigger().add("test");
        spec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        spec.getOpSpecs().getOpSpec().add(spec1);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("ReaderSightingSignals");

        boolean flat = true;
        new Report(spec, flat, null);
    }

    @Test(expected = ValidationException.class)
    public void reportWithTriggersAndUnkownProfile(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader)
            throws ValidationException, ImplementationException, NoSuchNameException {
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

        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.getTriggerList().getTrigger().add("test");
        spec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        spec.getOpSpecs().getOpSpec().add(spec1);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("Whatever");

        boolean flat = true;
        new Report(spec, flat, null);
    }

    @Test
    public void disposeFilter(@Mocked final Filter filter) throws ValidationException, ImplementationException, NoSuchNameException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.setOpSpecs(new PCOpSpecs());
        final PCFilterSpec filterSpec = new PCFilterSpec();
        spec.setFilterSpec(filterSpec);

        new NonStrictExpectations() {
            {
                new Filter(withEqual(filterSpec));
                result = filter;
            }
        };

        boolean flat = false;
        Report report = new Report(spec, flat, null);

        report.dispose();

        new Verifications() {
            {
                filter.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void isCompletedWithTrigger(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final Operation operation)
            throws ValidationException, ImplementationException, NoSuchNameException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.getTriggerList().getTrigger().add("test");
        spec.setOpSpecs(new PCOpSpecs());
        final List<PCOpSpec> opSpec = spec.getOpSpecs().getOpSpec();

        final havis.middleware.ale.base.operation.port.Operation portOperation = new havis.middleware.ale.base.operation.port.Operation("name", Type.READ,
                Byte.valueOf((byte) 0x00), Long.valueOf(1000), new Pin(1, havis.middleware.ale.base.operation.port.Pin.Type.INPUT));

        new NonStrictExpectations() {
            {
                Trigger.getInstance(this.<String> withNotNull(), withEqual("test"), this.<Trigger.Callback> withNotNull());
                result = trigger;

                trigger.getUri();
                result = "test";

                LR.getInstance();
                result = lr;

                lr.lock("somereader");
                result = reader;

                Operation.get(opSpec);
                result = new ArrayList<Operation>(Arrays.asList(operation));

                operation.getPortOperation();
                result = portOperation;
            }
        };

        boolean flat = true;
        Report report = new Report(spec, flat, null);

        Event event = new Event("blub");
        Assert.assertTrue(report.isCompleted(event)); // no match

        event = new Event(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789"));
        Assert.assertTrue(report.isCompleted(event)); // no match

        event = new Event("testbla");
        event.setResult(null);
        Assert.assertFalse(report.isCompleted(event)); // match, but no result

        event = new Event("testbla");
        event.setResult(new HashMap<Integer, Result>());
        event.getResult().put(Integer.valueOf(1), new Result(State.SUCCESS));
        portOperation.setId(2);
        Assert.assertFalse(report.isCompleted(event)); // match, but no result
                                                       // with ID

        event = new Event("testbla");
        event.setResult(new HashMap<Integer, Result>());
        event.getResult().put(Integer.valueOf(1), new Result(State.SUCCESS));
        event.getResult().put(Integer.valueOf(2), new Result(State.SUCCESS));
        portOperation.setId(2);
        Assert.assertTrue(report.isCompleted(event)); // match and valid result
    }

    @Test
    public void getWithFilter() throws ValidationException, ImplementationException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.setOpSpecs(new PCOpSpecs());
        PCFilterSpec filterSpec = new PCFilterSpec();
        filterSpec.setExtension(new PCFilterSpecExtension());
        filterSpec.setFilterList(new FilterList());
        ECFilterListMember member1 = new ECFilterListMember();
        member1.setPatList(new PatList());
        member1.getPatList().getPat().add("urn:epc:pat:sgtin-96:3.0614141.812345.6789");
        member1.setIncludeExclude("INCLUDE");
        member1.setFieldspec(new ECFieldSpec("epc"));
        filterSpec.getFilterList().getFilter().add(member1);
        spec.setFilterSpec(filterSpec);

        spec.setStatProfileNames(new StatProfileNames());
        spec.getStatProfileNames().getStatProfileName().add("ReaderNames");
        spec.getStatProfileNames().getStatProfileName().add("ReaderSightingSignals");

        boolean flat = false;
        Report report = new Report(spec, flat, null);

        Set<Event> events = new HashSet<>();
        Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tag.setResult(new HashMap<Integer, havis.middleware.ale.base.operation.tag.result.Result>());
        tag.getResult().put(Integer.valueOf(1),
                new havis.middleware.ale.base.operation.tag.result.ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        events.add(new Event(tag)); // match

        PCReport result = report.get(events);
        Assert.assertNotNull(result);
        Assert.assertEquals("spec", result.getReportName());
        Assert.assertEquals(1, result.getEventReports().getEventReport().size());
        Assert.assertEquals("urn:epc:tag:sgtin-96:3.0614141.812345.6789", result.getEventReports().getEventReport().get(0).getId());
        Assert.assertEquals(0, result.getEventReports().getEventReport().get(0).getOpReports().getOpReport().size());
        Assert.assertEquals(2, result.getEventReports().getEventReport().get(0).getStats().getStat().size());
        Assert.assertEquals("ReaderNames", result.getEventReports().getEventReport().get(0).getStats().getStat().get(0).getProfile());
        Assert.assertEquals("ReaderSightingSignals", result.getEventReports().getEventReport().get(0).getStats().getStat().get(1).getProfile());

        events.clear();
        tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6790");
        tag.setResult(new HashMap<Integer, havis.middleware.ale.base.operation.tag.result.Result>());
        tag.getResult().put(Integer.valueOf(1),
                new havis.middleware.ale.base.operation.tag.result.ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x86 }));
        events.add(new Event(tag)); // no match

        result = report.get(events);
        Assert.assertNotNull(result);
        Assert.assertEquals("spec", result.getReportName());
        Assert.assertEquals(0, result.getEventReports().getEventReport().size());
    }

    @Test
    public void getWithTriggers(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException,
            ImplementationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                Trigger.getInstance(this.<String> withNotNull(), withEqual("test"), this.<Trigger.Callback> withNotNull());
                result = trigger;

                trigger.getUri();
                result = "test";

                LR.getInstance();
                result = lr;

                lr.lock("somereader");
                result = reader;
            }
        };

        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.getTriggerList().getTrigger().add("test");
        spec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        spec.getOpSpecs().getOpSpec().add(spec1);

        boolean flat = false;
        Report report = new Report(spec, flat, null);

        report.getOperations().get(0).getPortOperation().setId(1);

        Set<Event> events = new HashSet<>();
        Event event = new Event("testbla");
        event.setResult(new HashMap<Integer, Result>());
        event.getResult().put(Integer.valueOf(1), new ReadResult(State.SUCCESS, (byte) 0x01));
        events.add(event); // match

        PCReport result = report.get(events);
        Assert.assertNotNull(result);
        Assert.assertEquals("spec", result.getReportName());
        Assert.assertEquals(1, result.getEventReports().getEventReport().size());
        Assert.assertEquals("testbla", result.getEventReports().getEventReport().get(0).getId());
        Assert.assertEquals(1, result.getEventReports().getEventReport().get(0).getOpReports().getOpReport().size());
        Assert.assertEquals("spec1", result.getEventReports().getEventReport().get(0).getOpReports().getOpReport().get(0).getOpName());
        Assert.assertEquals(Boolean.TRUE, result.getEventReports().getEventReport().get(0).getOpReports().getOpReport().get(0).isState());
        Assert.assertEquals("SUCCESS", result.getEventReports().getEventReport().get(0).getOpReports().getOpReport().get(0).getOpStatus());
        Assert.assertNull(result.getEventReports().getEventReport().get(0).getStats());
    }

    @Test
    public void disposeTriggerAndOperation(@Mocked final Trigger trigger, @Mocked final LR lr, @Mocked final LogicalReader reader,
            @Mocked final Operation operation) throws ValidationException, ImplementationException, NoSuchNameException {
        PCReportSpec spec = new PCReportSpec();
        spec.setName("spec");
        spec.setTriggerList(new TriggerList());
        spec.getTriggerList().getTrigger().add("test");
        spec.setOpSpecs(new PCOpSpecs());
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");
        final List<PCOpSpec> opSpec = spec.getOpSpecs().getOpSpec();
        opSpec.add(spec1);

        new NonStrictExpectations() {
            {
                Trigger.getInstance(this.<String> withNotNull(), withEqual("test"), this.<Trigger.Callback> withNotNull());
                result = trigger;

                LR.getInstance();
                result = lr;

                lr.lock("somereader");
                result = reader;

                Operation.get(opSpec);
                result = new ArrayList<Operation>(Arrays.asList(operation));
            }
        };

        boolean flat = false;
        Report report = new Report(spec, flat, null);

        report.dispose();

        new Verifications() {
            {
                trigger.dispose();
                times = 1;

                operation.dispose();
                times = 1;
            }
        };
    }
}
