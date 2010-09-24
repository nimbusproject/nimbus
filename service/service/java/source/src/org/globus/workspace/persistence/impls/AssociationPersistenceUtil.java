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

import org.globus.workspace.network.Association;
import org.globus.workspace.network.AssociationEntry;
import org.globus.workspace.persistence.PersistenceAdapterConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class AssociationPersistenceUtil
                        implements PersistenceAdapterConstants {

    public static final String EXPLICIT_MAC_PREFIX = "X";

    // not necessary to switch to prep statement, this only happens at startup
    public static String[] insertAllAssociationsSQL(Hashtable associations) {

        final ArrayList inserts = new ArrayList();
        Iterator iter = associations.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            Association assoc = (Association)associations.get(name);
            StringBuffer buf = new StringBuffer();
            buf.append("INSERT INTO associations VALUES ('").
                append(name).
                append("'");

            if (assoc.getDns() != null) {
                buf.append(", '").
                    append(assoc.getDns()).
                    append("'");
            } else {
                buf.append(", NULL");
            }

            buf.append(", ").
                append(assoc.getFileTime()).
                append(")");

            inserts.add(buf.toString());

            if (assoc.getEntries() == null) {
                continue;
            }

            List entries = assoc.getEntries();
            Iterator innerIter = entries.iterator();
            while (innerIter.hasNext()) {
                buf = new StringBuffer();
                AssociationEntry entry = (AssociationEntry)innerIter.next();
                buf.append("INSERT INTO association_entries VALUES (");

                buf.append("'").
                    append(name).
                    append("'");

                // IP address must be present
                buf.append(", '").
                    append(entry.getIpAddress()).
                    append("'");

                if (entry.getMac() != null) {
                    // Prefix explicit MAC addresses so they can be detected on load
                    buf.append(", '").
                        append(entry.isExplicitMac() ? EXPLICIT_MAC_PREFIX : "").
                        append(entry.getMac()).
                        append("'");
                } else {
                    buf.append(", NULL");
                }

                if (entry.getHostname() != null) {
                    buf.append(", '").
                        append(entry.getHostname()).
                        append("'");
                } else {
                    buf.append(", NULL");
                }

                if (entry.getGateway() != null) {
                    buf.append(", '").
                        append(entry.getGateway()).
                        append("'");
                } else {
                    buf.append(", NULL");
                }

                if (entry.getBroadcast() != null) {
                    buf.append(", '").
                        append(entry.getBroadcast()).
                        append("'");
                } else {
                    buf.append(", NULL");
                }

                if (entry.getSubnetMask() != null) {
                    buf.append(", '").
                        append(entry.getSubnetMask()).
                        append("'");
                } else {
                    buf.append(", NULL");
                }

                if (entry.isInUse()) {
                    buf.append(", 1)");
                } else {
                    buf.append(", 0)");
                }

                inserts.add(buf.toString());
            }
        }

        return (String[])inserts.toArray(new String[inserts.size()]);
    }

    public static PreparedStatement updateEntryInUse(String name,
                                                     AssociationEntry entry,
                                                     Connection c)
            throws SQLException {

        PreparedStatement pstmt =
                    c.prepareStatement(SQL_UPDATE_ASSOCIATION_ENTRY);

        if (entry.isInUse()) {
            pstmt.setInt(1, 1);
        } else {
            pstmt.setInt(1, 0);
        }

        pstmt.setString(2, name);
        pstmt.setString(3, entry.getIpAddress());

        return pstmt;
    }
}
