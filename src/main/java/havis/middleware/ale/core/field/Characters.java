package havis.middleware.ale.core.field;

import havis.middleware.ale.base.operation.tag.result.ResultState;

/**
 * String value
 */
public class Characters extends RawData {

	private String value;

	/**
	 * Creates a new instance
	 * 
	 * @param value
	 *            The string value
	 * @param state
	 *            The result state
	 */
	public Characters(String value, ResultState state) {
		super(state);
		this.value = value;
	}

	/**
	 * Create a new instance with SUCCESS result state
	 * 
	 * @param value
	 *            The string value
	 */
	public Characters(String value) {
		this(value, ResultState.SUCCESS);
	}

	/**
	 * Create a new instance without a value
	 * 
	 * @param state
	 *            The result state
	 */
	public Characters(ResultState state) {
		this(null, state);
	}

	/**
	 * @return The string value
	 */
	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Characters))
			return false;
		Characters other = (Characters) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Characters [value=" + value + ", state=" + state + "]";
	}
}
