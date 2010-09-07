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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.rm.defaults;

import java.io.Serializable;

public class IDMap implements Serializable {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    public static final int INST = 1;
    public static final int GRP = 2;
    public static final int COSCH = 4;


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    public final String elastic;
    public final String manager;
    public final int type;
    public final String elasticReservationID; // backmap instances to resid
    public final String sshkeyUsed; // name of key, could be null

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public IDMap(String elastic, String manager, int type) {
        this(elastic, manager, type, null, null);
    }

    public IDMap(String elasticReservationID,
                 String elasticInstanceID,
                 String managerInstanceID,
                 String sshkeyUsed) {
        this(elasticInstanceID, managerInstanceID, INST,
             elasticReservationID, sshkeyUsed);
        if (elasticReservationID == null) {
            throw new IllegalArgumentException("elasticReservationID may not be null");
        }
    }

    private IDMap(String elastic, String manager, int type,
                  String elasticReservationID, String sshkeyUsed) {

        if (elastic == null) {
            throw new IllegalArgumentException("elastic may not be null");
        }
        if (manager == null) {
            throw new IllegalArgumentException("manager may not be null");
        }

        switch (type) {
            case INST: break;
            case GRP: break;
            case COSCH: break;
            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }

        this.elastic = elastic;
        this.manager = manager;
        this.type = type;
        this.elasticReservationID = elasticReservationID;
        this.sshkeyUsed = sshkeyUsed;
    }
}
