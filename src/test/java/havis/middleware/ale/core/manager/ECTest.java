package havis.middleware.ale.core.manager;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.config.PropertiesType;
import havis.middleware.ale.core.config.ConfigResetter;
import havis.middleware.ale.core.cycle.EventCycle;
import havis.middleware.ale.core.subscriber.Subscriber;
import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.core.subscriber.SubscriberListener;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.ec.ECSpec;
import havis.middleware.ale.service.ec.ECTagCountStat;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

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

public class ECTest {

	@BeforeClass
	public static void init() {
		ConfigResetter.reset();
		ConfigResetter.disablePersistence();
	}

	@Before
	public void reset() {
		EC.getInstance().dispose();
	}

	@Test
	public void getInstance() {
		Assert.assertNotNull(EC.getInstance());
	}

	@Test
	public void define(@Mocked final EventCycle eventCycle, @Mocked final havis.middleware.ale.core.depot.service.ec.EventCycle depot) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();
		ec.define("name", spec, true);
		Assert.assertEquals("name", ec.getNames().get(0));

		new Verifications() {
			{
				depot.add(withEqual("name"), withEqual(spec));
				times = 1;

				EventCycle cycle = new EventCycle(withEqual("name"), withEqual(spec));
				times = 1;

				cycle.start();
				times = 1;
			}
		};
	}

	@Test
	public void defineDuplicateName(@Mocked final EventCycle eventCycle) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();
		ec.define("name", spec, false);

		try {
			ec.define("name", new ECSpec(), false);
			Assert.fail("Expected DuplicateNameException");
		} catch (DuplicateNameException e) {
			// ignore
		}
	}

	@Test(expected = ValidationException.class)
	public void defineNoSpec() throws Exception {
		EC.getInstance().define("name", null, false);
	}

	@Test
	public void undefine(@Mocked final EventCycle eventCycle, @Mocked final havis.middleware.ale.core.depot.service.ec.EventCycle depot) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();
		ec.define("name", spec, true);
		Assert.assertEquals("name", ec.getNames().get(0));

		ec.undefine("name", true);

		Assert.assertEquals(0, ec.getNames().size());

		new Verifications() {
			{
				depot.add(withEqual("name"), withEqual(spec));
				times = 1;

				EventCycle cycle = new EventCycle(withEqual("name"), withEqual(spec));
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
			EC.getInstance().undefine("nonexistent", false);
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void getSpec(@Mocked final EventCycle eventCycle) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;

				eventCycle.getSpec();
				result = spec;
			}
		};

		ec.define("name", spec, false);

		Assert.assertEquals("name", ec.getNames().get(0));

		Assert.assertSame(spec, ec.getSpec("name"));
	}

	@Test
	public void getSpecNoSuchName() throws NoSuchNameException {
		try {
			EC.getInstance().getSpec("nonexistent");
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void getNames(@Mocked final EventCycle eventCycle) throws Exception {
		EC ec = EC.getInstance();
		Assert.assertEquals(Arrays.asList(), ec.getNames());

		ec.define("name1", new ECSpec(), false);
		ec.define("name2", new ECSpec(), false);
		ec.define("name3", new ECSpec(), false);

		Assert.assertEquals(Arrays.asList("name1", "name2", "name3"), ec.getNames());
	}

	@Test
	public void subscribe(final @Mocked EventCycle eventCycle, @Mocked final havis.middleware.ale.core.depot.service.ec.EventCycle depot,
			final @Mocked Subscriber subscriber, final @Mocked SubscriberController controller) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();
		final PropertiesType props = new PropertiesType();

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;

				eventCycle.exists(withEqual(new URI("test://1")));
				result = Boolean.FALSE;

				Subscriber.getInstance();
				result = subscriber;

				subscriber.get(withEqual(new URI("test://1")), props, ECReports.class);
				result = controller;

			}
		};

		ec.define("name", spec, false);
		ec.subscribe("name", "test://1", props, true);

		new Verifications() {
			{
				EventCycle cycle = new EventCycle(withEqual("name"), withEqual(spec));
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
	public void subscribeDuplicateName(@Mocked final EventCycle eventCycle, @Mocked final havis.middleware.ale.core.depot.service.ec.EventCycle depot)
			throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;

				eventCycle.exists(withEqual(new URI("test://1")));
				result = Boolean.TRUE;
			}
		};

		ec.define("name", spec, false);
		try {
			ec.subscribe("name", "test://1", null, false);
			Assert.fail("Expected DuplicateSubscriptionException");
		} catch (DuplicateSubscriptionException e) {
			// ignore
		}

		new Verifications() {
			{
				EventCycle cycle = new EventCycle(withEqual("name"), withEqual(spec));
				times = 1;

				cycle.start();
				times = 1;

				cycle.exists(withEqual(new URI("test://1")));
				times = 1;
			}
		};
	}

	@Test(expected = NoSuchNameException.class)
	public void subscribeNoSuchNameExceptionTest(@Mocked final EventCycle eventCycle) throws Exception {
		EC.getInstance().subscribe("name", "test://1", null, false);
	}

	@Test
	public void subscribeInvalidURIExceptionTest(@Mocked final EventCycle eventCycle) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;
			}
		};

		ec.define("name", spec, false);

		try {
			ec.subscribe("name", ":invalid:", null, false);
			Assert.fail("Expected InvalidURIException");
		} catch (InvalidURIException e) {
			// ignore
		}
	}

	@Test
	public void unsubscribe(@Mocked final EventCycle eventCycle, @Mocked final havis.middleware.ale.core.depot.service.ec.EventCycle depot,
			final @Mocked Subscriber subscriber, final @Mocked SubscriberController controller) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;

				eventCycle.exists(withEqual(new URI("test://1")));
				result = Boolean.FALSE;

				Subscriber.getInstance();
				result = subscriber;

				subscriber.get(withEqual(new URI("test://1")), null, ECReports.class);
				result = controller;

				eventCycle.find(withEqual(new URI("test://1")));
				result = controller;
			}
		};

		ec.define("name", spec, false);
		ec.subscribe("name", "test://1", null, false);

		ec.unsubscribe("name", "test://1", true);

		new Verifications() {
			{
				EventCycle cycle = new EventCycle(withEqual("name"), withEqual(spec));
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
	public void unsubscribeNoSuchSubscriber(@Mocked final EventCycle eventCycle, @Mocked final havis.middleware.ale.core.depot.service.ec.EventCycle depot)
			throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;

				eventCycle.find(withEqual(new URI("test://1")));
				result = null;
			}
		};

		ec.define("name", spec, false);
		try {
			ec.unsubscribe("name", "test://1", false);
			Assert.fail("Expected NoSuchSubscriberException");
		} catch (NoSuchSubscriberException e) {
			// ignore
		}
	}

	@Test(expected = NoSuchNameException.class)
	public void unsubscribeNoSuchNameExceptionTest(@Mocked final EventCycle eventCycle) throws Exception {
		EC.getInstance().unsubscribe("name", "test://1", false);
	}

	@Test
	public void unsubscribeInvalidURIExceptionTest(@Mocked final EventCycle eventCycle) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;
			}
		};

		ec.define("name", spec, false);

		try {
			ec.unsubscribe("name", ":invalid:", false);
			Assert.fail("Expected InvalidURIException");
		} catch (InvalidURIException e) {
			// ignore
		}
	}

	@Test
	public void poll(@Mocked final EventCycle eventCycle, @Mocked final SubscriberListener<ECReports> subscriber) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();
		final ECReports report = new ECReports();

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;

				new SubscriberListener<ECReports>(1);
				result = subscriber;

				subscriber.dequeue();
				result = report;
			}
		};

		ec.define("name", spec, false);

		Assert.assertSame(report, ec.poll("name"));

		new Verifications() {
			{
				eventCycle.add(withEqual(subscriber));
				times = 1;

				subscriber.dequeue();
				times = 1;
			}
		};
	}

	@Test
	public void pollNoSuchName() throws Exception {
		try {
			EC.getInstance().poll("nonexistent");
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void immediate(@Mocked final EventCycle eventCycle, @Mocked final SubscriberListener<ECReports> subscriber) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();
		final ECReports report = new ECReports();

		new NonStrictExpectations() {
			{
				new SubscriberListener<ECReports>(1);
				result = subscriber;

				new EventCycle(null, withEqual(spec));
				result = eventCycle;

				subscriber.dequeue();
				result = report;
			}
		};

		Assert.assertSame(report, ec.immediate(spec));

		new VerificationsInOrder() {
			{
				eventCycle.start();
				times = 1;

				eventCycle.add(withEqual(subscriber));
				times = 1;

				subscriber.dequeue();
				times = 1;

				eventCycle.dispose();
				times = 1;
			}
		};
	}

	@Test
	public void immediateDispose(@Mocked final EventCycle eventCycle) throws Exception {
		final EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();
		final ECReports report = new ECReports();
		final CountDownLatch ready = new CountDownLatch(1);
		final CountDownLatch sendReport = new CountDownLatch(1);

		MockUp<SubscriberListener<ECReports>> mockUp = new MockUp<SubscriberListener<ECReports>>() {
			@Mock
			ECReports dequeue() {
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
					new EventCycle(null, withEqual(spec));
					result = eventCycle;
				}
			};

			ExecutorService excecutor = Executors.newSingleThreadExecutor();
			Future<ECReports> future = excecutor.submit(new Callable<ECReports>() {
				@Override
				public ECReports call() throws Exception {
					ready.countDown();
					return ec.immediate(spec);
				}
			});

			Assert.assertTrue(ready.await(100, TimeUnit.MILLISECONDS));
			Thread.sleep(50);
			Assert.assertFalse(future.isDone());

			ec.dispose();
			sendReport.countDown();

			Assert.assertSame(future.get(), ec.immediate(spec));

			new VerificationsInOrder() {
				{
					eventCycle.start();
					times = 1;

					eventCycle.add(this.<SubscriberListener<ECReports>> withNotNull());
					times = 1;

					eventCycle.dispose();
					times = 2;
				}
			};
		} finally {
			mockUp.tearDown();
		}
	}

	@Test
	public void immediateImplementationException(@Mocked final EventCycle eventCycle, @Mocked final SubscriberListener<ECReports> subscriber) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();

		new NonStrictExpectations() {
			{
				new SubscriberListener<ECReports>(1);
				result = subscriber;

				new EventCycle(null, withEqual(spec));
				result = new ImplementationException();
			}
		};

		try {
			ec.immediate(spec);
			Assert.fail("Expected ImplementationException");
		} catch (ImplementationException e) {
			// ignore
		}
	}

	@Test
	public void immediateException(@Mocked final EventCycle eventCycle, @Mocked final SubscriberListener<ECReports> subscriber) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();

		new NonStrictExpectations() {
			{
				new SubscriberListener<ECReports>(1);
				result = subscriber;

				new EventCycle(null, withEqual(spec));
				result = new ValidationException();
			}
		};

		try {
			ec.immediate(spec);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}
	}

	@Test
	public void getSubscribers(@Mocked final EventCycle eventCycle) throws Exception {
		EC ec = EC.getInstance();
		final ECSpec spec = new ECSpec();
		final List<String> subscribers = new ArrayList<>(Arrays.asList("sub1", "sub2"));

		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name"), withEqual(spec));
				result = eventCycle;

				eventCycle.getSubscribers();
				result = subscribers;
			}
		};

		ec.define("name", spec, false);

		Assert.assertSame(subscribers, ec.getSubscribers("name"));
	}

	@Test
	public void getSubscribersNoSuchName(@Mocked final EventCycle eventCycle) throws Exception {
		EC ec = EC.getInstance();

		try {
			ec.getSubscribers("name");
			Assert.fail("Expected NoSuchNameException");
		} catch (NoSuchNameException e) {
			// ignore
		}
	}

	@Test
	public void getStandardVersionTest() {
		Assert.assertEquals("1.1", EC.getStandardVersion());
	}

	@Test
	public void dispose(@Mocked final EventCycle eventCycle1, @Mocked final EventCycle eventCycle2, @Mocked final EventCycle eventCycle3) throws Exception {
		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name1"), this.<ECSpec> withNotNull());
				result = eventCycle1;

				new EventCycle(withEqual("name2"), this.<ECSpec> withNotNull());
				result = eventCycle2;

				new EventCycle(withEqual("name3"), this.<ECSpec> withNotNull());
				result = eventCycle3;
			}
		};

		EC ec = EC.getInstance();
		ec.define("name1", new ECSpec(), false);
		ec.define("name2", new ECSpec(), false);
		ec.define("name3", new ECSpec(), false);

		Assert.assertEquals(Arrays.asList("name1", "name2", "name3"), ec.getNames());

		ec.dispose();

		Assert.assertEquals(Arrays.asList(), ec.getNames());

		new VerificationsInOrder() {
			{
				eventCycle1.dispose();
				times = 1;

				eventCycle2.dispose();
				times = 1;

				eventCycle3.dispose();
				times = 1;
			}
		};
	}

	@Test
	public void disposeError(@Mocked final EventCycle eventCycle1, @Mocked final EventCycle eventCycle2, @Mocked final EventCycle eventCycle3) throws Exception {
		new NonStrictExpectations() {
			{
				new EventCycle(withEqual("name1"), this.<ECSpec> withNotNull());
				result = eventCycle1;

				new EventCycle(withEqual("name2"), this.<ECSpec> withNotNull());
				result = eventCycle2;

				new EventCycle(withEqual("name3"), this.<ECSpec> withNotNull());
				result = eventCycle3;

				eventCycle1.dispose();
				result = new IllegalStateException();
			}
		};

		EC ec = EC.getInstance();
		ec.define("name1", new ECSpec(), false);
		ec.define("name2", new ECSpec(), false);
		ec.define("name3", new ECSpec(), false);

		Assert.assertEquals(Arrays.asList("name1", "name2", "name3"), ec.getNames());

		ec.dispose();

		Assert.assertEquals(Arrays.asList(), ec.getNames());

		new VerificationsInOrder() {
			{
				eventCycle1.dispose();
				times = 1;

				eventCycle2.dispose();
				times = 1;

				eventCycle3.dispose();
				times = 1;
			}
		};
	}

	// @Test
	public void testPoll() throws JAXBException, IOException {
		String xml = "<ECReports xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" schemaVersion=\"1.1\" creationDate=\"2016-01-29T10:52:09.7448691+01:00\" specName=\"ec001_0ed250c4b67d472882a099d8fdee318b\" date=\"2016-01-29T10:52:09.7448691+01:00\" ALEID=\"Ha-VIS Middleware\" totalMilliseconds=\"1015\" initiationCondition=\"REQUESTED\" terminationCondition=\"DURATION\" xmlns=\"urn:epcglobal:ale:wsdl:1\" xmlns:ale=\"urn:epcglobal:ale:xsd:1\"><reports xmlns=\"\"><report reportName=\"Report\"><group><groupList><member><epc>urn:epc:id:sgtin:426025746.0001.1</epc><extension><stats><stat xsi:type=\"q1:ECTagCountStat\" xmlns:q1=\"urn:havis:ale:xsd:1\"><profile>TagCount</profile><count>12</count></stat></stats></extension></member></groupList></group></report></reports></ECReports>";
		JAXBContext jaxbContext = JAXBContext.newInstance(ECReports.class);
		StringReader reader = new StringReader(xml);
		// Unmarshalling
		ECReports value = jaxbContext.createUnmarshaller().unmarshal(new StreamSource(reader), ECReports.class).getValue();

		ECTagCountStat element = new ECTagCountStat();
		element.setProfile("TagCount");
		element.setCount(15);
		value.getReports().getReport().get(0).getGroup().get(0).getGroupList().getMember().get(0).getExtension().getStats().getStat().set(0, element);

		Marshaller marshaller = jaxbContext.createMarshaller();
		StringWriter stringWriter = new StringWriter();
		QName qName = new QName("", "ECReports");
		@SuppressWarnings({ "rawtypes", "unchecked" })
		JAXBElement<?> root = new JAXBElement(qName, ECReports.class, value);
		marshaller.marshal(root, stringWriter);
		String result = stringWriter.toString();
		stringWriter.close();
		System.out.println(result);

	}
}