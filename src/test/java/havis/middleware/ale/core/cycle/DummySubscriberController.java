package havis.middleware.ale.core.cycle;

import havis.middleware.ale.core.subscriber.SubscriberController;
import havis.middleware.ale.service.IReports;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.pc.PCReports;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DummySubscriberController implements SubscriberController {
	private AtomicInteger count = new AtomicInteger(0);
	private List<IReports> reports = new ArrayList<>();
	private URI uri;
	private boolean active;
	private long incDelay = 0;

	public DummySubscriberController(String uri) {
		try {
			this.uri = new URI(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public DummySubscriberController(String uri, long incDelay) {
		this(uri);
		this.incDelay = incDelay;
	}

	public List<IReports> getReports() {
		return reports;
	}

	public int getCount() {
		return count.intValue();
	}

	@Override
	public void setActive(boolean state) {
		active = state;
	}

	@Override
	public void inc() {
		count.incrementAndGet();
		if (incDelay > 0) {
			try {
				Thread.sleep(incDelay);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	@Override
	public void dec() {
		count.decrementAndGet();
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public boolean getActive() {
		return active;
	}

	@Override
	public synchronized void enqueue(PCReports reports) {
		this.reports.add(reports);
		dec();
	}

	@Override
	public synchronized void enqueue(CCReports reports) {
		this.reports.add(reports);
		dec();
	}

	@Override
	public synchronized void enqueue(ECReports reports) {
		this.reports.add(reports);
		dec();
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isErrorState() {
		return false;
	}
}
