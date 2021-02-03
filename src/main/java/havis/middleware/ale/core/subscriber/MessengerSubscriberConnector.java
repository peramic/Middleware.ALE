package havis.middleware.ale.core.subscriber;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.core.config.Config;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.service.IReports;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.ec.ECReports;
import havis.middleware.ale.service.pc.PCReports;
import havis.middleware.ale.subscriber.SubscriberConnector;
import havis.transport.Messenger;
import havis.transport.TransportException;
import havis.transport.ValidationException;
import havis.transport.common.CommonMessenger;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Bridge connector to transport backend
 */
public class MessengerSubscriberConnector implements SubscriberConnector {

	@SuppressWarnings("rawtypes")
	private CommonMessenger messenger = new CommonMessenger();
	private URI uri;
	private Class<?> reportClass;

	public Class<?> getReportClass() {
		return this.reportClass;
	}

	public void setReportClass(Class<?> reportClass) {
		this.reportClass = reportClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(URI uri, Map<String, String> properties) throws InvalidURIException, ImplementationException {
		this.uri = uri;
		if (this.reportClass == null)
			throw new ImplementationException("reportClass was not set");

		messenger.setErrorLogging(false);

		// Setting default values from the MW configuration here,
		// the user can override these.
		Map<String, String> props = new HashMap<>();
		props.put(Messenger.TCP_TIMEOUT_PROPERTY, Integer.toString(Config.getInstance().getGlobal().getSubscriber().getConnectTimeout()));
		props.put(Messenger.MQTT_TIMEOUT_PROPERTY, Integer.toString(Config.getInstance().getGlobal().getSubscriber().getConnectTimeout()));
		props.put(Messenger.HTTP_TIMEOUT_PROPERTY, Integer.toString(Config.getInstance().getGlobal().getSubscriber().getConnectTimeout()));
		props.put(Messenger.HTTPS_BYPASS_SSL_VERIFICATION_PROPERTY, Boolean.toString(!Config.getInstance().getGlobal().getSubscriber().isHttpsSecurity()));

		// To make JDBC work by default, set "report" as table name
		// and expect a "date" and a "data" column in that table.
		props.put(Messenger.JDBC_TABLE_NAME_PROPERTY, "report");
		if (this.reportClass.equals(ECReports.class)) {
			props.put(Messenger.DATA_CONVERTER_EXPRESSION_PROPERTY,
					"date, ((((((value as data in epc?) in member?) in groupList) in group?) in report?) in reports)");
		} else if (this.reportClass.equals(CCReports.class)) {
			props.put(Messenger.DATA_CONVERTER_EXPRESSION_PROPERTY,
					"date, ((((((data in opReport?) in opReports) in tagReport?) in tagReports) in cmdReport?) in cmdReports)");
		} else if (this.reportClass.equals(PCReports.class)) {
			props.put(Messenger.DATA_CONVERTER_EXPRESSION_PROPERTY,
					"date, ((((((opStatus as data in opReport?) in opReports) in eventReport?) in eventReports) in report?) in reports)");
		}

		if (properties != null)
			props.putAll(properties);

		try {
			messenger.init(this.reportClass, uri, props);
		} catch (ValidationException e) {
			throw new InvalidURIException(e.getMessage());
		}
	}

	@Override
	public void send(ECReports report) throws ImplementationException {
		sendReport(report);
	}

	@Override
	public void send(CCReports report) throws ImplementationException {
		sendReport(report);
	}

	@Override
	public void send(PCReports report) throws ImplementationException {
		sendReport(report);
	}

	@SuppressWarnings("unchecked")
	private void sendReport(IReports report) throws ImplementationException {
		try {
			this.messenger.send(report).get();
		} catch (TransportException e) {
			throw new ImplementationException(e.getMessage());
		} catch (InterruptedException e) {
			Exits.Log.logp(Exits.Level.Warning, Exits.Subscriber.Controller.Name, Exits.Subscriber.Controller.DeliverFailed,
					"Subscriber {0} delivery was interrupted", new Object[] { this.uri.toString() });
		}
	}

	@Override
	public void dispose() throws ImplementationException {
		this.messenger.dispose();
	}
}
