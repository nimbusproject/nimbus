/*
 * Copyright 1999-2011 University of Chicago
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
package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.general;

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.security.SSHKey;

import java.util.List;

public interface ElasticPersistence {
    String INSERT_INSTANCE = "insert into instances " +
            "(elastic_id, manager_id, reservation_id, sshkey) values(?,?,?,?)";
    String GET_MANAGER_FROM_ELASTIC_INSTANCE =
            "select manager_id from instances where elastic_id = ?";
    String GET_ELASTIC_FROM_MANAGER_INSTANCE =
            "select elastic_id from instances where manager_id = ?";
    String INSERT_RESERVATION = "insert into reservations " +
            "(reservation_id, group_id, cosched_id) values(?,?,?)";
    String GET_SSHKEY_FROM_ELASTIC_INSTANCE =
            "select sshkey from instances where elastic_id = ?";
    String GET_GROUP_FROM_ELASTIC_RESERVATION =
            "select group_id from reservations where reservation_id = ?";
    String GET_COSCHED_FROM_ELASTIC_RESERVATION =
            "select cosched_id from reservations where reservation_id = ?";
    String GET_RESERVATION_FROM_MANAGER_INSTANCE =
            "select reservation_id from instances where manager_id = ?";
    String GET_RESERVATION_FROM_GROUP =
            "select reservation_id from reservations where group_id = ?";
    String GET_RESERVATION_FROM_COSCHED =
            "select reservation_id from reservations where cosched_id = ?";
    // a little silly
    String GET_RESERVATION =
            "select reservation_id from reservations where reservation_id = ?";
    String GET_SSH_KEY = "select owner, keyname, pubkey, fingerprint " +
            "from ssh_keypairs where owner = ? and keyname = ?";
    String GET_SSH_KEYS_BY_OWNER = "select owner, keyname, pubkey, " +
            "fingerprint from ssh_keypairs where owner = ?";
    String INSERT_SSH_KEY = "insert into ssh_keypairs " +
            "(owner, keyname, pubkey, fingerprint) values(?,?,?,?)";
    String UPDATE_SSH_KEY = "updatessh_keypairs " +
            "set pubkey = ?, fingerprint = ? where owner = ? and keyname = ?";
    String DELETE_SSH_KEY = "delete from ssh_keypairs " +
            "where owner = ? and keyname = ?";

    void insertInstance(String elasticInstanceId,
                        String managerInstanceId,
                        String elasticReservationId,
                        String sshKeyUsed) throws Exception;

    void insertReservation(String elasticReservationId,
                           String groupId, String coschedId)
                                       throws Exception;

    String selectIdFromId(String query, String id);

    List<SSHKey> getSSHKeys(String owner) throws Exception;

    SSHKey getSSHKey(String owner, String keyname) throws Exception;

    void putSSHKey(SSHKey key) throws Exception;

    boolean updateSSHKey(SSHKey key) throws Exception;

    boolean deleteSSHKey(String owner, String keyname) throws Exception;
}
