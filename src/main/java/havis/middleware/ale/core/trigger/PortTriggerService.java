package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.port.result.ReadResult;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implement the port trigger service
 */
public class PortTriggerService {

	private static PortTriggerService instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new PortTriggerService();
	}

	protected static class TriggerData {
		private LogicalReader reader = null;
		private String guid = UUID.randomUUID().toString();
		private PortObservation observation = new PortObservation();
		private List<PortTrigger> triggers = new ArrayList<>();

		public void add(PortTrigger trigger) throws ValidationException, ImplementationException {
			defineObservation(trigger);
			triggers.add(trigger);
		}

		private void defineObservation(PortTrigger trigger) throws ValidationException, ImplementationException {
			if (reader == null) {
				try {
					reader = LR.getInstance().lock(trigger.getReaderName());
				} catch (NoSuchNameException e) {
					throw new ValidationException(e.getReason());
				}
				try {
					reader.define(observation, new Caller<Port>() {
						@Override
						public void invoke(Port port, ReaderController controller) {
							Result result = port.getResult().get(Integer.valueOf(0));
							if (result != null && result.getState() == Result.State.SUCCESS && (result instanceof ReadResult)) {
								handle(port.getPin(), ((ReadResult) result).getData());
							}
						}
					}, this.guid);
					reader.enable(observation);
				} catch (ALEException e) {
					undefineObservation();
					throw e;
				}
			}
		}

		private void undefineObservation() {
			if (reader != null) {
				try {
					reader.disable(observation);
					reader.undefine(observation, this.guid);
				} catch (ImplementationException e) {
					// ignore
				}
				reader.unlock();
				reader = null;
			}
		}

		public void remove(PortTrigger trigger) {
			Iterator<PortTrigger> iterator = triggers.iterator();
			while (iterator.hasNext()) {
				// remove the exact instance only
				if (iterator.next() == trigger) {
					iterator.remove();
					break;
				}
			}
			if (triggers.size() == 0) {
				undefineObservation();
			}
		}

		private void handle(Pin pin, byte state) {
			Set<PortTrigger> invoked = new HashSet<>();
			for (PortTrigger t : triggers) {
				if (pin != null && pin.matches(t.getPin()) && (t.getState() == null || state == t.getState().byteValue())) {
					// Make sure we trigger equal instances only once, we try
					// triggering all triggers with the same URI that are
					// not equal (with a different creator ID).
					if (!invoked.contains(t)) {
						if (t.invoke()) {
							invoked.add(t);
						}
					}
				}
			}
		}

		public boolean isEmpty() {
			return triggers.isEmpty();
		}
	}

	protected Map<String, TriggerData> triggers = new HashMap<String, TriggerData>();

	public static PortTriggerService getInstance() {
		return instance;
	}

	/**
	 * Creates a new instance
	 */
	PortTriggerService() {
	}

	/**
	 * Adds a new port trigger
	 * 
	 * @param trigger
	 *            The trigger
	 */
	void add(PortTrigger trigger) throws ValidationException, ImplementationException {
		TriggerData data = this.triggers.get(trigger.getReaderName());
		boolean put = false;
		if (data == null) {
			data = new TriggerData();
			put = true;
		}
		data.add(trigger); // could fail with exception
		if (put)
			this.triggers.put(trigger.getReaderName(), data);
		
	}

	/**
	 * Removes the port trigger
	 * 
	 * @param trigger
	 *            The trigger
	 */
	void remove(PortTrigger trigger) {
		TriggerData data = this.triggers.get(trigger.getReaderName());
		if (data != null) {
			data.remove(trigger);
			if (data.isEmpty()) {
				this.triggers.remove(trigger.getReaderName());
			}
		}
	}
}