package havis.middleware.ale.core.reader;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.tag.Operation;
import havis.middleware.ale.base.operation.tag.OperationType;
import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.base.operation.tag.TagOperation;
import havis.middleware.ale.base.operation.tag.result.FaultResult;
import havis.middleware.ale.base.operation.tag.result.Result;
import havis.middleware.ale.base.operation.tag.result.ResultState;
import havis.middleware.ale.base.operation.tag.result.WriteResult;
import havis.middleware.ale.exit.Exits;
import havis.middleware.ale.reader.ReaderConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TagExecutor extends Executor<Tag> {

	private TagOperation operation;
	private int timeoutMs;
	private boolean optimizeWriteOperations;
	private Map<Integer, WriteResult> optimizedWriteResults;
	private AtomicBoolean errorState;
	private CountDownLatch received = new CountDownLatch(1);

	public TagExecutor(CallbackHandler callbackHandler, long id, ReaderController controller, ReaderConnector connector, String name, Caller<Tag> caller,
			TagOperation operation, int timeoutMs, boolean optimizeWriteOperations, AtomicBoolean errorState) {
		super(callbackHandler, id, controller, connector, name, caller);
		this.operation = operation;
		this.timeoutMs = timeoutMs;
		this.optimizeWriteOperations = optimizeWriteOperations;
		this.errorState = errorState;
	}

	@Override
	public void execute() {
		try {
			if (this.controller.isConnected()) {
				if (!this.errorState.get()) {
					try {
						TagOperation tagOperation = getTagOperation();
						this.connector.executeTagOperation(this.id, tagOperation);
						// wait here for the result
						if (!received()) {
							Exits.Log.logp(Exits.Level.Warning, Exits.Reader.Controller.Name, Exits.Reader.Controller.Warning,
									"Reader {0} failed to send result for tag operation after " + this.timeoutMs + " ms",
									new Object[] { this.controller.getName(), tagOperation });
							if (!this.errorState.get()) {
								this.errorState.set(true);

								if (this.id > 0) {
									try {
										this.connector.abortTagOperation(this.id);
									} catch (Exception e) {
										Exits.Log.logp(Exits.Level.Error, Exits.Reader.Controller.Name, Exits.Reader.Controller.Error, "Reader "
												+ this.controller.getName() + " failed to abort tag operation", e);
									}
								}
								sendFaultResult();
							}
						}
					} catch (ValidationException e) {
						// not connected anymore
						sendEmptyResult();
					} catch (Exception e) {
						Exits.Log.logp(Exits.Level.Error, Exits.Reader.Controller.Name, Exits.Reader.Controller.Error, "Reader " + this.controller.getName()
								+ " failed to execute tag operation", e);
						sendFaultResult();
					}
				} else {
					// already in error state
					sendFaultResult();
				}
			} else {
				// not connected
				sendEmptyResult();
			}
		} finally {
			removeCallback();
		}
	}

	private TagOperation getTagOperation() {
		this.optimizedWriteResults = new HashMap<>();
		if (this.optimizeWriteOperations && this.operation.getOperations() != null && this.operation.getOperations().size() > 0) {
			// remove all write operations which are overwritten by later write operations
			List<Integer> skippedOperations = new ArrayList<>();
			for (int i = this.operation.getOperations().size() - 1; i >= 0; i--) {
				Operation current = this.operation.getOperations().get(i);
				for (int j = i - 1; j >= 0; j--) {
					Integer index = Integer.valueOf(j);
					if (!skippedOperations.contains(index)) {
						Operation previous = this.operation.getOperations().get(j);
						if (overwrites(current, previous)) {
							skippedOperations.add(index);
						} else {
							// only optimize block wise
							break;
						}
					}
				}
			}
			if (skippedOperations.size() > 0) {
				List<Operation> operations = new ArrayList<>();
				for (int i = 0; i < this.operation.getOperations().size(); i++) {
					Operation current = this.operation.getOperations().get(i);
					if (!skippedOperations.contains(Integer.valueOf(i))) {
						operations.add(current);
					} else {
						this.optimizedWriteResults.put(Integer.valueOf(current.getId()), new WriteResult(ResultState.SUCCESS));
					}
				}
				return new TagOperation(operations, this.operation.getFilter());
			}
		}
		return this.operation;
	}

	private boolean overwrites(Operation current, Operation previous) {
		if (current.getType() == OperationType.WRITE && previous.getType() == OperationType.WRITE && current.getField() != null && previous.getField() != null
				&& current.getField().getBank() == previous.getField().getBank() && current.getData() != null && previous.getData() != null) {
			return current.getField().getOffset() <= previous.getField().getOffset()
					&& (current.getField().getOffset() + current.getField().getLength()) >= (previous.getField().getOffset() + previous.getField().getLength());
		}
		return false;
	}

	private boolean received() {
		try {
			return received.await(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return true;
		}
	}

	@Override
	public void receive(Tag tag) {
		if (this.errorState.get()) {
			// ignore
		} else {
			this.errorState.set(false);
			for (Entry<Integer, Result> entry : tag.getResult().entrySet()) {
				if (entry.getValue().getState() != ResultState.SUCCESS) {
					this.errorState.set(true);
					break;
				}
			}

			// make sure the EPC has the original value
			tag.apply(this.operation.getFilter());

			for (Operation op : this.operation.getOperations()) {
				Integer id = Integer.valueOf(op.getId());
				Result result = getResult(tag, id);
				if (result != null) {
					if (result.getState() == ResultState.SUCCESS) {
						if (result instanceof WriteResult) {
							tag.apply(op);
						}
					} else {
						this.errorState.set(true);
					}
				} else {
					tag.getResult().put(id, new FaultResult(ResultState.MISC_ERROR_TOTAL));
				}
			}

			try {
				this.caller.invoke(tag, this.controller);
			} finally {
				received.countDown();
			}
		}
	}

	private Result getResult(Tag tag, Integer id) {
		Result result = tag.getResult().get(id);
		if (result == null) {
			result = this.optimizedWriteResults.get(id);
			if (result != null) {
				tag.getResult().put(id, result);
			}
		}
		return result;
	}

	private void sendEmptyResult() {
		Tag tag = new Tag(this.operation.getFilter());
		tag.setCompleted(true);
		this.caller.invoke(tag, this.controller);
	}

	private void sendFaultResult() {
		Map<Integer, Result> result = new HashMap<Integer, Result>();
		for (Operation operation : this.operation.getOperations()) {
			result.put(Integer.valueOf(operation.getId()), new FaultResult(ResultState.MISC_ERROR_TOTAL));
		}
		Tag tag = new Tag(this.operation.getFilter());
		tag.setResult(result);
		this.caller.invoke(tag, this.controller);
	}
}