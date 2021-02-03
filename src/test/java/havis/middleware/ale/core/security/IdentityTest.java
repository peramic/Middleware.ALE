package havis.middleware.ale.core.security;

import havis.middleware.ale.base.exception.ClientIdentityValidationException;
import havis.middleware.ale.base.exception.DuplicatePermissionException;
import havis.middleware.ale.base.exception.DuplicateRoleException;
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
import java.util.Date;
import java.util.List;

import mockit.Mocked;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class IdentityTest {
	@Test(expected = ClientIdentityValidationException.class)
	public void identityRoleExceptionTest() throws ClientIdentityValidationException {
		ACClientIdentity acSpec = new ACClientIdentity();
		RoleNames names = new RoleNames();
		names.getRoleName().add("hans");
		acSpec.setRoleNames(names);

		new Identity("exception", acSpec);
	}
	@Test
	public void identityAddAndRemoveTest() throws ClientIdentityValidationException, DuplicatePermissionException, PermissionValidationException, NoSuchRoleException, DuplicateRoleException, RoleValidationException {
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("peterPermission", permSpec);

		ACClientIdentity acSpec = new ACClientIdentity();
		RoleNames names = new RoleNames();
		names.getRoleName().add("hansRole");
		acSpec.setRoleNames(names);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("peterPermission");
		spec.setPermissionNames(value);

		Roles.getInstance().define("hansRole", spec);
		Roles.getInstance().define("juergenRole", spec);

		Identity identity = new Identity("werner", acSpec);

		Assert.assertEquals(1, identity.roles.size());

		List<String> list = new ArrayList<>();
		list.add("juergenRole");
		identity.add(list);

		Assert.assertEquals(2, identity.roles.size());
		Assert.assertEquals(spec, identity.roles.get(1).getSpec());
		Assert.assertTrue(identity.roles.get(1).isUsed());

		identity.remove(list);
		Assert.assertEquals(1, identity.roles.size());
	}

	@Test
	public void identityContainsTest() throws ClientIdentityValidationException, DuplicatePermissionException, PermissionValidationException, NoSuchRoleException, DuplicateRoleException, RoleValidationException {
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("gerdaPermission", permSpec);

		ACClientIdentity acSpec = new ACClientIdentity();
		RoleNames names = new RoleNames();
		names.getRoleName().add("achimRole");
		acSpec.setRoleNames(names);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("gerdaPermission");
		spec.setPermissionNames(value);

		Roles.getInstance().define("achimRole", spec);
		Roles.getInstance().define("fritzRole", spec);

		Identity identity = new Identity("eugen", acSpec);

		Assert.assertEquals(1, identity.roles.size());

		List<String> list = new ArrayList<>();
		list.add("fritzRole");
		identity.add(list);

		Assert.assertEquals(2, identity.roles.size());
		Assert.assertEquals(spec, identity.roles.get(1).getSpec());
		Assert.assertTrue(identity.roles.get(1).isUsed());

		identity.set(list);

		Assert.assertEquals(1, identity.roles.size());
		Assert.assertEquals("fritzRole", identity.roles.get(0).name);
	}

	@Test
	public void getPermissionNamesTest() throws ClientIdentityValidationException, DuplicatePermissionException, PermissionValidationException, NoSuchRoleException, DuplicateRoleException, RoleValidationException {
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("martaPermission", permSpec);

		ACClientIdentity acSpec = new ACClientIdentity();
		RoleNames names = new RoleNames();
		names.getRoleName().add("rolandRole");
		acSpec.setRoleNames(names);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("martaPermission");
		spec.setPermissionNames(value);

		Roles.getInstance().define("rolandRole", spec);

		Identity identity = new Identity("helmut", acSpec);

		List<String> expected = new ArrayList<>();
		expected.add("martaPermission");

		Assert.assertEquals(expected, identity.getPermissionNames());

		Permissions.getInstance().define("alexPermission", permSpec);
		names.getRoleName().add("viktorRole");
		value.getPermissionName().add("alexPermission");
		spec.setPermissionNames(value);
		acSpec.setRoleNames(names);
		Roles.getInstance().define("viktorRole", spec);
		identity = new Identity("hanne", acSpec);

		Assert.assertNotSame(expected, identity.getPermissionNames());
		expected.add("martaPermission");
		expected.add("alexPermission");
		Assert.assertEquals(expected, identity.getPermissionNames());
	}

	@Test
	public void containsTest() throws ClientIdentityValidationException, DuplicatePermissionException, PermissionValidationException, NoSuchRoleException, DuplicateRoleException, RoleValidationException {
		ACPermission permSpec = new ACPermission();
		Instances instances = new Instances();

		instances.getInstance().add("*");

		permSpec.setPermissionClass("Method");
		permSpec.setCreationDate(new Date());
		permSpec.setExtension(new ACPermissionExtension());
		permSpec.setInstances(instances);
		permSpec.setSchemaVersion(BigDecimal.valueOf(2.5));

		Permissions.getInstance().define("silkePermission", permSpec);

		ACClientIdentity acSpec = new ACClientIdentity();
		RoleNames names = new RoleNames();
		names.getRoleName().add("sergeyRole");
		acSpec.setRoleNames(names);

		ACRole spec = new ACRole();
		PermissionNames value = new PermissionNames();
		value.getPermissionName().add("silkePermission");
		spec.setPermissionNames(value);

		Roles.getInstance().define("sergeyRole", spec);

		Identity identity = new Identity("fabian", acSpec);

		Assert.assertTrue(identity.containsPermission(Method.ALE_define, Method.ALE_poll));

		List<String> input = new ArrayList<>();
		input.add("sergeyRole");
		identity.remove(input);

		Assert.assertFalse(identity.containsPermission(Method.ALE_define, Method.ALE_poll));
	}

	@Test
	public void specTest(@Mocked final ACClientIdentity clientID) throws ClientIdentityValidationException{
		final Identity identity = new Identity("test", clientID);
		identity.setSpec(clientID);
		Assert.assertEquals(clientID, identity.getSpec());
		new Verifications() {
			{
				clientID.setRoleNameList(identity.getPermissionNames());
				times = 1;
			}
		};
	}

	@Test
	public void disposeTest(@Mocked final ACClientIdentity clientID) throws ClientIdentityValidationException{
		Identity identity = new Identity("test", clientID);
		identity.dispose();
		//TODO method is blank
	}
}
