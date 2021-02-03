package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.ByRef;
import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.port.result.Result.State;
import havis.middleware.ale.base.operation.port.result.WriteResult;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.cycle.PortCycle;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.report.pc.Operation;
import havis.middleware.ale.core.subscriber.Subscriber;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.pc.PCOpReport;
import havis.middleware.ale.service.pc.PCOpSpec;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.service.pc.PCSpec;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PCTest {

	@BeforeClass
	public static void init() {
		ConfigResetter.reset();
		ConfigResetter.disablePersistence();
	}

	@Before
	public void reset() {
		PC.getInstance().dispose();
	}

	@Test
	public void getInstance() {
		Assert.assertNotNull(PC.getInstance());
	}

	@Test
	public void define(@Mocked final PortCycle portCycle, @Mocked final havis.middleware.ale.core.depot.service.pc.PortCycle depot) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();
		pc.define("name", spec, true);
		Assert.assertEquals("name", pc.getNames().get(0));

		new Verifications() {
			{
				depot.add(withEqual("name"), withEqual(spec));
				times = 1;

				PortCycle cycle = new PortCycle(withEqual("name"), withEqual(spec));
				times = 1;

				cycle.start();
				times = 1;
			}
		};
	}

	@Test
	public void defineDuplicateName(@Mocked final PortCycle portCycle) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();
		pc.define("name", spec, false);

		try {
			pc.define("name", new PCSpec(), false);
			Assert.fail("Expected DuplicateNameException");
		} catch (DuplicateNameException e) {
			// ignore
		}
	}

	@Test(expected = ValidationException.class)
	public void defineNoSpec() throws ValidationException, DuplicateNameException, ImplementationException {
		PC.getInstance().define("name", null, false);
	}

	@Test
	public void undefine(@Mocked final PortCycle portCycle, @Mocked final havis.middleware.ale.core.depot.service.pc.PortCycle depot) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();
		pc.define("name", spec, true);
		Assert.assertEquals("name", pc.getNames().get(0));

		pc.undefine("name", true);

		Assert.assertEquals(0, pc.getNames().size());

		new Verifications() {
			{
				depot.add(withEqual("name"), withEqual(spec));
				times = 1;

				PortCycle cycle = new PortCycle(withEqual("name"), withEqual(spec));
				times = 1;

				cycle.start();
				times = 1;

				cycle.dispose();
				times = 1;

				depot.remove(withEqual("name"));
				times = 1;
			}
		};
	}

	@Test
	public void undefineUndefined() throws NoSuchNameException {
		try {
			PC.getInstance().undefine("nonexistent", false);
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void getSpec(@Mocked final PortCycle portCycle) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;

				portCycle.getSpec();
				result = spec;
			}
		};

		pc.define("name", spec, false);

		Assert.assertEquals("name", pc.getNames().get(0));

		Assert.assertSame(spec, pc.getSpec("name"));
	}

	@Test
	public void getSpecNoSuchName() throws NoSuchNameException {
		try {
			PC.getInstance().getSpec("nonexistent");
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void getNames(@Mocked final PortCycle portCycle) throws Exception {
		PC pc = PC.getInstance();
		Assert.assertEquals(Arrays.asList(), pc.getNames());

		pc.define("name1", new PCSpec(), false);
		pc.define("name2", new PCSpec(), false);
		pc.define("name3", new PCSpec(), false);

		Assert.assertEquals(Arrays.asList("name1", "name2", "name3"), pc.getNames());
	}

	@Test
	public void subscribe(final @Mocked PortCycle portCycle, @Mocked final havis.middleware.ale.core.depot.service.pc.PortCycle depot,
			final @Mocked Subscriber subscriber, final @Mocked SubscriberController controller) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();
		final PropertiesType props = new PropertiesType();

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;

				portCycle.exists(withEqual(new URI("test://1")));
				result = Boolean.FALSE;

				Subscriber.getInstance();
				result = subscriber;

				subscriber.get(withEqual(new URI("test://1")), props, ECReports.class);
				result = controller;

			}
		};

		pc.define("name", spec, false);
		pc.subscribe("name", "test://1", props, true);

		new Verifications() {
			{
				PortCycle cycle = new PortCycle(withEqual("name"), withEqual(spec));
				times = 1;

				cycle.start();
				times = 1;

				cycle.exists(withEqual(new URI("test://1")));
				times = 1;

				cycle.add(withEqual(controller));
				times = 1;

				depot.add(withEqual("name"), withEqual(new URI("test://1")));
				times = 1;
			}
		};
	}

	@Test
	public void subscribeDuplicateName(@Mocked final PortCycle portCycle, @Mocked final havis.middleware.ale.core.depot.service.pc.PortCycle depot)
			throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;

				portCycle.exists(withEqual(new URI("test://1")));
				result = Boolean.TRUE;
			}
		};

		pc.define("name", spec, false);
		try {
			pc.subscribe("name", "test://1", null, false);
			Assert.fail("Expected DuplicateSubscriptionException");
		} catch (DuplicateSubscriptionException e) {
			// ignore
		}

		new Verifications() {
			{
				PortCycle cycle = new PortCycle(withEqual("name"), withEqual(spec));
				times = 1;

				cycle.start();
				times = 1;

				cycle.exists(withEqual(new URI("test://1")));
				times = 1;
			}
		};
	}

	@Test(expected = NoSuchNameException.class)
	public void subscribeNoSuchNameExceptionTest(@Mocked final PortCycle portCycle) throws Exception {
		PC.getInstance().subscribe("name", "test://1", null, false);
	}

	@Test
	public void subscribeInvalidURIExceptionTest(@Mocked final PortCycle portCycle) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;
			}
		};

		pc.define("name", spec, false);

		try {
			pc.subscribe("name", ":invalid:", null, false);
			Assert.fail("Expected InvalidURIException");
		} catch (InvalidURIException e) {
			// ignore
		}
	}

	@Test
	public void unsubscribe(@Mocked final PortCycle portCycle, @Mocked final havis.middleware.ale.core.depot.service.pc.PortCycle depot,
			final @Mocked Subscriber subscriber, final @Mocked SubscriberController controller) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;

				portCycle.exists(withEqual(new URI("test://1")));
				result = Boolean.FALSE;

				Subscriber.getInstance();
				result = subscriber;

				subscriber.get(withEqual(new URI("test://1")), null, ECReports.class);
				result = controller;

				portCycle.find(withEqual(new URI("test://1")));
				result = controller;
			}
		};

		pc.define("name", spec, false);
		pc.subscribe("name", "test://1", null, false);

		pc.unsubscribe("name", "test://1", true);

		new Verifications() {
			{
				PortCycle cycle = new PortCycle(withEqual("name"), withEqual(spec));
				times = 1;

				cycle.start();
				times = 1;

				cycle.exists(withEqual(new URI("test://1")));
				times = 1;

				cycle.add(withEqual(controller));
				times = 1;

				cycle.remove(withEqual(controller));
				times = 1;

				depot.remove(withEqual("name"), withEqual(new URI("test://1")));
				times = 1;
			}
		};
	}

	@Test
	public void unsubscribeNoSuchSubscriber(@Mocked final PortCycle portCycle, @Mocked final havis.middleware.ale.core.depot.service.pc.PortCycle depot)
			throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;

				portCycle.find(withEqual(new URI("test://1")));
				result = null;
			}
		};

		pc.define("name", spec, false);
		try {
			pc.unsubscribe("name", "test://1", false);
			Assert.fail("Expected NoSuchSubscriberException");
		} catch (NoSuchSubscriberException e) {
			// ignore
		}
	}

	@Test(expected = NoSuchNameException.class)
	public void unsubscribeNoSuchNameExceptionTest(@Mocked final PortCycle portCycle) throws Exception {
		PC.getInstance().unsubscribe("name", "test://1", false);
	}

	@Test
	public void unsubscribeInvalidURIExceptionTest(@Mocked final PortCycle portCycle) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;
			}
		};

		pc.define("name", spec, false);

		try {
			pc.unsubscribe("name", ":invalid:", false);
			Assert.fail("Expected InvalidURIException");
		} catch (InvalidURIException e) {
			// ignore
		}
	}

	@Test
	public void poll(@Mocked final PortCycle portCycle, @Mocked final SubscriberListener<PCReports> subscriber) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();
		final PCReports report = new PCReports();

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;

				new SubscriberListener<PCReports>(1);
				result = subscriber;

				subscriber.dequeue();
				result = report;
			}
		};

		pc.define("name", spec, false);

		Assert.assertSame(report, pc.poll("name"));

		new Verifications() {
			{
				portCycle.add(withEqual(subscriber));
				times = 1;

				subscriber.dequeue();
				times = 1;
			}
		};
	}

	@Test
	public void pollNoSuchName() throws Exception {
		try {
			PC.getInstance().poll("nonexistent");
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void immediate(@Mocked final PortCycle portCycle, @Mocked final SubscriberListener<PCReports> subscriber) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();
		final PCReports report = new PCReports();

		new NonStrictExpectations() {
			{
				new SubscriberListener<PCReports>(1);
				result = subscriber;

				new PortCycle(null, withEqual(spec));
				result = portCycle;

				subscriber.dequeue();
				result = report;
			}
		};

		Assert.assertSame(report, pc.immediate(spec));

		new VerificationsInOrder() {
			{
				portCycle.start();
				times = 1;

				portCycle.add(withEqual(subscriber));
				times = 1;

				subscriber.dequeue();
				times = 1;

				portCycle.dispose();
				times = 1;
			}
		};
	}

	@Test
	public void immediateDispose(@Mocked final PortCycle portCycle) throws Exception {
		final PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();
		final PCReports report = new PCReports();
		final CountDownLatch ready = new CountDownLatch(1);
		final CountDownLatch sendReport = new CountDownLatch(1);

		MockUp<SubscriberListener<PCReports>> mockUp = new MockUp<SubscriberListener<PCReports>>() {
			@Mock
			PCReports dequeue() {
				try {
					sendReport.await();
				} catch (InterruptedException e) {
				}
				return report;
			}
		};
		try {
			new NonStrictExpectations() {
				{
					new PortCycle(null, withEqual(spec));
					result = portCycle;
				}
			};

			ExecutorService excecutor = Executors.newSingleThreadExecutor();
			Future<PCReports> future = excecutor.submit(new Callable<PCReports>() {
				@Override
				public PCReports call() throws Exception {
					ready.countDown();
					return pc.immediate(spec);
				}
			});

			Assert.assertTrue(ready.await(100, TimeUnit.MILLISECONDS));
			Thread.sleep(50);
			Assert.assertFalse(future.isDone());

			pc.dispose();
			sendReport.countDown();

			Assert.assertSame(future.get(), pc.immediate(spec));

			new VerificationsInOrder() {
				{
					portCycle.start();
					times = 1;

					portCycle.add(this.<SubscriberListener<PCReports>> withNotNull());
					times = 1;

					portCycle.dispose();
					times = 2;
				}
			};
		} finally {
			mockUp.tearDown();
		}
	}

	@Test
	public void immediateImplementationException(@Mocked final PortCycle portCycle, @Mocked final SubscriberListener<PCReports> subscriber) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();

		new NonStrictExpectations() {
			{
				new SubscriberListener<PCReports>(1);
				result = subscriber;

				new PortCycle(null, withEqual(spec));
				result = new ImplementationException();
			}
		};

		try {
			pc.immediate(spec);
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}
	}

	@Test
	public void immediateException(@Mocked final PortCycle portCycle, @Mocked final SubscriberListener<PCReports> subscriber) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();

		new NonStrictExpectations() {
			{
				new SubscriberListener<PCReports>(1);
				result = subscriber;

				new PortCycle(null, withEqual(spec));
				result = new ValidationException();
			}
		};

		try {
			pc.immediate(spec);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void getSubscribers(@Mocked final PortCycle portCycle) throws Exception {
		PC pc = PC.getInstance();
		final PCSpec spec = new PCSpec();
		final List<String> subscribers = new ArrayList<>(Arrays.asList("sub1", "sub2"));

		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name"), withEqual(spec));
				result = portCycle;

				portCycle.getSubscribers();
				result = subscribers;
			}
		};

		pc.define("name", spec, false);

		Assert.assertSame(subscribers, pc.getSubscribers("name"));
	}

	@Test
	public void getSubscribersNoSuchName(@Mocked final PortCycle portCycle) throws Exception {
		PC pc = PC.getInstance();

		try {
			pc.getSubscribers("name");
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void execute(@Mocked final PortCycle portCycle, @Mocked final Operation operation) throws Exception {
		final List<PCOpSpec> specs = new ArrayList<>(Arrays.asList(new PCOpSpec()));
		final List<Operation> operations = Arrays.asList(operation);
		final havis.middleware.ale.base.operation.port.Operation portOperation = new havis.middleware.ale.base.operation.port.Operation("op",
				havis.middleware.ale.base.operation.port.Operation.Type.WRITE, Byte.valueOf((byte) 0x01), Long.valueOf(1000), new Pin(1, Pin.Type.OUTPUT));
		final Result writeResult = new WriteResult(State.SUCCESS);
		final PCOpReport report = new PCOpReport();
		new NonStrictExpectations() {
			{
				Operation.get(withEqual(specs));
				result = operations;

				operation.getPortOperation();
				result = portOperation;

				operation.getReport(withEqual(writeResult));
				result = report;
			}
		};

		final ByRef<List<PCOpReport>> executeResult = new ByRef<List<PCOpReport>>(null);
		final CountDownLatch ready = new CountDownLatch(1);
		final CountDownLatch done = new CountDownLatch(1);

		Thread executeThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ready.countDown();
					executeResult.setValue(PC.getInstance().execute(specs));
				} catch (ValidationException e) {
					Assert.fail(e.getMessage());
				} finally {
					done.countDown();
				}
			}
		});
		executeThread.start();

		// wait for execute to be called
		Assert.assertTrue("Expect execute thread to be ready", ready.await(100, TimeUnit.MILLISECONDS));
		Thread.sleep(25);

		Assert.assertTrue("Expect execute to be waiting for reader result", executeThread.isAlive());

		final ByRef<Caller<Port>> callbackHolder = new ByRef<Caller<Port>>(null);
		new Verifications() {
			{
				Caller<Port> callback;
				PortCycle.execute(withEqual(operations), callback = withCapture());
				times = 1;

				Assert.assertNotNull(callback);
				callbackHolder.setValue(callback);
			}
		};

		// send the result from the reader
		HashMap<Integer, Result> result = new HashMap<Integer, Result>();
		result.put(Integer.valueOf(1), writeResult);
		callbackHolder.getValue().invoke(new Port(result), null);

		// wait for the execute to finish
		Assert.assertTrue("Expect execute to be finished", done.await(100, TimeUnit.MILLISECONDS));
		executeThread.join(25);

		Assert.assertFalse("Expect execute to be finished", executeThread.isAlive());

		Assert.assertNotNull(executeResult.getValue());
		Assert.assertEquals(1, executeResult.getValue().size());
		Assert.assertSame(report, executeResult.getValue().get(0));

		new Verifications() {
			{
				operation.dispose();
				times = 1;
			}
		};
	}

	@Test
	public void executeNoSpecs(@Mocked final PortCycle portCycle, @Mocked final Operation operation) throws Exception {
		try {
			PC.getInstance().execute(null);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
		try {
			PC.getInstance().execute(new ArrayList<PCOpSpec>());
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void getStandardVersionTest() {
		Assert.assertEquals("1.1", PC.getStandardVersion());
	}

	@Test
	public void dispose(@Mocked final PortCycle portCycle1, @Mocked final PortCycle portCycle2, @Mocked final PortCycle portCycle3) throws Exception {
		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name1"), this.<PCSpec> withNotNull());
				result = portCycle1;

				new PortCycle(withEqual("name2"), this.<PCSpec> withNotNull());
				result = portCycle2;

				new PortCycle(withEqual("name3"), this.<PCSpec> withNotNull());
				result = portCycle3;
			}
		};

		PC pc = PC.getInstance();
		pc.define("name1", new PCSpec(), false);
		pc.define("name2", new PCSpec(), false);
		pc.define("name3", new PCSpec(), false);

		Assert.assertEquals(Arrays.asList("name1", "name2", "name3"), pc.getNames());

		pc.dispose();

		Assert.assertEquals(Arrays.asList(), pc.getNames());

		new VerificationsInOrder() {
			{
				portCycle1.dispose();
				times = 1;

				portCycle2.dispose();
				times = 1;

				portCycle3.dispose();
				times = 1;
			}
		};
	}

	@Test
	public void disposeError(@Mocked final PortCycle portCycle1, @Mocked final PortCycle portCycle2, @Mocked final PortCycle portCycle3) throws Exception {
		new NonStrictExpectations() {
			{
				new PortCycle(withEqual("name1"), this.<PCSpec> withNotNull());
				result = portCycle1;

				new PortCycle(withEqual("name2"), this.<PCSpec> withNotNull());
				result = portCycle2;

				new PortCycle(withEqual("name3"), this.<PCSpec> withNotNull());
				result = portCycle3;

				portCycle1.dispose();
				result = new IllegalStateException();
			}
		};

		PC pc = PC.getInstance();
		pc.define("name1", new PCSpec(), false);
		pc.define("name2", new PCSpec(), false);
		pc.define("name3", new PCSpec(), false);

		Assert.assertEquals(Arrays.asList("name1", "name2", "name3"), pc.getNames());

		pc.dispose();

		Assert.assertEquals(Arrays.asList(), pc.getNames());

		new VerificationsInOrder() {
			{
				portCycle1.dispose();
				times = 1;

				portCycle2.dispose();
				times = 1;

				portCycle3.dispose();
				times = 1;
			}
		};
	}
}
