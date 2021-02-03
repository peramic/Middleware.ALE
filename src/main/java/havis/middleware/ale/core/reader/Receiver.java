package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.operation.Data;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.reader.ReaderConnector;

public abstract class Receiver<T extends Data> {

	private CallbackHandler callbackHandler;
	protected long id;
	protected ReaderController controller;
	protected ReaderConnector connector;

	public Receiver(CallbackHandler callbackHandler, long id, ReaderController controller, ReaderConnector connector) {
		this.callbackHandler = callbackHandler;
		this.id = id;
		this.controller = controller;
		this.connector = connector;

		addCallback();
	}

	public long getId() {
		return id;
	}

	public abstract void receive(T data);

	public void error(String name, String text, Exception e) {
		Exits.Log.logp(Exits.Level.Error, Exits.Reader.Controller.Name, Exits.Reader.Controller.Error, text + ": " + e.getMessage(), e);
	}

	public void dispose() throws ImplementationException {
		removeCallback();
	}

	protected void addCallback() {
		this.callbackHandler.add(this);
	}

	protected boolean removeCallback() {
		return this.callbackHandler.remove(this.id);
	}
}