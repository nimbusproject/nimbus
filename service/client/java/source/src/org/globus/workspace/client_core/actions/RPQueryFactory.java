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
import org.apache.axis.types.Duration;
import org.ggf.jsdl.ProcessorArchitectureEnumeration;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.nimbustools.messaging.gt4_0.common.Constants_GT4_0;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_common.CommonStrings;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client_core.WSAction_Factory;
import org.globus.workspace.client_core.repr.FactoryRPs;
import org.nimbustools.messaging.gt4_0.generated.FactoryRPSet;
import org.nimbustools.messaging.gt4_0.generated.WorkspaceFactoryPortType;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.VMM_Type;
import org.nimbustools.messaging.gt4_0.generated.metadata.definition.VMM_TypeType;
import org.nimbustools.messaging.gt4_0.generated.types.Associations;
import org.globus.wsrf.WSRFConstants;
import org.oasis.wsrf.properties.QueryExpressionType;
import org.oasis.wsrf.properties.QueryResourcePropertiesResponse;
import org.oasis.wsrf.properties.QueryResourceProperties_Element;
import org.oasis.wsrf.faults.BaseFaultType;

import java.io.IOException;

public class RPQueryFactory extends WSAction_Factory {


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------
       
    /**
     * @see WSAction_Factory
     */
    public RPQueryFactory(EndpointReferenceType epr,
                          StubConfigurator stubConf,
                          Print debug) {
        super(epr, stubConf, debug);
    }

    /**
     * @see WSAction_Factory
     */
    public RPQueryFactory(WorkspaceFactoryPortType factoryPortType,
                          Print debug) {
        super(factoryPortType, debug);
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
     * @return FactoryRPs never null
     * @throws ParameterProblem validation problem
     * @throws ExecutionProblem problem running
     */
    public FactoryRPs queryOnce() throws ParameterProblem, ExecutionProblem {

        this.validateAll();

        try {
            return this._queryOnce();
        } catch (BaseFaultType e) {
            final String err = CommonStrings.faultStringOrCommonCause(e);
            throw new ExecutionProblem(err, e);
        } catch (Exception e) {
            final String err =
                    "Problem querying factory resource properties: " +
                            CommonUtil.genericExceptionMessageWrapper(e);
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }

    private FactoryRPs _queryOnce() throws Exception {

        // return the entire RP document
        final String queryStr = "/";

        final QueryResourceProperties_Element query =
                new QueryResourceProperties_Element();

        query.setQueryExpression(
             createQueryExpression(WSRFConstants.XPATH_1_DIALECT,
                   queryStr));

        final QueryResourcePropertiesResponse resp =
                ((WorkspaceFactoryPortType)this.portType).
                                            queryResourceProperties(query);

        final FactoryRPSet rpSet = (FactoryRPSet) resp.get_any()[0].
                        getValueAsType(Constants_GT4_0.FACTORY_RP_SET,
                                       FactoryRPSet.class);

        if (rpSet == null) {
            throw new ExecutionProblem("No factory RP set returned");
        }
        return convert(rpSet);
    }

    /**
     * @param rpSet may not be null
     * @return FactoryRPs representation
     */
    public static FactoryRPs convert(FactoryRPSet rpSet)
            throws InvalidDurationException {

        if (rpSet == null) {
            throw new IllegalArgumentException("rpSet may not be null");
        }

        final FactoryRPs rps = new FactoryRPs();

        final Duration defaultRunningTime = rpSet.getDefaultRunningTime();
        if (defaultRunningTime != null) {
            rps.setDefaultRunningSeconds(
                    CommonUtil.durationToSeconds(defaultRunningTime));
        }

        final Duration maxRunningTime = rpSet.getMaximumRunningTime();
        if (maxRunningTime != null) {
            rps.setMaximumRunningSeconds(
                    CommonUtil.durationToSeconds(maxRunningTime));
        }

        final Associations assocs = rpSet.getAssociations();
        if (assocs != null) {
            rps.setAssociations(assocs.getAssociation());
        } else {
            rps.setAssociations(null);
        }

        final VMM_Type vmm = rpSet.getVMM();
        if (vmm != null) {

            final VMM_TypeType type = vmm.getType();
            if (type != null) {
                rps.setVMM(type.toString());
            }

            rps.setVmmVersions(vmm.getVersion());
        }

        final ProcessorArchitectureEnumeration cpuArch =
                                    rpSet.getCPUArchitectureName();
        if (cpuArch != null) {
            rps.setCpuArchitectureName(cpuArch.toString());
        }

        return rps;
    }

    private static QueryExpressionType createQueryExpression(String dialect,
                                                             String queryString)
            throws IOException {

        final QueryExpressionType query = new QueryExpressionType();
        query.setDialect(dialect);

        if (queryString != null) {
            query.setValue(queryString);
        }

        return query;
    }
}
