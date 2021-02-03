package havis.middleware.ale.core.manager;

import org.junit.Assert;
import org.junit.Test;

public class ACTest {
	@Test
	public void getStandardTest(){
		Assert.assertEquals("1.1", AC.getStandardVersion());
	}
}
