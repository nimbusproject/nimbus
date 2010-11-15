/*
 * Copyright 1999-2008 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.globus.workspace.persistence;

public interface PersistenceAdapterConstants {

    /* Prepared Statements using TYPE_SCROLL_INSENSITIVE + CONCUR_UPDATABLE */

    //public static final String[] INSENSITIVE_PREPARED_STATEMENTS = {};

    /* Prepared Statements */

    public static final String SQL_SELECT_RESOURCES =
            "SELECT id FROM resources";

    public static final String SQL_SELECT_ALL_ASSOCIATIONS =
            "SELECT * FROM associations";

    public static final String SQL_SELECT_MULTIPLE_OF_AVAILABLE_MEMORY =
        "SELECT SUM(available_memory - MOD(CAST (available_memory AS INT), ?)) FROM resourcepool_entries";    
    
    public static final String SQL_SELECT_TOTAL_AVAILABLE_MEMORY =
        "SELECT SUM(available_memory) FROM resourcepool_entries";
    
    public static final String SQL_SELECT_TOTAL_MAX_MEMORY =
        "SELECT SUM(maximum_memory) FROM resourcepool_entries";

    public static final String SQL_SELECT_TOTAL_PREEMPTABLE_MEMORY =
        "SELECT SUM(preemptable_memory) FROM resourcepool_entries";    

    public static final String SQL_SELECT_USED_NON_PREEMPTABLE_MEMORY =
        "SELECT SUM(maximum_memory-available_memory-preemptable_memory) FROM resourcepool_entries";     
    
    /* Prepared Statements with dynamic markers */

    public static final String SQL_SET_STATE =
            "UPDATE resources SET state=?, error_fault=? WHERE id=?";

    public static final String SQL_SET_TARGET_STATE =
            "UPDATE resources SET target_state=? WHERE id=?";

    public static final String SQL_SET_OPS_ENABLED =
            "UPDATE resources SET ops_enabled=? WHERE id=?";

    public static final String SQL_SET_NETWORKING =
            "UPDATE vms SET network=? WHERE id=?";

    public static final String SQL_SET_VMM_ACCESS_OK =
            "UPDATE resources SET vmm_access_ok=? WHERE id=?";

    public static final String SQL_SET_HOSTNAME =
            "UPDATE vms SET node=? WHERE id=?";

    public static final String SQL_SET_ROOT_UNPROP_TARGET =
            "UPDATE vm_partitions SET alternate_unprop=?, unprop_required=1 " +
            "WHERE vmid=? AND rootdisk=1";

    public static final String SQL_UNSET_ROOT_UNPROP_TARGET =
            "UPDATE vm_partitions SET alternate_unprop=? " +
            "WHERE vmid=? AND rootdisk=1";

    public static final String SQL_SET_VM_CUSTOMIZATION_SENT =
            "UPDATE vm_customization SET sent=? " +
            "WHERE vmid=? AND sourcepath=? AND destpath=?";

    public static final String SQL_SET_STARTTIME =
            "UPDATE resources SET start_time=? WHERE id=?";

    public static final String SQL_SET_TERMTIME =
            "UPDATE resources SET term_time=? WHERE id=?";

    public static final String SQL_DELETE_RESOURCE =
            "DELETE FROM resources WHERE id=?";

    public static final String SQL_DELETE_GROUP_RESOURCE =
            "DELETE FROM groupresources WHERE groupid=?";

    public static final String SQL_DELETE_VM =
            "DELETE from vms WHERE id=?";

    public static final String SQL_DELETE_VM_PARTITIONS =
            "DELETE from vm_partitions WHERE vmid=?";

    public static final String SQL_DELETE_VM_DEPLOYMENT =
            "DELETE from vm_deployment WHERE vmid=?";

    public static final String SQL_DELETE_VM_CUSTOMIZATION =
            "DELETE from vm_customization WHERE vmid=?";

    public static final String SQL_INSERT_RESOURCE =
            "INSERT INTO resources VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String SQL_INSERT_GROUP_RESOURCE =
            "INSERT INTO groupresources VALUES(?,?)";

    public static final String SQL_INSERT_VM =
            "INSERT INTO vms VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String SQL_INSERT_VM_PARTITION =
            "INSERT INTO vm_partitions VALUES(?,?,?,?,?,?,?,?,?)";

    public static final String SQL_INSERT_VM_DEPLOYMENT =
            "INSERT INTO vm_deployment VALUES(?,?,?,?,?,?)";

    public static final String SQL_INSERT_VM_CUSTOMIZATION =
            "INSERT INTO vm_customization VALUES(?,?,?,?)";

    public static final String SQL_LOAD_RESOURCE =
            "SELECT name, state, target_state, term_time, ops_enabled, " +
                    "creator_dn, start_time, vmm_access_ok, " +
                    "ensembleid, groupid, groupsize, last_in_group, " +
                    "launch_index, error_fault " +
                    "FROM resources WHERE id=?";

    public static final String SQL_LOAD_GROUP_RESOURCE =
            "SELECT creator_dn FROM groupresources WHERE groupid=?";

    public static final String SQL_LOAD_RESOURCE_NAME =
            "SELECT name FROM resources WHERE id=?";

    public static final String SQL_LOAD_VM =
            "SELECT name, node, prop_required, unprop_required, network, " +
                    "kernel_parameters, vmm, vmm_version, assocs_needed, " +
                    "md_user_data, preemptable " +
                    "FROM vms WHERE id=?";
    
    public static final String SQL_LOAD_VM_PARTITIONS =
            "SELECT image, imagemount, readwrite, rootdisk, blankspace, " +
                    "prop_required, unprop_required, alternate_unprop " +
                    "FROM vm_partitions WHERE vmid=?";

    public static final String SQL_LOAD_VM_DEPLOYMENT =
            "SELECT requested_state, requested_shutdown, min_duration, " +
                    "ind_physmem, ind_physcpu " +
                    "FROM vm_deployment WHERE vmid=?";

    public static final String SQL_LOAD_VM_CUSTOMIZATION =
            "SELECT sourcepath, destpath, sent " +
            "FROM vm_customization WHERE vmid=?";

    public static final String SQL_UPDATE_ASSOCIATION_ENTRY =
            "UPDATE association_entries SET used=? " +
            "WHERE association=? AND ipaddress=?";

    public static final String SQL_DELETE_ALL_ASSOCIATIONS =
            "DELETE FROM associations";

    public static final String SQL_DELETE_ALL_ASSOCIATION_ENTRIES =
            "DELETE FROM association_entries";

    public static final String SQL_SELECT_ASSOCIATION =
            "SELECT * FROM association_entries WHERE association=?";

    public static final String SQL_SELECT_ALL_RESOURCE_POOL_ENTRIES =
                "SELECT * FROM resourcepool_entries ORDER BY hostname";

    public static final String SQL_SELECT_RESOURCE_POOL_ENTRY =
            "SELECT * FROM resourcepool_entries WHERE hostname = ?";

    public static final String SQL_INSERT_RESOURCE_POOL_ENTRY =
            "INSERT INTO resourcepool_entries (resourcepool,hostname," +
                    "associations,maximum_memory,available_memory,active) " +
                    "VALUES(?,?,?,?,?,?)";

    public static final String SQL_UPDATE_RESOURCE_POOL_ENTRY_MEMORY =
            "UPDATE resourcepool_entries SET available_memory=?, preemptable_memory=? " +
            "WHERE hostname=?";

    // not a prepared statement, the skeleton for custom update queries
    public static final String SQL_UPDATE_RESOURCE_POOL_ENTRY_SKELETAL =
            "UPDATE resourcepool_entries SET %s WHERE hostname=?";

    public static final String SQL_DELETE_RESOURCE_POOL_ENTRY =
            "DELETE FROM resourcepool_entries WHERE hostname = ?";

    public static final String SQL_SELECT_RESOURCE_POOL =
            "SELECT * FROM resourcepool_entries WHERE resourcepool=?";

    public static final String SQL_JOIN_SELECT_RESOURCE_POOL_MEMORY =
            "SELECT vm_deployment.ind_physmem FROM vm_deployment,vms " +
                    "WHERE vms.node=? AND vm_deployment.vmid=vms.id";

    public static final String SQL_SELECT_ALL_VMS_IN_GROUP =
            "SELECT id FROM resources WHERE groupid=?";

    public static final String SQL_SELECT_ALL_VMS_IN_ENSEMBLE =
            "SELECT id FROM resources WHERE ensembleid=?";

    public static final String SQL_SELECT_ALL_VMS_BY_OWNER =
            "SELECT id FROM resources WHERE creator_dn=?";
    
    public static final String SQL_SELECT_AVAILABLE_ENTRIES =
        "SELECT * FROM resourcepool_entries WHERE active = 1 AND " +
                "available_memory >= ? " +
                "ORDER BY (available_memory/maximum_memory) ASC";
    
    public static final String SQL_INSERT_SPOT_PRICE =
            "INSERT INTO spot_prices VALUES(?,?)";    
    
    public static final String SQL_SELECT_LAST_SPOT_PRICE =
        "SELECT price FROM spot_prices WHERE tstamp=(select max(tstamp) from spot_prices)";
    
    public static final String SQL_SELECT_SPOT_PRICE =
            "SELECT * FROM spot_prices";
    
    public static final String[] PREPARED_STATEMENTS = {
                                    SQL_SELECT_RESOURCES,
                                    SQL_SELECT_ALL_ASSOCIATIONS,
                                    SQL_SET_STATE,
                                    SQL_SET_OPS_ENABLED,
                                    SQL_SET_NETWORKING,
                                    SQL_SET_VMM_ACCESS_OK,
                                    SQL_SET_HOSTNAME,
                                    SQL_SET_ROOT_UNPROP_TARGET,
                                    SQL_UNSET_ROOT_UNPROP_TARGET,
                                    SQL_SET_VM_CUSTOMIZATION_SENT,
                                    SQL_SET_STARTTIME,
                                    SQL_SET_TERMTIME,
                                    SQL_DELETE_RESOURCE,
                                    SQL_DELETE_GROUP_RESOURCE,
                                    SQL_DELETE_VM,
                                    SQL_DELETE_VM_PARTITIONS,
                                    SQL_DELETE_VM_DEPLOYMENT,
                                    SQL_DELETE_VM_CUSTOMIZATION,
                                    SQL_INSERT_RESOURCE,
                                    SQL_INSERT_VM,
                                    SQL_INSERT_VM_PARTITION,
                                    SQL_INSERT_VM_DEPLOYMENT,
                                    SQL_INSERT_VM_CUSTOMIZATION,
                                    SQL_LOAD_RESOURCE,
                                    SQL_LOAD_RESOURCE_NAME,
                                    SQL_LOAD_GROUP_RESOURCE,
                                    SQL_LOAD_VM,
                                    SQL_LOAD_VM_PARTITIONS,
                                    SQL_LOAD_VM_DEPLOYMENT,
                                    SQL_LOAD_VM_CUSTOMIZATION,
                                    SQL_UPDATE_ASSOCIATION_ENTRY,
                                    SQL_DELETE_ALL_ASSOCIATIONS,
                                    SQL_DELETE_ALL_ASSOCIATION_ENTRIES,
                                    SQL_SELECT_ASSOCIATION,
                                    SQL_SELECT_ALL_RESOURCE_POOL_ENTRIES,
                                    SQL_SELECT_RESOURCE_POOL_ENTRY,
                                    SQL_INSERT_RESOURCE_POOL_ENTRY,
                                    SQL_UPDATE_RESOURCE_POOL_ENTRY_MEMORY,
                                    SQL_DELETE_RESOURCE_POOL_ENTRY,
                                    SQL_SELECT_RESOURCE_POOL,
                                    SQL_JOIN_SELECT_RESOURCE_POOL_MEMORY,
                                    SQL_SELECT_ALL_VMS_IN_GROUP,
                                    SQL_SELECT_ALL_VMS_IN_ENSEMBLE,
                                    SQL_SELECT_ALL_VMS_BY_OWNER,
                                    SQL_SELECT_AVAILABLE_ENTRIES,
                                    SQL_SELECT_MULTIPLE_OF_AVAILABLE_MEMORY,
                                    SQL_SELECT_TOTAL_AVAILABLE_MEMORY,
                                    SQL_SELECT_TOTAL_MAX_MEMORY,
                                    SQL_SELECT_TOTAL_PREEMPTABLE_MEMORY,
                                    SQL_SELECT_USED_NON_PREEMPTABLE_MEMORY,
                                    SQL_INSERT_SPOT_PRICE,
                                    SQL_SELECT_LAST_SPOT_PRICE};
}
