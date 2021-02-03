package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.core.config.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the port trigger
 */
public class PortTrigger extends Trigger {
	/**
	 * Retrieves the regular expression for port trigger urn
	 */
	private static Pattern PORT;
	static {
		reset();
	}

	public static void reset() {
		PORT = Pattern.compile(Config.getInstance().getGlobal().getUrn().getTrigger().getPort());
	}

	private String readerName;
	private Pin pin;
	private Byte state;

	PortTrigger(String creatorId, String uri, final Trigger.Callback callback) throws ValidationException, ImplementationException {
		super(creatorId, uri, callback);
		Matcher match = PORT.matcher(uri);
		if (match.matches()) {
			readerName = match.group("reader");
			int id = -1;
			String idGroup = match.group("id");
			if (idGroup != null && !idGroup.isEmpty()) {
				try {
					id = Integer.parseInt(match.group("id"));
				} catch (NumberFormatException e) {
					// ignore
				}
			}
			pin = new Pin(id, "in".equals(match.group("type")) ? Pin.Type.INPUT : Pin.Type.OUTPUT);
			String stateGroup = match.group("state");
			if (stateGroup != null && !stateGroup.isEmpty()) {
				try {
					state = Byte.valueOf((byte) Short.parseShort(stateGroup));
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			PortTriggerService.getInstance().add(this);
		} else {
			throw new ValidationException("No reader, type, pin or state given in port trigger urn");
		}
	}

	/**
	 * @return the reader name
	 */
	public String getReaderName() {
		return readerName;
	}

	/**
	 * @return the pin
	 */
	public Pin getPin() {
		return pin;
	}

	/**
	 * @return the state
	 */
	public Byte getState() {
		return state;
	}

	/**
	 * Deactivates the internal timer
	 */
	@Override
	public void dispose() {
		PortTriggerService.getInstance().remove(this);
	}
}