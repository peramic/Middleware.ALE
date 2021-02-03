package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.DuplicateRoleException;
import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.NoSuchRoleException;
import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.base.exception.RoleValidationException;
import havis.middleware.ale.service.ac.ACPermission;
import havis.middleware.ale.service.ac.ACRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class RolesTest {
	@Test(expected = NoSuchRoleException.class)
	public void removeRoleExceptionTest() throws NoSuchRoleException, NoSuchPermissionException{
		Roles roles = new Roles();
		roles.remove("exception", null);
	}
	@Test(expected = NoSuchRoleException.class)
	public void getRoleExceptionTest() throws NoSuchRoleException{
		Roles roles = new Roles();
		List<String> input = new ArrayList<>();
		input.add("exception");
		roles.get(input);
	}
	@Test(expected = NoSuchRoleException.class)
	public void setRoleExceptionTest() throws NoSuchRoleException, NoSuchPermissionException{
		Roles roles = new Roles();
		List<String> input = new ArrayList<>();
		input.add("exception");
		roles.set("exception", input);
	}
	@Test(expected = NoSuchRoleException.class)
	public void addRoleExceptionTest() throws NoSuchRoleException, NoSuchPermissionException{
		Roles roles = new Roles();
		List<String> input = new ArrayList<>();
		input.add("exception");
		roles.add("exception", input);
	}
	@Test(expected = NoSuchRoleException.class)
	public void undefineRoleExceptionTest() throws NoSuchRoleException, NoSuchPermissionException{
		Roles roles = new Roles();
		roles.undefine("exception");
	}
	@Test(expected = NoSuchRoleException.class)
	public void getACRoleExceptionTest() throws NoSuchRoleException, NoSuchPermissionException{
		Roles roles = new Roles();
		roles.get("exception");
	}
	@Test(expected = NoSuchRoleException.class)
	public void updateRoleExceptionTest() throws NoSuchRoleException, NoSuchPermissionException, RoleValidationException{
		Roles roles = new Roles();
		roles.update("exception", new ACRole());
	}
	@Test
	public void getEmptyListOfNamesTest(){
		Roles.instance = new Roles();
		Assert.assertEquals(new ArrayList<String>(), Roles.getInstance().getNames());
	}
	@Test(expected = DuplicateRoleException.class)
	public void defineDublicateRolesExceptionTest(@Mocked ACRole role) throws DuplicateRoleException, RoleValidationException{
		Roles.instance = new Roles();
		Roles.getInstance().define("test", role);
		Roles.getInstance().define("test", role);
	}
	@Test
	public void updateRoleTest(@Mocked ACRole role, @Mocked ACRole updatedRole) throws DuplicateRoleException, RoleValidationException, NoSuchRoleException{
		Roles.instance = new Roles();
		Roles.getInstance().define("test", role);
		Roles.getInstance().update("test", updatedRole);
		Assert.assertEquals(updatedRole.getSchemaVersion(), Roles.getInstance().get("test").getSchemaVersion());
	}
	@Test(expected = NoSuchRoleException.class)
	public void undefineRoleTest(@Mocked ACRole role) throws DuplicateRoleException, RoleValidationException, NoSuchRoleException{
		Roles.instance = new Roles();
		Roles.getInstance().define("test", role);

		Assert.assertNotNull(Roles.getInstance().get("test"));

		Roles.getInstance().undefine("test");
		Roles.getInstance().get("test");
	}
	@Test
	public void addRoleTest(@Mocked ACRole role, @Mocked final ACPermission permission, @Mocked final Permission perm) throws DuplicateRoleException, RoleValidationException, NoSuchRoleException, NoSuchPermissionException, PermissionValidationException {
		Roles.instance = new Roles();
		Roles.getInstance().define("test", role);
		List<String> input = new ArrayList<>();
		input.add("testPermission");
		new NonStrictExpectations() {
			{
				permission.getPermissionClass();
				result = "Method";
			}
		};
		Permissions.getInstance().permissions.put("testPermission", new Permission("testPermissioN", permission));
		Roles.getInstance().add("test", input);

		new Verifications() {
			{
				perm.inc();
				times = 1;
			}
		};
		Assert.assertEquals(Roles.getInstance().getNames().size(), 1);
	}
	@Test
	public void setRoleTest(@Mocked ACRole role, @Mocked final ACPermission permission, @Mocked final Permission perm) throws DuplicateRoleException, RoleValidationException, PermissionValidationException, NoSuchRoleException, NoSuchPermissionException {
		Roles.instance = new Roles();
		Roles.getInstance().define("test", role);
		List<String> input = new ArrayList<>();
		input.add("testPermission");
		new NonStrictExpectations() {
			{
				permission.getPermissionClass();
				result = "Method";
			}
		};
		Permissions.getInstance().permissions.put("testPermission", new Permission("testPermissioN", permission));
		Roles.getInstance().set("test", input);

		new Verifications() {
			{
				perm.inc();
				times = 1;
			}
		};
		Assert.assertEquals(Roles.getInstance().getNames().size(), 1);
	}
	@Test
	public void removeRoleTest(@Mocked ACRole role, @Mocked final ACPermission permission, @Mocked final Permission perm) throws DuplicateRoleException, RoleValidationException, NoSuchRoleException, NoSuchPermissionException, PermissionValidationException {
		Roles.instance = new Roles();
		Roles.getInstance().define("test", role);
		List<String> input = new ArrayList<>();
		input.add("testPermission");
		new NonStrictExpectations() {
			{
				permission.getPermissionClass();
				result = "Method";
			}
		};
		Permissions.getInstance().permissions.put("testPermission", new Permission("testPermissioN", permission));
		Roles.getInstance().remove("test", input);

		new Verifications() {
			{
				perm.dec();
				times = 1;
			}
		};
		Assert.assertEquals(Roles.getInstance().getNames().size(), 1);
	}

	@Test
	public void getTest(@Mocked Role role) throws NoSuchRoleException{
		Roles.getInstance().roles.put("test", role);
		List<Role> expectedList = new ArrayList<>();
		expectedList.add(role);
		Assert.assertEquals(expectedList, Roles.getInstance().get(Arrays.asList("test")));
	}
}
