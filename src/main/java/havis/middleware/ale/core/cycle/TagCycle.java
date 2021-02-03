package havis.middleware.ale.core.cycle;

import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.TagDecoder;
import havis.middleware.ale.core.reader.Caller;
import havis.middleware.ale.core.reader.ReaderController;
import havis.middleware.ale.core.report.IDatas;
import havis.middleware.ale.core.report.IReports;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a common tag cycle
 *
 * @param <Spec>
 *            The specification type
 * @param <Result>
 *            The result type
 * @param <Reports>
 *            The reports type
 * @param <Tags>
 *            The tags type
 */
public abstract class TagCycle<Spec, Result extends havis.middleware.ale.service.IReports, Reports extends IReports<Result, Tags>, Tags extends IDatas>
		extends CommonCycle<Spec, Result, Reports, Tags, Tag> {

	/**
	 * Creates a new instance. Keep parameters and initializes class parameters
	 *
	 * @param name
	 *            The cycle name
	 * @param spec
	 *            The cycle specification
	 */
	protected TagCycle(String name, Spec spec) {
		super(name, spec);
	}

	protected abstract void onNotifyStarted(String name, String reader, Tag tag);

	protected abstract void onFiltered(String name, String reader, Tag tag);

	/**
	 * Retrieves the reader operation
	 */
	protected abstract TagOperation getTagOperation();

	/**
	 * Defines reader operation on each reader in logical reader list
	 *
	 * @throws ValidationException
	 */
	@Override
	protected void define() throws ImplementationException, ValidationException {
		if (getTagOperation() != null) {
			readersLock.lock();
			try {
				List<LogicalReader> list = new ArrayList<LogicalReader>();
				try {
					for (LogicalReader logicalReader : logicalReaders) {
						final String name = logicalReader.getName();
						logicalReader.define(getTagOperation(), new Caller<Tag>() {
							@Override
							public void invoke(Tag tag, ReaderController controller) {
								TagCycle.this.notify(name, tag, controller);
							}
						}, this.guid);
						list.add(logicalReader);
					}
				} catch (ALEException e) {
					for (LogicalReader logicalReader : list) {
						try {
							logicalReader.undefine(getTagOperation(), this.guid);
						} catch (ImplementationException e1) {
							e1.printStackTrace();
						}
					}
					throw e;
				}
			} finally {
				readersLock.unlock();
			}
		}
	}

	/**
	 * Un-defines logical readers
	 */
	@Override
	protected void undefine() {
		if (getTagOperation() != null) {
			readersLock.lock();
			try {
				for (LogicalReader logicalReader : logicalReaders) {
					try {
						logicalReader.undefine(getTagOperation(), this.guid);
					} catch (ImplementationException e) {
						e.printStackTrace();
					}
				}
			} finally {
				readersLock.unlock();
			}
		}
	}

	/**
	 * Disables logical readers
	 */
	@Override
	protected void disable() {
		if (getTagOperation() != null) {
			readersLock.lock();
			try {
				for (LogicalReader logicalReader : logicalReaders) {
					try {
						logicalReader.disable(getTagOperation());
					} catch (ImplementationException e) {
						e.printStackTrace();
					}
				}
			} finally {
				readersLock.unlock();
			}
		}
	}

	/**
	 * Enables logical readers
	 */
	@Override
	protected void enable() {
		if (getTagOperation() != null) {
			readersLock.lock();
			try {
				for (LogicalReader logicalReader : logicalReaders) {
					try {
						logicalReader.enable(getTagOperation());
					} catch (ImplementationException e) {
						e.printStackTrace();
					}
				}
			} finally {
				readersLock.unlock();
			}
		}
	}

	@Override
	protected void notify(String reader, Tag tag, ReaderController controller) {
		onNotifyStarted(name, reader, TagDecoder.getInstance().enable(tag));
	}
}