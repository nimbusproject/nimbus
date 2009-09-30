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

package org.globus.workspace.accounting.impls.dbdefault;

public interface DBAccountingConstants {

    /* Prepared Statements using TYPE_SCROLL_INSENSITIVE + CONCUR_UPDATABLE */

    // public static final String[] INSENSITIVE_PREPARED_STATEMENTS = {};

    /* Prepared Statements */

    public static final String SQL_FORCE_ALL_INACTIVE =
            "UPDATE deployments SET active=0 WHERE active=1";

    public static final String SQL_ALL_CURRENT_RESERVATIONS =
            "SELECT uuid, workspaceid, creator_dn, creation_time, " +
                    "requested_duration FROM deployments WHERE active=1";

    /* Prepared Statements with dynamic markers */

    public static final String SQL_INSERT_DEPLOYMENT =
            "INSERT INTO deployments VALUES(?,?,?,?,?,?,?)";

    public static final String SQL_LOAD_DEPLOYMENT =
            "SELECT uuid, creator_dn, creation_time, requested_duration " +
                    "FROM deployments " +
                    "WHERE workspaceid=? AND active=1";

    public static final String SQL_UPDATE_END_DEPLOYMENT =
            "UPDATE deployments SET elapsed_minutes=?, active=0 " +
            "WHERE uuid=?";

    public static final String SQL_SUM_RESERVED =
            "SELECT SUM(requested_duration) FROM deployments " +
            "WHERE creator_dn=? AND active=1";

    public static final String SQL_SUM_ELAPSED =
            "SELECT SUM(elapsed_minutes) FROM deployments " +
            "WHERE creator_dn=? AND active=0";

    public static final String[] PREPARED_STATEMENTS =
                                           {SQL_FORCE_ALL_INACTIVE,
                                            SQL_ALL_CURRENT_RESERVATIONS,
                                            SQL_INSERT_DEPLOYMENT,
                                            SQL_LOAD_DEPLOYMENT,
                                            SQL_UPDATE_END_DEPLOYMENT,
                                            SQL_SUM_ELAPSED,
                                            SQL_SUM_RESERVED};
}
