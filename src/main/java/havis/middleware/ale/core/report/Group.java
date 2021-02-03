package havis.middleware.ale.core.report;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.report.pattern.PatternType;
import havis.middleware.ale.core.report.pattern.Patterns;
import havis.middleware.ale.service.ec.ECGroupSpec;

import java.util.List;

/**
 * This class is used for tag grouping in reports. The group name is generating
 * from tag
 */
public class Group {

	@SuppressWarnings("unused")
	private ECGroupSpec spec;

	private Patterns patterns;

	/**
	 * Creates a new instance. Keep specification.
	 * 
	 * Creates adn keeps a new <see cref="Patterns"/> instance from pattern
	 * attribute with extention field data.
	 * 
	 * @param spec
	 *            The event cycle group specification
	 * @throws ValidationException
	 *             if a pattern is invalid.
	 */
	public Group(ECGroupSpec spec) throws ValidationException {
		this.spec = spec;
		patterns = new Patterns(PatternType.GROUP, spec.getPattern(),
				spec.getExtension() == null ? null : spec.getExtension()
						.getFieldspec());
	}

	/**
	 * Retrieves the read operations which are necessary for extended filter
	 * operations i.e. filter by alternative fields or datatypes
	 * 
	 * @return The operation
	 */
	public Operation getOperation() {
		return patterns.getOperation();
	}

	/**
	 * Returns the Name of the group depending on tag
	 * 
	 * @param tag
	 *            The tag
	 * @return The group names
	 */
	public List<String> name(Tag tag) {
		return patterns.name(tag);
	}

	/**
	 * Returns the Name of the group depending on port
	 * 
	 * @param port
	 *            The port
	 * @return The group names
	 * @throws UnsupportedOperationException
	 *             always
	 */
	List<String> name(Port port) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Disposes the instance
	 */
	public void dispose() {
		if (patterns != null) {
			patterns.dispose();
			patterns = null;
		}
	}
}
