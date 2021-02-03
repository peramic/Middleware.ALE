package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.ClientIdentityValidationException;
import havis.middleware.ale.base.exception.DuplicateClientIdentityException;
import havis.middleware.ale.base.exception.DuplicatePermissionException;
import havis.middleware.ale.base.exception.DuplicateRoleException;
import havis.middleware.ale.base.exception.NoSuchClientIdentityException;
import havis.middleware.ale.base.exception.NoSuchRoleException;
import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.base.exception.RoleValidationException;
import havis.middleware.ale.service.ac.ACClientIdentity;
import havis.middleware.ale.service.ac.ACClientIdentity.RoleNames;
import havis.middleware.ale.service.ac.ACPermission;
import havis.middleware.ale.service.ac.ACPermission.Instances;
import havis.middleware.ale.service.ac.ACPermissionExtension;
import havis.middleware.ale.service.ac.ACRole;
import havis.middleware.ale.service.ac.ACRole.PermissionNames;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IdentitiesTest {

	@Before
	public void before(){
		Identities.instance = new Identities();
	}
	@Test(expected = NoSuchClientIdentityException.class)
	public void addException() throws NoSuchRoleException, NoSuchClientIdentityException{
		Identities.getInstance().add("addList", new ArrayList<String>());
	}
	@Test(expected = NoSuchClientIdentityException.class)
	public void removeException() throws NoSuchRoleException, NoSuchClientIdentityException{
		Identities.getInstance().remove("addList", new ArrayList<String>());
	}
	@Test(expected = NoSuchClientIdentityException.class)
	public void setException() throws NoSuchRoleException, NoSuchClientIdentityException{
		Identities.getInstance().set("addList", new ArrayList<String>());
	}
	@Test(expected = SecurityException.class)
	public void containsSecurityExceptionTest() throws DuplicatePermissionException, PermissionValidationException, DuplicateRoleException, RoleValidationException, ClientIdentityValidationException, DuplicateClientIdentityException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("elkePermission", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("elkePermission");
		spec.setPermissionNames(value);

		Roles.getInstance().define("fredRole", spec);

		ACClientIdentity acSpec = new ACClientIdentity();
		RoleNames names = new RoleNames();
		names.getRoleName().add("fredRole");
		acSpec.setRoleNames(names);

		Identities.getInstance().define("wernerIdentity", acSpec);
		Identities.getInstance().containsPermission("Permission", Method.ALE_define, Method.ALE_immediate);
	}
	@Test
	public void containsTest() throws DuplicatePermissionException, PermissionValidationException, DuplicateRoleException, RoleValidationException, ClientIdentityValidationException, DuplicateClientIdentityException{
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("wernerPermission", permSpec);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("wernerPermission");
		spec.setPermissionNames(value);

		Roles.getInstance().define("angiRole", spec);

		ACClientIdentity acSpec = new ACClientIdentity();
		RoleNames names = new RoleNames();
		names.getRoleName().add("angiRole");
		acSpec.setRoleNames(names);

		Identities.getInstance().define("siegfriedIdentity", acSpec);
		Identities.getInstance().containsPermission("siegfriedIdentity", Method.ALE_define, Method.ALE_immediate);
	}
	@Test(expected = SecurityException.class)
	public void containsAccessDeniedExceptionTest(@Mocked final Identity identity) throws DuplicatePermissionException, PermissionValidationException, DuplicateRoleException, RoleValidationException, ClientIdentityValidationException, DuplicateClientIdentityException{
		Identities.getInstance().identities.put("test", identity);
		new NonStrictExpectations() {
			{
				identity.containsPermission(Method.ALE, Method.ALE);
				result = Boolean.FALSE;
			}
		};
		Identities.getInstance().containsPermission("test", Method.ALE, Method.ALE);
	}
	@Test
	public void getNamesEmptyListTest(@Mocked final Identity identity){
		Assert.assertEquals(Collections.<String>emptyList(), Identities.getInstance().getNames());
		Identities.getInstance().identities.put("test", identity);
		Assert.assertEquals(Arrays.asList("test"), Identities.getInstance().getNames());
		Identities.getInstance().identities.remove("test");
		Assert.assertEquals(Collections.<String>emptyList(), Identities.getInstance().getNames());
	}
	@Test(expected = DuplicateClientIdentityException.class)
	public void defineExceptionTest(@Mocked final Map<String, Identity> ident) throws ClientIdentityValidationException, DuplicateClientIdentityException{
		Identities.getInstance().identities = ident;
		new NonStrictExpectations() {
			{
				ident.containsKey("a");
				result = Boolean.TRUE;
			}
		};
		Identities.getInstance().define("a", null);
	}
	@Test(expected = NoSuchClientIdentityException.class)
	public void updateExceptionTest() throws NoSuchClientIdentityException{
		Identities.getInstance().update("test", new ACClientIdentity());
	}
	@Test
	public void updateTest(@Mocked ACClientIdentity acID, @Mocked final ACClientIdentity updateID) throws NoSuchClientIdentityException, ClientIdentityValidationException, DuplicateClientIdentityException{
		Identities.getInstance().define("test", acID);
		Assert.assertNull(Identities.getInstance().get("test").getSchemaVersion());

		Identities.getInstance().update("test", updateID);

		new NonStrictExpectations() {
			{
				updateID.getSchemaVersion();
				result = BigDecimal.valueOf(1);
			}
		};
		Assert.assertEquals(Identities.getInstance().get("test").getSchemaVersion(), BigDecimal.valueOf(1));
	}
	@Test(expected = NoSuchClientIdentityException.class)
	public void getExceptionTest() throws NoSuchClientIdentityException {
		Identities.getInstance().get("test");
	}
	@Test(expected = NoSuchClientIdentityException.class)
	public void getPermissionNamesExceptionTest() throws NoSuchClientIdentityException{
		Identities.getInstance().getPermissionNames("test");
	}
	@Test
	public void getPermissionNamesTest(@Mocked Permission perm, @Mocked ACClientIdentity acID, @Mocked final Identity ident) throws ClientIdentityValidationException, DuplicateClientIdentityException, NoSuchClientIdentityException{
		Identities.getInstance().identities.put("id", ident);
		new NonStrictExpectations() {
			{
				ident.getPermissionNames();
				result = Arrays.asList("test");
			}
		};
		Assert.assertEquals(Arrays.asList("test"), Identities.getInstance().getPermissionNames("id"));
	}
	@Test(expected = NoSuchClientIdentityException.class)
	public void undefineExceptionTest() throws NoSuchClientIdentityException{
		Identities.getInstance().undefine("test");
	}
	@Test
	public void undefineTest(@Mocked Identity identity) throws NoSuchClientIdentityException{
		Identities.getInstance().identities.put("test", identity);
		Assert.assertEquals(1, Identities.getInstance().identities.size());
		Identities.getInstance().undefine("test");
		Assert.assertEquals(0, Identities.getInstance().identities.size());
	}
	@Test
	public void addTest(@Mocked final Identity identity, @Mocked final List<String> input) throws NoSuchRoleException, NoSuchClientIdentityException{
		Identities.getInstance().identities.put("test", identity);
		Identities.getInstance().add("test", input);
		new Verifications() {
			{
				identity.add(input);
				times = 1;
			}
		};
		new NonStrictExpectations() {
			{
				identity.getSpec().getRoleNames().getRoleName().size();
				result = Integer.valueOf(1);
			}
		};
		Assert.assertEquals(1, Identities.getInstance().get("test").getRoleNames().getRoleName().size());
	}
	@Test
	public void setTest(@Mocked final Identity identity, @Mocked final List<String> input) throws NoSuchRoleException, NoSuchClientIdentityException{
		Identities.getInstance().identities.put("test", identity);
		Identities.getInstance().set("test", input);
		new Verifications() {
			{
				identity.set(input);
				times = 1;
			}
		};
		new NonStrictExpectations() {
			{
				identity.getSpec().getRoleNames().getRoleName().size();
				result = Integer.valueOf(1);
			}
		};
		Assert.assertEquals(1, Identities.getInstance().get("test").getRoleNames().getRoleName().size());
	}
	@Test
	public void removeTest(@Mocked final Identity identity, @Mocked final List<String> input) throws NoSuchRoleException, NoSuchClientIdentityException{
		Identities.getInstance().identities.put("test", identity);
		Identities.getInstance().remove("test", input);
		new Verifications() {
			{
				identity.remove(input);
				times = 1;
			}
		};
		Assert.assertEquals(0, Identities.getInstance().get("test").getRoleNames().getRoleName().size());
	}
}
