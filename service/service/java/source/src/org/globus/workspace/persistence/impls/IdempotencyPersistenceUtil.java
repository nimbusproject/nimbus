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
package org.globus.workspace.persistence.impls;

import org.globus.workspace.creation.IdempotentInstance;
import org.globus.workspace.creation.IdempotentReservation;
import org.globus.workspace.persistence.PersistenceAdapterConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class IdempotencyPersistenceUtil implements PersistenceAdapterConstants {

    public static PreparedStatement[] getInsertReservation(IdempotentReservation reservation, Connection conn)
            throws SQLException {

        if (reservation == null) {
            throw new IllegalArgumentException("reservation may not be null");
        }

        final String creatorId = reservation.getCreatorId();
        if (creatorId == null) {
            throw new IllegalArgumentException("reservation creatorId may not be null");
        }
        final String clientToken = reservation.getClientToken();
        if (clientToken == null) {
            throw new IllegalArgumentException("reservation clientToken may not be null");
        }

        final String groupId = reservation.getGroupId();

        final List<IdempotentInstance> instances = reservation.getInstances();
        if (instances == null || instances.isEmpty()) {
            throw new IllegalArgumentException("instances may not be null or empty");
        }

        PreparedStatement[] statements = new PreparedStatement[instances.size()];
        for (int i = 0; i < statements.length; i++) {

            final IdempotentInstance instance = instances.get(i);
            if (instance == null) {
                throw new IllegalArgumentException("reservation has null instance");
            }

            final PreparedStatement statement =
                    conn.prepareStatement(SQL_INSERT_IDEMPOTENT_CREATION);

            statement.setString(1, creatorId);
            statement.setString(2, clientToken);
            statement.setInt(3, instance.getID());
            statement.setString(4, groupId);
            statement.setString(5, instance.getName());
            statement.setInt(6, instance.getLaunchIndex());

            statements[i] = statement;
        }

        return statements;
    }
}
