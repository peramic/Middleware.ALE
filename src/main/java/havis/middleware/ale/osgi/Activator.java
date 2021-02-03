package havis.middleware.ale.osgi;

import havis.middleware.ale.Connector;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.core.doc.ClasspathDocumentService;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.subscriber.MessengerSubscriberConnector;
import havis.middleware.ale.host.Main;
import havis.middleware.ale.reader.ImmutableReaderConnector;
import havis.middleware.ale.service.doc.DocumentService;
import havis.middleware.ale.service.mc.MC;
import havis.middleware.ale.subscriber.SubscriberConnector;
import havis.transport.Transporter;
import havis.util.monitor.Broker;
import havis.util.monitor.Event;
import havis.util.monitor.Source;

import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private final static Logger log = Logger.getLogger(Activator.class.getName());
	private ServiceRegistration<Registry> registry;
	private ServiceRegistration<MC> mc;
	private ServiceRegistration<DocumentService> doc;
	private ServiceTracker<ImmutableReaderConnector, ImmutableReaderConnector> immutableReaderTracker;
	private Main main = new Main();

	@Override
	public void start(final BundleContext context) throws Exception {
		long start = System.currentTimeMillis();

		// create connector factory
		Connector.createFactory(new Connector() {

			private Broker broker = null;
			private Lock lock = new ReentrantLock();

			@Override
			public <S> S newInstance(Class<S> clazz, String type) throws ImplementationException {
				try {
					if (SubscriberConnector.class.equals(clazz) && getTypes(Transporter.class).contains(type)) {
						@SuppressWarnings("unchecked")
						S messenger = (S) new MessengerSubscriberConnector();
						return messenger;
					}
					for (ServiceReference<S> reference : context.getServiceReferences(clazz, "(name=" + type + ")")) {
						ServiceObjects<S> objects = context.getServiceObjects(reference);
						if (objects != null) {
							return objects.getService();
						}
					}
					return null;
				} catch (InvalidSyntaxException e) {
					throw new ImplementationException(e.getMessage());
				}
			}

			@Override
			public <S> List<String> getTypes(Class<S> clazz) throws ImplementationException {
				List<String> types = new ArrayList<>();
				try {
					if (SubscriberConnector.class.equals(clazz)) {
						addTypes(context, Transporter.class, types);
					}
					addTypes(context, clazz, types);
				} catch (InvalidSyntaxException e) {
					throw new ImplementationException(e.getMessage());
				}
				return types;
			}

			private <S> void addTypes(final BundleContext context, Class<S> clazz, List<String> types) throws InvalidSyntaxException {
				for (ServiceReference<S> reference : context.getServiceReferences(clazz, null)) {
					Object type = reference.getProperty("name");
					if (type instanceof String) {
						String name = (String) type;
						if (!types.contains(name)) {
							types.add(name);
						}
					}
				}
			}

			@Override
			public Broker getBroker() {
				if (broker == null) {
					lock.lock();
					try {
						if (broker == null) {
							try {
								for (ServiceReference<Broker> reference : context.getServiceReferences(Broker.class, null)) {
									ServiceObjects<Broker> objects = context.getServiceObjects(reference);
									if (objects != null) {
										broker = objects.getService();
										break;
									}
								}
							} catch (InvalidSyntaxException e) {
								// ignore
							}
						}
					} finally {
						lock.unlock();
					}
				}
				if (broker == null) {
					return new Broker() {
						@Override
						public void notify(Source arg0, Event arg1) {
						}
					};
				}
				return broker;
			}
		});

		// preloading
		main.init(context.getBundle().getSymbolicName());

		// create and open immutable reader connector tracker
		immutableReaderTracker = new ServiceTracker<ImmutableReaderConnector, ImmutableReaderConnector>(context, ImmutableReaderConnector.class, null) {
			@Override
			public ImmutableReaderConnector addingService(ServiceReference<ImmutableReaderConnector> reference) {
				ImmutableReaderConnector service = super.addingService(reference);
				log.log(Level.FINE, "Adding immutable reader {0}.", service.getClass().getName());
				try {
					LR.getInstance().add((String) reference.getProperty("name"), service);
				} catch (ALEException e) {
					log.log(Level.SEVERE, "Failed to add immutable reader", e);
				}
				return service;
			}

			@Override
			public void removedService(ServiceReference<ImmutableReaderConnector> reference, ImmutableReaderConnector service) {
				log.log(Level.FINE, "Removing immutable reader {0}.", service.getClass().getName());
				try {
					LR.getInstance().remove((String) reference.getProperty("name"));
				} catch (ImplementationException e) {
					log.log(Level.SEVERE, "Failed to remove immutable reader", e);
				}
				super.removedService(reference, service);
			}
		};

		log.log(Level.FINE, "Opening tracker {0}.", immutableReaderTracker.getClass().getName());
		immutableReaderTracker.open();

		// start main
		main.start();

		// register management and configuration service
		mc = context.registerService(MC.class, havis.middleware.ale.server.MC.getInstance(), null);

		// register document service
		doc = context.registerService(DocumentService.class, ClasspathDocumentService.getInstance(), null);

		log.log(Level.FINE, "Bundle start took {0}ms", String.valueOf(System.currentTimeMillis() - start));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		log.log(Level.FINE, "Closing tracker {0}.", immutableReaderTracker.getClass().getName());
		immutableReaderTracker.close();
		if (registry != null) {
			registry.unregister();
			registry = null;
		}
		if (mc != null) {
			mc.unregister();
			mc = null;
		}
		if (doc != null) {
			doc.unregister();
			doc = null;
		}
		main.stop();
		Connector.clearFactory();
	}
}