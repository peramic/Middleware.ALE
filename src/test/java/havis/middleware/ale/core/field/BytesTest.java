package havis.middleware.ale.core.field;

import havis.middleware.ale.base.operation.tag.result.ResultState;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class BytesTest {

    @Test
    public void bytesTest() {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8, ResultState.SUCCESS);
        Assert.assertEquals(8, bytes.getLength());
        Assert.assertEquals(ResultState.SUCCESS, bytes.getResultState());
        Assert.assertEquals(FieldDatatype.EPC, bytes.getDatatype());
        Assert.assertTrue(Arrays.equals(new byte[] { (byte) 0xFF }, bytes.getValue()));
    }

    @Test
    public void bytesSecondTest() {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8);
        Assert.assertEquals(8, bytes.getLength());
        Assert.assertEquals(ResultState.SUCCESS, bytes.getResultState());
        Assert.assertEquals(FieldDatatype.EPC, bytes.getDatatype());
        Assert.assertTrue(Arrays.equals(new byte[] { (byte) 0xFF }, bytes.getValue()));
    }

    @Test
    public void bytesThirdTest() {
        Bytes bytes = new Bytes(ResultState.SUCCESS);
        Assert.assertEquals(0, bytes.getLength());
        Assert.assertEquals(ResultState.SUCCESS, bytes.getResultState());
        Assert.assertEquals(FieldDatatype.EPC, bytes.getDatatype());
        Assert.assertTrue(Arrays.equals(new byte[] {}, bytes.getValue()));
    }

    @Test
    public void bytesFourthTest() {
        Bytes bytes = new Bytes(new byte[] { (byte) 0xFF });
        Assert.assertEquals(0, bytes.getLength());
        Assert.assertEquals(ResultState.SUCCESS, bytes.getResultState());
        Assert.assertEquals(FieldDatatype.EPC, bytes.getDatatype());
        Assert.assertTrue(Arrays.equals(new byte[] { (byte) 0xFF }, bytes.getValue()));
    }

    @Test
    public void bytesFifthTest() {
        Bytes bytes = new Bytes();
        Assert.assertEquals(0, bytes.getLength());
        Assert.assertEquals(ResultState.SUCCESS, bytes.getResultState());
        Assert.assertEquals(FieldDatatype.EPC, bytes.getDatatype());
        Assert.assertTrue(Arrays.equals(new byte[] {}, bytes.getValue()));
    }

    @Test
    public void equalsTest() {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8);
        Bytes bytesMatch1 = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8, ResultState.SUCCESS);
        Bytes bytesMatch2 = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8, ResultState.MISC_ERROR_TOTAL);
        Bytes bytesNoMatch1 = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 7, ResultState.SUCCESS);
        Bytes bytesNoMatch2 = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFE }, 8, ResultState.SUCCESS);
        Bytes bytesNoMatch3 = new Bytes(FieldDatatype.BITS, new byte[] { (byte) 0xFF }, 8, ResultState.SUCCESS);
        Bytes bytesNoDataType = new Bytes(null, new byte[] { (byte) 0xFF }, 8);

        Assert.assertTrue(bytes.equals(bytes));
        Assert.assertTrue(bytes.equals(bytesMatch1));
        Assert.assertTrue(bytesMatch1.equals(bytes));
        Assert.assertTrue(bytes.equals(bytesMatch2));
        Assert.assertTrue(bytesMatch2.equals(bytes));

        Assert.assertFalse(bytes.equals(null));
        Assert.assertFalse(bytes.equals(""));
        Assert.assertFalse(bytes.equals(bytesNoMatch1));
        Assert.assertFalse(bytesNoMatch1.equals(bytes));
        Assert.assertFalse(bytes.equals(bytesNoMatch2));
        Assert.assertFalse(bytesNoMatch2.equals(bytes));
        Assert.assertFalse(bytes.equals(bytesNoMatch3));
        Assert.assertFalse(bytesNoMatch3.equals(bytes));
        Assert.assertFalse(bytes.equals(bytesNoDataType));
        Assert.assertFalse(bytesNoDataType.equals(bytes));
    }

    @Test
    public void hashCodeTest() {
        Bytes bytes = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8);
        Bytes bytesMatch1 = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8, ResultState.SUCCESS);
        Bytes bytesMatch2 = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 8, ResultState.MISC_ERROR_TOTAL);
        Bytes bytesNoMatch1 = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFF }, 7, ResultState.SUCCESS);
        Bytes bytesNoMatch2 = new Bytes(FieldDatatype.EPC, new byte[] { (byte) 0xFE }, 8, ResultState.SUCCESS);
        Bytes bytesNoMatch3 = new Bytes(FieldDatatype.BITS, new byte[] { (byte) 0xFF }, 8, ResultState.SUCCESS);
        Bytes bytesNoDataType = new Bytes(null, new byte[] { (byte) 0xFF }, 8);


        Assert.assertEquals(bytes.hashCode(), bytesMatch1.hashCode());
        Assert.assertEquals(bytes.hashCode(), bytesMatch2.hashCode());

        Assert.assertNotEquals(bytes.hashCode(), bytesNoMatch1.hashCode());
        Assert.assertNotEquals(bytes.hashCode(), bytesNoMatch2.hashCode());
        Assert.assertNotEquals(bytes.hashCode(), bytesNoMatch3.hashCode());
        Assert.assertNotEquals(bytes.hashCode(), bytesNoDataType.hashCode());
    }
}
