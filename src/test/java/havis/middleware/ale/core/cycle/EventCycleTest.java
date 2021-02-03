package havis.middleware.ale.core.cycle;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.Sighting;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.base.operation.tag.result.ReadResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.config.ReaderCycleType;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.report.Termination;
import havis.middleware.ale.core.report.ec.PrimaryKey;
import havis.middleware.ale.core.report.ec.Reports;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.core.trigger.Trigger;
import havis.middleware.ale.core.trigger.Trigger.Callback;
import havis.middleware.ale.service.ECTime;
import havis.middleware.ale.service.IReports;
import havis.middleware.ale.service.ec.ECBoundarySpec;
import havis.middleware.ale.service.ec.ECBoundarySpecExtension;
import havis.middleware.ale.service.ec.ECBoundarySpecExtension.StartTriggerList;
import havis.middleware.ale.service.ec.ECBoundarySpecExtension.StopTriggerList;
import havis.middleware.ale.service.ec.ECReportOutputSpec;
import havis.middleware.ale.service.ec.ECReportSetSpec;
import havis.middleware.ale.service.ec.ECReportSpec;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.ale.service.ec.ECSpec.LogicalReaders;
import havis.middleware.ale.service.ec.ECSpec.ReportSpecs;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

public class EventCycleTest {

	@BeforeClass
	public static void init() {
		ConfigResetter.reset();
		ConfigResetter.disablePersistence();
	}

	@Test
	public void eventCycle(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final Trigger trigger1,
			@Mocked final Trigger trigger2, @Mocked final LR lr, @Mocked final LogicalReader reader) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setStartTriggerList(new StartTriggerList());
		spec.getBoundarySpec().getExtension().getStartTriggerList().getStartTrigger().add("trigger1");
		spec.getBoundarySpec().getExtension().setStopTriggerList(new StopTriggerList());
		spec.getBoundarySpec().getExtension().getStopTriggerList().getStopTrigger().add("trigger2");
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.FALSE);
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
		spec.getBoundarySpec().setStableSetInterval(time);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withSameInstance(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				Trigger.getInstance(this.<String> withNotNull(), "trigger1", this.<Callback> withNotNull());
				result = trigger1;

				Trigger.getInstance(this.<String> withNotNull(), "trigger2", this.<Callback> withNotNull());
				result = trigger2;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;
			}
		};

		final EventCycle cycle = new EventCycle("name", spec);

		Assert.assertEquals(Arrays.asList("reader1"), cycle.getLogicalReaders());
		Assert.assertEquals(Termination.STABLE_SET, cycle.getInterval());
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

		Assert.assertEquals(1, cycle.logicalReaders.size());
		Assert.assertSame(reader, cycle.logicalReaders.get(0));

		new Verifications() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				times = 1;

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

				lr.lock(withEqual("reader1"));
				times = 1;

				reader.define(withEqual(tagOperation), this.<Caller<Tag>> withNotNull(), withEqual(cycle.guid));
				times = 1;
			}
		};
	}

	@Test
	public void eventCycleDeprecatedTrigger(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final Trigger trigger1,
			@Mocked final Trigger trigger2, @Mocked final LR lr, @Mocked final LogicalReader reader) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setStartTrigger("trigger1");
		spec.getBoundarySpec().setStopTrigger("trigger2");
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.FALSE);
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
		spec.getBoundarySpec().setStableSetInterval(time);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withSameInstance(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				Trigger.getInstance(this.<String> withNotNull(), "trigger1", this.<Callback> withNotNull());
				result = trigger1;

				Trigger.getInstance(this.<String> withNotNull(), "trigger2", this.<Callback> withNotNull());
				result = trigger2;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;
			}
		};

		final EventCycle cycle = new EventCycle("name", spec);

		Assert.assertEquals(Arrays.asList("reader1"), cycle.getLogicalReaders());
		Assert.assertEquals(Termination.STABLE_SET, cycle.getInterval());
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

		Assert.assertEquals(1, cycle.logicalReaders.size());
		Assert.assertSame(reader, cycle.logicalReaders.get(0));

		new Verifications() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				times = 1;

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

				lr.lock(withEqual("reader1"));
				times = 1;

				reader.define(withEqual(tagOperation), this.<Caller<Tag>> withNotNull(), withEqual(cycle.guid));
				times = 1;
			}
		};
	}

	@Test
	public void eventCycleReaderDefineFails(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr,
			@Mocked final LogicalReader reader) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withSameInstance(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;

				reader.define(withEqual(tagOperation), this.<Caller<Tag>> withNotNull(), this.<String> withNotNull());
				result = new ImplementationException();
			}
		};

		try {
			new EventCycle("name", spec);
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}

		new Verifications() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				times = 1;

				lr.lock(withEqual("reader1"));
				times = 1;

				reader.define(withEqual(tagOperation), this.<Caller<Tag>> withNotNull(), this.<String> withNotNull());
				times = 1;

				reader.unlock();
				times = 1;

				reports.dispose();
				times = 1;
			}
		};
	}

	@Test
	public void eventCycleNoReaders(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withSameInstance(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;
			}
		};

		try {
			new EventCycle("name", spec);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				times = 1;

				reports.dispose();
				times = 1;
			}
		};
	}

	@Test
	public void eventCycleNoSuchReader(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withSameInstance(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = new NoSuchNameException();
			}
		};

		try {
			new EventCycle("name", spec);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				times = 1;

				lr.lock(withEqual("reader1"));
				times = 1;

				reports.dispose();
				times = 1;
			}
		};
	}

	@Test
	public void eventCycleInvalidRepeatPeriod(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
			ImplementationException, NoSuchNameException {
		final ECSpec spec = new ECSpec();
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);
		spec.getBoundarySpec().setRepeatPeriod(new ECTime());

		try {
			new EventCycle(null, spec);
			Assert.fail("Expected ImplementationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void eventCycleInvalidDuration(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
			ImplementationException, NoSuchNameException {
		final ECSpec spec = new ECSpec();
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);
		spec.getBoundarySpec().setDuration(new ECTime());

		try {
			new EventCycle(null, spec);
			Assert.fail("Expected ImplementationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void eventCycleInvalidInterval(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
			ImplementationException, NoSuchNameException {
		final ECSpec spec = new ECSpec();
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);
		spec.getBoundarySpec().setStableSetInterval(new ECTime());

		try {
			new EventCycle(null, spec);
			Assert.fail("Expected ImplementationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void eventCycleNoStopCondition(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
			ImplementationException, NoSuchNameException {
		final ECSpec spec = new ECSpec();
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.FALSE);

		try {
			new EventCycle(null, spec);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void eventCycleNoBoundarySpec(@Mocked final Reports reports, @Mocked final TagOperation tagOperation) throws ValidationException,
			ImplementationException, NoSuchNameException {
		final ECSpec spec = new ECSpec();

		try {
			new EventCycle(null, spec);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void startStopTrigger(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final Trigger trigger1,
			@Mocked final Trigger trigger2, @Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final SubscriberController subscriber)
			throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setStartTriggerList(new StartTriggerList());
		spec.getBoundarySpec().getExtension().getStartTriggerList().getStartTrigger().add("trigger1");
		spec.getBoundarySpec().getExtension().setStopTriggerList(new StopTriggerList());
		spec.getBoundarySpec().getExtension().getStopTriggerList().getStopTrigger().add("trigger2");
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.FALSE);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				Trigger.getInstance(this.<String> withNotNull(), "trigger1", this.<Callback> withNotNull());
				result = trigger1;

				Trigger.getInstance(this.<String> withNotNull(), "trigger2", this.<Callback> withNotNull());
				result = trigger2;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;
			}
		};
		final EventCycle cycle = new EventCycle("name", spec);

		Assert.assertEquals(Arrays.asList(trigger1, trigger2), cycle.trigger);

		final ByRef<Callback> callbackHolderTrigger1 = new ByRef<Trigger.Callback>(null);
		final ByRef<Callback> callbackHolderTrigger2 = new ByRef<Trigger.Callback>(null);

		new Verifications() {
			{
				new Reports(withEqual("name"), withEqual(spec));
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
	public void notify(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr, @Mocked final LogicalReader reader,
			@Mocked final SubscriberController subscriber, @Mocked final ReaderController controller, @Mocked final ReaderCycleType cycleType) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		final Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		tag.setTid(new byte[] { 0x01 });
		tag.setResult(new HashMap<Integer, Result>());
		tag.getResult().put(
				Integer.valueOf(1),
				new ReadResult(ResultState.PERMISSION_ERROR,
						new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
		tag.setSighting(new Sighting("somehost", (short) 1, 2));
		tag.setCompleted(false);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;

				cycleType.getDuration();
				result = Integer.valueOf(0);

				reports.getFields();
				result = null;

				reports.match(withSameInstance(tag));
				result = Boolean.TRUE;

				reports.isCompleted(withSameInstance(tag));
				result = Boolean.FALSE;
			}
		};

		EventCycle cycle = new EventCycle("name", spec);
		Assert.assertTrue(cycle.immediate);

		cycle.add(subscriber); // will set the cycle to requested and start it

		Assert.assertEquals(0, cycle.datas.getPresent().size());
		Assert.assertEquals(0, cycle.datas.getPast().size());
		Assert.assertNull(cycle.termination);
		Assert.assertNotNull(tag.getSightings());
		Assert.assertEquals(0, tag.getSightings().size());

		// see the tag
		cycle.notify("somereader", tag, controller);

		Assert.assertEquals(1, tag.getCount());
		Assert.assertFalse(tag.isCompleted());
		Assert.assertEquals(ResultState.PERMISSION_ERROR, tag.getResult().get(Integer.valueOf(1)).getState());
		Assert.assertNotNull(tag.getSightings());
		Assert.assertEquals(1, tag.getSightings().size());
		Assert.assertNotNull(tag.getSightings().get("somereader"));
		Assert.assertEquals(1, tag.getSightings().get("somereader").size());
		long t = tag.getSightings().get("somereader").get(0).getTimestamp().getTime();
		Assert.assertEquals(new Sighting("somehost", (short) 1, 2, new Date(t)), tag.getSightings().get("somereader").get(0));

		Assert.assertEquals(1, cycle.datas.getPresent().size());
		Tag seenTag = cycle.datas.get(new PrimaryKey(tag, null));
		Assert.assertSame(seenTag, tag);

		final Tag tagAgain = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		tagAgain.setTid(new byte[] { 0x01 });
		tagAgain.setResult(new HashMap<Integer, Result>());
		tagAgain.getResult().put(Integer.valueOf(1),
				new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
		tagAgain.setSighting(new Sighting("somehost", (short) 2, 3));

		new NonStrictExpectations() {
			{
				reports.isCompleted(withSameInstance(tag));
				result = Boolean.TRUE;
			}
		};

		// see the tag again
		cycle.notify("somereader", tagAgain, controller);

		Assert.assertTrue(tag.isCompleted());
		Assert.assertEquals(2, tag.getCount());
		Assert.assertEquals(ResultState.SUCCESS, tag.getResult().get(Integer.valueOf(1)).getState());
		Assert.assertNotNull(tag.getSightings());
		Assert.assertEquals(1, tag.getSightings().size());
		Assert.assertNotNull(tag.getSightings().get("somereader"));
		Assert.assertEquals(2, tag.getSightings().get("somereader").size());
		t = tag.getSightings().get("somereader").get(0).getTimestamp().getTime();
		Assert.assertEquals(new Sighting("somehost", (short) 1, 2, new Date(t)), tag.getSightings().get("somereader").get(0));
		t = tag.getSightings().get("somereader").get(1).getTimestamp().getTime();
		Assert.assertEquals(new Sighting("somehost", (short) 2, 3, new Date(t)), tag.getSightings().get("somereader").get(1));
	}

	@Test
	public void notifyWithCycleDuration(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr,
			@Mocked final LogicalReader reader, @Mocked final SubscriberController subscriber, @Mocked final ReaderController controller,
			@Mocked final ReaderCycleType cycleType) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		final Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		tag.setTid(new byte[] { 0x01 });
		tag.setResult(new HashMap<Integer, Result>());
		tag.getResult().put(Integer.valueOf(1),
				new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;

				cycleType.getDuration();
				result = Integer.valueOf(50); // wait 50ms

				reports.getFields();
				result = null;

				reports.match(withSameInstance(tag));
				result = Boolean.TRUE;

				reports.isCompleted(withSameInstance(tag));
				result = Boolean.TRUE;
			}
		};

		EventCycle cycle = new EventCycle("name", spec);
		Assert.assertTrue(cycle.immediate);

		cycle.add(subscriber); // will set the cycle to requested and start it

		Assert.assertEquals(0, cycle.datas.getPresent().size());
		Assert.assertEquals(0, cycle.datas.getPast().size());
		Assert.assertNull(cycle.termination);
		Assert.assertNotNull(tag.getSightings());
		Assert.assertEquals(0, tag.getSightings().size());

		// check that no signal is waiting
		Assert.assertTrue("Expects cycle lock to not be acquired", cycle.lock.tryLock());
		try {
			Assert.assertFalse("Expects the condition not to be signaled", cycle.condition.await(50, TimeUnit.MILLISECONDS));
		} finally {
			cycle.lock.unlock();
		}

		// see the tag
		cycle.notify("somereader", tag, controller);

		// wait for the signal
		Assert.assertTrue("Expects cycle lock to not be acquired", cycle.lock.tryLock());
		try {
			long start = System.currentTimeMillis();
			Assert.assertTrue("Expects the condition to be signaled", cycle.condition.await(100, TimeUnit.MILLISECONDS));
			Assert.assertTrue("Expects a delay of at least 25ms", (System.currentTimeMillis() - start) > 25);
		} finally {
			cycle.lock.unlock();
		}

		Assert.assertTrue(tag.isCompleted());
		Assert.assertEquals(1, tag.getCount());

		Assert.assertEquals(1, cycle.datas.getPresent().size());
		Tag seenTag = cycle.datas.get(new PrimaryKey(tag, null));
		Assert.assertSame(seenTag, tag);
	}

	@Test
	public void interrupt(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr, @Mocked final LogicalReader reader,
			@Mocked final SubscriberController subscriber, @Mocked final ReaderController controller) throws Exception, InterruptedException {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.FALSE);
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(10);
		spec.getBoundarySpec().setStableSetInterval(time);

		final Tag tag = TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		tag.setTid(new byte[] { 0x01 });
		tag.setResult(new HashMap<Integer, Result>());
		tag.getResult().put(Integer.valueOf(1),
				new ReadResult(ResultState.SUCCESS, new byte[] { 0x30, 0x74, 0x25, 0x7B, (byte) 0xF7, 0x19, 0x4E, 0x40, 0x00, 0x00, 0x1A, (byte) 0x85 }));
		tag.setSighting(new Sighting("somehost", (short) 1, 2));

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;

				reports.getFields();
				result = null;

				reports.match(withSameInstance(tag));
				result = Boolean.TRUE;

				reports.isCompleted(withSameInstance(tag));
				result = Boolean.TRUE;
			}
		};

		EventCycle cycle = new EventCycle("name", spec);
		Assert.assertTrue(cycle.immediate);

		cycle.add(subscriber); // will set the cycle to requested and start it

		Assert.assertEquals(0, cycle.datas.getPresent().size());
		Assert.assertEquals(0, cycle.datas.getPast().size());
		Assert.assertNull(cycle.termination);

		// see the tag
		cycle.notify("somereader", tag, controller);

		Thread.sleep(50);

		Assert.assertEquals(Termination.STABLE_SET, cycle.termination);

		Assert.assertTrue(tag.isCompleted());
		Assert.assertEquals(1, tag.getCount());

		Assert.assertEquals(1, cycle.datas.getPresent().size());
		Tag seenTag = cycle.datas.get(new PrimaryKey(tag, null));
		Assert.assertSame(seenTag, tag);
	}

	@Test
	public void find(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr, @Mocked final LogicalReader reader,
			@Mocked final SubscriberController subscriber) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;

				subscriber.getURI();
				result = new URI("http://test");
			}
		};

		EventCycle cycle = new EventCycle("name", spec);
		cycle.add(subscriber);

		Assert.assertSame(subscriber, cycle.find(new URI("http://test")));
		Assert.assertNull(cycle.find(new URI("http://test2")));
	}

	@Test
	public void exists(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr, @Mocked final LogicalReader reader,
			@Mocked final SubscriberController subscriber, @Mocked final SubscriberListener<ECReports> subscriberListener) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;

				subscriber.getURI();
				result = new URI("http://test");

				subscriberListener.getURI();
				result = new URI("http://listener");
			}
		};

		EventCycle cycle = new EventCycle("name", spec);
		cycle.add(subscriber);
		cycle.add(subscriberListener);

		Assert.assertTrue(cycle.exists(new URI("http://test")));
		Assert.assertFalse(cycle.exists(new URI("http://listener")));
		Assert.assertFalse(cycle.exists(new URI("http://test2")));
	}

	@Test
	public void getSubscribers(@Mocked final Reports reports, @Mocked final TagOperation tagOperation, @Mocked final LR lr, @Mocked final LogicalReader reader,
			@Mocked final SubscriberController subscriber, @Mocked final SubscriberListener<ECReports> subscriberListener) throws Exception {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add("reader1");
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		new NonStrictExpectations() {
			{
				new Reports(withEqual("name"), withEqual(spec));
				result = reports;

				reports.getReaderOperation();
				result = tagOperation;

				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;

				subscriber.getURI();
				result = new URI("http://test");

				subscriberListener.getURI();
				result = new URI("http://listener");
			}
		};

		EventCycle cycle = new EventCycle("name", spec);
		cycle.add(subscriber);
		cycle.add(subscriberListener);

		Assert.assertEquals(Arrays.asList("http://test"), cycle.getSubscribers());
	}

	private ECSpec createCycleSpec(String readerName, String reportName) {
		final ECSpec spec = new ECSpec();
		spec.setLogicalReaders(new LogicalReaders());
		spec.getLogicalReaders().getLogicalReader().add(readerName);
		spec.setBoundarySpec(new ECBoundarySpec());
		spec.setReportSpecs(new ReportSpecs());
		ECReportSpec reportSpec = new ECReportSpec();
		reportSpec.setReportName(reportName);
		reportSpec.setReportSet(new ECReportSetSpec());
		reportSpec.getReportSet().setSet("CURRENT");
		reportSpec.setOutput(new ECReportOutputSpec());
		reportSpec.getOutput().setIncludeTag(Boolean.TRUE);
		spec.getReportSpecs().getReportSpec().add(reportSpec);
		return spec;
	}

	private void defineReaderExpectations(final LR lr, final LogicalReader reader) throws NoSuchNameException {
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock(withEqual("reader1"));
				result = reader;
			}
		};
	}

	private Caller<Tag> getReaderCallback(final LogicalReader reader, final TagOperation tagOperation) throws ImplementationException, ValidationException {
		final ByRef<Caller<Tag>> callbackHolder = new ByRef<Caller<Tag>>(null);
		new Verifications() {
			{
				Caller<Tag> callback;
				reader.define(withEqual(tagOperation), callback = withCapture(), this.<String> withNotNull());
				times = 1;
				callbackHolder.setValue(callback);
			}
		};
		return callbackHolder.getValue();
	}

	private void reportTag(Tag tag, long reportDuration, Caller<Tag> callback, ReaderController controller) throws InterruptedException {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < reportDuration) {
			callback.invoke(tag, controller);
			Thread.sleep(5);
		}
	}

	private void verifyCycleRuns(final LogicalReader reader, final TagOperation tagOperation, final int runs) throws ImplementationException,
			ValidationException {
		new Verifications() {
			{
				String name;
				reader.define(withEqual(tagOperation), this.<Caller<Tag>> withNotNull(), name = withCapture());
				times = 1;

				reader.enable(withEqual(tagOperation));
				times = runs;

				reader.disable(withEqual(tagOperation));
				times = runs;

				reader.undefine(withEqual(tagOperation), withEqual(name));
				times = 1;
			}
		};
	}

	private void verfiyReport(IReports reports, String reportName, long duration, String initiationCondition, String initiationTrigger,
			String terminationCondition, String terminationTrigger, String... tags) {
		Assert.assertEquals(ECReports.class, reports.getClass());
		ECReports ecReports = (ECReports) reports;
		Assert.assertTrue("Expected totalMilliseconds=" + duration + ", but was " + ecReports.getTotalMilliseconds(),
				ecReports.getTotalMilliseconds() >= Math.max(duration - 25, 0) && ecReports.getTotalMilliseconds() <= duration + 15);
		Assert.assertEquals(initiationCondition, ecReports.getInitiationCondition());
		Assert.assertEquals(initiationTrigger, ecReports.getInitiationTrigger());
		Assert.assertEquals(terminationCondition, ecReports.getTerminationCondition());
		Assert.assertEquals(terminationTrigger, ecReports.getTerminationTrigger());
		Assert.assertEquals(1, ecReports.getReports().getReport().size());
		Assert.assertEquals(reportName, ecReports.getReports().getReport().get(0).getReportName());
		Assert.assertEquals(tags.length > 0 ? 1 : 0, ecReports.getReports().getReport().get(0).getGroup().size());
		if (tags.length > 0) {
			Assert.assertEquals(null, ecReports.getReports().getReport().get(0).getGroup().get(0).getGroupName());
			Assert.assertEquals(tags.length, ecReports.getReports().getReport().get(0).getGroup().get(0).getGroupList().getMember().size());
			int i = 0;
			for (String tag : tags) {
				Assert.assertEquals(tag, ecReports.getReports().getReport().get(0).getGroup().get(0).getGroupList().getMember().get(i++).getTag().getValue());
			}
		}
	}

	@Test
	public void testCycleExecutionWithDuration(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller)
			throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setDuration(time);

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 100ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 100, callback, controller);

		// cycle not running anymore (50ms duration)
		Assert.assertTrue(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());
		Assert.assertEquals(0, subscriber.getCount());

		// verify report
		Assert.assertEquals(1, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 50, "REQUESTED", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());
		Assert.assertFalse(cycle.isBusy());

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 1);
	}

	@Test
	public void testCycleExecutionWithDurationAndRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller)
			throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(25);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setRepeatPeriod(time);

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 80ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 80, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);

		Thread.sleep(10);

		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());
		Assert.assertEquals(0, subscriber.getCount());

		// verify report
		Assert.assertEquals(2, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 25, "REQUESTED", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", 25, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 2);
	}

	@Test
	public void testCycleExecutionWithDurationAndRepeatPeriodAndStopTriggerAfterDurationAndBeforeRepeat(@Mocked final LR lr,
			@Mocked final LogicalReader reader, @Mocked final ReaderController controller, @Mocked final Trigger trigger) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(5);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setRepeatPeriod(time);

		// report empty, we won't have tags
		spec.getReportSpecs().getReportSpec().get(0).setReportIfEmpty(Boolean.TRUE);

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		Thread.sleep(10);
		
		// duration has passed
		Assert.assertTrue(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// stop cycle via trigger
		Assert.assertTrue(cycle.stop(trigger));

		Thread.sleep(70);

		// move to unrequested state
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());
		Assert.assertEquals(0, subscriber.getCount());

		// verify report
		Assert.assertEquals(1, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 5, "REQUESTED", null, "DURATION", null);

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 1);
	}

	@Test
	public void testCycleExecutionWithSameDurationAndRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader,
			@Mocked final ReaderController controller) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setRepeatPeriod(time);

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 120ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 120, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// verify report
		Assert.assertEquals(2, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 50, "REQUESTED", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		// verify fast path because repeat period = duration,
		// operation is not disabled and re-enabled, therefore only one run
		verifyCycleRuns(reader, tagOperation, 1);
	}

	@Test
	public void testCycleExecutionWithDurationAndZeroRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader,
			@Mocked final ReaderController controller) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(0);
		spec.getBoundarySpec().setRepeatPeriod(time);

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 110ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 110, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// verify report
		Assert.assertEquals(2, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 50, "REQUESTED", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		// verify fast path because repeat period = 0,
		// operation is not disabled and re-enabled, therefore only one run
		verifyCycleRuns(reader, tagOperation, 1);
	}

	@Test
	public void testCycleExecutionWithAlmostSameDurationAndRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader,
			@Mocked final ReaderController controller) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(55);
		spec.getBoundarySpec().setRepeatPeriod(time);

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		// subscriber which delays the cycle by 5ms
		DummySubscriberController subscriber = new DummySubscriberController("http://test", 5);
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 120ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 120, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// verify report
		Assert.assertEquals(2, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 50, "REQUESTED", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		// we have slowness, because of the introduced delay, but we correct
		// with reduced durations, therefore it should always be 2 runs
		verifyCycleRuns(reader, tagOperation, 2);
	}

	@Test
	public void testCycleExecutionWithStableSetInterval(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller)
			throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setStableSetInterval(time);

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 100ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 100, callback, controller);

		// cycle not running anymore (50ms stable set interval)
		Assert.assertTrue(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());
		Assert.assertEquals(0, subscriber.getCount());

		// verify report
		Assert.assertEquals(1, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 50, "REQUESTED", null, "STABLE_SET", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());
		Assert.assertFalse(cycle.isBusy());

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 1);
	}

	@Test
	public void testCycleExecutionWithWhenDataAvailable(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller,
			@Mocked final ReaderCycleType cycleConfig) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		final int whenDataAvailableDelay = 50;

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		new NonStrictExpectations() {
			{
				cycleConfig.getDuration();
				result = Integer.valueOf(50);
			}
		};
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 100ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 100, callback, controller);

		// cycle not running anymore (50ms global reader cycle setting)
		Assert.assertTrue(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());
		Assert.assertEquals(0, subscriber.getCount());

		// verify report
		Assert.assertEquals(1, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", whenDataAvailableDelay + 5, "REQUESTED", null, "DATA_AVAILABLE", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());
		Assert.assertFalse(cycle.isBusy());

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 1);
	}

	@Test
	public void testCycleExecutionWithWhenDataAvailableAndDurationAndRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller,
			@Mocked final ReaderCycleType cycleConfig) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(45);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setRepeatPeriod(time);
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		final int whenDataAvailableDelay = 10;

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		new NonStrictExpectations() {
			{
				cycleConfig.getDuration();
				result = Integer.valueOf(whenDataAvailableDelay);
			}
		};
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 75ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 75, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// verify report
		Assert.assertEquals(2, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", whenDataAvailableDelay + 5, "REQUESTED", null, "DATA_AVAILABLE", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", whenDataAvailableDelay + 5, "REPEAT_PERIOD", null, "DATA_AVAILABLE", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 2);
	}

	@Test
	public void testCycleExecutionWithWhenDataAvailableAndSameDurationAndRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller,
			@Mocked final ReaderCycleType cycleConfig) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setRepeatPeriod(time);
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		final int whenDataAvailableDelay = 10;

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		new NonStrictExpectations() {
			{
				cycleConfig.getDuration();
				result = Integer.valueOf(whenDataAvailableDelay);
			}
		};
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 80ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 80, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// verify report
		Assert.assertEquals(2, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", whenDataAvailableDelay + 5, "REQUESTED", null, "DATA_AVAILABLE", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", whenDataAvailableDelay + 5, "REPEAT_PERIOD", null, "DATA_AVAILABLE", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 2);
	}

	@Test
	public void testCycleExecutionWithTooLongWhenDataAvailableAndSameDurationAndRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller,
			@Mocked final ReaderCycleType cycleConfig) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setRepeatPeriod(time);
		spec.getBoundarySpec().setExtension(new ECBoundarySpecExtension());
		spec.getBoundarySpec().getExtension().setWhenDataAvailable(Boolean.TRUE);

		final int whenDataAvailableDelay = 100;

		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);
		new NonStrictExpectations() {
			{
				cycleConfig.getDuration();
				result = Integer.valueOf(whenDataAvailableDelay);
			}
		};
		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 210ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 210, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// verify report, data available never has any effect
		Assert.assertEquals(4, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 50, "REQUESTED", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(2), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(3), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 1);
	}

	@Test
	public void testCycleExecutionWithTooLongStableSetAndSameDurationAndRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setRepeatPeriod(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(100);
		spec.getBoundarySpec().setStableSetInterval(time);


		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);

		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 210ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 210, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// verify report, stable set timer never has any effect
		Assert.assertEquals(4, subscriber.getReports().size());
		verfiyReport(subscriber.getReports().get(0), "report1", 50, "REQUESTED", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(2), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(3), "report1", 50, "REPEAT_PERIOD", null, "DURATION", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 1);
	}

	@Test
	public void testCycleExecutionWithStableSetAndDurationAndZeroRepeatPeriod(@Mocked final LR lr, @Mocked final LogicalReader reader, @Mocked final ReaderController controller) throws Exception {
		final ECSpec spec = createCycleSpec("reader1", "report1");
		ECTime time = new ECTime();
		time.setUnit("MS");
		time.setValue(50);
		spec.getBoundarySpec().setDuration(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(0);
		spec.getBoundarySpec().setRepeatPeriod(time);
		time = new ECTime();
		time.setUnit("MS");
		time.setValue(25);
		spec.getBoundarySpec().setStableSetInterval(time);


		final TagOperation tagOperation = new TagOperation(new ArrayList<Operation>());
		defineReaderExpectations(lr, reader);

		DummySubscriberController subscriber = new DummySubscriberController("http://test");
		subscriber.setActive(true);

		final EventCycle cycle = new EventCycle("name", spec);
		Caller<Tag> callback = getReaderCallback(reader, tagOperation);

		cycle.start();
		Thread.sleep(25);
		cycle.add(subscriber); // cycle is running now
		Assert.assertTrue(cycle.isBusy());
		Assert.assertTrue(cycle.isActive());

		// report the tag for 210ms
		reportTag(TagDecoder.getInstance().fromUrn("urn:epc:tag:sgtin-96:3.0614141.812345.6789", new byte[] { 0x01 }), 210, callback, controller);

		// stop the cycle
		cycle.remove(subscriber);
		Assert.assertFalse(subscriber.getActive());

		Assert.assertFalse(cycle.isBusy());
		Assert.assertFalse(cycle.isActive());

		// verify report
		Assert.assertTrue("Expected a report size of 7 or 8", subscriber.getReports().size() == 7 || subscriber.getReports().size() == 8);
		verfiyReport(subscriber.getReports().get(0), "report1", 25 + 10, "REQUESTED", null, "STABLE_SET", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(1), "report1", 25 + 10, "REPEAT_PERIOD", null, "STABLE_SET", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(2), "report1", 25 + 10, "REPEAT_PERIOD", null, "STABLE_SET", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(3), "report1", 25 + 10, "REPEAT_PERIOD", null, "STABLE_SET", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(4), "report1", 25 + 10, "REPEAT_PERIOD", null, "STABLE_SET", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(5), "report1", 25 + 10, "REPEAT_PERIOD", null, "STABLE_SET", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");
		verfiyReport(subscriber.getReports().get(6), "report1", 25 + 10, "REPEAT_PERIOD", null, "STABLE_SET", null, "urn:epc:tag:sgtin-96:3.0614141.812345.6789");

		cycle.dispose();

		verifyCycleRuns(reader, tagOperation, 1);
	}
}
