package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.operation.Data;
import havis.middleware.ale.reader.ReaderConnector;

import java.util.HashMap;
import java.util.Map;

public abstract class Operator<T extends Data> extends Receiver<T> {

	protected boolean active;
	protected Map<String, Caller<T>> callers;

	public Operator(CallbackHandler callbackHandler, long id, ReaderController controller, ReaderConnector connector) {
		super(callbackHandler, id, controller, connector);
		callers = new HashMap<>();
	}

	public void put(String name, Caller<T> caller) {
		callers.put(name, caller);
	}

	public void remove(String name) {
		callers.remove(name);
	}

	public boolean hasCallers() {
		return callers.size() > 0;
	}

	@Override
	public void dispose() throws ImplementationException {
		this.active = false;
		super.dispose();
	}
}