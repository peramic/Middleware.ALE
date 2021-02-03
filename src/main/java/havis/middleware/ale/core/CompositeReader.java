package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NonBaseReaderException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.port.PortOperation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.reader.Property;
import havis.middleware.ale.service.lr.LRSpec;
import havis.middleware.ale.service.lr.LRSpec.Readers;
import havis.middleware.ale.service.rc.RCConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that represents a composite reader which is aggregation of one or more
 * other logical reader.
 */
public class CompositeReader extends LogicalReader {

	/**
	 * Readers lock
	 */
	private Lock lock = new ReentrantLock();

	/**
	 * List of component readers for this composite reader.
	 */
	private List<LogicalReader> readers;

	/**
	 * Indicates if antenna restriction is activated for this composite reader
	 */
	protected boolean restricted = false;

	/**
	 * Tag smoothing lost timeout defined for this logical reader
	 */
	protected short antenna = 0;

	/**
	 * Initializes a new instance of the
	 * Havis.Middleware.ALE.LogicalReader.CompositeReader class.
	 *
	 * @param name
	 *            The name of the logical reader
	 * @param spec
	 *            The specification of the logical reader
	 * @throws ValidationException
	 * @throws ReaderLoopException
	 * @throws ImmutableReaderException 
	 */
	public CompositeReader(String name, LRSpec spec)
			throws ValidationException, ReaderLoopException, ImmutableReaderException {
		super(name, spec);
		readers = new ArrayList<LogicalReader>();
		this.set(spec);
		try {
			this.detectAntennaRestriction();
		} catch (Exception e) {
			this.removeAll();
			throw e;
		}
	}

	/**
	 * Sets the list of component readers to the list specified in spec.
	 *
	 * @param spec
	 *            The specification containing the new list of component readers
	 * @throws ValidationException
	 * @throws ReaderLoopException
	 */
	private void set(LRSpec spec) throws ValidationException,
			ReaderLoopException {
		List<LogicalReader> readers = new ArrayList<LogicalReader>();

		try {
			if (spec.getReaders() == null) {
				spec.setReaders(new LRSpec.Readers());
			}
			for (String name : spec.getReaders().getReader()) {
				LogicalReader reader;
				try {
					reader = LR.getInstance().lock(name);
				} catch (NoSuchNameException e) {
					throw new ValidationException("The reader '" + name
							+ "' is not a known logical reader!");
				}

				if (!reader.contains(this)) {
					readers.add(reader);
				} else {
					reader.unlock();
					throw new ReaderLoopException("Adding the reader '" + name
							+ "' would cause a hierarchy loop!");
				}
			}
		} catch (ValidationException | ReaderLoopException e) {
		    // unlock all readers we have collected before
			for (LogicalReader reader : readers) {
				reader.unlock();
			}
			throw e;
		}
		this.removeAll(); // remove old readers
		this.add(readers);
	}

	/**
	 * Adds the component readers
	 *
	 * @param readers
	 *            The list of readers to add
	 */
	private void add(List<LogicalReader> readers) {
		for (LogicalReader reader : readers) {
			this.add(reader);
		}
	}

	/**
	 * Removes all component readers from this composite reader.
	 */
	private void removeAll() {
		for (LogicalReader reader : readers) {
			reader.remove(this);
			reader.unlock();
		}
		this.readers.clear();
	}

	/**
	 * Adds the component reader
	 *
	 * @param reader
	 *            The component reader to add
	 */
	private void add(LogicalReader reader) {
		try {
			lock.lock();
			if (!this.readers.contains(reader)) {
				this.readers.add(reader);
				reader.add(this);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Validates if all properties are recognized.
	 *
	 * @throws ValidationException
	 *             for the first non recognized property
	 */
	@Override
    protected void validate() throws ValidationException {
		for (Entry<String, String> property : this.properties.entrySet()) {
			switch (property.getKey()) {
			case Property.AntennaID:
			case Property.GlimpsedTimeout:
			case Property.ObservedTimeThreshold:
			case Property.ObservedCountThreshold:
			case Property.LostTimeout:
				break;
			default:
				throw new ValidationException("Property '" + property.getKey()
						+ "' is not recognized!");
			}
		}
	}

	/**
	 * Updates this logical reader according to spec
	 *
	 * @param spec
	 *            The update informations
	 */
	@Override
    protected void update(LRSpec spec) throws ValidationException,
			ReaderLoopException {
		if (Boolean.FALSE.equals(spec.isIsComposite())) {
			throw new ValidationException(
					"To change the isComposite flag during an update is not allowed!");
		} else {
			if (spec.getReaders() == null) {
				spec.setReaders(new Readers());
			}
            if (!this.spec.getReaders().getReader().equals(spec.getReaders().getReader())) {
                this.set(spec);
            }
            boolean success = false;
            try {
                this.detectAntennaRestriction();
                success = true;
            } finally {
                if (!success) { // reset if validation failed
                    this.set(this.spec);
                }
            }
        }
    }

	/**
	 * Undefines this logical reader.
	 */
	@Override
    protected void undefineReader() {
		this.removeAll();
	}

	/**
	 * Adds a reader to this logical reader.
	 *
	 * @param readers
	 *            The list of names of logical readers to add
	 * @param persist
	 *            Persist changes
	 * @throws ReaderLoopException
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException 
	 */
	@Override
    public void add(List<String> readers, boolean persist)
			throws ImplementationException, InUseException,
			ValidationException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
		LRSpec spec = cloneLRSpec(this.spec);
		if (readers != null) {
			for (String reader : readers) {
				if (!spec.getReaders().getReader().contains(reader)) {
					spec.getReaders().getReader().add(reader);
				}
			}
		}
		this.update(spec, persist);
	}

	/**
	 * Set the readers for this logical reader.
	 *
	 * @param readers
	 *            The list of names of logical readers to set
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException 
	 */
	@Override
    public void set(List<String> readers, boolean persist)
			throws ImplementationException, InUseException,
			ValidationException, ReaderLoopException, NoSuchNameException, ImmutableReaderException {
		LRSpec spec = cloneLRSpec(this.spec);
		spec.setReaders(new Readers());
		if (readers != null) {
			for (String reader : readers) {
				if (!spec.getReaders().getReader().contains(reader)) {
					spec.getReaders().getReader().add(reader);
				}
			}
		}
		this.update(spec, persist);
	}

	/**
	 * Removes reader form this logical reader.
	 *
	 * @param readers
	 *            The list of names of logical readers to remove
	 * @param persist
	 *            Persist changes
	 * @throws NoSuchNameException
	 * @throws ImmutableReaderException 
	 */
	@Override
    public void remove(List<String> readers, boolean persist)
			throws ImplementationException, InUseException, NoSuchNameException, ImmutableReaderException {
		LRSpec spec = cloneLRSpec(this.spec);
		List<String> tmpReaders = new ArrayList<String>();
		if (!spec.getReaders().getReader().isEmpty()) {
			for (String reader : spec.getReaders().getReader()) {
				if (readers == null || !readers.contains(reader)) {
					tmpReaders.add(reader);
				}
			}
		}
		spec.setReaders(new Readers());
		for (String reader : tmpReaders) {
            spec.getReaders().getReader().add(reader);
		}
		try {
			this.update(spec, persist);
		} catch (ValidationException | ReaderLoopException e) {
			throw new ImplementationException(e.getReason());
		}
	}

	/**
	 * Returns a value that indicates if this logical reader contains reader
	 * 
	 * @param reader
	 *            The reader to check
	 * @return True if this contains reader
	 */
	@Override
    public boolean contains(LogicalReader reader) {
		if (this == reader) {
			return true;
		}
		for (LogicalReader read : readers) {
			if (read.contains(reader))
				return true;
		}
		return false;
	}

	/**
	 * Defines the reader operation at the this logical.
	 *
	 * @param operation
	 *            The operation to define
	 * @param callback
	 *            The method to use for reporting results
	 * @param name
	 *            The name of the parent
	 * @throws ValidationException
	 */
	@Override
	protected void defineCurrent(TagOperation operation, final Caller<Tag> callback, String name) throws ImplementationException, ValidationException {
		List<LogicalReader> list = new ArrayList<>();
		String id = name + "-" + this.guid;
		try {
			for (LogicalReader reader : readers) {
				if (isTagSmoothingEnabled()) {
					reader.define(operation, new Caller<Tag>() {
						@Override
						public void invoke(Tag tag, ReaderController controller) {
							if (antennaRestrictionCheck(tag)) {
								tagSmoothingHandler.process(tag, callback, controller);
							}
						}
					}, id);
				} else if (restricted) {
					reader.define(operation, new Caller<Tag>() {
						@Override
						public void invoke(Tag tag, ReaderController controller) {
							if (antennaRestrictionCheck(tag)) {
								callback.invoke(tag, controller);
							}
						}
					}, id);
				} else {
					reader.define(operation, callback, id);
				}
				list.add(reader);
			}
		} catch (ALEException e) {
			for (LogicalReader reader : list) {
				reader.undefine(operation, id);
			}
			throw e;
		}
	}

	/**
	 * Undefines the reader operation <paramref name="operation"/> at this
	 * logical.
	 *
	 * @param operation
	 *            The operation to undefine
	 * @param name
	 *            The name of the parent
	 */
	@Override
    protected void undefineCurrent(TagOperation operation, String name)
			throws ImplementationException {
		String id = name + "-" + this.guid;
		for (LogicalReader reader : readers) {
			reader.undefine(operation, id);
		}
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
			this.lock.lock();
			synchronized (readers) {
				for (LogicalReader reader : readers) {
					reader.enable(operation);
				}
			}
			listenerCount.incrementAndGet();
		} finally {
			this.lock.unlock();
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
			this.lock.lock();
			synchronized (readers) {
				for (LogicalReader reader : readers) {
					reader.disable(operation);
				}
			}
			listenerCount.decrementAndGet();
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Returns the reader configuration
	 *
	 * @return The reader configuration
	 */
	@Override
    public RCConfig getConfig() throws NonBaseReaderException {
		throw new NonBaseReaderException(
				"Try to get configuration from composite reader!");
	}

	/**
	 * Defines port observation
	 *
	 * @param observation
	 *            The observation
	 * @param callback
	 *            The port callback
	 * @param name
	 *            The name of the parent
	 * @throws ValidationException
	 */
	@Override
	protected void defineCurrent(PortObservation observation, Caller<Port> callback, String name) throws ImplementationException, ValidationException {
		List<LogicalReader> list = new ArrayList<LogicalReader>();
		String id = name + "-" + this.guid;
		try {
			for (LogicalReader reader : readers) {
				reader.define(observation, callback, id);
				list.add(reader);
			}
		} catch (ALEException e) {
			for (LogicalReader reader : list) {
				reader.undefine(observation, id);
			}
			throw e;
		}
	}

	/**
	 * Undefines port observation
	 *
	 * @param observation
	 *            The observation
	 * @param name
	 *            The name of the parent
	 */
	@Override
    protected void undefineCurrent(PortObservation observation, String name)
			throws ImplementationException {
		String id = name + "-" + this.guid;
		for (LogicalReader reader : readers) {
			reader.undefine(observation, id);
		}
	}

	/**
	 * Enables port observation
	 *
	 * @param observation
	 *            The observation
	 */
	@Override
    public void enable(PortObservation observation)
			throws ImplementationException {
		try {
			this.lock.lock();
			for (LogicalReader reader : readers) {
				reader.enable(observation);
			}
			listenerCount.incrementAndGet();
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Disables port observation
	 *
	 * @param observation
	 *            The observation
	 */
	@Override
    public void disable(PortObservation observation)
			throws ImplementationException {
		try {
			this.lock.lock();
			for (LogicalReader reader : readers) {
				reader.disable(observation);
			}
			listenerCount.decrementAndGet();
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Executes port operation
	 *
	 * @param operation
	 *            The port operation
	 */
	@Override
    public void execute(PortOperation operation, Caller<Port> callback)
			throws ValidationException {
		throw new ValidationException(
				"Could not execute a port operation on a composite reader!");
	}

	/**
	 * Verifies if antenna restriction is defined for this composite reader and
	 * to retrieve the antenna restriction properties
	 *
	 * @throws ValidationException
	 */
	private void detectAntennaRestriction() throws ValidationException {
	    short antenna = 0;
		boolean restricted = false;

		for (Entry<String, String> property : this.properties.entrySet()) {
			try {
				switch (property.getKey()) {
				case Property.AntennaID:
				    antenna = Short.parseShort(property.getValue());
					restricted = true;
					break;
				default:
					break;
				}
			} catch (Exception e) {
				throw new ValidationException("Property " + property.getKey()
						+ " must be integer value 0-65535!");
			}
		}

		if (restricted
				&& (this.readers.size() != 1 || !(this.readers.get(0) instanceof BaseReader))) {
			throw new ValidationException(
					"If Property 'AntennaID' is defined composite reader must contain exectly 1 BaseReader");
		}

		this.antenna = antenna;
		this.restricted = restricted;
	}

	/**
	 * Verifies if an Tag matches the antenna restriction.
	 *
	 * @param tag
	 *            The tag to match
	 * @return Returns true if the tag matches the antenna restriction
	 */
	private boolean antennaRestrictionCheck(Tag tag) {
		if (this.restricted) {
			if ((tag.getSighting() != null)
					&& (tag.getSighting().getAntenna() == this.antenna))
				return true;
		} else {
			return true;
		}
		return false;
	}
}
