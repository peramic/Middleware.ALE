package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonCompositeReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.reader.Prefix;
import havis.middleware.ale.reader.ReaderConnector;
import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.lr.LRSpec.Properties;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ImmutableReader extends BaseReader {

	public ImmutableReader(String name, ReaderConnector connector, LRSpec spec)
			throws ValidationException, ImplementationException,
			ImmutableReaderException {
		super(name, null, new ReaderController(name, connector, getControllerProperties(fixSpec(spec))));
	}

	private static LRSpec fixSpec(LRSpec spec) {
		if (spec != null) {
			spec.setIsComposite(Boolean.FALSE);
			spec.setExtension(null);
			spec.setReaders(null);
			spec.getAny().clear();
		}
		return spec;
	}

	private static Map<String, String> getControllerProperties(LRSpec spec) {
		Map<String, String> properties = new HashMap<>();
		if (spec != null && spec.getProperties() != null) {
			properties = getControllerProperties(spec.getProperties().getProperty());
		}
		return properties;
	}

	private static Map<String, String> getControllerProperties(List<LRProperty> properties) {
		Map<String, String> result = new HashMap<>();
		Iterator<LRProperty> it = properties.iterator();
		while (it.hasNext()) {
			LRProperty p = it.next();
			if (p.getName() != null && p.getName().startsWith(Prefix.Controller)) {
				result.put(p.getName(), p.getValue());
			} else {
				// remove all other properties
				it.remove();
			}
		}
		return result;
	}

	@Override
	protected void setProperties(LRSpec spec) throws ValidationException,
			ImmutableReaderException {
		if (spec != null)
			throw new ImmutableReaderException(
					"Unable to modify an immutable reader");
	}

	@Override
	protected void validate() throws ValidationException {
	}

	@Override
	public void update(LRSpec spec, boolean persist) throws ImplementationException, InUseException, ValidationException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
		// only handle controller properties
		fixSpec(spec);
		if (spec != null && spec.getProperties() != null)
			setProperties(spec.getProperties().getProperty(), persist);
	}

	@Override
	public void undefine() throws InUseException, ImplementationException,
			ImmutableReaderException {
		throw new ImmutableReaderException(
				"Unable to modify an immutable reader");
	}

	@Override
	public void setProperties(List<LRProperty> properties, boolean persist)
			throws ImplementationException, InUseException,
			ValidationException, NoSuchNameException, ImmutableReaderException {
		// only handle controller properties
		Map<String, String> props = getControllerProperties(properties);
		controller.update(props);
		if (persist) {
			try {
				LRSpec lrSpec = new LRSpec();
				lrSpec.setProperties(new Properties());
				for (Entry<String, String> prop : props.entrySet()) {
					LRProperty p = new LRProperty();
					p.setName(prop.getKey());
					p.setValue(prop.getValue());
					lrSpec.getProperties().getProperty().add(p);
				}

				havis.middleware.ale.core.depot.service.lr.LogicalReader.getInstance().update(name, lrSpec);
			} catch (NoSuchIdException e) {
				throw new ImplementationException(e);
			}
		}
	}

	@Override
	public void add(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImmutableReaderException {
		throw new ImmutableReaderException(
				"Unable to modify an immutable reader");
	}

	@Override
	public void set(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImmutableReaderException {
		throw new ImmutableReaderException(
				"Unable to modify an immutable reader");
	}

	@Override
	public void remove(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImmutableReaderException {
		throw new ImmutableReaderException(
				"Unable to modify an immutable reader");
	}

	/**
	 * Dispose the immutable reader
	 * 
	 * @throws ImplementationException
	 */
	public void dispose() throws ImplementationException {
		undefineReader();
	}
}