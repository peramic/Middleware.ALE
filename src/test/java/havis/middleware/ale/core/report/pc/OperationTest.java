package havis.middleware.ale.core.report.pc;

import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.base.operation.port.Pin;
import havis.middleware.ale.base.operation.port.result.ReadResult;
import havis.middleware.ale.base.operation.port.result.Result;
import havis.middleware.ale.base.operation.port.result.Result.State;
import havis.middleware.ale.core.CompositeReader;
import havis.middleware.ale.core.LogicalReader;
import havis.middleware.ale.core.manager.LR;
import havis.middleware.ale.service.ECTime;
import havis.middleware.ale.service.pc.PCOpReport;
import havis.middleware.ale.service.pc.PCOpSpec;
import havis.middleware.ale.service.pc.PCPortSpec;

import java.util.Arrays;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import mockit.VerificationsInOrder;

import org.junit.Assert;
import org.junit.Test;

public class OperationTest {

    @Test
    public void operationRead(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = reader;
            }
        };
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");
        Operation operation = new Operation(spec);

        Assert.assertSame(reader, operation.getLogicalReader());
        Assert.assertSame(spec, operation.getSpec());
        Assert.assertEquals(havis.middleware.ale.base.operation.port.Operation.Type.READ, operation.getPortOperation().getType());
        Assert.assertNull(operation.getPortOperation().getData());
        Assert.assertNull(operation.getPortOperation().getDuration());
        Assert.assertEquals("spec", operation.getPortOperation().getName());
        Assert.assertEquals(Pin.Type.INPUT, operation.getPortOperation().getPin().getType());

        new Verifications() {
            {
                lr.lock(withEqual("somereader"));
                times = 1;
            }
        };
    }

    @Test
    public void operationWriteInitialOn(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = reader;
            }
        };
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("OUTPUT");
        spec.setOpType("WRITE");
        spec.setState(Boolean.TRUE);
        spec.setDuration(new ECTime());
        spec.getDuration().setUnit("MS");
        spec.getDuration().setValue(1000);
        Operation operation = new Operation(spec);

        Assert.assertSame(reader, operation.getLogicalReader());
        Assert.assertSame(spec, operation.getSpec());
        Assert.assertEquals(havis.middleware.ale.base.operation.port.Operation.Type.WRITE, operation.getPortOperation().getType());
        Assert.assertEquals(Byte.valueOf((byte) 0x01), operation.getPortOperation().getData());
        Assert.assertEquals(Long.valueOf(1000), operation.getPortOperation().getDuration());
        Assert.assertEquals("spec", operation.getPortOperation().getName());
        Assert.assertEquals(Pin.Type.OUTPUT, operation.getPortOperation().getPin().getType());

        new Verifications() {
            {
                lr.lock(withEqual("somereader"));
                times = 1;
            }
        };
    }

    @Test
    public void operationWriteInitialOff(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = reader;
            }
        };
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("OUTPUT");
        spec.setOpType("WRITE");
        spec.setState(Boolean.FALSE);
        spec.setDuration(new ECTime());
        spec.getDuration().setValue(1000);
        spec.getDuration().setUnit("MS");
        Operation operation = new Operation(spec);

        Assert.assertSame(reader, operation.getLogicalReader());
        Assert.assertSame(spec, operation.getSpec());
        Assert.assertEquals(havis.middleware.ale.base.operation.port.Operation.Type.WRITE, operation.getPortOperation().getType());
        Assert.assertEquals(Byte.valueOf((byte) 0x00), operation.getPortOperation().getData());
        Assert.assertEquals(Long.valueOf(1000), operation.getPortOperation().getDuration());
        Assert.assertEquals("spec", operation.getPortOperation().getName());
        Assert.assertEquals(Pin.Type.OUTPUT, operation.getPortOperation().getPin().getType());

        new Verifications() {
            {
                lr.lock(withEqual("somereader"));
                times = 1;
            }
        };
    }

    @Test(expected = ValidationException.class)
    public void operationNoPortSpec() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(null);
        spec.setOpType("READ");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationNoReader() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader(null);
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationEmptyReader() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationNoType() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType(null);
        spec.setOpType("READ");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationNoOperationType() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType(null);
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationEmptyOperationType() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationNegativePortId() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.getPortSpec().setId(-1);
        spec.setOpType("READ");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationUnknownOperationType() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("whatever");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationUnknownInputType() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("whatever");
        spec.setOpType("READ");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationReadWithInitialState() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");
        spec.setState(Boolean.TRUE);
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationReadWithDuration() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");
        spec.setDuration(new ECTime());
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationWriteWithoutState() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("WRITE");
        spec.setState(null);
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationWriteWithInvalidDuration() throws ValidationException, NoSuchNameException {
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("WRITE");
        spec.setState(Boolean.TRUE);
        spec.setDuration(new ECTime());
        spec.getDuration().setValue(1000);
        spec.getDuration().setUnit("whatever");
        new Operation(spec);
    }

    @Test(expected = ValidationException.class)
    public void operationReadNoSuchReader(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = new NoSuchNameException();
            }
        };
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");
        new Operation(spec);
    }

    @Test
    public void operationCompositeReader(@Mocked final LR lr, @Mocked final CompositeReader reader) throws ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = reader;
            }
        };
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");

		try {
			new Operation(spec);
			Assert.fail("Expected ValidationException");
		} catch (ValidationException e) {
			// ignore
		}

		new Verifications() {
			{
				lr.lock(withEqual("somereader"));
				times = 1;

				reader.unlock();
				times = 1;
			}
		};
	}

    @Test
    public void get(@Mocked final LR lr, @Mocked final LogicalReader reader) throws NoSuchNameException, ValidationException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = reader;
            }
        };
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("spec1");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");

        PCOpSpec spec2 = new PCOpSpec();
        spec2.setOpName("spec2");
        spec2.setPortSpec(new PCPortSpec());
        spec2.getPortSpec().setReader("somereader");
        spec2.getPortSpec().setType("OUTPUT");
        spec2.setOpType("WRITE");
        spec2.setState(Boolean.TRUE);
        spec2.setDuration(new ECTime());
        spec2.getDuration().setUnit("MS");
        spec2.getDuration().setValue(1000);

        List<Operation> operations = Operation.get(Arrays.asList(spec1, spec2));

        Assert.assertNotNull(operations);
        Assert.assertEquals(2, operations.size());

        Assert.assertSame(reader, operations.get(0).getLogicalReader());
        Assert.assertSame(spec1, operations.get(0).getSpec());
        Assert.assertEquals(havis.middleware.ale.base.operation.port.Operation.Type.READ, operations.get(0).getPortOperation().getType());
        Assert.assertNull(operations.get(0).getPortOperation().getData());
        Assert.assertNull(operations.get(0).getPortOperation().getDuration());
        Assert.assertEquals("spec1", operations.get(0).getPortOperation().getName());
        Assert.assertEquals(Pin.Type.INPUT, operations.get(0).getPortOperation().getPin().getType());

        Assert.assertSame(reader, operations.get(1).getLogicalReader());
        Assert.assertSame(spec2, operations.get(1).getSpec());
        Assert.assertEquals(havis.middleware.ale.base.operation.port.Operation.Type.WRITE, operations.get(1).getPortOperation().getType());
        Assert.assertEquals(Byte.valueOf((byte) 0x01), operations.get(1).getPortOperation().getData());
        Assert.assertEquals(Long.valueOf(1000), operations.get(1).getPortOperation().getDuration());
        Assert.assertEquals("spec2", operations.get(1).getPortOperation().getName());
        Assert.assertEquals(Pin.Type.OUTPUT, operations.get(1).getPortOperation().getPin().getType());

        new Verifications() {
            {
                lr.lock(withEqual("somereader"));
                times = 2;
            }
        };
    }

    @Test
    public void getDuplicateName(@Mocked final LR lr, @Mocked final LogicalReader reader) throws NoSuchNameException, ValidationException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = reader;
            }
        };
        PCOpSpec spec1 = new PCOpSpec();
        spec1.setOpName("samename");
        spec1.setPortSpec(new PCPortSpec());
        spec1.getPortSpec().setReader("somereader");
        spec1.getPortSpec().setType("INPUT");
        spec1.setOpType("READ");

        PCOpSpec spec2 = new PCOpSpec();
        spec2.setOpName("samename");
        spec2.setPortSpec(new PCPortSpec());
        spec2.getPortSpec().setReader("somereader");
        spec2.getPortSpec().setType("OUTPUT");
        spec2.setOpType("WRITE");
        spec2.setState(Boolean.TRUE);
        spec2.setDuration(new ECTime());
        spec2.getDuration().setUnit("MS");
        spec2.getDuration().setValue(1000);

        try {
            Operation.get(Arrays.asList(spec1, spec2));
            Assert.fail("Expected ValidationException");
        } catch (ValidationException e) {
            // ignore
        }

        new Verifications() {
            {
                // all from the first operation

                lr.lock(withEqual("somereader"));
                times = 1;

                reader.unlock();
                times = 1;
            }
        };
    }

    @Test
    public void getReport(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = reader;
            }
        };
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");
        Operation operation = new Operation(spec);
        
        PCOpReport report = operation.getReport(null);
        Assert.assertNotNull(report);
        Assert.assertEquals("spec", report.getOpName());
        Assert.assertNull(report.isState());
        Assert.assertNull(report.getOpStatus());
        
        report = operation.getReport(new Result(null));
        Assert.assertNotNull(report);
        Assert.assertEquals("spec", report.getOpName());
        Assert.assertNull(report.isState());
        Assert.assertNull(report.getOpStatus());

        report = operation.getReport(new Result(State.SUCCESS));
        Assert.assertNotNull(report);
        Assert.assertEquals("spec", report.getOpName());
        Assert.assertNull(report.isState());
        Assert.assertEquals("SUCCESS", report.getOpStatus());
        
        report = operation.getReport(new Result(null));
        Assert.assertNotNull(report);
        Assert.assertEquals("spec", report.getOpName());
        Assert.assertNull(report.isState());
        Assert.assertNull(report.getOpStatus());

        report = operation.getReport(new ReadResult(State.MISC_ERROR_TOTAL, (byte) 0x00));
        Assert.assertNotNull(report);
        Assert.assertEquals("spec", report.getOpName());
        Assert.assertNull(report.isState());
        Assert.assertEquals("MISC_ERROR_TOTAL", report.getOpStatus());

        report = operation.getReport(new ReadResult(State.OP_NOT_POSSIBLE_ERROR, (byte) 0x01));
        Assert.assertNotNull(report);
        Assert.assertEquals("spec", report.getOpName());
        Assert.assertNull(report.isState());
        Assert.assertEquals("OP_NOT_POSSIBLE_ERROR", report.getOpStatus());
        
        report = operation.getReport(new ReadResult(State.SUCCESS, (byte) 0x00));
        Assert.assertNotNull(report);
        Assert.assertEquals("spec", report.getOpName());
        Assert.assertEquals(Boolean.FALSE, report.isState());
        Assert.assertEquals("SUCCESS", report.getOpStatus());

        report = operation.getReport(new ReadResult(State.SUCCESS, (byte) 0x02));
        Assert.assertNotNull(report);
        Assert.assertEquals("spec", report.getOpName());
        Assert.assertEquals(Boolean.TRUE, report.isState());
        Assert.assertEquals("SUCCESS", report.getOpStatus());

        new Verifications() {
            {
                lr.lock(withEqual("somereader"));
                times = 1;
            }
        };
    }

    @Test
    public void dispose(@Mocked final LR lr, @Mocked final LogicalReader reader) throws ValidationException, NoSuchNameException {
        new NonStrictExpectations() {
            {
                LR.getInstance();
                result = lr;

                lr.lock(withEqual("somereader"));
                result = reader;
            }
        };
        PCOpSpec spec = new PCOpSpec();
        spec.setOpName("spec");
        spec.setPortSpec(new PCPortSpec());
        spec.getPortSpec().setReader("somereader");
        spec.getPortSpec().setType("INPUT");
        spec.setOpType("READ");
        Operation operation = new Operation(spec);
        operation.dispose();

        new VerificationsInOrder() {
            {
                lr.lock(withEqual("somereader"));
                times = 1;

                reader.unlock();
                times = 1;
            }
        };
    }
}
