package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;
import havis.middleware.ale.core.field.Fields;
import havis.middleware.ale.core.field.RawData;
import havis.middleware.ale.core.report.cc.data.Parameter.Callback;
import havis.middleware.ale.service.cc.CCParameterListEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the parameter management
 */
public class Parameters {

	private Map<String, Parameter> parameters = new HashMap<>();

	private Map<String, RawData> dataValues = new HashMap<>();

	/**
	 * Creates a new parameters instance
	 */
	public Parameters() {
	}

	/**
	 * Update the parameter values
	 * 
	 * @param entries
	 *            the entries
	 * @throws ParameterException
	 *             if parameter values are not valid
	 */
	public void updateParameterValues(List<CCParameterListEntry> entries) throws ParameterException {
		List<String> names = new ArrayList<>();
		int count = 0;
		for (CCParameterListEntry entry : entries) {
			if (entry.getName() == null) {
				throw new ParameterException("Parameters contain an entry without a name");
			}
			if (entry.getValue() == null) {
				throw new ParameterException("Parameters contain an entry without a value");
			}
			if (names.contains(entry.getName())) {
				throw new ParameterException("Parameter '" + entry.getName() + "' already specified");
			}

			Parameter parameter = parameters.get(entry.getName());
			if (parameter != null) {
				if (parameter.getFieldFormat() == FieldFormat.STRING) {
					dataValues.put(entry.getName(), new Characters(entry.getValue()));
				} else {
					dataValues.put(entry.getName(), toBytes(parameter, entry.getName(), entry.getValue()));
				}
				count++;
			}
			names.add(entry.getName());
		}

		if (count < parameters.size()) {
			throw new ParameterException("Missing parameters, not all parameters have been specified");
		}
	}

	private Bytes toBytes(Parameter parameter, String name, String value) throws ParameterException {
		try {
			Bytes bytes = Fields.toBytes(parameter.getFieldDatatype(), parameter.getFieldFormat(), value);
			if (bytes == null) {
				throw new ParameterException("Failed to parse value for parameter '" + name + "'.");
			}
			return bytes;
		} catch (ValidationException e) {
			throw new ParameterException("Invalid value for parameter '" + name + "'. " + e.getReason());
		}
	}

	/**
	 * Clear the parameter values
	 */
	public void clearParameterValues() {
		this.dataValues.clear();
	}

	/**
	 * Returns an exist or new created parameter by name
	 * 
	 * @param name
	 *            The parameter name
	 * @param datatype
	 *            The data type
	 * @param format
	 *            The format
	 * @return The parameter instance
	 * @throws ValidationException
	 *             if data type or format not equal to existing set
	 */
	Parameter get(final String name, FieldDatatype datatype, FieldFormat format) throws ValidationException {
		Parameter parameter = parameters.get(name);
		if (parameter != null) {
			if ((datatype != parameter.getFieldDatatype()) || (format != parameter.getFieldFormat())) {
				throw new ValidationException("Different datatype or format for parameter '" + name + "'");
			}
		} else {
			parameters.put(name, parameter = new Parameter(new Callback() {
				@Override
				public Bytes getBytes() {
					if (dataValues.get(name) instanceof Bytes) {
						return (Bytes) dataValues.get(name);
					}
					return new Bytes(ResultState.MISC_ERROR_TOTAL);
				}

				@Override
				public Characters getCharacters() {
					if (dataValues.get(name) instanceof Characters) {
						return (Characters) dataValues.get(name);
					}
					return new Characters(ResultState.MISC_ERROR_TOTAL);
				}
			}, datatype, format));
		}
		return parameter;
	}

	/**
	 * @return Whether parameters are defined
	 */
	public boolean hasParameters() {
		return parameters.size() > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataValues == null) ? 0 : dataValues.hashCode());
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Parameters))
			return false;
		Parameters other = (Parameters) obj;
		if (dataValues == null) {
			if (other.dataValues != null)
				return false;
		} else if (!dataValues.equals(other.dataValues))
			return false;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Parameters [parameters=" + parameters + ", dataValues=" + dataValues + "]";
	}
}