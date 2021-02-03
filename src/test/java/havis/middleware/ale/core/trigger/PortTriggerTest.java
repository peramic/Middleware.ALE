package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.port.result.ReadResult;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.trigger.Trigger.Callback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.junit.Assert;
import org.junit.Test;

public class PortTriggerTest {

	@Test
	public void portTriggerIn(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, ImplementationException, NoSuchNameException {
		PortTriggerService.reset();
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = reader;
			}
		};

		final AtomicInteger count = new AtomicInteger(0);
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				count.incrementAndGet();
				Assert.assertNotNull(trigger);
				return true;
			}
		};

		final PortTrigger trigger = new PortTrigger("1", "urn:havis:ale:trigger:port:test:in:1.1", callback);

		Assert.assertEquals("urn:havis:ale:trigger:port:test:in:1.1", trigger.getUri());
		Assert.assertNotNull(trigger.getPin());
		Assert.assertEquals(1, trigger.getPin().getId());
		Assert.assertEquals(Pin.Type.INPUT, trigger.getPin().getType());
		Assert.assertEquals(Byte.valueOf((byte) 0x01), trigger.getState());

		final ByRef<Caller<Port>> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				lr.lock("test");
				times = 1;

				Caller<Port> callback;
				reader.define(withEqual(new PortObservation()), callback = withCapture(), this.<String> withNotNull());
				times = 1;

				Assert.assertNotNull(callback);
				callbackHolder.setValue(callback);

				reader.enable(withEqual(new PortObservation()));
				times = 1;
			}
		};

		Map<Integer, Result> result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x00));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.MISC_ERROR_PARTIAL, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new Result(Result.State.SUCCESS));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(2, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(1, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(-1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(2, count.intValue());
	}

	@Test
	public void portTriggerInStartStop(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, ImplementationException,
			NoSuchNameException {
		PortTriggerService.reset();

		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = reader;
			}
		};

		final AtomicBoolean running = new AtomicBoolean(false);

		Callback start = new Callback() {

			@Override
			public boolean invoke(Trigger trigger) {
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
				if (running.get()) {
					running.set(false);
					return true;
				}
				return false;
			}
		};

		PortTrigger tStart = new PortTrigger("1", "urn:havis:ale:trigger:port:test:in:1.1", start);
		PortTrigger tStop = new PortTrigger("1", "urn:havis:ale:trigger:port:test:in:1.1", stop);

		final ByRef<Caller<Port>> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				lr.lock("test");
				times = 1;

				PortObservation observation;
				Caller<Port> callback;
				String guid;
				reader.define(observation = withCapture(), callback = withCapture(), guid = withCapture());
				times = 1;

				Assert.assertNotNull(observation);
				Assert.assertNotNull(callback);
				Assert.assertNotNull(guid);
				callbackHolder.setValue(callback);

				reader.enable(withSameInstance(observation));
				times = 1;
			}
		};

		Map<Integer, Result> result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x01));
		Port port = new Port(new Pin(1, Pin.Type.INPUT), "whatever", result);

		callbackHolder.getValue().invoke(port, null);

		Assert.assertTrue(running.get());

		callbackHolder.getValue().invoke(port, null);

		Assert.assertFalse(running.get());

		callbackHolder.getValue().invoke(port, null);

		Assert.assertTrue(running.get());

		callbackHolder.getValue().invoke(port, null);

		Assert.assertFalse(running.get());

		tStart.dispose();
		tStop.dispose();
	}

	@Test
	public void portTriggerOut(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, ImplementationException,
			NoSuchNameException {
		PortTriggerService.reset();
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("testout");
				result = reader;
			}
		};

		final AtomicInteger count = new AtomicInteger(0);
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				count.incrementAndGet();
				Assert.assertNotNull(trigger);
				return true;
			}
		};

		final PortTrigger trigger = new PortTrigger("1", "urn:havis:ale:trigger:port:testout:out:2.0", callback);

		Assert.assertEquals("urn:havis:ale:trigger:port:testout:out:2.0", trigger.getUri());
		Assert.assertNotNull(trigger.getPin());
		Assert.assertEquals(2, trigger.getPin().getId());
		Assert.assertEquals(Pin.Type.OUTPUT, trigger.getPin().getType());
		Assert.assertEquals(Byte.valueOf((byte) 0x00), trigger.getState());

		final ByRef<Caller<Port>> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				lr.lock("testout");
				times = 1;

				Caller<Port> callback;
				reader.define(withEqual(new PortObservation()), callback = withCapture(), this.<String> withNotNull());
				times = 1;

				Assert.assertNotNull(callback);
				callbackHolder.setValue(callback);

				reader.enable(withEqual(new PortObservation()));
				times = 1;
			}
		};

		Map<Integer, Result> result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(2, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		callbackHolder.getValue().invoke(new Port(new Pin(2, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.MISC_ERROR_PARTIAL, (byte) 0x00));
		callbackHolder.getValue().invoke(new Port(new Pin(2, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new Result(Result.State.SUCCESS));
		callbackHolder.getValue().invoke(new Port(new Pin(2, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x00));
		callbackHolder.getValue().invoke(new Port(new Pin(2, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x00));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x00));
		callbackHolder.getValue().invoke(new Port(new Pin(2, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(1, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x00));
		callbackHolder.getValue().invoke(new Port(new Pin(-1, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(2, count.intValue());
	}

	@Test
	public void portTriggerAnyIdAndAnyState(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, ImplementationException,
			NoSuchNameException {
		PortTriggerService.reset();
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = reader;
			}
		};

		final AtomicInteger count = new AtomicInteger(0);
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				count.incrementAndGet();
				Assert.assertNotNull(trigger);
				return true;
			}
		};

		final PortTrigger trigger = new PortTrigger("1", "urn:havis:ale:trigger:port:test:in", callback);

		Assert.assertEquals("urn:havis:ale:trigger:port:test:in", trigger.getUri());
		Assert.assertNotNull(trigger.getPin());
		Assert.assertEquals(-1, trigger.getPin().getId());
		Assert.assertEquals(Pin.Type.INPUT, trigger.getPin().getType());
		Assert.assertNull(trigger.getState());

		final ByRef<Caller<Port>> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				lr.lock("test");
				times = 1;

				Caller<Port> callback;
				reader.define(withEqual(new PortObservation()), callback = withCapture(), this.<String> withNotNull());
				times = 1;

				Assert.assertNotNull(callback);
				callbackHolder.setValue(callback);

				reader.enable(withEqual(new PortObservation()));
				times = 1;
			}
		};

		Map<Integer, Result> result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.MISC_ERROR_PARTIAL, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new Result(Result.State.SUCCESS));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.OUTPUT), "whatever", result), null);
		Assert.assertEquals(0, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x01));
		callbackHolder.getValue().invoke(new Port(new Pin(1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(1, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS, (byte) 0x00));
		callbackHolder.getValue().invoke(new Port(new Pin(-1, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(2, count.intValue());

		result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(0), new ReadResult(Result.State.SUCCESS));
		callbackHolder.getValue().invoke(new Port(new Pin(2, Pin.Type.INPUT), "whatever", result), null);
		Assert.assertEquals(3, count.intValue());
	}

	@Test(expected = ValidationException.class)
	public void portTriggerNoMatch() throws ValidationException, ImplementationException {
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		new PortTrigger("1", "nomatch", callback);
	}

	@Test(expected = IllegalArgumentException.class)
	public void portTriggerNoCallback() throws ValidationException, ImplementationException {
		new PortTrigger("1", "urn:havis:ale:trigger:port:test:in", null);
	}

	@Test(expected = ValidationException.class)
	public void portTriggerNoSuchName(@Mocked final LR lr) throws ValidationException, ImplementationException, NoSuchNameException {
		PortTriggerService.reset();
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = new NoSuchNameException();
			}
		};
		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};
		new PortTrigger("1", "urn:havis:ale:trigger:port:test:in", callback);
	}

	@Test
	public void portTriggerFailedToDefine(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, ImplementationException,
			NoSuchNameException {
		PortTriggerService.reset();
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = reader;

				reader.define(this.<PortObservation> withNotNull(), this.<Caller<Port>> withNotNull(), this.<String> withNotNull());
				result = new ValidationException();
			}
		};

		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};

		try {
			new PortTrigger("1", "urn:havis:ale:trigger:port:test:in:1.1", callback);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				lr.lock("test");
				times = 1;

				String name;
				PortObservation observation;
				reader.define(observation = withCapture(), this.<Caller<Port>> withNotNull(), name = withCapture());
				times = 1;

				reader.disable(withSameInstance(observation));
				times = 1;

				reader.undefine(withSameInstance(observation), withEqual(name));
				times = 1;

				reader.enable(this.<PortObservation> withNotNull());
				times = 0;

				reader.unlock();
				times = 1;
			}
		};
	}

	@Test
	public void portTriggerFailedToEnable(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, ImplementationException,
			NoSuchNameException {
		PortTriggerService.reset();
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = reader;

				reader.enable(this.<PortObservation> withNotNull());
				result = new ImplementationException();
			}
		};

		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};

		try {
			new PortTrigger("1", "urn:havis:ale:trigger:port:test:in:1.1", callback);
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}

		new Verifications() {
			{
				lr.lock("test");
				times = 1;

				String name;
				PortObservation observation;
				reader.define(observation = withCapture(), this.<Caller<Port>> withNotNull(), name = withCapture());
				times = 1;

				reader.enable(withSameInstance(observation));
				times = 1;

				reader.disable(withSameInstance(observation));
				times = 1;

				reader.undefine(withSameInstance(observation), withEqual(name));
				times = 1;

				reader.unlock();
				times = 1;
			}
		};
	}

	@Test
	public void portTriggerFailedToDefineFailedToDisable(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException,
			ImplementationException, NoSuchNameException {
		PortTriggerService.reset();
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = reader;

				reader.define(this.<PortObservation> withNotNull(), this.<Caller<Port>> withNotNull(), this.<String> withNotNull());
				result = new ValidationException();

				reader.disable(this.<PortObservation> withNotNull());
				result = new ImplementationException();
			}
		};

		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};

		try {
			new PortTrigger("1", "urn:havis:ale:trigger:port:test:in:1.1", callback);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				lr.lock("test");
				times = 1;

				String name;
				PortObservation observation;
				reader.define(observation = withCapture(), this.<Caller<Port>> withNotNull(), name = withCapture());
				times = 1;

				reader.disable(withSameInstance(observation));
				times = 1;

				reader.undefine(withSameInstance(observation), withEqual(name));
				times = 0;

				reader.enable(this.<PortObservation> withNotNull());
				times = 0;

				reader.unlock();
				times = 1;
			}
		};
	}

	@Test
	public void dispose(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, ImplementationException, NoSuchNameException {
		PortTriggerService.reset();
		new NonStrictExpectations() {
			{
				LR.getInstance();
				result = lr;

				lr.lock("test");
				result = reader;
			}
		};

		Callback callback = new Callback() {
			@Override
			public boolean invoke(Trigger trigger) {
				return true;
			}
		};

		final PortTrigger t1 = new PortTrigger("1", "urn:havis:ale:trigger:port:test:in:1.1", callback);
		final PortTrigger t2 = new PortTrigger("2", "urn:havis:ale:trigger:port:test:in:1.1", callback);

		t1.dispose();

		final ByRef<String> guidHolder = new ByRef<String>(null);
		final ByRef<PortObservation> observationHolder = new ByRef<PortObservation>(null);

		new VerificationsInOrder() {
			{
				lr.lock("test");
				times = 1;

				String guid;
				PortObservation observation;
				reader.define(observation = withCapture(), this.<Caller<Port>> withNotNull(), guid = withCapture());
				times = 1;

				Assert.assertNotNull(observation);
				observationHolder.setValue(observation);
				Assert.assertNotNull(guid);
				guidHolder.setValue(guid);

				reader.enable(withSameInstance(observation));
				times = 1;

				reader.disable(withSameInstance(observation));
				times = 0;

				reader.undefine(withSameInstance(observation), withEqual(guid));
				times = 0;

				reader.unlock();
				times = 0;
			}
		};

		t2.dispose();

		new VerificationsInOrder() {
			{
				reader.disable(withSameInstance(observationHolder.getValue()));
				times = 1;

				reader.undefine(withSameInstance(observationHolder.getValue()), withEqual(guidHolder.getValue()));
				times = 1;

				reader.unlock();
				times = 1;
			}
		};
	}
}
