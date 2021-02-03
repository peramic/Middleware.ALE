package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.operation.Data;

public class Notify {

	long id;
	Data data;

	public Notify(long id, Data data) {
		this.id = id;
		this.data = data;
	}

	public long getId() {
		return id;
	}

	public Data getData() {
		return data;
	}
}