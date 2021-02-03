package havis.middleware.ale.core.field;

import havis.middleware.ale.base.operation.tag.result.ResultState;

public class RawData {

	/**
	 * Empty raw data
	 */
	public static final RawData EMPTY = new RawData(ResultState.SUCCESS);

	/**
	 * The result state
	 */
	protected ResultState state;

	/**
	 * @param state
	 *            the state
	 */
	public RawData(ResultState state) {
		this.state = state;
	}

	/**
	 * Gets the result state
	 * 
	 * @return The result state
	 */
	public ResultState getResultState() {
		return state;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof RawData))
			return false;
		RawData other = (RawData) obj;
		if (state != other.state)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RawData [state=" + state + "]";
	}
}