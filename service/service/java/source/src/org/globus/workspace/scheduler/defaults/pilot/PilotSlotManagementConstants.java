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

package org.globus.workspace.scheduler.defaults.pilot;

public interface PilotSlotManagementConstants {
    
    /* Prepared Statements using TYPE_SCROLL_INSENSITIVE + CONCUR_UPDATABLE */

    // public static final String[] INSENSITIVE_PREPARED_STATEMENTS = {};

    /* Prepared Statements */

    public static final String SQL_DELETE_CLEAR_SLOTS =
            "DELETE FROM pilot_slots WHERE 1=1";

    public static final String SQL_SELECT_NUM_SLOTS =
            "SELECT COUNT(id) FROM pilot_slots";

    /* Prepared Statements with dynamic markers */

    public static final String SQL_INSERT_SLOT =
            "INSERT INTO pilot_slots VALUES(?,?,1,0,?,?,0,0,?)";

    public static final String SQL_INSERT_GROUP_SLOT =
            "INSERT INTO pilot_slots VALUES(?,?,1,0,?,?,1,0,?)";

    public static final String SQL_DELETE_SLOT =
            "DELETE FROM pilot_slots WHERE id=?";

    public static final String SQL_LOAD_SLOT =
            "SELECT vmid, pending, terminal, lrmhandle, " +
                    "duration, partofgroup, pendingremove " +
                    "FROM pilot_slots WHERE id=? AND nodename=?";
    
    public static final String SQL_LOAD_SLOTS_NOHOSTNAME =
            "SELECT vmid, terminal, lrmhandle, duration, " +
                    "partofgroup, pendingremove " +
                    "FROM pilot_slots WHERE id=? AND nodename IS NULL";

    public static final String SQL_LOAD_SLOT_BY_VM =
            "SELECT id, pending, terminal, lrmhandle, duration, " +
                    "partofgroup, pendingremove, nodename " +
                    "FROM pilot_slots WHERE vmid=?";

    // 1 is true, 0 is false
    
    public static final String SQL_UPDATE_SLOT_TERMINAL_SET_TRUE =
            "UPDATE pilot_slots SET terminal=1 WHERE id=? AND nodename=?";

    public static final String SQL_UPDATE_SLOT_PENDING_REMOVE_SET_TRUE =
            "UPDATE pilot_slots SET pendingremove=1 WHERE id=? AND nodename=?";

    // this is always accompanied by setting pending to false
    public static final String SQL_UPDATE_SLOT_NODENAME =
            "UPDATE pilot_slots SET nodename=?,pending=0 " +
            "WHERE id=? AND vmid=?";

    // group actions

    public static final String SQL_INSERT_GROUP_MEMBER =
            "INSERT INTO pilot_groups VALUES(?,?)";

    public static final String SQL_SELECT_ALL_VMS_IN_GROUP =
            "SELECT vmid FROM pilot_groups WHERE groupid=?";

    // for notification

    public static final String SQL_UPDATE_CURSOR_POSITION =
            "UPDATE pilot_notification_position SET position=? WHERE 1=1";

    public static final String SQL_INSERT_CURSOR_POSITION =
            "INSERT INTO pilot_notification_position VALUES(0)";

    public static final String SQL_SELECT_CURSOR_POSITION =
            "SELECT position FROM pilot_notification_position";

    public static final String[] PREPARED_STATEMENTS =
                                   {SQL_DELETE_CLEAR_SLOTS,
                                    SQL_INSERT_SLOT,
                                    SQL_INSERT_GROUP_SLOT,
                                    SQL_DELETE_SLOT,
                                    SQL_LOAD_SLOT,
                                    SQL_LOAD_SLOTS_NOHOSTNAME,
                                    SQL_LOAD_SLOT_BY_VM,
                                    SQL_UPDATE_SLOT_TERMINAL_SET_TRUE,
                                    SQL_UPDATE_SLOT_PENDING_REMOVE_SET_TRUE,
                                    SQL_UPDATE_SLOT_NODENAME,
                                    SQL_INSERT_GROUP_MEMBER,
                                    SQL_SELECT_ALL_VMS_IN_GROUP,
                                    SQL_UPDATE_CURSOR_POSITION,
                                    SQL_INSERT_CURSOR_POSITION,
                                    SQL_SELECT_CURSOR_POSITION};
}
