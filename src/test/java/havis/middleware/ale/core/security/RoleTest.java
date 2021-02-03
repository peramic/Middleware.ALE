package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.DuplicatePermissionException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.base.exception.RoleValidationException;
import havis.middleware.ale.service.ac.ACPermission;
import havis.middleware.ale.service.ac.ACPermission.Instances;
import havis.middleware.ale.service.ac.ACPermissionExtension;
import havis.middleware.ale.service.ac.ACRole;
import havis.middleware.ale.service.ac.ACRole.PermissionNames;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RoleTest {

    @Before
    public void before(){
        ACResetter.reset();
    }

	@Test(expected = RoleValidationException.class)
	public void roleExceptionTest() throws RoleValidationException{
		ACRole acRole = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("affeaffe");
		acRole.setPermissionNames(value);
		new Role("test", acRole);
	}
	@Test(expected = RoleValidationException.class)
	public void setRoleExceptionTest() throws RoleValidationException, PermissionValidationException, DuplicatePermissionException{
		ACPermission permSpec = new ACPermission();
		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(new Instances());
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("admin", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("admin");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);
		spec.getPermissionNames().getPermissionName().clear();
		spec.getPermissionNames().getPermissionName().add("abcdefg");
		role.setSpec(spec);
	}

	@Test
	public void removeRoleTest() throws RoleValidationException, PermissionValidationException, DuplicatePermissionException, NoSuchPermissionException{
		ACPermission permSpec = new ACPermission();
		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(new Instances());
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("test", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("test");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);

		List<String> input = new ArrayList<>();
		List<String> expected = role.getPermissionNames();
		input.add("test");

		Assert.assertEquals("test", expected.get(0));
		role.remove(input);
		Assert.assertNotSame(expected, role.getPermissionNames());
	}

	@Test
	public void containsPermissionTest() throws RoleValidationException, PermissionValidationException, DuplicatePermissionException, NoSuchPermissionException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("value", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("value");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);

		Assert.assertTrue(role.containsPermission(Method.ALECC_poll , Method.ALE_subscribe));
		List<String> input = new ArrayList<>();
		input.add("value");
		role.remove(input);
		Assert.assertFalse(role.containsPermission(Method.ALECC_poll , Method.ALE_subscribe));
	}

	@Test
	public void countTest() throws DuplicatePermissionException, PermissionValidationException, RoleValidationException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("exception", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("exception");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);

		Assert.assertFalse(role.isUsed());
		role.inc();
		Assert.assertTrue(role.isUsed());
		role.dec();
		Assert.assertFalse(role.isUsed());
	}

	@Test
	public void equalsTest() throws DuplicatePermissionException, PermissionValidationException, RoleValidationException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("kermit", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("kermit");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);
		Role otherRole = new Role("otherRole", spec);

		Assert.assertFalse(role.equals(otherRole));
		Assert.assertTrue(role.equals(role));
	}

	@Test(expected = InUseException.class)
	public void permissionInUseExceptionTest() throws DuplicatePermissionException, PermissionValidationException, RoleValidationException, InUseException, NoSuchPermissionException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("jonas", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("jonas");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);

		Assert.assertFalse(role.isUsed());
		role.inc();
		Assert.assertTrue(role.isUsed());

		Permissions.getInstance().undefine("jonas");
	}

	@Test
    public void getSpecTest() throws RoleValidationException, DuplicatePermissionException, PermissionValidationException {
        ACPermission permSpec = new ACPermission();
        Instances instances = new Instances();
        instances.getInstance().add("*");
        permSpec.setPermissionClass("Method");
        permSpec.setCreationDate(new Date());
        permSpec.setExtension(new ACPermissionExtension());
        permSpec.setInstances(instances);
        permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));
        Permissions.getInstance().define("jonas", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("jonas");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);
		Roles.getInstance().roles.put("test", role);
		Assert.assertEquals(spec.getPermissionNames().getPermissionName(), role.getSpec().getPermissionNames().getPermissionName());
	}

	@Test
	public void disposeTest(@Mocked final Permission permission, @Mocked ACRole acRole) throws RoleValidationException{
		Role role = new Role("test", acRole);
		role.permissions.add(permission);
		role.dispose();
		new Verifications() {
			{
				permission.dec();
			}
		};
	}

	@Test
	public void setSpecTest(@Mocked final ACRole acRole) throws RoleValidationException, DuplicatePermissionException, PermissionValidationException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("gerald", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("gerald");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);
		role.setSpec(acRole);
		new NonStrictExpectations() {
			{
				acRole.getSchemaVersion();
				result = BigDecimal.valueOf(100);
			}
		};
		Assert.assertEquals(BigDecimal.valueOf(100), role.getSpec().getSchemaVersion());
	}

	@Test
	public void addTest() throws RoleValidationException, DuplicatePermissionException, PermissionValidationException, NoSuchPermissionException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("manfred", permSpec);
		Permissions.getInstance().define("erwin", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("manfred");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);

		role.add(Arrays.asList("erwin"));
		Assert.assertEquals(Arrays.asList("manfred", "erwin"), role.getPermissionNames());
	}

	@Test
	public void setTest() throws RoleValidationException, DuplicatePermissionException, PermissionValidationException, NoSuchPermissionException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("gloria", permSpec);
		Permissions.getInstance().define("victoria", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("gloria");
		spec.setPermissionNames(value);
		Role role = new Role("role", spec);
		Assert.assertEquals(Arrays.asList("gloria"), role.getPermissionNames());
		role.set(Arrays.asList("victoria"));
		Assert.assertEquals(Arrays.asList("victoria"), role.getPermissionNames());
	}
}
