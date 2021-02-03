package havis.middleware.ale.core.trigger;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.Name;
import havis.middleware.ale.core.config.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the web trigger
 */
public class HttpTrigger extends Trigger {

	/**
	 * Retrieves the regular expression for http trigger urn
	 */
	private static Pattern HTTP;
	static {
		reset();
	}

	public static void reset() {
		HTTP = Pattern.compile(Config.getInstance().getGlobal().getUrn().getTrigger().getHttp());
	}

	private String name;

	/**
	 * Creates a new instance
	 * 
	 * @param creatorId
	 *            The ID of the trigger creator
	 * @param uri
	 *            The uri
	 * @param callback
	 *            The callback
	 * @throws ValidationException
	 */
	HttpTrigger(String creatorId, String uri, Trigger.Callback callback) throws ValidationException {
		super(creatorId, uri, callback);
		Matcher match = HTTP.matcher(uri);
		if (match.matches()) {
			this.name = match.group("name");
			if (Name.isValid(this.name)) {
				HttpTriggerService.getInstance().add(this);
			}
		} else {
			throw new ValidationException("No name given in http trigger urn");
		}
	}

	/**
	 * Gets the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Dispose the instance
	 */
	@Override
	public void dispose() {
		HttpTriggerService.getInstance().remove(this);
	}
}