package havis.middleware.ale.host;

import havis.middleware.ale.Connector;
import havis.middleware.ale.base.annotation.EndpointMetadata;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.core.depot.service.cc.Association;
import havis.middleware.ale.core.depot.service.cc.Cache;
import havis.middleware.ale.core.depot.service.cc.CommandCycle;
import havis.middleware.ale.core.depot.service.cc.Random;
import havis.middleware.ale.core.depot.service.ec.EventCycle;
import havis.middleware.ale.core.depot.service.lr.LogicalReader;
import havis.middleware.ale.core.depot.service.pc.PortCycle;
import havis.middleware.ale.core.depot.service.tm.TagMemory;
import havis.middleware.ale.core.doc.ClasspathDocumentService;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.report.cc.data.Associations;
import havis.middleware.ale.core.report.cc.data.Caches;
import havis.middleware.ale.core.report.cc.data.Randoms;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.core.trigger.HttpTrigger;
import havis.middleware.ale.core.trigger.HttpTriggerService;
import havis.middleware.ale.core.trigger.PortTrigger;
import havis.middleware.ale.core.trigger.PortTriggerService;
import havis.middleware.ale.core.trigger.RtcTrigger;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.doc.DocumentService;
import havis.middleware.misc.TdtWrapper;
import havis.middleware.utils.threading.NamedThreadFactory;
import havis.middleware.utils.threading.ThreadManager;
import havis.util.monitor.ServiceSource;
import havis.util.monitor.UsabilityChanged;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceContext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.xml.ws.api.server.LazyMOMProvider;

public class Main implements ServiceSource {

	private final static Logger log = Logger.getLogger(Main.class.getName());

	public final static String ADDRESS = "havis.middleware.ale.address";
	private final static String DOCUMENT_PATH = "/doc/";

	static String address = "http://[0:0:0:0:0:0:0:0]:8888/services/ALE";

	private Endpoint lr, tm, ec, cc, pc, mc;

	private Thread asyncStarter;
	private HttpServer httpServer;

	private static URL httpServerUrl;

	private String name;

	static {
		LazyMOMProvider.INSTANCE.initMOMForScope(LazyMOMProvider.Scope.GLASSFISH_NO_JMX);
	}

	public static String getVendorVersionUrl(WebServiceContext context) {
		if (context != null) {
			HttpExchange exchange = (HttpExchange) context.getMessageContext().get("com.sun.xml.internal.ws.http.exchange");
			if (exchange != null) {
				try {
					URL serverUrl = getHttpServerUrl();
					String externalHost;
					// try to retrieve host from header
					List<String> headerHosts;
					if ((headerHosts = exchange.getRequestHeaders().get("Host")) != null && headerHosts.size() == 1) {
						// strip port
						URL url = new URL(serverUrl.getProtocol() + "://" + headerHosts.get(0));
						externalHost = url.getHost();

					} else {
						// fall back to host from request URI
						externalHost = exchange.getLocalAddress().getHostString();
					}

					return new URL(serverUrl.getProtocol(), externalHost, serverUrl.getPort(), serverUrl.getPath() + DOCUMENT_PATH
							+ ClasspathDocumentService.VENDOR_SPECIFICATION_NAME).toString();
				} catch (MalformedURLException e) {
					return null;
				}
			}
		}
		return null;
	}

	public Main() {
		String addr = System.getProperty(ADDRESS);
		if (addr != null) {
			log.log(Level.FINE, "Changing default address of SOAP server to {0}.", addr);
			address = addr;
		}
	}

	private static URL getHttpServerUrl() {
		if (httpServerUrl == null) {
			try {
				URL url = new URL(address);
				if (url.getPort() == -1) {
					// fall back to default port
					url = new URL(url.getProtocol(), url.getHost(), url.getDefaultPort(), url.getFile());
				}
				httpServerUrl = url;
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Failed to create url from address " + address, e);
			}
		}
		return httpServerUrl;
	}

	private HttpServer getHttpServer() {
		if (httpServer == null) {
			log.log(Level.FINE, "Starting http server on " + address);
			try {
				URL url = getHttpServerUrl();
				InetSocketAddress inetAddress = new InetSocketAddress(url.getHost(), url.getPort());
				HttpServer server = HttpServer.create(inetAddress, 0);
				server.setExecutor(Executors.newCachedThreadPool(new NamedThreadFactory("WSHTTPServer:" + url.getPort())));
				server.start();
				httpServer = server;
			} catch (Exception e) {
				log.log(Level.SEVERE, "Failed to start http server on " + address, e);
			}
		}
		return httpServer;
	}

	private void registerTriggerService(URL url, HttpServer server) {
		log.log(Level.FINE, "Registering trigger service");
		final String triggerUrl = url.getPath() + "/trigger/";
		server.createContext(triggerUrl, new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				boolean handled = false;
				String path = exchange.getRequestURI().getPath();
				if (path.startsWith(triggerUrl)) {
					handled = HttpTriggerService.getInstance().handle(path.substring(triggerUrl.length()));
				}
				exchange.sendResponseHeaders(handled ? HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_NOT_FOUND, -1 /*
																														 * response
																														 * without
																														 * body
																														 */);
			}
		});
	}

	private void registerDocumentService(URL url, HttpServer server) {
		log.log(Level.FINE, "Registering document service");
		final String documentUrl = url.getPath() + DOCUMENT_PATH;
		server.createContext(documentUrl, new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				String name = exchange.getRequestURI().getPath().substring(documentUrl.length());
				DocumentService service = ClasspathDocumentService.getInstance();
				if (service.hasDocument(name)) {
					exchange.getResponseHeaders().put("Content-Type", Arrays.asList(service.getMimetype(name)));
					exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, service.getSize(name));
					OutputStream out = exchange.getResponseBody();
					try {
						service.writeContent(name, out);
					} finally {
						out.close();
					}
				} else {
					exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1 /*
																					   * response
																					   * without
																					   * body
																					   */);
				}
			}
		});
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void init(String name) {
		this.name = name;
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			long start = System.currentTimeMillis();
			loadConfig();
			log.log(Level.FINE, "Loading config took {0}ms", String.valueOf(System.currentTimeMillis() - start));
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
	}

	public void start() {
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

			long start = System.currentTimeMillis();
			initializeDepots();
			log.log(Level.FINE, "Initializing depots took {0}ms", String.valueOf(System.currentTimeMillis() - start));

			asyncStarter = new Thread(new Runnable() {
				@Override
				public void run() {
					long start = System.currentTimeMillis();

					URL url = getHttpServerUrl();
					HttpServer server = getHttpServer();

					registerTriggerService(url, server);

					if (Config.isSoapService()) {
						log.log(Level.FINE, "Starting LR");
						lr = Endpoint.create(new LR());
						lr.setMetadata(openXsdResources(LR.class));
						lr.publish(server.createContext(url.getPath() + "/LR"));

						log.log(Level.FINE, "Starting TM");
						tm = Endpoint.create(new TM());
						tm.setMetadata(openXsdResources(TM.class));
						tm.publish(server.createContext(url.getPath() + "/TM"));

						log.log(Level.FINE, "Starting EC");
						ec = Endpoint.create(new EC());
						ec.setMetadata(openXsdResources(EC.class));
						ec.publish(server.createContext(url.getPath() + "/EC"));

						log.log(Level.FINE, "Starting CC");
						cc = Endpoint.create(new CC());
						cc.setMetadata(openXsdResources(CC.class));
						cc.publish(server.createContext(url.getPath() + "/CC"));

						log.log(Level.FINE, "Starting PC");
						pc = Endpoint.create(new PC());
						pc.setMetadata(openXsdResources(PC.class));
						pc.publish(server.createContext(url.getPath() + "/PC"));
					}

					registerDocumentService(url, server);

					log.log(Level.FINE, "Endpoint start took {0}ms", String.valueOf(System.currentTimeMillis() - start));

					start = System.currentTimeMillis();

					TdtWrapper.getTdt();

					log.log(Level.FINE, "TDT load took {0}ms", String.valueOf(System.currentTimeMillis() - start));

					Connector.getFactory().getBroker().notify(Main.this, new UsabilityChanged(new Date(), true));
				}
			}, this.getClass().getSimpleName() + " start()");
			asyncStarter.start();

		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
	}

	private List<Source> openXsdResources(Class<?> wsdlImplementor) {
		List<Source> result = new ArrayList<>();
		for (String location : wsdlImplementor.getAnnotation(EndpointMetadata.class).xsdLocations()) {
			URL url = wsdlImplementor.getClassLoader().getResource(location);
			try {
				result.add(new StreamSource(url.openStream(), url.toExternalForm()));
			} catch (IOException e) {
				log.log(Level.SEVERE, "Failed to load resource " + url.toString(), e);
			}
		}
		return result;
	}

	private boolean isFirstStart() {
		java.lang.reflect.Method m;
		try {
			m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
			m.setAccessible(true);
			return m.invoke(getClass().getClassLoader(), "havis.middleware.ale.core.config.Config") == null;
		} catch (Exception e) {
			return false;
		}
	}

	private void loadConfig() {
		boolean isFirstStart = isFirstStart();
		Config.reset();
		Config.getInstance();
		if (!isFirstStart) {
			resetStaticInstances();
		}
	}

	private void resetStaticInstances() {
		TagMemory.reset();
		LogicalReader.reset();
		EventCycle.reset();
		Cache.reset();
		Association.reset();
		Random.reset();
		CommandCycle.reset();
		PortCycle.reset();

		havis.middleware.ale.core.manager.TM.reset();
		havis.middleware.ale.core.manager.LR.reset();
		havis.middleware.ale.core.manager.EC.reset();
		havis.middleware.ale.core.manager.CC.reset();
		havis.middleware.ale.core.manager.PC.reset();
		havis.middleware.ale.core.manager.MC.reset();

		Associations.reset();
		Caches.reset();
		Randoms.reset();

		Fields.reset();
		Patterns.reset();
		HttpTrigger.reset();
		PortTrigger.reset();
		RtcTrigger.reset();
		HttpTriggerService.reset();
		PortTriggerService.reset();
		Name.reset();
	}

	private void initializeDepots() {
		try {
			TagMemory.getInstance().init();
			LogicalReader.getInstance().init();
			EventCycle.getInstance().init();
			Cache.getInstance().init();
			Association.getInstance().init();
			Random.getInstance().init();
			CommandCycle.getInstance().init();
			PortCycle.getInstance().init();
		} catch (ALEException e) {
			Exits.Log.logp(Exits.Level.Error, Exits.Common.Name, Exits.Common.Error, "Failed to initialize depots: " + e.getMessage(), e);
		}
	}

	public void stop() {

		Connector.getFactory().getBroker().notify(this, new UsabilityChanged(new Date(), false));

		if (asyncStarter != null) {
			if (asyncStarter.isAlive()) {
				asyncStarter.interrupt();
				try {
					asyncStarter.join(5000);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			asyncStarter = null;
		}

		if (mc != null) {
			log.log(Level.FINE, "Stopping MC");
			mc.stop();
			mc = null;
		}

		if (pc != null) {
			log.log(Level.FINE, "Stopping PC");
			pc.stop();
			pc = null;
		} else
			havis.middleware.ale.core.manager.PC.getInstance().dispose();

		if (cc != null) {
			log.log(Level.FINE, "Stopping CC");
			cc.stop();
			cc = null;
		} else {
			havis.middleware.ale.core.manager.CC.getInstance().dispose();
			Caches.getInstance().dispose();
			Associations.getInstance().dispose();
			Randoms.getInstance().dispose();
		}

		if (ec != null) {
			log.log(Level.FINE, "Stopping EC");
			ec.stop();
			ec = null;
		} else
			havis.middleware.ale.core.manager.EC.getInstance().dispose();

		if (tm != null) {
			log.log(Level.FINE, "Stopping TM");
			tm.stop();
			tm = null;
		} else
			havis.middleware.ale.core.manager.TM.getInstance().dispose();

		if (lr != null) {
			log.log(Level.FINE, "Stopping LR");
			lr.stop();
			lr = null;
		} else
			havis.middleware.ale.core.manager.LR.getInstance().dispose();

		if (httpServer != null) {
			log.log(Level.FINE, "Stopping http server");
			httpServer.stop(0);
		}

		ThreadManager.dispose();
	}
}