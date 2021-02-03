package havis.middleware.ale.core.exit.event;

import havis.middleware.ale.exit.event.EventArgs;

public interface EventHandler<T extends EventArgs> {

	public void invoke(Object sender, T e);
}