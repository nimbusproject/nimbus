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

package org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.service;

import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceRM;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceGeneral;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceSecurity;
import org.nimbustools.messaging.gt4_0_elastic.v2008_05_05.ServiceImage;
import org.nimbustools.messaging.gt4_0_elastic.generated.v2009_08_15.*;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.safehaus.uuid.UUIDGenerator;

import java.rmi.RemoteException;

public abstract class DelegatingService extends UnimplementedOperations {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DelegatingService.class.getName());

    public static final String UNCAUGHT_ERRORS = "Unexpected internal " +
            "service problem.  If this is an issue for you, please send " +
            "the administrator this key along with support request: ";
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected ServiceRM rm = null;
    protected ServiceGeneral general = null;
    protected ServiceSecurity security = null;
    protected ServiceImage image = null;
    protected final UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DelegatingService() throws Exception {

        try {
            this.findGeneral();
            this.findImage();
            this.findSecurity();
            this.findManager();
        } catch (Throwable t) {

            final String msg = "Problem initializing the service: " +
                        CommonUtil.recurseForRootString(t, true, 1, true);

            throw new Exception(msg);
        }
    }


    // -------------------------------------------------------------------------
    // FIND IMPLS
    // -------------------------------------------------------------------------

    // This class is instantiated with no-arg constructor outside of any IoC
    // system etc., so we hook into one here.

    protected abstract void findManager() throws Exception;

    protected abstract void findGeneral() throws Exception;

    // could leave "this.security" null
    protected abstract void findSecurity() throws Exception;

    protected abstract void findImage() throws Exception;


    // -------------------------------------------------------------------------
    // UNKNOWN ERRORS
    // -------------------------------------------------------------------------

    protected String unknown(Throwable t, String opName) {
        final String uuid = this.uuidGen.generateRandomBasedUUID().toString();
        final String err =
                "UNKNOWN-ERROR '" + uuid + "' from OP:" + opName + " -- ";
        if (t != null) {
            logger.fatal(err + t.getMessage(), t);
        } else {
            logger.fatal(err + "[[NULL THROWABLE]]");
        }
        return UNCAUGHT_ERRORS + uuid;
    }


    // -------------------------------------------------------------------------
    // RM RELATED
    // -------------------------------------------------------------------------

    public RunInstancesResponseType runInstances(
                        RunInstancesType runInstancesRequestMsg)
            throws RemoteException {

        try {
            if (this.rm == null) { this.findManager(); }
            return this.rm.runInstances(runInstancesRequestMsg);
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(this.unknown(t, "runInstances"));
        }
    }

    public TerminateInstancesResponseType terminateInstances(
                        TerminateInstancesType terminateInstancesRequestMsg)
            throws RemoteException {

        try {
            if (this.rm == null) { this.findManager(); }
            return this.rm.terminateInstances(terminateInstancesRequestMsg);
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(this.unknown(t, "terminateInstances"));
        }
    }

    public RebootInstancesResponseType rebootInstances(
                        RebootInstancesType rebootInstancesRequestMsg)
            throws RemoteException {

        try {
            if (this.rm == null) { this.findManager(); }
            return this.rm.rebootInstances(rebootInstancesRequestMsg);
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(this.unknown(t, "rebootInstances"));
        }
    }

    public DescribeInstancesResponseType describeInstances(
                        DescribeInstancesType describeInstancesRequestMsg)
            throws RemoteException {
        
        try {
            if (this.rm == null) { this.findManager(); }
            return this.rm.describeInstances(describeInstancesRequestMsg);
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(this.unknown(t, "describeInstances"));
        }
    }


    // -------------------------------------------------------------------------
    // GENERAL QUERIES
    // -------------------------------------------------------------------------
    
    public DescribeAvailabilityZonesResponseType describeAvailabilityZones(
            DescribeAvailabilityZonesType describeAvailabilityZonesRequestMsg)
            throws RemoteException {
        try {
            if (this.general == null) { this.findGeneral(); }
            return this.general.describeAvailabilityZones(describeAvailabilityZonesRequestMsg);
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(
                    this.unknown(t, "describeAvailabilityZones"));
        }
    }


    // -------------------------------------------------------------------------
    // SECURITY RELATED
    // -------------------------------------------------------------------------

    public CreateKeyPairResponseType createKeyPair(
                        CreateKeyPairType createKeyPairRequestMsg)
            throws RemoteException {

        try {
            this.findSecurity();
            if (this.security != null) {
                return this.security.createKeyPair(createKeyPairRequestMsg);
            } else {
                return super.createKeyPair(createKeyPairRequestMsg);
            }
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(this.unknown(t, "createKeyPair"));
        }
    }

    public DescribeKeyPairsResponseType describeKeyPairs(
                        DescribeKeyPairsType describeKeyPairsRequestMsg)
            throws RemoteException {

        try {
            this.findSecurity();
            if (this.security != null) {
                return this.security.describeKeyPairs(describeKeyPairsRequestMsg);
            } else {
                return super.describeKeyPairs(describeKeyPairsRequestMsg);
            }
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(this.unknown(t, "describeKeyPairs"));
        }
    }

    public DeleteKeyPairResponseType deleteKeyPair(
                        DeleteKeyPairType deleteKeyPairRequestMsg)
            throws RemoteException {

        try {
            this.findSecurity();
            if (this.security != null) {
                return this.security.deleteKeyPair(deleteKeyPairRequestMsg);
            } else {
                return super.deleteKeyPair(deleteKeyPairRequestMsg);
            }
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(this.unknown(t, "deleteKeyPair"));
        }
    }


    // -------------------------------------------------------------------------
    // IMAGE RELATED
    // -------------------------------------------------------------------------

    public DescribeImagesResponseType describeImages(DescribeImagesType req)
            throws RemoteException {

        try {
            if (this.image == null) { this.findImage(); }
            return this.image.describeImages(req);
        } catch (RemoteException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            throw e;
        } catch (Throwable t) {
            throw new RemoteException(this.unknown(t, "describeImages"));
        }
    }
}
