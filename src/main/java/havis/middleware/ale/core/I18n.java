package havis.middleware.ale.core;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {

	static ResourceBundle _;

	public I18n(Class<?> clazz) {
		_ = ResourceBundle.getBundle(clazz.getName(), Locale.getDefault(),
				clazz.getClassLoader());
	}

	public String get(String key, Object... args) {
		return MessageFormat.format(_.getString(key), args);
	}
}