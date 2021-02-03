package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class can be used as a parent for specific triggers i.e. rtc
 */
public abstract class Trigger {

	/**
	 * Callback for triggers
	 */
	public static interface Callback {
		public boolean invoke(Trigger trigger);
	}

	/**
	 * Milliseconds a day
	 */
	protected static long DAY = 24 * 60 * 60 * 1000;

	/**
	 * Retrieves the regular expression for trigger urn
	 */
	private static Pattern URN = Pattern.compile("urn:(?<scheme>[a-z]+):ale:trigger:(?<type>[a-z]+):(?<parameter>.*)");

	/**
	 * Provides the uri
	 */
	private String uri;

	/**
	 * ID of the creator of this trigger
	 */
	private String creatorId;

	/**
	 * The callback to invoke if trigger will be triggered
	 */
	private Callback callback;

	/**
	 * This constructor takes the callback
	 * 
	 * @param uri
	 *            The uri of the trigger for reporting purpose
	 * @param callback
	 *            The callback to invoke
	 */
	protected Trigger(String creatorId, String uri, Callback callback) {
		if (callback == null)
			throw new IllegalArgumentException("callback must not be null");
		this.uri = uri;
		this.creatorId = creatorId;
		this.callback = callback;
	}

	/**
	 * Invoke the trigger
	 * 
	 * @return return true if invoke was successful, false otherwise
	 */
	protected boolean invoke() {
		return this.callback.invoke(this);
	}

	/**
	 * @return the URI
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri
	 *            the URI
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Returns the instance of a child trigger by uri
	 * 
	 * @param creatorId
	 *            The ID of the trigger creator
	 * @param uri
	 *            The trigger uri
	 * @param callback
	 *            The callback
	 * @return Concrete trigger instance
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	public static Trigger getInstance(String creatorId, String uri, Callback callback) throws ImplementationException, ValidationException {
		try {
			if (uri.length() > 0) {
				Matcher match = URN.matcher(uri);
				if (match.matches()) {
					switch (match.group("scheme")) {
					case "epcglobal":
						switch (match.group("type")) {
						case "rtc":
							return new RtcTrigger(creatorId, uri, callback);
						default:
							throw new ValidationException("EPCglobal trigger is not supported");
						}
					case "havis":
						switch (match.group("type")) {
						case "http":
							return new HttpTrigger(creatorId, uri, callback);
						case "port":
							return new PortTrigger(creatorId, uri, callback);
						default:
							throw new ValidationException("Ha-VIS trigger is not supported");
						}
					default:
						throw new ValidationException("Unknown scheme '" + match.group("scheme") + "'");
					}
				}
				throw new ValidationException("Trigger is not supported");
			} else {
				throw new ValidationException("Trigger uri could not be a empty string");
			}
		} catch (ValidationException e) {
			e.setReason("The trigger '" + uri + "' is invalid. " + e.getReason());
			throw e;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creatorId == null) ? 0 : creatorId.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Trigger))
			return false;
		Trigger other = (Trigger) obj;
		if (creatorId == null) {
			if (other.creatorId != null)
				return false;
		} else if (!creatorId.equals(other.creatorId))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	/**
	 * This method was called to dispose the trigger. It can be use for cleaning
	 * up i.e. stopping all timers
	 */
	public abstract void dispose();
}