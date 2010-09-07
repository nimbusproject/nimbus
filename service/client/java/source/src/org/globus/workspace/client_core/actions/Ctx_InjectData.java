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

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Ctx;
import org.globus.workspace.client_core.utils.RMIUtils;
import org.globus.workspace.common.print.Print;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextBrokerPortType;
import org.nimbustools.ctxbroker.generated.gt4_0.broker.NimbusContextualizationFault;
import org.nimbustools.ctxbroker.generated.gt4_0.types.InjectData_TypeData;
import org.nimbustools.ctxbroker.generated.gt4_0.types.InjectData_Type;

import java.rmi.RemoteException;

public class Ctx_InjectData extends WSAction_Ctx {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected String[] dataNames;
    protected String[] dataValues;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /**
     * @see WSAction_Ctx
     */
    public Ctx_InjectData(EndpointReferenceType epr,
                          StubConfigurator stubConf,
                          Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Ctx
     */
    public Ctx_InjectData(NimbusContextBrokerPortType ctxBrokerPortType,
                          Print debug) {
        super(ctxBrokerPortType, debug);
    }


    // -------------------------------------------------------------------------
    // GET/SET OPTIONS
    // -------------------------------------------------------------------------

    public String[] getDataNames() {
        return this.dataNames;
    }

    public void setDataNames(String[] dataName) {
        this.dataNames = dataName;
    }

    public String[] getDataValues() {
        return this.dataValues;
    }

    public void setDataValues(String[] dataValue) {
        this.dataValues = dataValue;
    }

    // -------------------------------------------------------------------------
    // VALIDATE
    // -------------------------------------------------------------------------

    /**
     * @throws ParameterProblem issue that will stop creation attempt
     */
    public void validateAll() throws ParameterProblem {
        super.validateAll();
        this.validateData();
    }

    public void validateData() throws ParameterProblem {
        if (this.dataNames == null || this.dataNames.length == 0) {
            throw new ParameterProblem("no data names");
        }

        if (this.dataValues == null || this.dataValues.length == 0) {
            throw new ParameterProblem("no data values");
        }

        for (int i = 0; i < dataNames.length; i++) {
            if (this.dataNames[i] == null || this.dataNames[i].length() == 0) {
                throw new ParameterProblem("missing data name @ idx " + i);
            }
            if (this.dataValues[i] == null) {
                this.dataValues[i] = "";
            }
        }
    }


    // -------------------------------------------------------------------------
    // EXECUTE
    // -------------------------------------------------------------------------

    /**
     * Calls inject()
     *
     * @return null
     * @throws Exception see inject()
     * @see #inject()
     */
    protected Object action() throws Exception {
        this.inject();
        return null;
    }

    /**
     * Calls 'inject' on context broker resource.
     *
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem general problem running (connection errors etc)
     * @throws NimbusContextualizationFault broker reports problem
     */
    public void inject() throws ParameterProblem,
                                ExecutionProblem,
                                NimbusContextualizationFault {

        this.validateAll();

        final InjectData_TypeData[] data =
                    new InjectData_TypeData[this.dataNames.length];

        for (int i = 0; i < data.length; i++) {
            final InjectData_TypeData one = new InjectData_TypeData();
            one.setName(this.dataNames[i]);
            one.set_value(this.dataValues[i]);
            data[i] = one;
        }

        final InjectData_Type send = new InjectData_Type(data);
        
        try {
            ((NimbusContextBrokerPortType) this.portType).injectdata(send);
        } catch (NimbusContextualizationFault e) {
            throw e;
        } catch (RemoteException e) {
            throw RMIUtils.generalRemoteException(e);
        }
    }
}
