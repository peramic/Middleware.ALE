package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.DuplicatePermissionException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.service.ac.ACPermission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PermissionsTest {

	@Before
	public void before(){
		Permissions.instance = new Permissions();
	}

	@Test
	public void getNamesEpmtyListTest(){
		Assert.assertEquals(new ArrayList<String>(), Permissions.getInstance().getNames());
	}

	@Test
	public void getNamesTest(@Mocked final Permission permission) throws DuplicatePermissionException, PermissionValidationException, InUseException, NoSuchPermissionException{

		Permissions.getInstance().permissions.put("first", permission);

		List<String> expectedList = new ArrayList<>();
		expectedList.add("first");
		Assert.assertTrue(Permissions.getInstance().getNames().contains(expectedList.get(0)));
		Assert.assertEquals(1, Permissions.getInstance().getNames().size());

		Permissions.getInstance().permissions.put("second", permission);
		expectedList.add("second");
		Assert.assertTrue(Permissions.getInstance().getNames().contains(expectedList.get(1)));
		Assert.assertEquals(2, Permissions.getInstance().getNames().size());

		Permissions.getInstance().undefine("second");
		expectedList.remove(1);
		Assert.assertEquals(expectedList, Permissions.getInstance().getNames());
		Assert.assertEquals(1, Permissions.getInstance().getNames().size());

		Permissions.getInstance().undefine("first");
		expectedList.remove(0);
		Assert.assertEquals(expectedList, Permissions.getInstance().getNames());
		Assert.assertEquals(0, Permissions.getInstance().getNames().size());
	}

	@Test(expected = DuplicatePermissionException.class)
	public void defineExceptionTest(@Mocked final ACPermission permission, @Mocked final Map<String, Permission> permissions) throws DuplicatePermissionException, PermissionValidationException{
		Permissions.getInstance().permissions = permissions;

		new NonStrictExpectations() {
			{
				permissions.containsKey("test");
				result = Boolean.TRUE;
			}
		};

		Permissions.getInstance().define("test", permission);
	}

	@Test(expected = NoSuchPermissionException.class)
	public void updateExceptionTest(@Mocked final ACPermission permission, @Mocked final Map<String, Permission> permissions) throws DuplicatePermissionException, PermissionValidationException, NoSuchPermissionException{
		Permissions.getInstance().permissions = permissions;

		new NonStrictExpectations() {
			{
				permissions.containsKey("test");
				result = Boolean.FALSE;
			}
		};

		Permissions.getInstance().update("test", permission);
	}

	@Test
	public void updateTest(@Mocked Permission permission, @Mocked final Map<String, Permission> permissions, @Mocked final ACPermission perm) throws NoSuchPermissionException, PermissionValidationException{
		Permissions.getInstance().permissions = permissions;

		new NonStrictExpectations() {
			{
				permissions.containsKey("test");
				result = Boolean.TRUE;
			}
		};

		Permissions.getInstance().update("test", perm);

		new Verifications() {
			{
				permissions.put("test", new Permission("test", perm));
				times = 1;
			}
		};
	}

	@Test(expected = NoSuchPermissionException.class)
	public void getExceptionTest() throws NoSuchPermissionException{
		Permissions.getInstance().get("test");
	}

	@Test(expected = NoSuchPermissionException.class)
	public void undefineExceptionTest() throws NoSuchPermissionException, InUseException{
		Permissions.getInstance().undefine("test");
	}

	@Test
	public void getTest(@Mocked Permission permission) throws NoSuchPermissionException{
		Permissions.getInstance().permissions.put("test", permission);
		Assert.assertNotNull(Permissions.getInstance().get("test"));
	}

	@Test(expected = InUseException.class)
	public void undefineUsedExceptionTest(@Mocked final Permission permission) throws InUseException, NoSuchPermissionException{
		Permissions.getInstance().permissions.put("test", permission);
		new NonStrictExpectations() {
			{
				permission.isUsed();
				result = Boolean.TRUE;
			}
		};
		Permissions.getInstance().undefine("test");
	}

	@Test
	public void getEmptyListTest() throws NoSuchPermissionException{
		Assert.assertEquals(new ArrayList<String>(), Permissions.getInstance().get(new ArrayList<String>()));
	}

	@Test(expected = NoSuchPermissionException.class)
	public void getListExceptionTest() throws NoSuchPermissionException{
		Permissions.getInstance().get(Arrays.asList("test"));
	}

	@Test
	public void defineTest(@Mocked final ACPermission permission) throws DuplicatePermissionException, PermissionValidationException{

		new NonStrictExpectations() {
			{
				permission.getPermissionClass();
				result = "Method";
			}
		};

		Permissions.getInstance().define("test", permission);
		new Verifications() {
			{
				permission.getPermissionClass();
				times = 1;
			}
		};
		Assert.assertEquals(1, Permissions.getInstance().permissions.size());
	}

	@Test
	public void getListTest(@Mocked final Permission permission) throws NoSuchPermissionException{
		Permissions.getInstance().permissions.put("test", permission);
		List<Permission> expectedList = new ArrayList<>();
		expectedList.add(permission);
		Assert.assertEquals(expectedList, Permissions.getInstance().get(Arrays.asList("test")));
	}

	@Test
	public void lockTest(@Mocked final Permission permission) throws NoSuchPermissionException{
		Permissions.getInstance().permissions.put("test", permission);
		List<Permission> expectedList = new ArrayList<>();
		expectedList.add(permission);
		Assert.assertEquals(expectedList, Permissions.getInstance().lock(Arrays.asList("test")));
		new Verifications() {
			{
				permission.inc();
				times = 1;
			}
		};
	}

	@Test
	public void unlockNamesTest(@Mocked final Permission permission) throws NoSuchPermissionException{
		Permissions.getInstance().permissions.put("test", permission);
		List<Permission> expectedList = new ArrayList<>();
		expectedList.add(permission);
		Assert.assertEquals(expectedList, Permissions.getInstance().unlockNames(Arrays.asList("test")));
		new Verifications() {
			{
				permission.dec();
				times = 1;
			}
		};
	}

	@Test
	public void unlockTest(@Mocked final Permission permission) throws NoSuchPermissionException{
		Permissions.getInstance().permissions.put("test", permission);
		List<Permission> input = new ArrayList<>();
		input.add(permission);
		Permissions.getInstance().unlock(input);
		new Verifications() {
			{
				permission.dec();
				times = 1;
			}
		};
	}
}
