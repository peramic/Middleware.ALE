package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.base.operation.port.result.ReadResult;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.port.result.Result.State;
import havis.middleware.ale.core.CompositeReader;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.Time;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.service.pc.PCOpReport;
import havis.middleware.ale.service.pc.PCOpSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a port operation
 */
public class Operation {

    private LogicalReader logicalReader;

    private havis.middleware.ale.base.operation.port.Operation operation;

    private PCOpSpec spec;

    /**
     * Creates a new instance
     *
     * @param spec
     *            The operation specification
     * @throws ValidationException
     */
    public Operation(PCOpSpec spec) throws ValidationException {
    	if (spec.getPortSpec() == null)
            throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. No port specified.");
        if ((spec.getPortSpec().getReader() == null) || (spec.getPortSpec().getReader().length() == 0))
            throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. Reader name could not be null or empty.");
        if (spec.getPortSpec().getType() == null)
            throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. No port type specified.");
        if (spec.getOpType() == null || spec.getOpType().isEmpty())
            throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. No operation type specified.");
        if (spec.getPortSpec().getId() < 0)
            throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. Port ID must be a positive integer.");

        havis.middleware.ale.base.operation.port.Operation.Type operationType;
        try {
            operationType = havis.middleware.ale.base.operation.port.Operation.Type.valueOf(spec.getOpType());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. Unknown operation type '" + spec.getOpType() + "'");
        }

        Pin.Type pinType;
        try {
            pinType = Pin.Type.valueOf(spec.getPortSpec().getType());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. Unknown port type '" + spec.getPortSpec().getType()
                    + "'");
        }

        Byte data = null;
        Long duration = null;
        switch (operationType) {
        case READ:
            if (spec.isState() != null) {
                throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. State must be omitted for read operation.");
            }
            if (spec.getDuration() != null) {
                throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. Duration must be omitted for read operation.");
            }
            break;
        case WRITE:
            if (spec.isState() == null) {
                throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. State of write operation is missing.");
            }
            data = Byte.valueOf((byte) (Boolean.TRUE.equals(spec.isState()) ? 1 : 0));

            try {
                duration = Long.valueOf(Time.getValue(spec.getDuration()));
            } catch (ValidationException e) {
                e.setReason("Invalid operation specification '" + spec.getOpName() + "'. Duration of write operation invalid. " + e.getReason());
                throw e;
            }
            break;
        }

        Pin pin = new Pin(spec.getPortSpec().getId(), pinType);
        this.operation = new havis.middleware.ale.base.operation.port.Operation(spec.getOpName(), operationType, data, duration, pin);
        this.spec = spec;

        String reader = spec.getPortSpec().getReader();
        try {
            this.logicalReader = LR.getInstance().lock(reader);
			if (this.logicalReader instanceof CompositeReader) {
				logicalReader.unlock();
				logicalReader = null;
				throw new ValidationException("Invalid operation specification '" + spec.getOpName()
						+ "'. Could not execute a port operation on a composite reader '" + reader + "'");
			}
        } catch (NoSuchNameException e) {
            throw new ValidationException("Invalid operation specification '" + spec.getOpName() + "'. Unknown reader '" + reader + "'");
        }

    }

    /**
     * @return the spec
     */
    public PCOpSpec getSpec() {
        return spec;
    }

    /**
     * Gets the logical reader
     */
    public LogicalReader getLogicalReader() {
        return logicalReader;
    }

    /**
     * Gets the port operations.
     */
    public havis.middleware.ale.base.operation.port.Operation getPortOperation() {
        return operation;
    }

    /**
     * Returns the Operations
     *
     * @param specs
     *            The operation specifications
     * @return The operation array
     * @throws ValidationException
     */
    public static List<Operation> get(List<PCOpSpec> specs) throws ValidationException {
        List<Operation> operations = new ArrayList<Operation>();
        try {
            List<String> names = new ArrayList<String>();
            for (PCOpSpec spec : specs) {
                String name = spec.getOpName();
                if (name != null) {
                    if (names.contains(name)) {
                        throw new ValidationException("Operation name '" + name + "' already exists");
                    }
                    names.add(name);
                }
                operations.add(new Operation(spec));
            }
            return operations;
        } catch (ValidationException e) {
            for (Operation operation : operations) {
                operation.dispose();
            }
            throw e;
        }
    }

    /**
     * @param result
     *            the result to create a PC operation report for
     * @return a PC operation report for the specified result
     */
    public PCOpReport getReport(final Result result) {
        final Boolean currentState = (result instanceof ReadResult && result.getState() == State.SUCCESS) ? Boolean.valueOf(((ReadResult) result).getData() > 0) : null;
        PCOpReport report = new PCOpReport();
        report.setOpName(spec.getOpName());
        report.setState(currentState);
        report.setOpStatus(result != null && result.getState() != null ? result.getState().toString().toUpperCase() : null);
        return report;
    }

    /**
     * Disposes the instance
     */
    public void dispose() {
        if (logicalReader != null) {
            logicalReader.unlock();
            logicalReader = null;
        }
    }
}