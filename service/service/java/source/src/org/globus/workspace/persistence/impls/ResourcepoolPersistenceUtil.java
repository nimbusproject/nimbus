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

package org.globus.workspace.persistence.impls;

import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;
import org.globus.workspace.persistence.PersistenceAdapterConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ResourcepoolPersistenceUtil
                                implements PersistenceAdapterConstants {

    public static PreparedStatement updateAvailableMemory(
                                                   String name,
                                                   ResourcepoolEntry entry,
                                                   Connection c)
            throws SQLException {

        PreparedStatement pstmt =
                c.prepareStatement(SQL_UPDATE_RESOURCE_POOL_ENTRY_MEMORY);

        pstmt.setInt(1, entry.getMemCurrent());
        pstmt.setString(2, name);
        pstmt.setString(3, entry.getHostname());

        return pstmt;
    }
}
