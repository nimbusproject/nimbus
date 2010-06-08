/*
 * Copyright 1999-2010 University of Chicago
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

package org.globus.workspace.scheduler.defaults;

public interface DefaultSchedulerConstants {

    /* Prepared Statements using TYPE_SCROLL_INSENSITIVE + CONCUR_UPDATABLE */

    public static final String SQL_SELECT_DEFAULT_SCHED_REQ_ID =
            "SELECT id FROM default_scheduler_workspid";

    public static final String[] INSENSITIVE_PREPARED_STATEMENTS =
                                        {SQL_SELECT_DEFAULT_SCHED_REQ_ID};


    /* Prepared Statements with dynamic markers */

    public static final String SQL_INSERT_TASK =
            "INSERT INTO default_scheduler_current_tasks VALUES(?,?,?)";

    public static final String SQL_DELETE_TASKS =
            "DELETE FROM default_scheduler_current_tasks WHERE id=?";

    public static final String SQL_INSERT_DEFAULT_SCHED_REQ_ID =
            "INSERT INTO default_scheduler_workspid VALUES(?)";

    public static final String SQL_UPDATE_DEFAULT_SCHED_REQ_ID =
            "UPDATE default_scheduler_workspid SET id=?";

    public static final String SQL_SELECT_SHUTDOWN =
            "SELECT id FROM default_scheduler_current_tasks WHERE " +
            "default_scheduler_current_tasks.shutdown_time < ? " +
            "AND shutdown=0";

    public static final String SQL_SELECT_ANY_LEFT =
            "SELECT COUNT(id) FROM default_scheduler_current_tasks " +
            "WHERE shutdown=0";

    public static final String SQL_UPDATE_SHUTDOWN =
            "UPDATE default_scheduler_current_tasks SET shutdown=1 WHERE id=?";

    public static final String SQL_INSERT_NODE_REQUEST =
            "INSERT into default_scheduler_pending_ensemb VALUES(?,?,?,?,?,?)";

    public static final String SQL_DELETE_NODE_REQUESTS =
            "DELETE FROM default_scheduler_pending_ensemb WHERE coschedid=?";

    public static final String SQL_DELETE_ONE_NODE_REQUEST =
            "DELETE FROM default_scheduler_pending_ensemb WHERE id=?";

    public static final String SQL_SELECT_LOAD_NODE_REQUESTS =
            "SELECT groupid,id,min_duration,ind_physmem,assocs_needed FROM " +
            "default_scheduler_pending_ensemb WHERE coschedid=?";

    public static final String SQL_INSERT_NODE_REQUESTS_SENT =
            "INSERT into default_scheduler_done_ensemb VALUES(?)";

    public static final String SQL_SELECT_CHECK_NODEREQS_SENT=
            "SELECT COUNT(coschedid) FROM default_scheduler_done_ensemb " +
            "WHERE coschedid=?";
    
    public static final String[] PREPARED_STATEMENTS =
                                           {SQL_INSERT_TASK,
                                            SQL_DELETE_TASKS,
                                            SQL_INSERT_DEFAULT_SCHED_REQ_ID,
                                            SQL_UPDATE_DEFAULT_SCHED_REQ_ID,
                                            SQL_SELECT_SHUTDOWN,
                                            SQL_SELECT_ANY_LEFT,
                                            SQL_UPDATE_SHUTDOWN,
                                            SQL_INSERT_NODE_REQUEST,
                                            SQL_DELETE_NODE_REQUESTS,
                                            SQL_DELETE_ONE_NODE_REQUEST,
                                            SQL_SELECT_LOAD_NODE_REQUESTS,
                                            SQL_INSERT_NODE_REQUESTS_SENT,
                                            SQL_SELECT_CHECK_NODEREQS_SENT};

}
