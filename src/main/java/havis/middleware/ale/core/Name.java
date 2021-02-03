package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.config.Config;

import java.util.regex.Pattern;

/**
 * This class is used to validate names
 */
public class Name {

	public static I18n _ = new I18n(Name.class);

	static Pattern WHITE_SPACE;
	static Pattern SYNTAX;
	static {
		reset();
	}

	public static void reset() {
		WHITE_SPACE = Pattern.compile(Config.getInstance().getGlobal().getUnicode().getPatternWhiteSpace());
		SYNTAX = Pattern.compile(Config.getInstance().getGlobal().getUnicode().getPatternSyntax());
	}

	/**
     * Returns if a name is valid, throws a ValidationException otherwise
     *
     * @param name
     *            The name
     * @return true if valid, ValidationException otherwise
     * @throws ValidationException
     */
	public static boolean isValid(String name) throws ValidationException {
		return isValid(name, true);
	}

	/**
	 * Returns if a name is valid, throws a ValidationException otherwise
	 *
	 * @param name
	 *            The name
	 * @param pattern
	 *            Validate against UNICODE pattern
	 * @return true if valid, ValidationException otherwise
	 * @throws ValidationException
	 */
	public static boolean isValid(String name, boolean pattern)
			throws ValidationException {
		if (name == null) {
			throw new ValidationException(_.get("nameIsNull"));
		}
		if (name.isEmpty()) {
			throw new ValidationException(_.get("nameIsEmpty"));
		}
		if (pattern) {
			if (WHITE_SPACE.matcher(name).find()) {
				throw new ValidationException(_.get("nameContainsWhiteSpace",
						name));
			}
			if (SYNTAX.matcher(name).find()) {
				throw new ValidationException(_.get("nameContainsSyntax", name));
			}
		}
		return true;
	}
}