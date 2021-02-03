package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.reader.ReaderConnector;

import java.util.Map.Entry;

public class PortObservator extends Operator<Port> {

	PortObservation observation;

	public PortObservator(CallbackHandler callbackHandler, long id, ReaderController controller, ReaderConnector connector, PortObservation observation)
			throws ValidationException, ImplementationException {
		super(callbackHandler, id, controller, connector);
		this.observation = observation;

		try {
			define();
		} catch (ValidationException | ImplementationException e) {
			removeCallback();
		}
	}

	@Override
	public void receive(Port port) {
		if (active) {
			if (port != null) {
				for (Entry<String, Caller<Port>> caller : callers.entrySet()) {
					try {
						caller.getValue().invoke(port, controller);
					} catch (Exception e) {
						error(caller.getKey(), "An error occurred while notifying port", e);
					}
				}
			}
		}
	}

	private void define() throws ValidationException, ImplementationException {
		connector.definePortObservation(id, observation);
	}

	private void undefine() throws ImplementationException {
		connector.undefinePortObservation(id);
	}

	void enable() {
		if (!active) {
			try {
				connector.enablePortObservation(id);
				active = true;
			} catch (Exception e) {
				error(controller.getName(), "An error occurred while enabling port observation", e);
			}
		}
	}

	void disable() {
		if (active) {
			active = false;
			try {
				connector.disablePortObservation(id);
			} catch (Exception e) {
				error(controller.getName(), "An error occurred while disabling port observation", e);
			}
		}
	}

	@Override
	public void dispose() throws ImplementationException {
		undefine();
		super.dispose();
	}
}