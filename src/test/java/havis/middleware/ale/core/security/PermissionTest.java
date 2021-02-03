package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.DuplicatePermissionException;
import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.service.ac.ACPermission;
import havis.middleware.ale.service.ac.ACPermission.Instances;
import havis.middleware.ale.service.ac.ACPermissionExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Assert;
import org.junit.Test;

public class PermissionTest {
	@Test
	public void permissionClassExceptionTest(@Mocked final ACPermission permSpec) throws DuplicatePermissionException, PermissionValidationException{
		//TODO ExceptionBlock cannot be reached. if Permissionclass is null, valueOf Exception is thrown
	}
	@Test
	public void permissionDefaultMethodTest() throws DuplicatePermissionException, PermissionValidationException, NoSuchPermissionException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("peter", permSpec);
		Assert.assertEquals("*", Permissions.getInstance().get("peter").getInstances().getInstance().get(0));
	}
	@Test
	public void permissionUnknownMethodTest(@Mocked final ACPermission permSpec) throws DuplicatePermissionException, PermissionValidationException{
		//TODO ExceptionBlock cannot be reached. if the method of the permission does not match enum, valueOf Exception is thrown
	}
	@Test
	public void setSpecTest() throws DuplicatePermissionException, PermissionValidationException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("Any");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permission perm = new Permission("permission", permSpec);

		Assert.assertEquals("Any", perm.getSpec().getInstances().getInstance().get(0));

		permSpec.getInstances().getInstance().clear();
		permSpec.getInstances().getInstance().add("*");

		perm.setSpec(permSpec);

		Assert.assertEquals("*", perm.getSpec().getInstances().getInstance().get(0));
	}

	@Test
	public void disposeTest() throws PermissionValidationException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("Any");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permission perm = new Permission("permission", permSpec);
		perm.dispose();
		//TODO dispose is a blank method
	}

	@Test
	public void countTest(@Mocked final ACPermission perm) throws PermissionValidationException{
		new NonStrictExpectations() {
			{
				perm.getPermissionClass();
				result = "Method";
			}
		};
		Permission permission = new Permission("test", perm);
		Assert.assertFalse(permission.isUsed());
		permission.inc();
		Assert.assertTrue(permission.isUsed());
		permission.dec();
		Assert.assertFalse(permission.isUsed());
	}

	@Test
	public void getNameTest() throws PermissionValidationException, DuplicatePermissionException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("Any");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permission perm = new Permission("permission", permSpec);
		Assert.assertEquals("permission", perm.getName());
	}

	@Test
	public void containsTest(@Mocked final ACPermission permSpec, @Mocked final ACPermission permSpec2) throws PermissionValidationException, DuplicatePermissionException{

		new NonStrictExpectations() {
			{
				permSpec.getPermissionClass();
				result = "Method";

				permSpec.getInstances().getInstance();
				result = Arrays.asList("*", "Any");
			}
		};
		Permission perm = new Permission("permission", permSpec);
		new NonStrictExpectations() {
			{
				permSpec.getInstances().getInstance();
				result = Arrays.asList("Any");
			}
		};
		Assert.assertTrue(perm.containsMethod(Method.ALE, Method.ALE_define));
		new NonStrictExpectations() {
			{
				permSpec2.getPermissionClass();
				result = "Method";
			}
		};
		perm = new Permission("permission", permSpec2);
		new NonStrictExpectations() {
			{
				permSpec2.getInstances().getInstance();
				result = new ArrayList<String>();
			}
		};
		Assert.assertFalse(perm.containsMethod(Method.ALE_getECSpec, Method.ALE_define));
	}
}
