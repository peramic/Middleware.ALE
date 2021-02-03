package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ImmutableReaderException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ReaderLoopException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Port;
import havis.middleware.ale.base.operation.port.PortObservation;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.service.lr.LRSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * Dummy composite reader, when mocking is not possible
 */
public class DummyCompositeReader extends CompositeReader {

	private Map<String, Map<String, Object>> lastCall = new HashMap<>();

	private boolean throwExceptionOnDefine = false;

	/**
	 * @return the last call in this format: Map&lt;method, Map&lt;argument,
	 *         value&gt;&gt;
	 */
	public Map<String, Map<String, Object>> getLastCall() {
		return lastCall;
	}

	public DummyCompositeReader(String name) throws ValidationException, ReaderLoopException, ImmutableReaderException {
		super(name, new LRSpec() {
			{
				setReaders(new LRSpec.Readers());
				setProperties(new LRSpec.Properties());
			}
		});
	}

	public void setThrowExceptionOnDefine(boolean throwExceptionOnDefine) {
		this.throwExceptionOnDefine = throwExceptionOnDefine;
	}

	@Override
	public void define(TagOperation operation, Caller<Tag> callback, String name) throws ImplementationException, ValidationException {
		if (throwExceptionOnDefine) {
			throw new ImplementationException();
		}

		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("operation", operation);
		arguments.put("callback", callback);
		arguments.put("name", name);
		lastCall.put("define", arguments);
	}

	@Override
	public void define(PortObservation observation, Caller<Port> callback, String name) throws ImplementationException, ValidationException {
		if (throwExceptionOnDefine) {
			throw new ImplementationException();
		}

		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("observation", observation);
		arguments.put("callback", callback);
		arguments.put("name", name);
		lastCall.put("define", arguments);
	}

	@Override
	public void undefine(TagOperation operation, String name) throws ImplementationException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("operation", operation);
		arguments.put("name", name);
		lastCall.put("undefine", arguments);
	}

	@Override
	public void undefine(PortObservation observation, String name) throws ImplementationException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("observation", observation);
		arguments.put("name", name);
		lastCall.put("undefine", arguments);
	}

	@Override
	public void enable(TagOperation operation) throws ImplementationException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("operation", operation);
		lastCall.put("enable", arguments);
	}

	@Override
	public void disable(TagOperation operation) throws ImplementationException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("operation", operation);
		lastCall.put("disable", arguments);
	}

	@Override
	public void enable(PortObservation observation) throws ImplementationException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("observation", observation);
		lastCall.put("enable", arguments);
	}

	@Override
	public void disable(PortObservation observation) throws ImplementationException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("observation", observation);
		lastCall.put("disable", arguments);
	}
}
