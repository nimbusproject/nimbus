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

package org.nimbustools.messaging.gt4_0.factory;

import org.nimbustools.api.repr.CannotTranslateException;
import org.nimbustools.api._repr._CreateRequest;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api._repr.vm._ResourceAllocation;
import org.nimbustools.api._repr.vm._Schedule;
import org.nimbustools.api._repr.vm._VMFile;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;
import org.nimbustools.messaging.gt4_0.common.InvalidDurationException;
import org.nimbustools.messaging.gt4_0.generated.negotiable.InitialState_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ResourceAllocation_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.ShutdownMechanism_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.WorkspaceDeployment_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.Storage_Type;
import org.nimbustools.messaging.gt4_0.generated.negotiable.Entry;
import org.ggf.jsdl.RangeValue_Type;
import org.ggf.jsdl.Exact_Type;

import java.util.HashMap;
import java.util.Set;

public class TranslateRAImpl implements TranslateRA {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final HashMap shutdownMap = new HashMap(4);
    static {
        shutdownMap.put(ShutdownMechanism_Type.Serialize,
                    CreateRequest.SHUTDOWN_TYPE_SERIALIZE);
        shutdownMap.put(ShutdownMechanism_Type.Trash,
                    CreateRequest.SHUTDOWN_TYPE_TRASH);
    }

    protected static final HashMap initialStateMap = new HashMap(4);
    static {
        initialStateMap.put(InitialState_Type.Propagated,
                            CreateRequest.INITIAL_STATE_PROPAGATED);
        initialStateMap.put(InitialState_Type.Unpropagated,
                            CreateRequest.INITIAL_STATE_UNPROPAGATED);
        initialStateMap.put(InitialState_Type.Running,
                            CreateRequest.INITIAL_STATE_RUNNING);
        initialStateMap.put(InitialState_Type.Paused,
                            CreateRequest.INITIAL_STATE_PAUSED);
    }

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final ReprFactory repr;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public TranslateRAImpl(ReprFactory reprFactory) {
        if (reprFactory == null) {
            throw new IllegalArgumentException("reprFactory may not be null");
        }
        this.repr = reprFactory;
    }


    // -------------------------------------------------------------------------
    // TRANSLATE
    // -------------------------------------------------------------------------

    public void translateResourceRelated(_CreateRequest req,
                                         WorkspaceDeployment_Type dep)
                throws CannotTranslateException {

        if (req == null) {
            throw new IllegalArgumentException("req may not be null");
        }
        if (dep == null) {
            throw new IllegalArgumentException("dep may not be null");
        }

        if (!(req.getRequestedRA() instanceof _ResourceAllocation)) {
            throw new CannotTranslateException(
                    "expecting writable ResourceAllocation");
        }

        _ResourceAllocation ra = (_ResourceAllocation) req.getRequestedRA();
        if (ra == null) {
            ra = this.repr._newResourceAllocation();
            req.setRequestedRA(ra);
        }

        final _Schedule sched = this.repr._newSchedule();
        if (dep.getDeploymentTime() != null) {
            final int requestedSecs;
            try {
                requestedSecs = CommonUtil.durationToSeconds(
                            dep.getDeploymentTime().getMinDuration());
            } catch (InvalidDurationException e) {
                throw new CannotTranslateException(e.getMessage(), e);
            }
            sched.setDurationSeconds(requestedSecs);
        }
        req.setRequestedSchedule(sched);

        ra.setNodeNumber(dep.getNodeNumber());
        
        // handled elsewhere: dep.getPostShutdown();

        final ShutdownMechanism_Type shutdownMech = dep.getShutdownMechanism();
        if (shutdownMech == null) {
            req.setShutdownType(CreateRequest.SHUTDOWN_TYPE_NORMAL);
        } else {
            final String type = (String) shutdownMap.get(shutdownMech);
            if (type == null) {
                throw new CannotTranslateException("unknown shutdown " +
                        "mechanism '" + shutdownMech.toString() + "'");
            } else {
                req.setShutdownType(type);
            }
        }

        final InitialState_Type initialState = dep.getInitialState();
        if (initialState == null) {
            req.setInitialStateRequest(CreateRequest.INITIAL_STATE_RUNNING);
        } else {
            final String state = (String) initialStateMap.get(initialState);
            if (state == null) {
                throw new CannotTranslateException("unknown initial " +
                        "state '" + initialState.toString() + "'");
            } else {
                req.setInitialStateRequest(state);
            }
        }

        final ResourceAllocation_Type wsra = dep.getResourceAllocation();
        if (wsra == null) {
            throw new CannotTranslateException(
                    "ResourceAllocation_Type may not be missing");
        }

        ra.setMemory(this.getMemoryMB(wsra));
        ra.setIndCpuCount(this.getCPUCount(wsra));

        boolean noFiles = false;
        final VMFile[] files = req.getVMFiles();
        if (files == null || files.length == 0) {
            noFiles = true;
        }

        final Storage_Type storage = wsra.getStorage();
        if (storage != null) {

            final Entry[] entries = storage.getEntry();
            if (entries != null && entries.length > 0) {

                if (noFiles) {
                    throw new CannotTranslateException("Cannot examine " +
                        "storage requests without previous file consumption");
                }

                this.consumeEntries(entries, files);
            }
        }

        //ignored: bandwidth, cpu/%
    }

    protected int getMemoryMB(ResourceAllocation_Type wsra)
            throws CannotTranslateException {

        if (wsra == null) {
            throw new IllegalArgumentException("wsra may not be null");
        }

        final RangeValue_Type memory =  wsra.getIndividualPhysicalMemory();
        if (memory == null) {
            throw new CannotTranslateException("no memory request");
        }

        //TODO: support ranges and Exact_Type[]
        final Exact_Type exact_mem = memory.getExact(0);
        if (exact_mem == null) {
            throw new CannotTranslateException("no exact memory request");
        }

        // casting double
        return (int) exact_mem.get_value();
    }

    protected int getCPUCount(ResourceAllocation_Type wsra)
            throws CannotTranslateException {

        if (wsra == null) {
            //throw new IllegalArgumentException("wsra may not be null");
            return -1;
        }

        final RangeValue_Type cores =  wsra.getIndividualCPUCount();
        if (cores == null) {
            //throw new CannotTranslateException("no multi core CPU request");
            return -1;
        }

        //TODO: support ranges and Exact_Type[]
        final Exact_Type exact_cores = cores.getExact(0);
        if (exact_cores == null) {
            throw new CannotTranslateException("no exact multi core CPU request");
        }

        // casting double
        return (int) exact_cores.get_value();
    }

    protected void consumeEntries(Entry[] entries, VMFile[] files)
            throws CannotTranslateException {

        if (entries == null) {
            throw new IllegalArgumentException("entries may not be null");
        }
        if (files == null) {
            throw new IllegalArgumentException("files may not be null");
        }

        final HashMap blankStorageRequests = new HashMap(8);

        for (int i = 0; i < entries.length; i++) {

            final Entry entry = entries[i];

            //TODO: support ranges and Exact_Type[]
            final Exact_Type exact_blank =
                    entry.getIndividualDiskSpace().getExact(0);
            if (exact_blank == null) {
                throw new CannotTranslateException(
                        "missing blankspace space specification");
            }

            // casting double
            final int space = (int) exact_blank.get_value();

            final String blankPartitionName = entry.getPartitionName();
            if (blankPartitionName == null) {
                throw new CannotTranslateException(
                        "missing blankspace name");
            }

            final Set keys = blankStorageRequests.keySet();
            if (keys.contains(blankPartitionName)) {
                throw new CannotTranslateException(
                        "blank space partition name '" + blankPartitionName +
                                "' is present more than once");
            }

            blankStorageRequests.put(blankPartitionName, new Integer(space));
        }

        for (int i = 0; i < files.length; i++) {

            if (!(files[i] instanceof _VMFile)) {
                throw new CannotTranslateException("expecting writable VMFile");
            }

            final _VMFile file =
                    (_VMFile)files[i];
            if (file == null) {
                continue;
            }

            final String name = file.getBlankSpaceName();
            if (name == null) {
                continue;
            }

            final Integer size = (Integer) blankStorageRequests.get(name);
            if (size == null) {
                throw new CannotTranslateException("blank space listed in" +
                        "definition but no matching blank storage request " +
                        "is in deployment request");
            }

            final int space = size.intValue();

            if (space < 1) {
                throw new CannotTranslateException(
                        "blank space request is for under 1 MB");
            }

            file.setBlankSpaceSize(space);

            // remove to mark 'used' so we can check at the end for extras
            blankStorageRequests.remove(name);
        }

        // check for any unused
        if (!blankStorageRequests.isEmpty()) {
            throw new CannotTranslateException("there are blank space " +
                    "requests in request that do not have a partition " +
                    "name match in the workspace definition");
        }
    }

}
