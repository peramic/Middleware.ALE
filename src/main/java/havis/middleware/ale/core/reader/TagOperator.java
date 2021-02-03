package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.reader.ReaderConnector;

import java.util.Map.Entry;

public class TagOperator extends Operator<Tag> {

	TagOperation operation;

	public TagOperator(CallbackHandler callbackHandler, long id, ReaderController controller, ReaderConnector connector, TagOperation operation)
			throws ValidationException, ImplementationException {
		super(callbackHandler, id, controller, connector);
		this.operation = operation;

		try {
			define();
		} catch (ValidationException | ImplementationException e) {
			removeCallback();
		}
	}

	@Override
	public void receive(Tag tag) {
		if (active) {
			if (tag != null) {
				if ((tag.getEpc() != null) && !(Tag.isExtended() && (tag.getTid() == null))) {

					for (Entry<String, Caller<Tag>> caller : callers.entrySet()) {
						try {
							caller.getValue().invoke(tag, controller);
						} catch (Exception e) {
							error(caller.getKey(), "An error occurred while notifying tag", e);
						}
					}

				}
			}
		}
	}

	private void define() throws ValidationException, ImplementationException {
		connector.defineTagOperation(id, operation);
	}

	private void undefine() throws ImplementationException {
		connector.undefineTagOperation(id);
	}

	public void enable() {
		if (!active) {
			try {
				connector.enableTagOperation(id);
				active = true;
			} catch (Exception e) {
				error(controller.getName(), "An error occurred while enabling tag operation", e);
			}
		}
	}

	public void disable() {
		if (active) {
			active = false;
			try {
				connector.disableTagOperation(id);
			} catch (Exception e) {
				error(controller.getName(), "An error occurred while disabling tag operation", e);
			}
		}
	}

	@Override
	public void dispose() throws ImplementationException {
		undefine();
		super.dispose();
	}
}