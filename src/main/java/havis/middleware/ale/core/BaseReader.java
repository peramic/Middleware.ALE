package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NonCompositeReaderException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.Reader;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.reader.Prefix;
import havis.middleware.ale.reader.Property;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.rc.RCConfig;

import java.util.List;
import java.util.Map.Entry;

/**
 * Class that represents a base reader.
 */
public class BaseReader extends LogicalReader {

	/**
	 * Controller for reliable access on the reader connector.
	 */
	protected ReaderController controller;

	private String type;

	/**
	 * @return the connector type for this base reader, empty string if type was not yet set. 
	 */
	public String getType() {
		return type;
	}

	protected BaseReader(String name, LRSpec spec, ReaderController controller)
			throws ValidationException, ImmutableReaderException {
		super(name, spec);
		this.controller = controller;
	}

	/**
	 * Initializes a new instance of the
	 * Havis.Middleware.ALE.LogicalReader.BaseReader class.
	 * 
	 * @param name
	 *            The name of the logical reader.
	 * @param spec
	 *            The specification of the logical reader.
	 * @throws ValidationException
	 * @throws ImplementationException
	 * @throws ImmutableReaderException 
	 */
	public BaseReader(String name, LRSpec spec) throws ValidationException,
			ImplementationException, ImmutableReaderException {
		super(name, spec);
		if ((spec.getReaders() != null)
				&& (spec.getReaders().getReader().size() > 0)) {
			throw new ValidationException(
					"The readers attribute was specified for base reader!");
		} else {
			String type = properties.get(Property.ReaderType);
			if (type != null) {
				this.type = type;
				controller = Reader.getInstance().get(name, type,
						this.properties);
			} else {
				throw new ValidationException(
						"The property 'ReaderType' is not specified for base reader");
			}
		}
	}

	/**
	 * Validates if all properties are recognized.
	 * 
	 * @throws ValidationException
	 *             For the first non recognized property.
	 */
	@Override
	protected void validate() throws ValidationException {
		for (Entry<String, String> property : this.properties.entrySet()) {
			switch (property.getKey()) {
			case Property.ReaderType:
			case Property.GlimpsedTimeout:
			case Property.ObservedTimeThreshold:
			case Property.ObservedCountThreshold:
			case Property.LostTimeout:
				break;
			default:
				if (property.getKey().startsWith(Prefix.Controller))
					break;
				if (property.getKey().startsWith(Prefix.Connector))
					break;
				if (property.getKey().startsWith(Prefix.Reader))
					break;
				throw new ValidationException("Property '" + property.getKey()
						+ "' is not recognized!");
			}
		}
	}

	/**
	 * Method to update this logical reader according to specification.
	 * 
	 * @param spec
	 *            The update informations.
	 * @throws ValidationException
	 *             if component readers are defined in the specification.
	 * @throws ImmutableReaderException 
	 */
	@Override
	protected void update(LRSpec spec) throws ValidationException,
			ImplementationException, ImmutableReaderException {
		if (Boolean.TRUE.equals(spec.isIsComposite())) {
			throw new ValidationException(
					"To change the isComposite flag during an update is not allowed!");
		} else if (spec.getReaders() != null
				&& !spec.getReaders().getReader().isEmpty()) {
			throw new ValidationException(
					"The readers attribute was specified for base reader!");
		} else {
			String type = properties.get(Property.ReaderType);
			if (type != null) {
				if (!type.equals(this.getType())) {
					ReaderController newController = Reader.getInstance().get(name, type, properties);
					if (controller != null) {
						controller.dispose();
					}
					controller = newController;
					this.type = type;
				} else {
					controller.update(this.properties);
				}
			} else {
				throw new ValidationException(
						"The property 'Connector.ReaderType' is not specified for base reader");
			}
		}
	}

	/**
	 * Try to reconnect
	 * 
	 * @return true if reconnect was started, false otherwise
	 */
	public boolean tryReconnect() {
		return this.controller.tryReconnect();
	}

	/**
	 * Undefines this logical reader.
	 */
	@Override
	protected void undefineReader() throws ImplementationException {
		controller.dispose();
	}

	/**
	 * Throws exception.
	 * 
	 * @param readers
	 *            The list of names of logical readers to add.
	 * @param persist
	 *            Persist changes
	 * @throws NonCompositeReaderException
	 *             Slways
	 * @throws ImmutableReaderException 
	 */
	@Override
	public void add(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImmutableReaderException {
		throw new NonCompositeReaderException(
				"Try to add readers to base reader!");
	}

	/**
	 * Throws exception.
	 * 
	 * @param readers
	 *            The list of names of logical readers to set.
	 * @param persist
	 *            Persist changes
	 * @throws NonCompositeReaderException
	 *             Always
	 * @throws ImmutableReaderException 
	 */
	@Override
	public void set(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImmutableReaderException {
		throw new NonCompositeReaderException(
				"Try to set readers attribute for base reader!");
	}

	/**
	 * Throw exception.
	 * 
	 * @param readers
	 *            The list of names of logical readers to set
	 * @param persist
	 *            Persist changes
	 * @throws NonCompositeReaderException
	 *             Always
	 * @throws ImmutableReaderException 
	 */
	@Override
	public void remove(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImmutableReaderException {
		throw new NonCompositeReaderException(
				"Try to remove readers from base reader!");
	}

	/**
	 * Returns a value that indicates if this logical reader contains reader
	 * 
	 * @param reader
	 *            The reader to check.
	 * @return True if this contains reader
	 */
	@Override
	public boolean contains(LogicalReader reader) {
		return reader == this;
	}

	/**
	 * Defines the reader operation operation at the this logical.
	 * 
	 * @param operation
	 *            The operation to define
	 * @param callback
	 *            The method to use for reporting results
	 * @param name
	 *            The name of the parent
	 */
	@Override
	protected void defineCurrent(TagOperation operation,
			final Caller<Tag> callback, String name)
			throws ImplementationException, ValidationException {
		if (isTagSmoothingEnabled()) {
			controller.define(operation, new Caller<Tag>() {
				@Override
				public void invoke(Tag tag, ReaderController controller) {
					tagSmoothingHandler.process(tag, callback, controller);
				}
			}, name);
		} else {
			controller.define(operation, callback, name);
		}
	}

	/**
	 * Undefines the reader operation at this logical.
	 * 
	 * @param operation
	 *            The operation to undefine
	 * @param name
	 *            The name of the parent
	 */
	@Override
	protected void undefineCurrent(TagOperation operation, String name)
			throws ImplementationException {
		controller.undefine(operation, name);
	}

	/**
	 * Enables the reader operation on this logical reader.
	 * 
	 * @param operation
	 *            The operation to enable
	 */
	@Override
	public void enable(TagOperation operation) throws ImplementationException {
		try {
			lock.lock();
			controller.enable(operation);
			listenerCount.incrementAndGet();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Disables the reader operation on this logical reader.
	 * 
	 * @param operation
	 *            The operation to disable
	 */
	@Override
	public void disable(TagOperation operation) throws ImplementationException {
		try {
			lock.lock();
			controller.disable(operation);
			listenerCount.decrementAndGet();
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Gets the reader configuration
	 * 
	 * @return The reader configuration
	 */
	@Override
	public RCConfig getConfig() throws ImplementationException {
		RCConfig configuration;
		try {
			lock.lock();
			configuration = controller.getConfig();
		} finally {
			lock.unlock();
		}
		return configuration;
	}

	/**
	 * Defines new port observation on the reader.
	 * 
	 * @param observation
	 *            The observation
	 * @param callback
	 *            A delegate which will be called to report the observation
	 *            result
	 * @param name
	 *            The name of the parent
	 */
	@Override
	protected void defineCurrent(PortObservation observation,
			Caller<Port> callback, String name) throws ImplementationException,
			ValidationException {
		controller.define(observation, callback, name);
	}

	/**
	 * Undefines a port observation on the reader.
	 * 
	 * @param observation
	 *            The observation
	 * @param name
	 *            The name of the parent
	 */
	@Override
	protected void undefineCurrent(PortObservation observation, String name)
			throws ImplementationException {
		controller.undefine(observation, name);
	}

	/**
	 * Enables a port observation on the reader. After this method the reader
	 * connector will report result using the given callback method.
	 * 
	 * @param observation
	 *            The observation
	 */
	@Override
	public void enable(PortObservation observation)
			throws ImplementationException {
		try {
			lock.lock();
			controller.enable(observation);
			listenerCount.incrementAndGet();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Disables a port observation on the reader. After this method the reader
	 * connector will no longer report results.
	 * 
	 * @param observation
	 *            The observation
	 */
	@Override
	public void disable(PortObservation observation)
			throws ImplementationException {
		try {
			lock.lock();
			controller.disable(observation);
			listenerCount.decrementAndGet();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Executes port observation
	 * 
	 * @param operation
	 *            The operation
	 * @param callback
	 *            The callback
	 */
	@Override
	public void execute(PortOperation operation, Caller<Port> callback)
			throws ImplementationException, ValidationException {
		try {
			lock.lock();
			controller.execute(operation, callback);
		} finally {
			lock.unlock();
		}
	}
}