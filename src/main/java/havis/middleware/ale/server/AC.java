package havis.middleware.ale.server;

import havis.middleware.ale.base.exception.ClientIdentityValidationException;
import havis.middleware.ale.base.exception.DuplicateClientIdentityException;
import havis.middleware.ale.base.exception.DuplicatePermissionException;
import havis.middleware.ale.base.exception.DuplicateRoleException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InUseException;
import havis.middleware.ale.base.exception.NoSuchClientIdentityException;
import havis.middleware.ale.base.exception.NoSuchPermissionException;
import havis.middleware.ale.base.exception.NoSuchRoleException;
import havis.middleware.ale.base.exception.PermissionValidationException;
import havis.middleware.ale.base.exception.RoleValidationException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.UnsupportedOperationException;
import havis.middleware.ale.core.security.Identities;
import havis.middleware.ale.core.security.Method;
import havis.middleware.ale.core.security.Permissions;
import havis.middleware.ale.core.security.Roles;
import havis.middleware.ale.service.ac.ACClientIdentity;
import havis.middleware.ale.service.ac.ACPermission;
import havis.middleware.ale.service.ac.ACRole;

import java.util.Arrays;
import java.util.List;

public class AC {

    private static Security security = Security.getInstance();
    private static AC instance = new AC();

    private AC() {
    }

    public static AC getInstance() {
        return instance;
    }

    public List<String> getPermissionNames() throws ImplementationException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_getPermissionNames);
        return Permissions.getInstance().getNames();
    }

    public void definePermission(String name, ACPermission perimission) throws DuplicatePermissionException, ImplementationException,
            PermissionValidationException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_definePermission);
        Permissions.getInstance().define(name, perimission);
    }

    public void updatePermission(String name, ACPermission perimission) throws ImplementationException, NoSuchPermissionException,
            PermissionValidationException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_undefinePermission);
        Permissions.getInstance().update(name, perimission);
    }

    public ACPermission getPermission(String name) throws ImplementationException, NoSuchPermissionException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_getPermission);
        return Permissions.getInstance().get(name);
    }

    public void undefinePermission(String name) throws ImplementationException, NoSuchPermissionException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_undefinePermission);
        try {
            Permissions.getInstance().undefine(name);
        } catch (InUseException e) {
            throw new ImplementationException(e.getReason());
        }
    }

    public List<String> getRoleNames() throws ImplementationException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_getRoleNames);
        return Roles.getInstance().getNames();
    }

    public void defineRole(String name, ACRole role) throws DuplicateRoleException, ImplementationException, RoleValidationException, SecurityException,
            UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_defineRole);
        Roles.getInstance().define(name, role);
    }

    public void updateRole(String name, ACRole role) throws ImplementationException, havis.middleware.ale.base.exception.DuplicateNameException,
            NoSuchRoleException, RoleValidationException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_updateRole);
        Roles.getInstance().update(name, role);
    }

    public ACRole getRole(String name) throws ImplementationException, NoSuchRoleException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_getRole);
        return Roles.getInstance().get(name);
    }

    public void undefineRole(String name) throws ImplementationException, NoSuchRoleException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_undefineRole);
        Roles.getInstance().undefine(name);
    }

    public void addPermissions(String name, List<String> permissions) throws ImplementationException, NoSuchPermissionException, NoSuchRoleException,
            SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_addPermissions);
        Roles.getInstance().add(name, permissions);
    }

    public void setPermissions(String name, List<String> permissions) throws ImplementationException, NoSuchPermissionException, NoSuchRoleException,
            SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_setPermissions);
        Roles.getInstance().set(name, permissions);
    }

    public void removePermissions(String name, List<String> permissions) throws ImplementationException, NoSuchRoleException, SecurityException,
            UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_removePermissions);
        try {
            Roles.getInstance().remove(name, permissions);
        } catch (NoSuchPermissionException e) {
            throw new ImplementationException(e.getReason());
        }
    }

    public List<String> getClientIdentityNames() throws ImplementationException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_getClientIdentityNames);
        return Identities.getInstance().getNames();
    }

    public void defineClientIdentity(String name, ACClientIdentity id) throws ClientIdentityValidationException, DuplicateClientIdentityException,
            ImplementationException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_defineClientIdentity);
        Identities.getInstance().define(name, id);
    }

    public void updateClientIdentity(String name, ACClientIdentity id) throws ClientIdentityValidationException, ImplementationException,
            NoSuchClientIdentityException, SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_updateClientIdentity);
        Identities.getInstance().update(name, id);
    }

    public ACClientIdentity getClientIdentity(String name) throws ImplementationException, NoSuchClientIdentityException, SecurityException,
            UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_getClientIdentity);
        return Identities.getInstance().get(name);
    }

    public java.util.List<String> getClientPermissionNames(String name) throws ImplementationException, NoSuchClientIdentityException, SecurityException,
            UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_getClientPermissionNames);
        return Identities.getInstance().getPermissionNames(name);
    }

    public void undefineClientIdentity(String name) throws ImplementationException, NoSuchClientIdentityException, SecurityException,
            UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_undefineClientIdentity);
        Identities.getInstance().undefine(name);
    }

    public void addRoles(String name, List<String> roles) throws ImplementationException, NoSuchClientIdentityException, SecurityException,
            NoSuchRoleException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_addRoles);
        Identities.getInstance().add(name, roles);
    }

    public void removeRoles(String name, List<String> roles) throws ImplementationException, NoSuchClientIdentityException, SecurityException,
            UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_removeRoles);
        try {
            Identities.getInstance().remove(name, roles);
        } catch (NoSuchRoleException e) {
            throw new ImplementationException(e.getReason());
        }
    }

    public void setRoles(String name, List<String> roles) throws ImplementationException, NoSuchClientIdentityException, NoSuchRoleException,
            SecurityException, UnsupportedOperationException {
        security.isAllowed(Method.ALEAC, Method.ALEAC_setRoles);
        Identities.getInstance().set(name, roles);
    }

    public List<String> getSupportedOperations() throws ImplementationException {
        return Arrays.asList("getStandardVersion", "getVendorVersion", "getSupportedOperations", "getPermissionNames", "definePermission", "updatePermission",
                "getPermission", "undefinePermission", "getRoleNames", "defineRole", "updateRole", "getRole", "undefineRole", "addPermissions",
                "setPermissions", "removePermissions", "getClientIdentityNames", "defineClientIdentity", "updateClientIdentity", "getClientIdentity",
                "getClientPermissionNames", "undefineClientIdentity", "addRoles", "removeRoles", "setRoles");
    }

    public String getStandardVersion() throws ImplementationException {
        return havis.middleware.ale.core.manager.AC.getStandardVersion();
    }
}