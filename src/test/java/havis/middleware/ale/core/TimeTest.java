package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.service.ECTime;

import org.junit.Assert;
import org.junit.Test;

public class TimeTest {

    @Test
    public void getValue() throws ValidationException {
        Assert.assertEquals(-1, Time.getValue(null));

        ECTime time = new ECTime();
        time.setUnit("MS");
        time.setValue(0);
        Assert.assertEquals(0, Time.getValue(time));

        time.setValue(10);
        Assert.assertEquals(10, Time.getValue(time));
    }

    @Test(expected=ValidationException.class)
    public void getValueNoUnit() throws ValidationException {
        ECTime time = new ECTime();
        time.setUnit(null);
        time.setValue(10);
        Time.getValue(time);
    }

    @Test(expected=ValidationException.class)
    public void getValueInvalidUnit() throws ValidationException {
        ECTime time = new ECTime();
        time.setUnit("whatever");
        time.setValue(10);
        Time.getValue(time);
    }

    @Test(expected=ValidationException.class)
    public void getValueNegativeValue() throws ValidationException {
        ECTime time = new ECTime();
        time.setUnit("MS");
        time.setValue(-10);
        Time.getValue(time);
    }
}
