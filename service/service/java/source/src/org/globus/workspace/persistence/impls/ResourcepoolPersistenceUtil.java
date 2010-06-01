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

import org.globus.workspace.scheduler.defaults.Resourcepool;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;
import org.globus.workspace.persistence.PersistenceAdapterConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

public class ResourcepoolPersistenceUtil
                                implements PersistenceAdapterConstants {

    // Prepared statement not necessary, this only happens at startup
    public static String[] insertAllResourcepoolsSQL(Hashtable resourcepools) {

        final ArrayList inserts = new ArrayList();
        Iterator iter = resourcepools.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            Resourcepool resourcepool = (Resourcepool)resourcepools.get(name);
            StringBuffer buf = new StringBuffer();
            buf.append("INSERT INTO resourcepools VALUES ('").
                append(name).
                append("'");

            buf.append(", ").
                append(resourcepool.getFileTime()).
                append(")");

            inserts.add(buf.toString());

            if (resourcepool.getEntries() == null) {
                continue;
            }

            Iterator innerIter = resourcepool.getEntries().values().iterator();
            while (innerIter.hasNext()) {
                buf = new StringBuffer();
                ResourcepoolEntry entry = (ResourcepoolEntry)innerIter.next();
                buf.append("INSERT INTO resourcepool_entries VALUES (");

                buf.append("'").
                    append(name).
                    append("'");

                buf.append(", '").
                    append(entry.getHostname()).
                    append("'");

                buf.append(", '").
                    append(entry.getSupportedAssociations()).
                    append("'");

                buf.append(", ").
                    append(entry.getMemMax());

                buf.append(", ").
                    append(entry.getMemCurrent());

                buf.append(")");
                inserts.add(buf.toString());
            }
        }

        return (String[])inserts.toArray(new String[inserts.size()]);
    }

    public static PreparedStatement updateAvailableMemory(
                                                   String name,
                                                   ResourcepoolEntry entry,
                                                   Connection c)
            throws SQLException {

        PreparedStatement pstmt =
                c.prepareStatement(SQL_UPDATE_RESOURCE_POOL_ENTRY);

        pstmt.setInt(1, entry.getMemCurrent());
        pstmt.setInt(2, entry.getMemPreemptable());
        pstmt.setString(3, name);
        pstmt.setString(4, entry.getHostname());

        return pstmt;
    }
}
