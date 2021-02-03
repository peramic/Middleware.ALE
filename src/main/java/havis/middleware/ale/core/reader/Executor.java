package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.operation.Data;
import havis.middleware.ale.reader.ReaderConnector;

public abstract class Executor<T extends Data> extends Receiver<T> {

	protected String name;
	protected Caller<T> caller;

	public Executor(CallbackHandler callbackHandler, long id, ReaderController controller, ReaderConnector connector, String name, Caller<T> caller) {
		super(callbackHandler, id, controller, connector);
		this.name = name;
		this.caller = caller;
	}
	
	public abstract void execute();
}