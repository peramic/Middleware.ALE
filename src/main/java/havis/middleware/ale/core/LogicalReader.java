package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchIdException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonBaseReaderException;
import havis.middleware.ale.base.exception.NonCompositeReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.reader.Prefix;
import havis.middleware.ale.reader.Property;
import havis.middleware.ale.service.lr.LRProperty;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.lr.LRSpec.Properties;
import havis.middleware.ale.service.rc.RCConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract class to abstract all logical reader implementations.
 */
public abstract class LogicalReader {

	/**
	 * Retrieves the unique ID
	 */
	protected String guid = UUID.randomUUID().toString();

	/**
	 * The logical reader.
	 */
	protected String name;

	/**
	 * The specification of the logical reader.
	 */
	protected LRSpec spec;

	/**
	 * Object to synchronize all operation on the {@link Properties} list.
	 */
	protected Lock syncPropertyList = new ReentrantLock();

	/**
	 * Gets the list of properties to set up the logical reader.
	 */
	protected Map<String, String> properties;

	/**
	 * Object to together with monitor operation, to lock the hierarchy during
	 * update operations.
	 */
	protected Lock lock = new ReentrantLock();

	/**
	 * Counter to store the number of composite reader this logical reader is
	 * part of.
	 */
	protected AtomicInteger compositeCount = new AtomicInteger();

	/**
	 * Counter to store the number of active listeners for the logical reader.
	 */
	protected AtomicInteger listenerCount = new AtomicInteger();

	/**
	 * Counter to store the number of all listeners (active and inactive) for
	 * the logical reader.
	 */
	protected AtomicInteger lockCount = new AtomicInteger();

	/**
	 * Dictionary that holds all Tag Operations defined for this logical reader.
	 */
	protected Map<TagOperation, Map<String, Caller<Tag>>> tagOperations;

	/**
	 * Dictionary that holds all port Observations defined for this logical
	 * reader.
	 */
	protected Map<PortObservation, Map<String, Caller<Port>>> portObservations;

	/**
	 * Handler for tag smoothing
	 */
	protected TagSmoothingHandler tagSmoothingHandler;

	/**
	 * Verifies if tag smoothing is defined for this logical reader and to
	 * retrieve the tag smoothing properties.
	 * 
	 * @throws ValidationException
	 */
	protected void setTagSmoothing() throws ValidationException {
		this.tagSmoothingHandler = null;
		Integer glimpsedTimeout = null;
		Integer lostTimeout = null;
		Integer observedCountThreshold = null;
		Integer observedTimeThreshold = null;

		for (Entry<String, String> property : this.properties.entrySet()) {
			try {
				switch (property.getKey()) {
				case Property.GlimpsedTimeout:
					glimpsedTimeout = Integer.valueOf(property.getValue());
					if (glimpsedTimeout.intValue() < 0)
						throw new NumberFormatException();
					break;
				case Property.ObservedTimeThreshold:
					observedTimeThreshold = Integer
							.valueOf(property.getValue());
					if (observedTimeThreshold.intValue() < 0)
						throw new NumberFormatException();
					break;
				case Property.ObservedCountThreshold:
					observedCountThreshold = Integer.valueOf(property
							.getValue());
					if (observedCountThreshold.intValue() < 0)
						throw new NumberFormatException();
					break;
				case Property.LostTimeout:
					lostTimeout = Integer.valueOf(property.getValue());
					if (lostTimeout.intValue() < 0)
						throw new NumberFormatException();
					break;
				default:
					break;
				}
			} catch (NumberFormatException e) {
				throw new ValidationException("Property " + property.getKey()
						+ " must be a positive integer value!");
			}
		}

		if (glimpsedTimeout != null || observedCountThreshold != null
				|| observedTimeThreshold != null || lostTimeout != null) {
			if (observedCountThreshold == null && observedTimeThreshold == null)
				throw new ValidationException(
						"Either property ObservedCountThreshold or ObservedTimeThreshold must be set when using TagSmoothing!");
			else {
				this.tagSmoothingHandler = new TagSmoothingHandler(
						glimpsedTimeout, observedTimeThreshold,
						observedCountThreshold, lostTimeout);
			}
		} else {
			this.tagSmoothingHandler = null;
		}
	}

	/**
	 * @return true when tag smoothing is enabled, false otherwise
	 */
	protected boolean isTagSmoothingEnabled() {
		return this.tagSmoothingHandler != null;
	}

	/**
	 * Initializes a new instance
	 * 
	 * @param name
	 *            The name of the logical reader
	 * @param spec
	 *            The specification of the logical reader
	 * @throws ValidationException
	 * @throws ImmutableReaderException
	 */
	public LogicalReader(String name, LRSpec spec) throws ValidationException,
			ImmutableReaderException {
		this.name = name;
		this.spec = spec;
		this.tagOperations = new HashMap<TagOperation, Map<String, Caller<Tag>>>();
		this.portObservations = new HashMap<PortObservation, Map<String, Caller<Port>>>();
		this.properties = new HashMap<String, String>();
		this.setProperties(spec);

		this.validatePrefix();
		this.validate();
	}

	public LRSpec getSpec() {
		return spec;
	}

	/**
	 * Adds a composite reader to the list of readers this logical reader is
	 * part of.
	 * 
	 * @param composite
	 *            The composite reader to add
	 */
	public void add(CompositeReader composite) {
		this.compositeCount.incrementAndGet();
	}

	/**
	 * Removes a composite reader from the list of reader this logical reader is
	 * part of.
	 * 
	 * @param composite
	 *            The composite reader to remove
	 */
	public void remove(CompositeReader composite) {
		this.compositeCount.decrementAndGet();
	}

	/**
	 * Locks this logical reader and prevent this reader for deletion
	 */
	public void lock() {
		lockCount.incrementAndGet();
	}

	/**
	 * Unlocks this logical reader and release the prevent on deletion.
	 */
	public void unlock() {
		lockCount.decrementAndGet();
	}

	/**
	 * Sets the properties for this logical reader.
	 * 
	 * @param spec
	 *            The specification that contains the properties
	 * @throws ValidationException
	 *             if property name in spec is empty or null
	 * @throws ImmutableReaderException
	 */
	protected void setProperties(LRSpec spec) throws ValidationException,
			ImmutableReaderException {
		List<LRProperty> properties = new ArrayList<LRProperty>();
		if (spec.getProperties() != null) {
			for (LRProperty property : spec.getProperties().getProperty()) {
				if ((property.getName() == null)
						|| (property.getName().length() == 0)) {
					throw new ValidationException(
							"A property name can not be empty or null!");
				} else {
					properties.add(property);
				}
			}
			synchronized (this.properties) {
				this.properties.clear();
				for (LRProperty property : properties) {
					if (!this.properties.containsKey(property.getName()))
						this.properties.put(property.getName(),
								property.getValue());
					else
						throw new ValidationException("The property named '"
								+ property.getName()
								+ "' was specified more than once!");
				}
			}
		}
		this.setTagSmoothing();
	}

	/**
	 * Validates if all properties are recognized.
	 * 
	 * @throws ValidationException
	 *             if a non recognized property found
	 */
	protected abstract void validate() throws ValidationException;

	/**
	 * Method to validate if all property prefixes are know and all properties
	 * without prefix are recognized
	 * 
	 * @throws ValidationException
	 *             if an unkown prefix occurred or an internal property is not
	 *             recognized.
	 */
	protected void validatePrefix() throws ValidationException {
		for (Entry<String, String> property : this.properties.entrySet()) {
			if (property.getKey().startsWith(Prefix.Controller))
				continue;
			if (property.getKey().startsWith(Prefix.Connector))
				continue;
			if (property.getKey().startsWith(Prefix.Reader))
				continue;

			if (property.getKey().contains("."))
				throw new ValidationException("Property '" + property.getKey()
						+ "' contains an unknown prefix!");
		}
	}

	/**
	 * Returns a value that indicates whether this reader is in used or not.
	 * 
	 * @return True if this reader has one or more active listeners
	 */
	public boolean isUsed() {
		return (listenerCount.get() > 0);
	}

	/**
	 * Returns a value that indicates whether this reader is part of a composite
	 * reader or not.
	 * 
	 * @return True if this reader is part of at least one composite reader
	 */
	public boolean isPartOfComposite() {
		return (this.compositeCount.get() > 0);
	}

	/**
	 * Returns a value that indicates whether this reader is lock or not.
	 * 
	 * @return True if this reader is locked
	 */
	public boolean isLocked() {
		return (this.lockCount.get() > 0);
	}

	protected static boolean isEqualSpec(LRSpec spec1, LRSpec spec2) {
		if (spec1 == spec2) {
			return true;
		}
		if (spec2 == null) {
			return false;
		}
		if (spec1.isIsComposite() == null) {
			if (spec2.isIsComposite() != null) {
				return false;
			}
		} else if (!spec1.isIsComposite().equals(spec2.isIsComposite())) {
			return false;
		}
		if (spec1.getProperties() == null) {
			if (spec2.getProperties() != null) {
				return false;
			}
		} else if (spec1.getProperties().getProperty().size() != (spec2.getProperties() != null ? spec2.getProperties().getProperty().size() : -1)) {
			return false;
		} else {
			for (LRProperty property : spec1.getProperties().getProperty()) {
				boolean found = false;
				for (LRProperty newProperty : spec2.getProperties().getProperty()) {
					if (property.getName() != null && property.getName().equals(newProperty.getName())) {
						if ((property.getValue() == null && newProperty.getValue() == null)
								|| (property.getValue() != null && property.getValue().equals(newProperty.getValue()))) {
							found = true;
						}
					}
				}
				if (!found) {
					return false;
				}
			}
		}
		if (spec1.getReaders() == null) {
			if (spec2.getReaders() != null) {
				return false;
			}
		} else if (spec1.getReaders().getReader().size() != (spec2.getReaders() != null ? spec2.getReaders().getReader().size() : -1)) {
			return false;
		} else {
			for (String reader : spec1.getReaders().getReader()) {
				if (!spec2.getReaders().getReader().contains(reader)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Updates this logical reader with the information from spec.
	 * 
	 * @param spec
	 *            The update informations
	 * @param persist
	 *            Persist changes
	 * @throws ImplementationException
	 * @throws InUseException
	 *             if the reader is used by the ale
	 * @throws ValidationException
	 *             if property name in spec is empty or null
	 * @throws ReaderLoopException
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException
	 */
	public void update(LRSpec spec, boolean persist)
			throws ImplementationException, InUseException,
			ValidationException, ReaderLoopException, NoSuchNameException,
			ImmutableReaderException {
		boolean reset = false;
		try {
			lock.lock();

			if (this.isUsed()) {
				// reconnect reader if the spec didn't change and it was disconnected
				if (isEqualSpec(this.spec, spec) && Boolean.FALSE.equals(spec.isIsComposite()) &&
						((BaseReader) this).tryReconnect()) {
					return;
				}
				throw new InUseException(
						"The reader can not be modified because it is in use!");
			} else {
				this.undefineAllPortObservations();
				this.undefineAllTagOperations();
				reset = true;

				try {
					if (this.spec.getProperties() != spec.getProperties()) {
						this.setProperties(spec);
					}
					this.validatePrefix();
					this.validate();
					this.update(spec);
					if (persist) {
						havis.middleware.ale.core.depot.service.lr.LogicalReader
								.getInstance().update(name, spec);
					}
					this.spec = spec;
				} catch (NoSuchIdException e) {
					this.setProperties(this.spec);
					this.update(this.spec);
					throw new ImplementationException(e);
				}
			}
		} finally {
			try {
				if (reset) { // try to reset
					this.defineAllTagOperations();
					this.defineAllPortObservations();
				}
			} catch (Exception e) {
				e.printStackTrace(); // ignore
			} finally {
				lock.unlock(); // unlock in any case
			}
		}
	}

	/**
	 * Undefines this logical reader.
	 * 
	 * @throws InUseException
	 *             if the reader is used by the ale
	 * @throws ImplementationException
	 * @throws ImmutableReaderException
	 */
	public void undefine() throws InUseException, ImplementationException,
			ImmutableReaderException {
		try {
			lock.lock();
			if (this.isUsed() || this.isPartOfComposite() || this.isLocked()) {
				throw new InUseException(
						"The reader can not be removed because it is in use!");
			} else {
				this.undefineReader();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Sets properties
	 * 
	 * @param properties
	 *            The list of properties to set
	 * @param persist
	 *            Persist changes
	 * @throws ImplementationException
	 * @throws InUseException
	 * @throws ValidationException
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException
	 */
	public void setProperties(List<LRProperty> properties, boolean persist)
			throws ImplementationException, InUseException,
			ValidationException, NoSuchNameException, ImmutableReaderException {
		List<LRProperty> tmpProperties = new ArrayList<LRProperty>();

		// add unchanged properties
		if (this.spec.getProperties() != null) {
			for (LRProperty property : this.spec.getProperties().getProperty()) {
				boolean contains = false;
				for (LRProperty newProperty : properties) {
					if (newProperty.getName() != null
							&& newProperty.getName().equals(property.getName())) {
						contains = true;
					}
				}
				if (!contains) {
					tmpProperties.add(property);
				}
			}
		}

		// add changed properties
		for (LRProperty property : properties) {
			if (property.getValue() != null) {
				tmpProperties.add(property);
			}
		}

		LRSpec originalSpec = cloneLRSpec(this.spec);
		LRSpec newSpec = cloneLRSpec(this.spec);
		newSpec.setProperties(new LRSpec.Properties());
		newSpec.getProperties().getProperty().addAll(tmpProperties);
		try {
			this.update(newSpec, persist);
		} catch (ReaderLoopException e) {
			// reset
			try {
				this.update(originalSpec, persist);
			} catch (Exception ex) {
				// ignore
			}
			throw new ImplementationException(e.getReason());
		}
	}

	protected LRSpec cloneLRSpec(LRSpec spec) {
		LRSpec s = new LRSpec();
		s.setIsComposite(spec.isIsComposite());
		s.setProperties(new LRSpec.Properties());
		if (spec.getProperties() != null) {
			s.getProperties().getProperty()
					.addAll(spec.getProperties().getProperty());
		}
		s.setReaders(new LRSpec.Readers());
		if (spec.getReaders() != null) {
			s.getReaders().getReader().addAll(spec.getReaders().getReader());
		}
		return s;
	}

	/**
	 * Gets the property value of the property named propertyName.
	 * 
	 * @param propertyName
	 *            The name of the requested property
	 * @return The requested property value as string or NULL if propertyName is
	 *         unknown
	 */
	public String getPropertyValue(String propertyName) {
		return properties.get(propertyName);
	}

	/**
	 * Indicates whether this instance and a specified object are equal.
	 * 
	 * @param obj
	 *            >The object to compare to
	 * @return True if obj and instance are equals else false
	 */
	@Override
	public boolean equals(Object obj) {
		if ((obj == this) || (obj.hashCode() == hashCode()))
			return true;
		else
			return false;
	}

	/**
	 * Returns the hash code of this instance.
	 * 
	 * @return The requested hash code
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * Defines the tag operation on this logical reader.
	 * 
	 * @param operation
	 *            The operation to define
	 * @param callback
	 *            The method to use for reporting results
	 * @param name
	 *            The name of the parent
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	public void define(TagOperation operation, Caller<Tag> callback, String name)
			throws ImplementationException, ValidationException {
		try {
			lock.lock();
			Map<String, Caller<Tag>> dict = this.tagOperations.get(operation);
			if (dict != null) {
				dict.put(name, callback);
			} else {
				dict = new HashMap<String, Caller<Tag>>();
				dict.put(name, callback);
				this.tagOperations.put(operation, dict);
			}
			try {
				this.defineCurrent(operation, callback, name);
			} catch (ALEException e) {
				this.tagOperations.remove(operation);
				throw e;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Defines all defined tag operations.
	 * 
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	protected void defineAllTagOperations() throws ImplementationException,
			ValidationException {
		for (Entry<TagOperation, Map<String, Caller<Tag>>> operation : this.tagOperations
				.entrySet()) {
			for (Entry<String, Caller<Tag>> callback : operation.getValue()
					.entrySet()) {
				this.defineCurrent(operation.getKey(), callback.getValue(),
						callback.getKey());
			}
		}
	}

	/**
	 * Undefines the reader operation at this logical.
	 * 
	 * @param operation
	 *            The operation to undefine
	 * @param name
	 *            The name of the parent, empty string is default if parent is a
	 *            cycle
	 * @throws ImplementationException
	 */
	public void undefine(TagOperation operation, String name)
			throws ImplementationException {
		try {
			lock.lock();
			Map<String, Caller<Tag>> dict = this.tagOperations.get(operation);
			if (dict != null) {
				if (dict.containsKey(name))
					dict.remove(name);
				if (dict.size() == 0)
					this.tagOperations.remove(operation);
			}
			this.undefineCurrent(operation, name);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Undefines all defined tag operations.
	 * 
	 * @throws ImplementationException
	 */
	protected void undefineAllTagOperations() throws ImplementationException {
		for (Entry<TagOperation, Map<String, Caller<Tag>>> operation : this.tagOperations
				.entrySet()) {
			for (Entry<String, Caller<Tag>> callback : operation.getValue()
					.entrySet()) {
				this.undefineCurrent(operation.getKey(), callback.getKey());
			}
		}
	}

	/**
	 * Defines the port observation at this logical reader.
	 * 
	 * @param observation
	 *            The observation to define
	 * @param callback
	 *            The method to use for reporting results
	 * @param name
	 *            The name of the parent, empty string is default if parent is a
	 *            cycle
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	public void define(PortObservation observation, Caller<Port> callback,
			String name) throws ImplementationException, ValidationException {
		try {
			lock.lock();
			Map<String, Caller<Port>> dict = this.portObservations
					.get(observation);
			if (dict != null) {
				dict.put(name, callback);
			} else {
				dict = new HashMap<String, Caller<Port>>();
				dict.put(name, callback);
				this.portObservations.put(observation, dict);
			}
			try {
				this.defineCurrent(observation, callback, name);
			} catch (ALEException e) {
				this.portObservations.remove(observation);
				throw e;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Defines all defined port observations.
	 * 
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	protected void defineAllPortObservations() throws ImplementationException,
			ValidationException {
		for (Entry<PortObservation, Map<String, Caller<Port>>> operation : this.portObservations
				.entrySet()) {
			for (Entry<String, Caller<Port>> callback : operation.getValue()
					.entrySet()) {
				this.defineCurrent(operation.getKey(), callback.getValue(),
						callback.getKey());
			}
		}
	}

	/**
	 * Undefines the port observation at this logical reader.
	 * 
	 * @param observation
	 *            The observation to undefine
	 * @param name
	 *            The name of the parent, empty string is default if parent is a
	 *            cycle
	 * @throws ImplementationException
	 */
	public void undefine(PortObservation observation, String name)
			throws ImplementationException {
		try {
			lock.lock();
			Map<String, Caller<Port>> dict = this.portObservations
					.get(observation);
			if (dict != null) {
				if (dict.containsKey(name))
					dict.remove(name);
				if (dict.size() == 0)
					this.portObservations.remove(observation);
			}
			this.undefineCurrent(observation, name);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Undefines all defined port observations.
	 * 
	 * @throws ImplementationException
	 */
	protected void undefineAllPortObservations() throws ImplementationException {
		for (Entry<PortObservation, Map<String, Caller<Port>>> operation : this.portObservations
				.entrySet()) {
			for (Entry<String, Caller<Port>> callback : operation.getValue()
					.entrySet()) {
				this.undefineCurrent(operation.getKey(), callback.getKey());
			}
		}
	}

	/**
	 * Method to update this logical reader according to spec. Method can cause
	 * ReaderLoopException.
	 * 
	 * @param spec
	 *            The update informations
	 * @throws ValidationException
	 * @throws ImplementationException
	 * @throws ReaderLoopException
	 * @throws ImmutableReaderException
	 */
	protected abstract void update(LRSpec spec) throws ValidationException,
			ImplementationException, ReaderLoopException,
			ImmutableReaderException;

	/**
	 * Undefines this logical reader.
	 * 
	 * @throws ImplementationException
	 */
	protected abstract void undefineReader() throws ImplementationException;

	/**
	 * Adds a reader to this logical reader. BaseReader will raise
	 * NonCompositeReaderException.
	 * 
	 * @param readers
	 *            The list of names of logical readers to add
	 * @param persist
	 *            Persist changes
	 * @throws NonCompositeReaderException
	 * @throws ValidationException
	 * @throws InUseException
	 * @throws ImplementationException
	 * @throws ReaderLoopException
	 *             if loop is detected
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException
	 */
	public abstract void add(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImplementationException,
			InUseException, ValidationException, ReaderLoopException,
			NoSuchNameException, ImmutableReaderException;

	/**
	 * Sets the readers for this logical reader.
	 * 
	 * @param readers
	 *            The list of names of logical readers to set
	 * @param persist
	 *            Persist changes
	 * @throws NonCompositeReaderException
	 *             if logical reader is a base reader
	 * @throws ValidationException
	 * @throws InUseException
	 * @throws ImplementationException
	 * @throws ReaderLoopException
	 *             if loop is detected
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException
	 */
	public abstract void set(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImplementationException,
			InUseException, ValidationException, ReaderLoopException,
			NoSuchNameException, ImmutableReaderException;

	/**
	 * Removes reader form this logical reader.
	 * 
	 * @param readers
	 *            The list of names of ligical readers to remove
	 * @param persist
	 *            Persist changes
	 * @throws NonCompositeReaderException
	 *             if logical reader is a base reader
	 * @throws ReaderLoopException
	 * @throws ValidationException
	 * @throws InUseException
	 * @throws ImplementationException
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException
	 */
	public abstract void remove(List<String> readers, boolean persist)
			throws NonCompositeReaderException, ImplementationException,
			InUseException, NoSuchNameException, ImmutableReaderException;

	/**
	 * Returns a value that indicates if this logical reader contains reader.
	 * 
	 * @param reader
	 *            The reader to check
	 * @return True if this contains reader
	 */
	public abstract boolean contains(LogicalReader reader);

	/**
	 * Defines the reader operation.
	 * 
	 * @param operation
	 *            The operation
	 * @param callback
	 *            The method to use for reporting results
	 * @param name
	 *            The name of the parent
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	protected abstract void defineCurrent(TagOperation operation,
			Caller<Tag> callback, String name) throws ImplementationException,
			ValidationException;

	/**
	 * Undefines the reader operation.
	 * 
	 * @param operation
	 *            The operation
	 * @param name
	 *            The name of the parent
	 * @throws ImplementationException
	 */
	protected abstract void undefineCurrent(TagOperation operation, String name)
			throws ImplementationException;

	/**
	 * Enables the reader operation.
	 * 
	 * @param operation
	 *            The operation
	 * @throws ImplementationException
	 */
	public abstract void enable(TagOperation operation)
			throws ImplementationException;

	/**
	 * Disable the reader operation.
	 * 
	 * @param operation
	 *            The operation
	 * @throws ImplementationException
	 */
	public abstract void disable(TagOperation operation)
			throws ImplementationException;

	/**
	 * Defines the port observation at this logical reader.
	 * 
	 * @param observation
	 *            The observation
	 * @param callback
	 *            The method to use for reporting results
	 * @param name
	 *            The name of the parent
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	protected abstract void defineCurrent(PortObservation observation,
			Caller<Port> callback, String name) throws ImplementationException,
			ValidationException;

	/**
	 * Undefines the port observation at this logical reader.
	 * 
	 * @param observation
	 *            The observation
	 * @param name
	 *            The name of the parent
	 * @throws ImplementationException
	 */
	protected abstract void undefineCurrent(PortObservation observation,
			String name) throws ImplementationException;

	/**
	 * Enables the port observation on this logical reader.
	 * 
	 * @param observation
	 *            The observation
	 * @throws ImplementationException
	 */
	public abstract void enable(PortObservation observation)
			throws ImplementationException;

	/**
	 * Disables the port observation on this logical reader.
	 * 
	 * @param observation
	 *            The observation
	 * @throws ImplementationException
	 */
	public abstract void disable(PortObservation observation)
			throws ImplementationException;

	/**
	 * Executes a port operation.
	 * 
	 * @param operation
	 *            The port operation to execute
	 * @param callback
	 *            The method to use for reporting results
	 * @throws ImplementationException
	 * @throws ValidationException
	 */
	public abstract void execute(PortOperation operation, Caller<Port> callback)
			throws ImplementationException, ValidationException;

	/**
	 * Returns the reader configuration
	 * 
	 * @return The reader configuration
	 * @throws ImplementationException
	 * @throws NonBaseReaderException
	 */
	public abstract RCConfig getConfig() throws ImplementationException,
			NonBaseReaderException;

	public String getName() {
		return name;
	}
}
