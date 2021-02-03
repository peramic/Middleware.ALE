package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.reader.ReaderConnector;

public class PortExecutor extends Executor<Port> {

	private PortOperation operation;

	public PortExecutor(CallbackHandler callbackHandler, long id, ReaderController controller, ReaderConnector connector, Caller<Port> caller, PortOperation operation) {
		super(callbackHandler, id, controller, connector, null, caller);
		this.operation = operation;
	}

	@Override
	public void execute() {
		try {
			this.connector.executePortOperation(getId(), this.operation);
		} catch (ALEException e) {
			Port port = new Port();
			port.setCompleted(true);
			receive(port);
			this.controller.error("An error occurred while executing port operation", e);
		}
	}

	@Override
	public void receive(Port port) {
		if (removeCallback()) {
			this.caller.invoke(port, this.controller);
		}
	}
}
