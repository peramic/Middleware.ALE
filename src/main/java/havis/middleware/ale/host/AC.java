package havis.middleware.ale.host;

import havis.middleware.ale.base.annotation.EndpointMetadata;
import havis.middleware.ale.base.exception.ALEException;
import havis.middleware.ale.base.exception.ClientIdentityValidationException;
import havis.middleware.ale.base.exception.DuplicateClientIdentityException;
import havis.middleware.ale.base.exception.DuplicatePermissionException;
import havis.middleware.ale.base.exception.DuplicateRoleException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.NoSuchClientIdentityException;
import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.NoSuchRoleException;
import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.base.exception.RoleValidationException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.UnsupportedOperationException;
import havis.middleware.ale.service.ac.ACClientIdentity;
import havis.middleware.ale.service.ac.ACPermission;
import havis.middleware.ale.service.ac.ACRole;
import havis.middleware.ale.service.ac.AddPermissions;
import havis.middleware.ale.service.ac.AddPermissionsResult;
import havis.middleware.ale.service.ac.AddRoles;
import havis.middleware.ale.service.ac.AddRolesResult;
import havis.middleware.ale.service.ac.ArrayOfString;
import havis.middleware.ale.service.ac.ClientIdentityValidationExceptionResponse;
import havis.middleware.ale.service.ac.DefineClientIdentity;
import havis.middleware.ale.service.ac.DefineClientIdentityResult;
import havis.middleware.ale.service.ac.DefinePermission;
import havis.middleware.ale.service.ac.DefinePermissionResult;
import havis.middleware.ale.service.ac.DefineRole;
import havis.middleware.ale.service.ac.DefineRoleResult;
import havis.middleware.ale.service.ac.DuplicateClientIdentityExceptionResponse;
import havis.middleware.ale.service.ac.DuplicatePermissionExceptionResponse;
import havis.middleware.ale.service.ac.DuplicateRoleExceptionResponse;
import havis.middleware.ale.service.ac.EmptyParms;
import havis.middleware.ale.service.ac.GetClientIdentity;
import havis.middleware.ale.service.ac.GetClientPermissionNames;
import havis.middleware.ale.service.ac.GetPermission;
import havis.middleware.ale.service.ac.GetRole;
import havis.middleware.ale.service.ac.ImplementationExceptionResponse;
import havis.middleware.ale.service.ac.NoSuchClientIdentityExceptionResponse;
import havis.middleware.ale.service.ac.NoSuchPermissionExceptionResponse;
import havis.middleware.ale.service.ac.NoSuchRoleExceptionResponse;
import havis.middleware.ale.service.ac.PermissionValidationExceptionResponse;
import havis.middleware.ale.service.ac.RemovePermissions;
import havis.middleware.ale.service.ac.RemovePermissionsResult;
import havis.middleware.ale.service.ac.RemoveRoles;
import havis.middleware.ale.service.ac.RemoveRolesResult;
import havis.middleware.ale.service.ac.RoleValidationExceptionResponse;
import havis.middleware.ale.service.ac.SecurityExceptionResponse;
import havis.middleware.ale.service.ac.SetPermissions;
import havis.middleware.ale.service.ac.SetPermissionsResult;
import havis.middleware.ale.service.ac.SetRoles;
import havis.middleware.ale.service.ac.SetRolesResult;
import havis.middleware.ale.service.ac.UndefineClientIdentity;
import havis.middleware.ale.service.ac.UndefineClientIdentityResult;
import havis.middleware.ale.service.ac.UndefinePermission;
import havis.middleware.ale.service.ac.UndefinePermissionResult;
import havis.middleware.ale.service.ac.UndefineRole;
import havis.middleware.ale.service.ac.UndefineRoleResult;
import havis.middleware.ale.service.ac.UnsupportedOperationExceptionResponse;
import havis.middleware.ale.service.ac.UpdateClientIdentity;
import havis.middleware.ale.service.ac.UpdateClientIdentityResult;
import havis.middleware.ale.service.ac.UpdatePermission;
import havis.middleware.ale.service.ac.UpdatePermissionResult;
import havis.middleware.ale.service.ac.UpdateRole;
import havis.middleware.ale.service.ac.UpdateRoleResult;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

@WebService(targetNamespace = "urn:epcglobal:aleac:wsdl:1", portName = "ALEACServicePort", serviceName = "ALEACService", endpointInterface = "havis.middleware.ale.service.ac.ALEACServicePortType", wsdlLocation = "havis/middleware/wsdl/EPCglobal-ale-1_1-aleac.wsdl")
@EndpointMetadata(xsdLocations = { "havis/middleware/wsdl/EPCglobal-ale-1_1-aleac.xsd", "havis/middleware/wsdl/EPCglobal.xsd" })
public class AC {

	@Resource
	WebServiceContext context;

	havis.middleware.ale.server.AC server = havis.middleware.ale.server.AC
			.getInstance();

	public ArrayOfString getPermissionNames(EmptyParms parms) throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getPermissionNames();
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public DefinePermissionResult definePermission(DefinePermission parms)
			throws Exception {
		try {
			server.definePermission(parms.getPermName(), parms.getPerm());
			return new DefinePermissionResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UpdatePermissionResult updatePermission(UpdatePermission parms)
			throws Exception {
		try {
			server.updatePermission(parms.getPermName(), parms.getPerm());
			return new UpdatePermissionResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ACPermission getPermission(GetPermission parms) throws Exception {
		try {
			return server.getPermission(parms.getPermName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UndefinePermissionResult undefinePermission(UndefinePermission parms)
			throws Exception {
		try {
			server.undefinePermission(parms.getPermName());
			return new UndefinePermissionResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getRoleNames(EmptyParms parms) throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getRoleNames();
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public DefineRoleResult defineRole(DefineRole parms) throws Exception {
		try {
			server.defineRole(parms.getRoleName(), parms.getRole());
			return new DefineRoleResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UpdateRoleResult updateRole(UpdateRole parms) throws Exception {
		try {
			server.updateRole(parms.getRoleName(), parms.getRole());
			return new UpdateRoleResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ACRole getRole(GetRole parms) throws Exception {
		try {
			return server.getRole(parms.getRoleName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UndefineRoleResult undefineRole(UndefineRole parms) throws Exception {
		try {
			server.undefineRole(parms.getRoleName());
			return new UndefineRoleResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public AddPermissionsResult addPermissions(AddPermissions parms)
			throws Exception {
		try {
			server.addPermissions(parms.getRoleName(), parms
					.getPermissionNames().getPermissionName());
			return new AddPermissionsResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public SetPermissionsResult setPermissions(SetPermissions parms)
			throws Exception {
		try {
			server.setPermissions(parms.getRoleName(), parms
					.getPermissionNames().getPermissionName());
			return new SetPermissionsResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public RemovePermissionsResult removePermissions(RemovePermissions parms)
			throws Exception {
		try {
			server.removePermissions(parms.getRoleName(), parms
					.getPermissionNames().getPermissionName());
			return new RemovePermissionsResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getClientIdentityNames(EmptyParms parms)
			throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getClientIdentityNames();
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public DefineClientIdentityResult defineClientIdentity(
			DefineClientIdentity parms) throws Exception {
		try {
			server.defineClientIdentity(parms.getIdentityName(), parms.getId());
			return new DefineClientIdentityResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UpdateClientIdentityResult updateClientIdentity(
			UpdateClientIdentity parms) throws Exception {
		try {
			server.updateClientIdentity(parms.getIdentityName(), parms.getId());
			return new UpdateClientIdentityResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ACClientIdentity getClientIdentity(GetClientIdentity parms)
			throws Exception {
		try {
			return server.getClientIdentity(parms.getIdentityName());
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getClientPermissionNames(
			final GetClientPermissionNames parms) throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getClientPermissionNames(parms
							.getIdentityName());
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public UndefineClientIdentityResult undefineClientIdentity(
			UndefineClientIdentity parms) throws Exception {
		try {
			server.undefineClientIdentity(parms.getIdentityName());
			return new UndefineClientIdentityResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public AddRolesResult addRoles(AddRoles parms) throws Exception {
		try {
			server.addRoles(parms.getIdentityName(), parms.getRoleNames()
					.getRoleName());
			return new AddRolesResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public RemoveRolesResult removeRoles(RemoveRoles parms) throws Exception {
		try {
			server.removeRoles(parms.getIdentityName(), parms.getRoleNames()
					.getRoleName());
			return new RemoveRolesResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public SetRolesResult setRoles(SetRoles parms) throws Exception {
		try {
			server.setRoles(parms.getIdentityName(), parms.getRoleNames()
					.getRoleName());
			return new SetRolesResult();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	public ArrayOfString getSupportedOperations(EmptyParms parms)
			throws Exception {
		try {
			return new ArrayOfString() {
				{
					string = server.getSupportedOperations();
				}
			};
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public String getStandardVersion(EmptyParms parms) throws Exception {
		try {
			return server.getStandardVersion();
		} catch (Exception e) {
			throw exception(e);
		}
	}

	@WebMethod
	public String getVendorVersion(EmptyParms parms) throws Exception {
		try {
			return Main.getVendorVersionUrl(this.context);
		} catch (Exception e) {
			throw exception(e);
		}
	}

	private Exception exception(Exception exception) {
		try {
			throw exception;
		} catch (SecurityException e) {
			return new SecurityExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchPermissionException e) {
			return new NoSuchPermissionExceptionResponse(null, removeStrackTrace(e));
		} catch (PermissionValidationException e) {
			return new PermissionValidationExceptionResponse(null, removeStrackTrace(e));
		} catch (DuplicatePermissionException e) {
			return new DuplicatePermissionExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchRoleException e) {
			return new NoSuchRoleExceptionResponse(null, removeStrackTrace(e));
		} catch (RoleValidationException e) {
			return new RoleValidationExceptionResponse(null, removeStrackTrace(e));
		} catch (DuplicateRoleException e) {
			return new DuplicateRoleExceptionResponse(null, removeStrackTrace(e));
		} catch (NoSuchClientIdentityException e) {
			return new NoSuchClientIdentityExceptionResponse(null, removeStrackTrace(e));
		} catch (ClientIdentityValidationException e) {
			return new ClientIdentityValidationExceptionResponse(null, removeStrackTrace(e));
		} catch (DuplicateClientIdentityException e) {
			return new DuplicateClientIdentityExceptionResponse(null, removeStrackTrace(e));
		} catch (UnsupportedOperationException e) {
			return new UnsupportedOperationExceptionResponse(null, removeStrackTrace(e));
		} catch (ImplementationException e) {
			return new ImplementationExceptionResponse(null, removeStrackTrace(e));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private <T extends ALEException> T removeStrackTrace(T exception) {
		exception.setStackTrace(new StackTraceElement[0]);
		return exception;
	}
}