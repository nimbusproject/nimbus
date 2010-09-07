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

package org.globus.workspace.client_core.actions;

import org.apache.axis.message.MessageElement;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Instance;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.repr.Schedule;
import org.nimbustools.messaging.gt4_0.generated.WorkspacePortType;
import org.nimbustools.messaging.gt4_0.generated.types.Schedule_Type;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.oasis.wsrf.properties.GetResourcePropertyResponse;

public class RPQuerySchedule extends WSAction_Instance {


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see org.globus.workspace.client_core.WSAction_Instance
     */
    public RPQuerySchedule(EndpointReferenceType epr,
                           StubConfigurator stubConf,
                           Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see org.globus.workspace.client_core.WSAction_Instance
     */
    public RPQuerySchedule(WorkspacePortType instancePortType,
                           Print debug) {
        super(instancePortType, debug);
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    protected Object action() throws Exception {
        return this.queryOnce();
    }

    
    /**
     * query once (the action)
     *
     * @return Schedule never null
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem problem running
     */
    public Schedule queryOnce() throws ParameterProblem,
                                       ExecutionProblem {

        this.validateAll();

        try {
            return this._queryOnce();
        } catch (Exception e) {
            final String err = "Problem querying for schedule: ";
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }

    private Schedule _queryOnce() throws Exception {

        final GetResourcePropertyResponse resp =
            ((WorkspacePortType) this.portType).getResourceProperty(
                                        Constants_GT4_0.RP_SCHEDULE);

        final MessageElement msg = resp.get_any()[0];

        final Schedule_Type schedule = (Schedule_Type)
                ObjectDeserializer.toObject(msg, Schedule_Type.class);

        if (schedule == null) {
            throw new Exception("no schedule");
        }

        return new Schedule(schedule);
    }

}
