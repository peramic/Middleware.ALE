package havis.middleware.ale.core.reader;

import havis.middleware.ale.Connector;
import havis.middleware.ale.base.message.Message;
import havis.middleware.ale.base.message.MessageHandler;
import havis.middleware.ale.base.operation.Data;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.exit.Exits;
import havis.middleware.utils.threading.Pipeline;
import havis.util.monitor.ReaderEvent;
import havis.util.monitor.ReaderSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements the callback handler interface
 */
public class QueuedCallbackHandler implements CallbackHandler {

	private static final int MIN_PORT = 11000;

	// array index + MIN_PORT = used port
	// value at index: <0 = currently used, >=0 = timestamp when used last
	private static final long[] usedPorts = new long[100];

	private static final Lock usedPortsLock = new ReentrantLock();

	// default timeout of 60s until a port is really free
	private static final long PORT_BLOCKED_DELAY = 60500;

	private Pipeline<Notify> tagPipeline = new Pipeline<>();
	private Pipeline<Notify> portPipeline = new Pipeline<>();
	private Thread portThread, tagThread;

	private MessageHandler messageHandler;
	private ReaderSource source;

	private Lock lock = new ReentrantLock();

	private String name;
	private Map<Long, Receiver<?>> receivers;

	public QueuedCallbackHandler(String name, MessageHandler messageHandler, ReaderSource source) {
		this.name = name;
		this.messageHandler = messageHandler;
		this.source = source;
		receivers = new HashMap<>();

		tagThread = new Thread(new Runnable() {
			@Override
			public void run() {
				processTagNotifications();
			}
		}, this.getClass().getSimpleName() + " " + (this.name != null ? this.name : "[no name]") + " tag notify()");
		tagThread.start();
		portThread = new Thread(new Runnable() {
			@Override
			public void run() {
				processPortNotifications();
			}
		}, this.getClass().getSimpleName() + " " + (this.name != null ? this.name : "[no name]") + " port notify()");
		portThread.start();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getReaderCycleDuration() {
		return Config.getInstance().getGlobal().getReaderCycle().getDuration();
	}

	@Override
	public int getNetworkPort() {
		usedPortsLock.lock();
		try {
			long limit = System.currentTimeMillis() - PORT_BLOCKED_DELAY;
			for (int i = 0; i < usedPorts.length; i++) {
				if (usedPorts[i] >= 0 && usedPorts[i] < limit) {
					usedPorts[i] = -1;
					return MIN_PORT + i;
				}
			}
		} finally {
			usedPortsLock.unlock();
		}
		throw new IllegalStateException("No more free ports");
	}

	@Override
	public void resetNetwortPort(int port) {
		usedPortsLock.lock();
		try {
			usedPorts[port - MIN_PORT] = System.currentTimeMillis();
		} finally {
			usedPortsLock.unlock();
		}
	}

	@Override
	public void add(Receiver<?> receiver) {
		try {
			lock.lock();
			receivers.put(Long.valueOf(receiver.getId()), receiver);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean remove(long id) {
		try {
			lock.lock();
			return receivers.remove(Long.valueOf(id)) != null;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Receiver<?> get(long id) {
		try {
			lock.lock();
			return receivers.get(Long.valueOf(id));
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void notify(long id, Tag tag) {
		if (tag != null) {
			if (Exits.Log.isLoggable(Exits.Level.Detail)) {
				Exits.Log.logp(Exits.Level.Detail, Exits.Reader.Controller.Callback.Name, Exits.Reader.Controller.Callback.NotifyTag, "{0} received {1}",
						new Object[] { name, tag.tag() });
			}
			tagPipeline.enqueue(new Notify(id, tag));
		}
	}

	@Override
	public void notify(long id, Port port) {
		if (port != null) {
			Exits.Log.logp(Exits.Level.Detail, Exits.Reader.Controller.Callback.Name, Exits.Reader.Controller.Callback.NotifyPort, "{0} received {1}",
					new Object[] { name, port.port() });
			portPipeline.enqueue(new Notify(id, port));
		}
	}

	@Override
	public void notify(Message message) {
		if (this.messageHandler != null)
			this.messageHandler.notify(message);
	}

	@Override
	public void notify(ReaderEvent event) {
		Connector.getFactory().getBroker().notify(this.source, event);
	}

	public void processTagNotifications() {
		while (processNotifications(tagPipeline)) {
		}
	}

	public void processPortNotifications() {
		while (processNotifications(portPipeline)) {
		}
	}

	private boolean processNotifications(Pipeline<Notify> pipeline) {
		Notify notify = pipeline.dequeue();
		if (notify != null) {
			@SuppressWarnings("unchecked")
			Receiver<Data> receiver = (Receiver<Data>) get(notify.getId());
			if (receiver != null) {
				receiver.receive(notify.getData());
			}
			return true;
		}
		return false;
	}

	@Override
	public void dispose() {
		tagPipeline.dispose();
		try {
			tagThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		portPipeline.dispose();
		try {
			portThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}