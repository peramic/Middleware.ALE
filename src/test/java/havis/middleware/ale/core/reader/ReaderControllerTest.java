package havis.middleware.ale.core.reader;

import havis.middleware.ale.Connector;
import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Operation;
import havis.middleware.ale.base.operation.port.Operation.Type;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.base.operation.tag.Field;
import havis.middleware.ale.base.operation.tag.Filter;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.operation.tag.result.WriteResult;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.reader.Callback;
import havis.middleware.ale.reader.Property;
import havis.middleware.ale.reader.ReaderConnector;
import havis.util.monitor.Broker;
import havis.util.monitor.Event;
import havis.util.monitor.Source;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReaderControllerTest {

	@BeforeClass
	public static void init() {
		TagExecutor.class.getName(); // make sure the class is loaded and
									 // visible in coverage
		ConfigResetter.reset();
		ConfigResetter.disablePersistence();

		Connector.createFactory(new Connector() {

			@Override
			public <S> S newInstance(Class<S> clazz, String type) throws ImplementationException {
				return null;
			}

			@Override
			public <S> List<String> getTypes(Class<S> clazz) throws ImplementationException {
				return null;
			}

			@Override
			public Broker getBroker() {
				return new Broker() {
					@Override
					public void notify(Source arg0, Event arg1) {
					}
				};
			}
		});
	}

	@AfterClass
	public static void cleanup() {
		Connector.clearFactory();
	}

	@Test
	public void executeTagOperation(@Mocked final ReaderConnector connector) throws Exception {
		final String readerName = "reader";
		final Map<String, String> properties = new HashMap<>();
		final ReaderController readerController = new ReaderController(readerName, connector, properties);
		final TagOperation operation = new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(1, OperationType.WRITE, new Field(
				"epc", 1, 32, 56), new byte[] { 0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x02 })), Arrays.asList(new Filter(1, 56, 0, new byte[] { 0x00, 0x00, 0x00,
				0x00, 0x01, 0x01, 0x01 }), new Filter(2, 56, 0, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 })));
		final Tag processedTag = new Tag(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 });
		processedTag.setTid(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 });
		processedTag.setResult(new HashMap<Integer, Result>());
		processedTag.getResult().put(Integer.valueOf(1), new WriteResult(ResultState.SUCCESS));
		processedTag.setCompleted(false);

		final Tag expectedTag = new Tag(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x02 });
		expectedTag.setTid(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 });
		expectedTag.setResult(new HashMap<Integer, Result>());
		expectedTag.getResult().put(Integer.valueOf(1), new WriteResult(ResultState.SUCCESS));
		expectedTag.setCompleted(false);

		final ByRef<CountDownLatch> callbackReceived = new ByRef<>(new CountDownLatch(1));
		final ByRef<AssertionError> errorReceived = new ByRef<>(null);

		Caller<Tag> caller = new Caller<Tag>() {
			@Override
			public void invoke(Tag tag, ReaderController controller) {
				try {
					Assert.assertTrue(callbackReceived.getValue().getCount() == 1);
					Assert.assertSame(readerController, controller);
					Assert.assertEquals(expectedTag, tag);
					Assert.assertEquals(expectedTag.getResult(), tag.getResult());
					callbackReceived.getValue().countDown();
				} catch (AssertionError e) {
					errorReceived.setValue(e);
				}
			}
		};

		final ByRef<Callback> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				Callback callback;
				connector.setCallback(callback = withCapture());
				callbackHolder.setValue(callback);
			}
		};
		Assert.assertNotNull(callbackHolder.getValue());

		// define some tag operation
		readerController.define(
				new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(1, OperationType.READ, new Field("epc", 1, 0, 56)))),
				new Caller<Tag>() {
					@Override
					public void invoke(Tag tag, ReaderController controller) {
						// ignore
					}
				}, "ignore");

		readerController.execute(readerName, operation, caller);
		Thread.sleep(50); // wait for execution

		new VerificationsInOrder() {
			{
				connector.connect();
				times = 1;

				connector.executeTagOperation(2L, withSameInstance(operation));
				times = 1;
			}
		};

		// send execute result
		callbackHolder.getValue().notify(2L, processedTag);
		Assert.assertTrue("Expected callback", callbackReceived.getValue().await(50, TimeUnit.MILLISECONDS));
		if (errorReceived.getValue() != null) {
			throw errorReceived.getValue();
		}
		Thread.sleep(10);
		callbackReceived.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, processedTag);
		Assert.assertFalse("Expected no callback", callbackReceived.getValue().await(50, TimeUnit.MILLISECONDS));
		if (errorReceived.getValue() != null) {
			throw errorReceived.getValue();
		}
	}

	@Test
	public void executeTagOperationWithOptimization(@Mocked final ReaderConnector connector) throws Exception {
		final String readerName = "reader";
		final Map<String, String> properties = new HashMap<>();
		final ReaderController readerController = new ReaderController(readerName, connector, properties);
		final TagOperation operation = new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(1, OperationType.WRITE, new Field(
				"user", 3, 72, 16), new byte[] { 0x01, 0x02 }), new havis.middleware.ale.base.operation.tag.Operation(2, OperationType.WRITE, new Field("user",
				3, 32, 16), new byte[] { 0x03, 0x04 }), new havis.middleware.ale.base.operation.tag.Operation(3, OperationType.WRITE, new Field("user", 3, 32,
				56), new byte[] { 0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x02 })), Arrays.asList(new Filter(1, 56, 0, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01,
				0x01, 0x01 }), new Filter(2, 56, 0, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 })));
		final TagOperation optimized = new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(3, OperationType.WRITE, new Field(
				"user", 3, 32, 56), new byte[] { 0x00, 0x00, 0x00, 0x00, 0x02, 0x02, 0x02 })), Arrays.asList(new Filter(1, 56, 0, new byte[] { 0x00, 0x00,
				0x00, 0x00, 0x01, 0x01, 0x01 }), new Filter(2, 56, 0, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 })));

		final Tag processedTag = new Tag(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 });
		processedTag.setTid(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 });
		processedTag.setResult(new HashMap<Integer, Result>());
		processedTag.getResult().put(Integer.valueOf(3), new WriteResult(ResultState.SUCCESS, 4));
		processedTag.setCompleted(false);

		final Tag expectedTag = new Tag(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 });
		expectedTag.setTid(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 });
		expectedTag.setResult(new HashMap<Integer, Result>());
		expectedTag.getResult().put(Integer.valueOf(1), new WriteResult(ResultState.SUCCESS));
		expectedTag.getResult().put(Integer.valueOf(2), new WriteResult(ResultState.SUCCESS));
		expectedTag.getResult().put(Integer.valueOf(3), new WriteResult(ResultState.SUCCESS, 4));
		expectedTag.setCompleted(false);

		final ByRef<CountDownLatch> callbackReceived = new ByRef<>(new CountDownLatch(1));
		final ByRef<AssertionError> errorReceived = new ByRef<>(null);

		Caller<Tag> caller = new Caller<Tag>() {
			@Override
			public void invoke(Tag tag, ReaderController controller) {
				try {
					Assert.assertSame(readerController, controller);
					Assert.assertEquals(expectedTag, tag);
					Assert.assertEquals(expectedTag.getResult(), tag.getResult());
					callbackReceived.getValue().countDown();
				} catch (AssertionError e) {
					errorReceived.setValue(e);
				}
			}
		};

		final ByRef<Callback> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				Callback callback;
				connector.setCallback(callback = withCapture());
				callbackHolder.setValue(callback);
			}
		};
		Assert.assertNotNull(callbackHolder.getValue());

		// define some tag operation
		readerController.define(
				new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(1, OperationType.READ, new Field("epc", 1, 0, 56)))),
				new Caller<Tag>() {
					@Override
					public void invoke(Tag tag, ReaderController controller) {
						// ignore
					}
				}, "ignore");

		readerController.execute(readerName, operation, caller);
		Thread.sleep(50); // wait for execution

		new VerificationsInOrder() {
			{
				connector.connect();
				times = 1;

				connector.executeTagOperation(2L, withEqual(optimized));
				times = 1;
			}
		};

		// send execute result
		callbackHolder.getValue().notify(2L, processedTag);
		Assert.assertTrue("Expected callback", callbackReceived.getValue().await(50, TimeUnit.MILLISECONDS));
		if (errorReceived.getValue() != null) {
			throw errorReceived.getValue();
		}
		Thread.sleep(10);
		callbackReceived.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, processedTag);
		Assert.assertFalse("Expected no callback", callbackReceived.getValue().await(50, TimeUnit.MILLISECONDS));
		if (errorReceived.getValue() != null) {
			throw errorReceived.getValue();
		}
	}

	// TODO lost EPC scenario

	@Test
	public void executePortOperation(@Mocked final ReaderConnector connector) throws Exception {
		final String name = "name";
		final Map<String, String> properties = new HashMap<>();
		final ReaderController readerController = new ReaderController(name, connector, properties);
		final PortOperation operation = new PortOperation(Arrays.asList(new Operation("op1", Type.WRITE, Byte.valueOf((byte) 0x01), Long.valueOf(10), new Pin(
				1, Pin.Type.OUTPUT))));
		final Port portResult = new Port();
		portResult.setCompleted(false);

		final ByRef<CountDownLatch> callbackReceived = new ByRef<>(new CountDownLatch(1));
		final ByRef<AssertionError> errorReceived = new ByRef<>(null);

		Caller<Port> caller = new Caller<Port>() {
			@Override
			public void invoke(Port port, ReaderController controller) {
				try {
					Assert.assertSame(readerController, controller);
					Assert.assertSame(portResult, port);
					callbackReceived.getValue().countDown();
				} catch (AssertionError e) {
					errorReceived.setValue(e);
				}
			}
		};

		final ByRef<Callback> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				Callback callback;
				connector.setCallback(callback = withCapture());
				callbackHolder.setValue(callback);
			}
		};
		Assert.assertNotNull(callbackHolder.getValue());

		readerController.execute(operation, caller);

		Thread.sleep(50);

		new VerificationsInOrder() {
			{
				connector.connect();
				times = 1;

				connector.executePortOperation(1L, withSameInstance(operation));
				times = 1;

				connector.disconnect();
				times = 1;
			}
		};

		// send execute result
		callbackHolder.getValue().notify(1L, portResult);
		Assert.assertTrue("Expected callback", callbackReceived.getValue().await(50, TimeUnit.MILLISECONDS));
		if (errorReceived.getValue() != null) {
			throw errorReceived.getValue();
		}

		callbackReceived.setValue(new CountDownLatch(1)); // reset count down

		callbackHolder.getValue().notify(1L, portResult);
		Assert.assertFalse("Expected no callback", callbackReceived.getValue().await(50, TimeUnit.MILLISECONDS));
		if (errorReceived.getValue() != null) {
			throw errorReceived.getValue();
		}
	}

	@Test
	public void executePortOperationImplementationException(@Mocked final ReaderConnector connector) throws Exception {
		final String name = "name";
		final Map<String, String> properties = new HashMap<>();
		final ReaderController readerController = new ReaderController(name, connector, properties);
		final PortOperation operation = new PortOperation(Arrays.asList(new Operation("op1", Type.WRITE, Byte.valueOf((byte) 0x01), Long.valueOf(10), new Pin(
				1, Pin.Type.OUTPUT))));
		final Port portResult = new Port();
		portResult.setCompleted(false);

		final ByRef<CountDownLatch> callbackReceived = new ByRef<>(new CountDownLatch(1));
		final ByRef<AssertionError> errorReceived = new ByRef<>(null);

		Caller<Port> caller = new Caller<Port>() {
			@Override
			public void invoke(Port port, ReaderController controller) {
				try {
					Assert.assertSame(readerController, controller);
					Assert.assertNotSame(portResult, port);
					Assert.assertTrue(port.isCompleted());
					callbackReceived.getValue().countDown();
				} catch (AssertionError e) {
					errorReceived.setValue(e);
				}
			}
		};

		final ByRef<Callback> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				Callback callback;
				connector.setCallback(callback = withCapture());
				callbackHolder.setValue(callback);
			}
		};
		Assert.assertNotNull(callbackHolder.getValue());

		new NonStrictExpectations() {
			{
				connector.executePortOperation(1L, withSameInstance(operation));
				result = new ImplementationException();
			}
		};

		readerController.execute(operation, caller);

		Assert.assertTrue("Expected callback", callbackReceived.getValue().await(20, TimeUnit.MILLISECONDS));
		if (errorReceived.getValue() != null) {
			throw errorReceived.getValue();
		}
		Thread.sleep(50); // sleep for disconnect

		new VerificationsInOrder() {
			{
				connector.connect();
				times = 1;

				connector.executePortOperation(1L, withSameInstance(operation));
				times = 1;

				connector.disconnect();
				times = 1;
			}
		};

		// make sure no results are received anymore by sending a result
		callbackReceived.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, portResult);
		Assert.assertFalse("Expected no callback", callbackReceived.getValue().await(20, TimeUnit.MILLISECONDS));
		if (errorReceived.getValue() != null) {
			throw errorReceived.getValue();
		}
	}

	@Test
	public void testTagOperation(@Mocked final ReaderConnector connector) throws Exception {
		final Map<String, String> properties = new HashMap<>();
		final ReaderController readerController = new ReaderController("reader", connector, properties);
		final TagOperation operation1 = new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(1, OperationType.READ,
				new byte[] { 0x01 })));
		final TagOperation operation2 = new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(1, OperationType.READ,
				new byte[] { 0x02 })));
		final Tag tag = new Tag(new byte[] { 0x01 });
		tag.setTid(new byte[] { 0x01 });

		final ByRef<CountDownLatch> callback1Received = new ByRef<>(new CountDownLatch(1));
		Caller<Tag> caller1 = new Caller<Tag>() {
			@Override
			public void invoke(Tag t, ReaderController controller) {
				Assert.assertSame(readerController, controller);
				Assert.assertSame(tag, t);
				callback1Received.getValue().countDown();
			}
		};

		final ByRef<CountDownLatch> callback2Received = new ByRef<>(new CountDownLatch(1));
		Caller<Tag> caller2 = new Caller<Tag>() {
			@Override
			public void invoke(Tag t, ReaderController controller) {
				Assert.assertSame(readerController, controller);
				Assert.assertSame(tag, t);
				callback2Received.getValue().countDown();
			}
		};

		final ByRef<CountDownLatch> callback3Received = new ByRef<>(new CountDownLatch(1));
		Caller<Tag> caller3 = new Caller<Tag>() {
			@Override
			public void invoke(Tag t, ReaderController controller) {
				Assert.assertSame(readerController, controller);
				Assert.assertSame(tag, t);
				callback3Received.getValue().countDown();
			}
		};

		final ByRef<Callback> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				Callback callback;
				connector.setCallback(callback = withCapture());
				callbackHolder.setValue(callback);
			}
		};

		readerController.define(operation1, caller1, "caller1");

		new VerificationsInOrder() {
			{
				connector.connect();
				times = 1;

				connector.defineTagOperation(1L, withSameInstance(operation1));
				times = 1;

				connector.disconnect();
				times = 0;
			}
		};

		// try sending a result
		callbackHolder.getValue().notify(1L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.enable(operation1);

		new VerificationsInOrder() {
			{
				connector.enableTagOperation(1L);
				times = 1;

				connector.disconnect();
				times = 0;
			}
		};

		// send 3 results
		callback1Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		Assert.assertTrue("Expected callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		Assert.assertTrue("Expected callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		Assert.assertTrue("Expected callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.disable(operation1);

		new VerificationsInOrder() {
			{
				connector.disableTagOperation(1L);
				times = 1;

				connector.disconnect();
				times = 0;
			}
		};

		readerController.undefine(operation1, "caller1");

		new VerificationsInOrder() {
			{
				connector.disconnect();
				times = 1;
			}
		};

		// try to send again
		callback1Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));

		// try undefine again
		readerController.undefine(operation1, "caller1");

		// should not call disconnect
		new VerificationsInOrder() {
			{
				connector.disconnect();
				times = 0;
			}
		};

		// now testing multiple callers and operations

		readerController.define(operation1, caller1, "caller1");

		new VerificationsInOrder() {
			{
				connector.connect();
				times = 1;

				connector.defineTagOperation(2L, withSameInstance(operation1));
				times = 1;

				connector.disconnect();
				times = 0;
			}
		};

		readerController.define(operation1, caller2, "caller2");

		new VerificationsInOrder() {
			{
				connector.connect();
				times = 0;

				connector.defineTagOperation(3L, withSameInstance(operation1));
				times = 0;

				connector.disconnect();
				times = 0;
			}
		};

		readerController.define(operation2, caller3, "caller3");

		new VerificationsInOrder() {
			{
				connector.connect();
				times = 0;

				connector.defineTagOperation(3L, withSameInstance(operation2));
				times = 1;

				connector.disconnect();
				times = 0;
			}
		};

		readerController.enable(operation1);

		new VerificationsInOrder() {
			{
				connector.enableTagOperation(2L);
				times = 1;

				connector.disconnect();
				times = 0;
			}
		};

		readerController.enable(operation2);

		new VerificationsInOrder() {
			{
				connector.enableTagOperation(3L);
				times = 1;

				connector.disconnect();
				times = 0;
			}
		};

		// send 3 results to caller 1 and 2
		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertTrue("Expected callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertTrue("Expected callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertTrue("Expected callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		// send 3 results to caller 3
		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.undefine(operation1, "caller1");

		new VerificationsInOrder() {
			{
				connector.disconnect();
				times = 0; // should not disconnect because of caller 2
			}
		};

		// send 3 results to caller 2
		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		// send 3 results to caller 3
		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.undefine(operation1, "caller2");

		new VerificationsInOrder() {
			{
				connector.disconnect();
				times = 0; // should not disconnect because of other operation
			}
		};

		// send 3 results to no caller
		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		// send 3 results to caller 3
		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.undefine(operation2, "caller3");

		new VerificationsInOrder() {
			{
				connector.disconnect();
				times = 1;
			}
		};

		// send 3 results to no caller
		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callback3Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(3L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback3Received.getValue().await(20, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testEqualTagOperation(@Mocked final ReaderConnector connector) throws Exception {
		final Map<String, String> properties = new HashMap<>();
		final ReaderController readerController = new ReaderController("reader", connector, properties);
		final TagOperation operation1 = new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(1, OperationType.READ,
				new byte[] { 0x01 })));
		final TagOperation operation2 = new TagOperation(Arrays.asList(new havis.middleware.ale.base.operation.tag.Operation(1, OperationType.READ,
				new byte[] { 0x01 })));
		final Tag tag = new Tag(new byte[] { 0x01 });
		tag.setTid(new byte[] { 0x01 });

		final ByRef<CountDownLatch> callback1Received = new ByRef<>(new CountDownLatch(1));
		Caller<Tag> caller1 = new Caller<Tag>() {
			@Override
			public void invoke(Tag t, ReaderController controller) {
				Assert.assertSame(readerController, controller);
				Assert.assertSame(tag, t);
				callback1Received.getValue().countDown();
			}
		};

		final ByRef<CountDownLatch> callback2Received = new ByRef<>(new CountDownLatch(1));
		Caller<Tag> caller2 = new Caller<Tag>() {
			@Override
			public void invoke(Tag t, ReaderController controller) {
				Assert.assertSame(readerController, controller);
				Assert.assertSame(tag, t);
				callback2Received.getValue().countDown();
			}
		};

		final ByRef<Callback> callbackHolder = new ByRef<>(null);
		new Verifications() {
			{
				Callback callback;
				connector.setCallback(callback = withCapture());
				callbackHolder.setValue(callback);
			}
		};

		readerController.define(operation1, caller1, "caller1");
		readerController.enable(operation1);

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertTrue("Expected callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.disable(operation1);

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.define(operation2, caller2, "caller2");
		readerController.enable(operation2);

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.enable(operation1);

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertTrue("Expected callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertTrue("Expected callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.disable(operation1);
		readerController.disable(operation2);

		callback1Received.setValue(new CountDownLatch(1));
		callback2Received.setValue(new CountDownLatch(1));
		callbackHolder.getValue().notify(1L, tag);
		callbackHolder.getValue().notify(2L, tag);
		Assert.assertFalse("Expected no callback", callback1Received.getValue().await(20, TimeUnit.MILLISECONDS));
		Assert.assertFalse("Expected no callback", callback2Received.getValue().await(20, TimeUnit.MILLISECONDS));

		readerController.undefine(operation1, "caller1");
		readerController.undefine(operation2, "caller2");
	}

	@Test
	public void update(@Mocked final ReaderConnector connector) throws Exception {
		Map<String, String> properties = new HashMap<>();
		ReaderController readerController = new ReaderController("reader", connector, properties);

		Assert.assertEquals(Integer.valueOf(3000), Deencapsulation.<Integer> getField(readerController, "executeTimeout"));
		Assert.assertEquals(Boolean.valueOf(true), Deencapsulation.<Boolean> getField(readerController, "optimizeWriteOperations"));

		readerController.update(properties);

		Assert.assertEquals(Integer.valueOf(3000), Deencapsulation.<Integer> getField(readerController, "executeTimeout"));
		Assert.assertEquals(Boolean.valueOf(true), Deencapsulation.<Boolean> getField(readerController, "optimizeWriteOperations"));

		properties = new HashMap<>();
		properties.put(Property.Controller.Timeout, "Test");
		try {
			readerController.update(properties);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		properties = new HashMap<>();
		properties.put(Property.Controller.OptimizeWriteOperations, "Test");
		try {
			readerController.update(properties);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		properties = new HashMap<>();
		properties.put(Property.Controller.OptimizeWriteOperations, "false");
		properties.put(Property.Controller.Timeout, "5000");
		readerController.update(properties);

		Assert.assertEquals(Integer.valueOf(5000), Deencapsulation.<Integer> getField(readerController, "executeTimeout"));
		Assert.assertEquals(Boolean.valueOf(false), Deencapsulation.<Boolean> getField(readerController, "optimizeWriteOperations"));

		properties = new HashMap<>();
		properties.put(Property.Controller.OptimizeWriteOperations, "FALSE");
		readerController.update(properties);

		Assert.assertEquals(Integer.valueOf(3000), Deencapsulation.<Integer> getField(readerController, "executeTimeout"));
		Assert.assertEquals(Boolean.valueOf(false), Deencapsulation.<Boolean> getField(readerController, "optimizeWriteOperations"));

		properties = new HashMap<>();
		readerController.update(properties);

		Assert.assertEquals(Integer.valueOf(3000), Deencapsulation.<Integer> getField(readerController, "executeTimeout"));
		Assert.assertEquals(Boolean.valueOf(true), Deencapsulation.<Boolean> getField(readerController, "optimizeWriteOperations"));

		properties = new HashMap<>();
		properties.put(Property.Controller.OptimizeWriteOperations, "true");
		readerController.update(properties);

		Assert.assertEquals(Integer.valueOf(3000), Deencapsulation.<Integer> getField(readerController, "executeTimeout"));
		Assert.assertEquals(Boolean.valueOf(true), Deencapsulation.<Boolean> getField(readerController, "optimizeWriteOperations"));

		properties = new HashMap<>();
		properties.put(Property.Controller.OptimizeWriteOperations, "TRUE");
		readerController.update(properties);

		Assert.assertEquals(Integer.valueOf(3000), Deencapsulation.<Integer> getField(readerController, "executeTimeout"));
		Assert.assertEquals(Boolean.valueOf(true), Deencapsulation.<Boolean> getField(readerController, "optimizeWriteOperations"));
	}
}
