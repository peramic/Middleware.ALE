package havis.middleware.ale.core.security;

/**
 * Provides the instances of method access class
 */
public enum Method {

	/**
	 * Any
	 */
	Any,

	/**
	 * ALE Reading API
	 */
	ALE,

	ALE_define,

	ALE_undefine,

	ALE_getECSpec,

	ALE_getECSpecNames,

	ALE_subscribe,

	ALE_unsubscribe,

	ALE_poll,

	ALE_immediate,

	ALE_getSubscribers,

	/**
	 * ALE Writing API
	 */
	ALECC,

	ALECC_define,

	ALECC_undefine,

	ALECC_getCCSpec,

	ALECC_getCCSpecNames,

	ALECC_subscribe,

	ALECC_unsubscribe,

	ALECC_poll,

	ALECC_immediate,

	ALECC_getSubscribers,

	ALECC_defineEPCCache,

	ALECC_undefineEPCCache,

	ALECC_getEPCCache,

	ALECC_getEPCCacheNames,

	ALECC_replenishEPCCache,

	ALECC_depleteEPCCache,

	ALECC_getEPCCacheContents,

	ALECC_defineAssocTable,

	ALECC_undefineAssocTable,

	ALECC_getAssocTableNames,

	ALECC_getAssocTable,

	ALECC_putAssocTableEntries,

	ALECC_getAssocTableValue,

	ALECC_getAssocTableEntries,

	ALECC_removeAssocTableEntry,

	ALECC_removeAssocTableEntries,

	ALECC_defineRNG,

	ALECC_undefineRNG,

	ALECC_getRNGNames,

	ALECC_getRNG,

	/**
	 * ALE Tag Memory API
	 */
	ALETM,

	ALETM_defineTMSpec,

	ALETM_undefineTMSpec,

	ALETM_getTMSpec,

	ALETM_getTMSpecNames,

	/**
	 * ALE Logical Reader API
	 */
	ALELR,

	ALELR_define,

	ALELR_update,

	ALELR_undefine,

	ALELR_getLogicalReaderNames,

	ALELR_getLRSpec,

	ALELR_addReaders,

	ALELR_setReaders,

	ALELR_removeReaders,

	ALELR_setProperties,

	ALELR_getPropertyValue,

	/**
	 * ALE Access Control API
	 */
	ALEAC,

	ALEAC_getPermissionNames,

	ALEAC_definePermission,

	ALEAC_updatePermission,

	ALEAC_getPermission,

	ALEAC_undefinePermission,

	ALEAC_getRoleNames,

	ALEAC_defineRole,

	ALEAC_updateRole,

	ALEAC_getRole,

	ALEAC_undefineRole,

	ALEAC_addPermissions,

	ALEAC_setPermissions,

	ALEAC_removePermissions,

	ALEAC_getClientIdentityNames,

	ALEAC_defineClientIdentity,

	ALEAC_updateClientIdentity,

	ALEAC_getClientIdentity,

	ALEAC_getClientPermissionNames,

	ALEAC_undefineClientIdentity,

	ALEAC_addRoles,

	ALEAC_removeRoles,

	ALEAC_setRoles,

	/**
	 * ALE Port Cycle API
	 */
	ALEPC,

	ALEPC_define,

	ALEPC_undefine,

	ALEPC_getPCSpec,

	ALEPC_getPCSpecNames,

	ALEPC_subscribe,

	ALEPC_unsubscribe,

	ALEPC_poll,

	ALEPC_immediate,

	ALEPC_getSubscribers,

	ALEPC_execute,

	/**
	 * Ha-VIS Management Console API
	 */
	HAVISMC,

	HAVISMC_add,

	HAVISMC_remove,

	HAVISMC_update,

	HAVISMC_get,

	HAVISMC_list,

	HAVISMC_getProperty,

	HAVISMC_setProperty,

	HAVISMC_execute
}