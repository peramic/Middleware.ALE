package havis.middleware.ale.core;

import havis.middleware.ale.base.exception.ValidationException;

import org.junit.Assert;
import org.junit.Test;

public class NameTest {

    @Test
    public void isValid() throws ValidationException {
        Assert.assertTrue(Name.isValid("test"));
        Assert.assertTrue(Name.isValid("test", true));
        Assert.assertTrue(Name.isValid("test", false));
        Assert.assertTrue(Name.isValid("test test", false));
    }

    @Test(expected = ValidationException.class)
    public void isValidNull() throws ValidationException {
        Name.isValid(null, false);
    }

    @Test(expected = ValidationException.class)
    public void isValidEmpty() throws ValidationException {
        Name.isValid("", false);
    }

    @Test(expected = ValidationException.class)
    public void isValidWhitespace1() throws ValidationException {
        Name.isValid("test test", true);
    }

    @Test(expected = ValidationException.class)
    public void isValidWhitespace2() throws ValidationException {
        Name.isValid("test\u200etest", true);
    }

    @Test(expected = ValidationException.class)
    public void isValidWhitespace3() throws ValidationException {
        Name.isValid("test\u200ftest", true);
    }

    @Test(expected = ValidationException.class)
    public void isValidSyntax() throws ValidationException {
        Name.isValid("test\u0021test", true);
    }
}
