package havis.middleware.ale.core.cycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.Event;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.port.result.Result.State;
import havis.middleware.ale.base.operation.tag.Sighting;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.report.pc.Operation;
import havis.middleware.ale.core.report.pc.Report;
import havis.middleware.ale.core.report.pc.Reports;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.core.trigger.Trigger.Callback;
import havis.middleware.ale.service.ECTime;
import havis.middleware.ale.service.pc.PCBoundarySpec;
import havis.middleware.ale.service.pc.PCBoundarySpec.StartTriggerList;
import havis.middleware.ale.service.pc.PCBoundarySpec.StopTriggerList;
import havis.middleware.ale.service.pc.PCSpec;
import havis.middleware.ale.service.pc.PCSpec.LogicalReaders;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

public class PortCycleTest {

    @BeforeClass
    public static void init() {
    	ConfigResetter.reset();
        ConfigResetter.disablePersistence();
    }

    @Test
    public void portCycleWithoutReaders(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final Trigger trigger1,
            @Mocked final Trigger trigger2) throws ValidationException, ImplementationException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setStartTriggerList(new StartTriggerList());
        spec.getBoundarySpec().getStartTriggerList().getStartTrigger().add("trigger1");
        spec.getBoundarySpec().setStopTriggerList(new StopTriggerList());
        spec.getBoundarySpec().getStopTriggerList().getStopTrigger().add("trigger2");
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.FALSE);
        ECTime time = new ECTime();
        time.setUnit("MS");
        time.setValue(1001);
        spec.getBoundarySpec().setRepeatPeriod(time);
        time = new ECTime();
        time.setUnit("MS");
        time.setValue(1002);
        spec.getBoundarySpec().setDuration(time);
        time = new ECTime();
        time.setUnit("MS");
        time.setValue(1003);
        spec.getBoundarySpec().setNoNewEventsInterval(time);

        new NonStrictExpectations() {
            {
                new Reports(withEqual("name"), withEqual(spec), true, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                result = reports;

                reports.getTagOperation();
                result = tagOperation;

                Trigger.getInstance(this.<String> withNotNull(), "trigger1", this.<Callback> withNotNull());
                result = trigger1;

                Trigger.getInstance(this.<String> withNotNull(), "trigger2", this.<Callback> withNotNull());
                result = trigger2;
            }
        };
        final PortCycle cycle = new PortCycle("name", spec);

        Assert.assertEquals(new ArrayList<String>(), cycle.getLogicalReaders());
        Assert.assertEquals(Termination.NO_NEW_EVENTS, cycle.getInterval());
        Assert.assertSame(spec, cycle.getSpec());
        Assert.assertEquals(new ArrayList<String>(), cycle.getSubscribers());
        Assert.assertSame(tagOperation, cycle.getTagOperation());
        Assert.assertFalse(cycle.isActive());
        Assert.assertFalse(cycle.isBusy());

        Assert.assertEquals(Arrays.asList(trigger1, trigger2), cycle.trigger);
        Assert.assertFalse(cycle.whenDataAvailable);
        Assert.assertEquals(1001, cycle.repeatPeriod);
        Assert.assertEquals(1002, cycle.duration);
        Assert.assertEquals(1003, cycle.interval);
        Assert.assertFalse(cycle.immediate);

        Assert.assertEquals(0, cycle.logicalReaders.size());

        new Verifications() {
            {
                havis.middleware.ale.core.report.pc.Callback callback;
                new Reports(withEqual("name"), withEqual(spec), true, callback = withCapture());
                times = 1;

                Assert.assertNotNull(callback);
                callback.invoke(null, null); // inactive cycle

                Callback callbackTrigger1;
                Trigger.getInstance(cycle.guid, "trigger1", callbackTrigger1 = withCapture());
                times = 1;

                Assert.assertNotNull(callbackTrigger1);
                callbackTrigger1.invoke(trigger1); // not requested

                Callback callbackTrigger2;
                Trigger.getInstance(cycle.guid, "trigger2", callbackTrigger2 = withCapture());
                times = 1;

                Assert.assertNotNull(callbackTrigger2);
                callbackTrigger2.invoke(trigger2); // not active
            }
        };
    }

    @Test
    public void portCycleWithReaders(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr,
            @Mocked final LogicalReader reader) throws ValidationException, ImplementationException, NoSuchNameException {
        final PCSpec spec = new PCSpec();
        spec.setLogicalReaders(new LogicalReaders());
        spec.getLogicalReaders().getLogicalReader().add("reader1");
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.TRUE);

        new NonStrictExpectations() {
            {
                new Reports(withEqual("name"), withEqual(spec), false, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                result = reports;

                reports.getTagOperation();
                result = tagOperation;

                LR.getInstance();
                result = lr;

                lr.lock(withEqual("reader1"));
                result = reader;
            }
        };

        final PortCycle cycle = new PortCycle("name", spec);

        Assert.assertEquals(Arrays.asList("reader1"), cycle.getLogicalReaders());
        Assert.assertEquals(Termination.NO_NEW_EVENTS, cycle.getInterval());
        Assert.assertSame(spec, cycle.getSpec());
        Assert.assertEquals(new ArrayList<String>(), cycle.getSubscribers());
        Assert.assertSame(tagOperation, cycle.getTagOperation());
        Assert.assertFalse(cycle.isActive());
        Assert.assertFalse(cycle.isBusy());

        Assert.assertEquals(Arrays.asList(), cycle.trigger);
        Assert.assertTrue(cycle.whenDataAvailable);
        Assert.assertEquals(-1, cycle.repeatPeriod);
        Assert.assertEquals(-1, cycle.duration);
        Assert.assertEquals(-1, cycle.interval);
        Assert.assertTrue(cycle.immediate);

        Assert.assertEquals(1, cycle.logicalReaders.size());
        Assert.assertSame(reader, cycle.logicalReaders.get(0));

        new Verifications() {
            {
                havis.middleware.ale.core.report.pc.Callback callback;
                new Reports(withEqual("name"), withEqual(spec), false, callback = withCapture());
                times = 1;

                Assert.assertNotNull(callback);
                callback.invoke(null, null); // inactive cycle

                lr.lock(withEqual("reader1"));
                times = 1;

                reader.define(withEqual(tagOperation), this.<Caller<Tag>> withNotNull(), withEqual(cycle.guid));
                times = 1;
            }
        };
    }

    @Test
    public void portCycleWithReadersDefineFails(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr,
            @Mocked final LogicalReader reader) throws ValidationException, ImplementationException, NoSuchNameException {
        final PCSpec spec = new PCSpec();
        spec.setLogicalReaders(new LogicalReaders());
        spec.getLogicalReaders().getLogicalReader().add("reader1");
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.TRUE);

        new NonStrictExpectations() {
            {
                new Reports(withEqual("name"), withEqual(spec), false, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                result = reports;

                reports.getTagOperation();
                result = tagOperation;

                LR.getInstance();
                result = lr;

                lr.lock(withEqual("reader1"));
                result = reader;

                reader.define(withEqual(tagOperation), this.<Caller<Tag>> withNotNull(), this.<String>withNotNull());
                result = new ImplementationException();
            }
        };

        try {
            new PortCycle("name", spec);
            Assert.fail("Expected ImplementationException");
        } catch (ImplementationException e) {
            // ignore
        }

        new Verifications() {
            {
                new Reports(withEqual("name"), withEqual(spec), false, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                times = 1;

                lr.lock(withEqual("reader1"));
                times = 1;

                reader.define(withEqual(tagOperation), this.<Caller<Tag>> withNotNull(), this.<String>withNotNull());
                times = 1;

                reader.unlock();
                times = 1;

                reports.dispose();
                times = 1;
            }
        };
    }

    @Test
    public void portCycleInvalidRepeatPeriod(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
            ImplementationException, NoSuchNameException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.TRUE);
        spec.getBoundarySpec().setRepeatPeriod(new ECTime());

        try {
            new PortCycle(null, spec);
            Assert.fail("Expected ImplementationException");
        } catch (ValidationException e) {
            // ignore
        }
    }

    @Test
    public void portCycleInvalidDuration(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
            ImplementationException, NoSuchNameException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.TRUE);
        spec.getBoundarySpec().setDuration(new ECTime());

        try {
            new PortCycle(null, spec);
            Assert.fail("Expected ImplementationException");
        } catch (ValidationException e) {
            // ignore
        }
    }

    @Test
    public void portCycleInvalidInterval(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
            ImplementationException, NoSuchNameException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.TRUE);
        spec.getBoundarySpec().setNoNewEventsInterval(new ECTime());

        try {
            new PortCycle(null, spec);
            Assert.fail("Expected ImplementationException");
        } catch (ValidationException e) {
            // ignore
        }
    }

    @Test
    public void portCycleNoStopCondition(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
            ImplementationException, NoSuchNameException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.FALSE);

        try {
            new PortCycle(null, spec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }
    }

    @Test
    public void portCycleNoBoundarySpec(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
            ImplementationException, NoSuchNameException {
        final PCSpec spec = new PCSpec();

        try {
            new PortCycle(null, spec);
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }
    }

    @Test
    public void execute(@Mocked final Operation operation1, @Mocked final havis.middleware.ale.base.operation.port.Operation portOperation1,
            @Mocked final Operation operation2, @Mocked final havis.middleware.ale.base.operation.port.Operation portOperation2,
            @Mocked final Operation operation3, @Mocked final havis.middleware.ale.base.operation.port.Operation portOperation3,
            @Mocked final Operation operation4, @Mocked final havis.middleware.ale.base.operation.port.Operation portOperation4,
            @Mocked final LogicalReader reader1, @Mocked final LogicalReader reader2, @Mocked final Caller<Port> callback) throws ImplementationException,
            ValidationException {
        new NonStrictExpectations() {
            {
                operation1.getLogicalReader();
                result = reader1;

                operation2.getLogicalReader();
                result = reader1;

                operation3.getLogicalReader();
                result = reader2;

                operation4.getLogicalReader();
                result = reader1;

                operation1.getPortOperation();
                result = portOperation1;

                operation2.getPortOperation();
                result = portOperation2;

                operation3.getPortOperation();
                result = portOperation3;

                operation4.getPortOperation();
                result = portOperation4;
            }
        };

        PortCycle.execute(Arrays.asList(operation1, operation2, operation3, operation4), callback);

        new VerificationsInOrder() {
            {
                reader1.execute(withEqual(new PortOperation(Arrays.asList(portOperation1, portOperation2))), withEqual(callback));
                times = 1;

                reader2.execute(withEqual(new PortOperation(Arrays.asList(portOperation3))), withEqual(callback));
                times = 1;

                reader1.execute(withEqual(new PortOperation(Arrays.asList(portOperation4))), withEqual(callback));
                times = 1;
            }
        };
    }

    @Test
    public void triggerReport(@Mocked final Reports reports, @Mocked final Report reportWithOperations, @Mocked final Report reportWithoutOperations,
            @Mocked final Operation operation, @Mocked final LogicalReader reader,
            @Mocked final havis.middleware.ale.base.operation.port.Operation portOperation, @Mocked final Trigger reportTrigger,
            @Mocked final TagOperation tagOperation, @Mocked final SubscriberController subscriber, @Mocked final ReaderController controller)
            throws ValidationException, ImplementationException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.TRUE);

        new NonStrictExpectations() {
            {
                new Reports(withEqual("name"), withEqual(spec), true, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                result = reports;

                reports.getTagOperation();
                result = tagOperation;

                reportTrigger.getUri();
                result = "reportTrigger";

                reportWithOperations.getOperations();
                result = new ArrayList<Operation>(Arrays.asList(operation));

                reportWithoutOperations.getOperations();
                result = new ArrayList<Operation>();

                operation.getLogicalReader();
                result = reader;

                operation.getPortOperation();
                result = portOperation;

                portOperation.getId();
                result = Integer.valueOf(1);
            }
        };

        PortCycle cycle = new PortCycle("name", spec);
        Assert.assertTrue(cycle.immediate);

        cycle.add(subscriber); // will set the cycle to requested and start it

        Assert.assertEquals(0, cycle.datas.getCount());
        Assert.assertNull(cycle.termination);

        final ByRef<havis.middleware.ale.core.report.pc.Callback> callbackHolder = new ByRef<havis.middleware.ale.core.report.pc.Callback>(null);

        new Verifications() {
            {
                havis.middleware.ale.core.report.pc.Callback callback;
                new Reports(withEqual("name"), withEqual(spec), true, callback = withCapture());
                times = 1;

                Assert.assertNotNull(callback);
                callbackHolder.setValue(callback);
            }
        };

        // triggers the report and executes the operation on the reader
        callbackHolder.getValue().invoke(reportWithOperations, reportTrigger);

        Assert.assertEquals(Termination.DATA_AVAILABLE, cycle.termination);

        Assert.assertEquals(1, cycle.datas.getCount());
        Event event = cycle.datas.get(new Event("reportTrigger"));
        Assert.assertNull(event);

        final ByRef<Caller<Port>> executeCallbackHolder = new ByRef<Caller<Port>>(null);

        new Verifications() {
            {
                Caller<Port> executeCallback;
                reader.execute(withEqual(new PortOperation(Arrays.asList(portOperation))), executeCallback = withCapture());
                times = 1;

                Assert.assertNotNull(executeCallback);
                executeCallbackHolder.setValue(executeCallback);
            }
        };

        Map<Integer, Result> result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new Result(State.SUCCESS));
        Port port = new Port(new Pin(1, Pin.Type.INPUT), "in", result);

        // signals the execution on the reader
        executeCallbackHolder.getValue().invoke(port, controller);

        Assert.assertEquals(1, cycle.datas.getCount());
        event = cycle.datas.get(new Event("reportTrigger"));
        Assert.assertNotNull(event);
        Assert.assertFalse(event.isCompleted());
        Assert.assertEquals(1, event.getCount());
        Assert.assertNotNull(event.getResult());
        Assert.assertEquals(1, event.getResult().size());
        Assert.assertEquals(new Result(State.SUCCESS), event.getResult().get(Integer.valueOf(1)));

        // triggers the report and executes the operation on the reader a second time
        callbackHolder.getValue().invoke(reportWithOperations, reportTrigger);

        Assert.assertEquals(1, cycle.datas.getCount());
        event = cycle.datas.get(new Event("reportTrigger"));
        Assert.assertNotNull(event);
        Assert.assertFalse(event.isCompleted());

        new Verifications() {
            {
                Caller<Port> executeCallback;
                reader.execute(withEqual(new PortOperation(Arrays.asList(portOperation))), executeCallback = withCapture());
                times = 1;

                Assert.assertNotNull(executeCallback);
                executeCallbackHolder.setValue(executeCallback);
            }
        };

        executeCallbackHolder.getValue().invoke(port, controller);

        Assert.assertEquals(1, cycle.datas.getCount());
        event = cycle.datas.get(new Event("reportTrigger"));
        Assert.assertNotNull(event);
        Assert.assertFalse(event.isCompleted());
        Assert.assertEquals(2, event.getCount());
        Assert.assertNotNull(event.getResult());
        Assert.assertEquals(1, event.getResult().size());
        Assert.assertEquals(new Result(State.SUCCESS), event.getResult().get(Integer.valueOf(1)));

        port.setCompleted(true); // will trigger the removal

        executeCallbackHolder.getValue().invoke(port, controller);

        Assert.assertEquals(0, cycle.datas.getCount());

        // triggers the report and executes the operation on the reader a third time (without operations)
        callbackHolder.getValue().invoke(reportWithoutOperations, reportTrigger);

        Assert.assertEquals(1, cycle.datas.getCount());
        event = cycle.datas.get(new Event("reportTrigger"));
        Assert.assertNotNull(event);
        Assert.assertTrue(event.isCompleted());
    }

    @Test
    public void startStopTrigger(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final Trigger trigger1,
            @Mocked final Trigger trigger2, @Mocked final SubscriberController subscriber) throws ValidationException, ImplementationException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setStartTriggerList(new StartTriggerList());
        spec.getBoundarySpec().getStartTriggerList().getStartTrigger().add("trigger1");
        spec.getBoundarySpec().setStopTriggerList(new StopTriggerList());
        spec.getBoundarySpec().getStopTriggerList().getStopTrigger().add("trigger2");
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.FALSE);

        new NonStrictExpectations() {
            {
                new Reports(withEqual("name"), withEqual(spec), true, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                result = reports;

                reports.getTagOperation();
                result = tagOperation;

                Trigger.getInstance(this.<String> withNotNull(), "trigger1", this.<Callback> withNotNull());
                result = trigger1;

                Trigger.getInstance(this.<String> withNotNull(), "trigger2", this.<Callback> withNotNull());
                result = trigger2;
            }
        };
        final PortCycle cycle = new PortCycle("name", spec);

        Assert.assertEquals(Arrays.asList(trigger1, trigger2), cycle.trigger);

        final ByRef<Callback> callbackHolderTrigger1 = new ByRef<Trigger.Callback>(null);
        final ByRef<Callback> callbackHolderTrigger2 = new ByRef<Trigger.Callback>(null);

        new Verifications() {
            {
                new Reports(withEqual("name"), withEqual(spec), true, this.<havis.middleware.ale.core.report.pc.Callback>withNotNull());
                times = 1;

                Callback callbackTrigger1;
                Trigger.getInstance(cycle.guid, "trigger1", callbackTrigger1 = withCapture());
                times = 1;

                Assert.assertNotNull(callbackTrigger1);
                callbackHolderTrigger1.setValue(callbackTrigger1);

                Callback callbackTrigger2;
                Trigger.getInstance(cycle.guid, "trigger2", callbackTrigger2 = withCapture());
                times = 1;

                Assert.assertNotNull(callbackTrigger2);
                callbackHolderTrigger2.setValue(callbackTrigger2);
            }
        };

        cycle.add(subscriber); // will set the cycle to requested

        Assert.assertFalse(cycle.isActive());

        callbackHolderTrigger1.getValue().invoke(trigger1);

        Assert.assertTrue(cycle.isActive());

        callbackHolderTrigger2.getValue().invoke(trigger2);

        Assert.assertFalse(cycle.isActive());
    }

    @Test
    public void interrupt(@Mocked final Reports reports, @Mocked final Report reportWithOperations, @Mocked final Report reportWithoutOperations,
            @Mocked final Operation operation, @Mocked final LogicalReader reader,
            @Mocked final havis.middleware.ale.base.operation.port.Operation portOperation, @Mocked final Trigger reportTrigger,
            @Mocked final TagOperation tagOperation, @Mocked final SubscriberController subscriber, @Mocked final ReaderController controller)
            throws ValidationException, ImplementationException, InterruptedException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.FALSE);
        ECTime time = new ECTime();
        time.setUnit("MS");
        time.setValue(10);
        spec.getBoundarySpec().setNoNewEventsInterval(time);

        new NonStrictExpectations() {
            {
                new Reports(withEqual("name"), withEqual(spec), true, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                result = reports;

                reports.getTagOperation();
                result = tagOperation;

                reportTrigger.getUri();
                result = "reportTrigger";

                reportWithOperations.getOperations();
                result = new ArrayList<Operation>(Arrays.asList(operation));

                reportWithoutOperations.getOperations();
                result = new ArrayList<Operation>();

                operation.getLogicalReader();
                result = reader;

                operation.getPortOperation();
                result = portOperation;

                portOperation.getId();
                result = Integer.valueOf(1);
            }
        };

        PortCycle cycle = new PortCycle("name", spec);
        Assert.assertTrue(cycle.immediate);

        cycle.add(subscriber); // will set the cycle to requested and start it

        Assert.assertEquals(0, cycle.datas.getCount());
        Assert.assertNull(cycle.termination);

        final ByRef<havis.middleware.ale.core.report.pc.Callback> callbackHolder = new ByRef<havis.middleware.ale.core.report.pc.Callback>(null);

        new Verifications() {
            {
                havis.middleware.ale.core.report.pc.Callback callback;
                new Reports(withEqual("name"), withEqual(spec), true, callback = withCapture());
                times = 1;

                Assert.assertNotNull(callback);
                callbackHolder.setValue(callback);
            }
        };

        // triggers the report and executes the operation on the reader
        callbackHolder.getValue().invoke(reportWithOperations, reportTrigger);

        new Verifications() {
            {
                reader.execute(withEqual(new PortOperation(Arrays.asList(portOperation))), this.<Caller<Port>>withNotNull());
                times = 1;
            }
        };

        Thread.sleep(50);

        Assert.assertEquals(Termination.NO_NEW_EVENTS, cycle.termination);

        Assert.assertEquals(1, cycle.datas.getCount());
        Event event = cycle.datas.get(new Event("reportTrigger"));
        Assert.assertNull(event);
    }

    @Test
    public void notify(@Mocked final Reports reports, @Mocked final Report reportWithOperations, @Mocked final Report reportWithoutOperations,
            @Mocked final Operation operation, @Mocked final LogicalReader reader,
            @Mocked final havis.middleware.ale.base.operation.port.Operation portOperation, @Mocked final Trigger reportTrigger,
            @Mocked final TagOperation tagOperation, @Mocked final SubscriberController subscriber, @Mocked final ReaderController controller)
            throws ValidationException, ImplementationException {
    	final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.TRUE);

        final Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tag.setTid(new byte[] { 0x01 });
        tag.setResult(new HashMap<Integer, havis.middleware.ale.base.operation.tag.result.Result>());
        tag.getResult().put(Integer.valueOf(1),
                new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tag.setSighting(new Sighting("somehost", (short) 1, 2));

        new NonStrictExpectations() {
            {
                new Reports(withEqual("name"), withEqual(spec), true, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                result = reports;

                reports.getTagOperation();
                result = tagOperation;

                reports.getPortOperation(withEqual(tag));
                result = new ArrayList<Operation>(Arrays.asList(operation));

                reportTrigger.getUri();
                result = "reportTrigger";

                operation.getLogicalReader();
                result = reader;

                operation.getPortOperation();
                result = portOperation;

                portOperation.getId();
                result = Integer.valueOf(1);
            }
        };

        PortCycle cycle = new PortCycle("name", spec);
        Assert.assertTrue(cycle.immediate);

        cycle.add(subscriber); // will set the cycle to requested and start it

        Assert.assertEquals(0, cycle.datas.getCount());
        Assert.assertNull(cycle.termination);
        Assert.assertNotNull(tag.getSightings());
        Assert.assertEquals(0, tag.getSightings().size());

        // see the tag
        cycle.notify("somereader", tag, controller);

        Assert.assertNotNull(tag.getSightings());
        Assert.assertEquals(1, tag.getSightings().size());
        Assert.assertNotNull(tag.getSightings().get("somereader"));
        Assert.assertEquals(1, tag.getSightings().get("somereader").size());
		long t = tag.getSightings().get("somereader").get(0).getTimestamp().getTime();
		Assert.assertEquals(new Sighting("somehost", (short) 1, 2, new Date(t)), tag.getSightings().get("somereader").get(0));
        Assert.assertEquals(Termination.DATA_AVAILABLE, cycle.termination);

        Assert.assertEquals(1, cycle.datas.getCount());
        Event event = cycle.datas.get(new Event(tag));
        Assert.assertNull(event);

        final ByRef<Caller<Port>> executeCallbackHolder = new ByRef<Caller<Port>>(null);

        new Verifications() {
            {
                Caller<Port> executeCallback;
                reader.execute(withEqual(new PortOperation(Arrays.asList(portOperation))), executeCallback = withCapture());
                times = 1;

                Assert.assertNotNull(executeCallback);
                executeCallbackHolder.setValue(executeCallback);
            }
        };

        Map<Integer, Result> result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new Result(State.SUCCESS));
        Port port = new Port(new Pin(1, Pin.Type.INPUT), "in", result);

        // signals the execution on the reader
        executeCallbackHolder.getValue().invoke(port, controller);

        Assert.assertEquals(1, cycle.datas.getCount());
        event = cycle.datas.get(new Event(tag));
        Assert.assertNotNull(event);
        Assert.assertFalse(event.isCompleted());
        Assert.assertEquals(1, event.getCount());
        Assert.assertNotNull(event.getResult());
        Assert.assertEquals(1, event.getResult().size());
        Assert.assertEquals(new Result(State.SUCCESS), event.getResult().get(Integer.valueOf(1)));

        // see the tag again
        cycle.notify("somereader", tag, controller);

        Assert.assertNotNull(tag.getSightings());
        Assert.assertEquals(1, tag.getSightings().size());
        Assert.assertNotNull(tag.getSightings().get("somereader"));
        Assert.assertEquals(2, tag.getSightings().get("somereader").size());
		t = tag.getSightings().get("somereader").get(0).getTimestamp().getTime();
		Assert.assertEquals(new Sighting("somehost", (short) 1, 2, new Date(t)), tag.getSightings().get("somereader").get(0));
		t = tag.getSightings().get("somereader").get(1).getTimestamp().getTime();
		Assert.assertEquals(new Sighting("somehost", (short) 1, 2, new Date(t)), tag.getSightings().get("somereader").get(1));
        Assert.assertEquals(Termination.DATA_AVAILABLE, cycle.termination);
    }
    
    @Test
    public void notifyWithLateExecute(@Mocked final Reports reports, @Mocked final Report reportWithOperations, @Mocked final Report reportWithoutOperations,
            @Mocked final Operation operation, @Mocked final LogicalReader reader,
            @Mocked final havis.middleware.ale.base.operation.port.Operation portOperation, @Mocked final Trigger reportTrigger,
            @Mocked final TagOperation tagOperation, @Mocked final SubscriberController subscriber, @Mocked final ReaderController controller)
            throws ValidationException, ImplementationException {
        final PCSpec spec = new PCSpec();
        spec.setBoundarySpec(new PCBoundarySpec());
        spec.getBoundarySpec().setWhenDataAvailable(Boolean.TRUE);

        final Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
        tag.setTid(new byte[] { 0x01 });
        tag.setResult(new HashMap<Integer, havis.middleware.ale.base.operation.tag.result.Result>());
        tag.getResult().put(Integer.valueOf(1),
                new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
        tag.setSighting(new Sighting("somehost", (short) 1, 2));

        new NonStrictExpectations() {
            {
                new Reports(withEqual("name"), withEqual(spec), true, this.<havis.middleware.ale.core.report.pc.Callback> withNotNull());
                result = reports;

                reports.getTagOperation();
                result = tagOperation;

                reports.getPortOperation(withEqual(tag));
                result = new ArrayList<Operation>(Arrays.asList(operation));

                reportTrigger.getUri();
                result = "reportTrigger";

                operation.getLogicalReader();
                result = reader;

                operation.getPortOperation();
                result = portOperation;

                portOperation.getId();
                result = Integer.valueOf(1);
            }
        };

        PortCycle cycle = new PortCycle("name", spec);
        Assert.assertTrue(cycle.immediate);

        cycle.add(subscriber); // will set the cycle to requested and start it

        Assert.assertEquals(0, cycle.datas.getCount());
        Assert.assertNull(cycle.termination);
        Assert.assertNotNull(tag.getSightings());
        Assert.assertEquals(0, tag.getSightings().size());

        // see the tag
        cycle.notify("somereader", tag, controller);

        Assert.assertNotNull(tag.getSightings());
        Assert.assertEquals(1, tag.getSightings().size());
        Assert.assertNotNull(tag.getSightings().get("somereader"));
        Assert.assertEquals(1, tag.getSightings().get("somereader").size());
		long t = tag.getSightings().get("somereader").get(0).getTimestamp().getTime();
		Assert.assertEquals(new Sighting("somehost", (short) 1, 2, new Date(t)), tag.getSightings().get("somereader").get(0));
        Assert.assertEquals(Termination.DATA_AVAILABLE, cycle.termination);

        Assert.assertEquals(1, cycle.datas.getCount());
        Event event = cycle.datas.get(new Event(tag));
        Assert.assertNull(event);

        // see the tag again, sighting will not count, the event is not yet handled
        cycle.notify("somereader", tag, controller);

        Assert.assertNotNull(tag.getSightings());
        Assert.assertEquals(1, tag.getSightings().size());
        Assert.assertNotNull(tag.getSightings().get("somereader"));
        Assert.assertEquals(1, tag.getSightings().get("somereader").size());
		t = tag.getSightings().get("somereader").get(0).getTimestamp().getTime();
		Assert.assertEquals(new Sighting("somehost", (short) 1, 2, new Date(t)), tag.getSightings().get("somereader").get(0));
        Assert.assertEquals(Termination.DATA_AVAILABLE, cycle.termination);
        
        Assert.assertEquals(1, cycle.datas.getCount());
        event = cycle.datas.get(new Event(tag));
        Assert.assertNull(event);
        
        final ByRef<Caller<Port>> executeCallbackHolder = new ByRef<Caller<Port>>(null);

        new Verifications() {
            {
                Caller<Port> executeCallback;
                reader.execute(withEqual(new PortOperation(Arrays.asList(portOperation))), executeCallback = withCapture());
                times = 1;

                Assert.assertNotNull(executeCallback);
                executeCallbackHolder.setValue(executeCallback);
            }
        };
        
        Map<Integer, Result> result = new HashMap<Integer, Result>();
        result.put(Integer.valueOf(1), new Result(State.SUCCESS));
        Port port = new Port(new Pin(1, Pin.Type.INPUT), "in", result);

        // signals the execution on the reader
        executeCallbackHolder.getValue().invoke(port, controller);

        Assert.assertEquals(1, cycle.datas.getCount());
        event = cycle.datas.get(new Event(tag));
        Assert.assertNotNull(event);
        Assert.assertFalse(event.isCompleted());
        Assert.assertEquals(1, event.getCount());
        Assert.assertNotNull(event.getResult());
        Assert.assertEquals(1, event.getResult().size());
        Assert.assertEquals(new Result(State.SUCCESS), event.getResult().get(Integer.valueOf(1)));
    }
}
