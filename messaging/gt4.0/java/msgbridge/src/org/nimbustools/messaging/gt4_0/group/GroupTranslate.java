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

package org.nimbustools.messaging.gt4_0.group;

import org.nimbustools.api._repr._ShutdownTasks;
import org.nimbustools.api.repr.ReprFactory;
import org.nimbustools.messaging.gt4_0.generated.negotiable.PostShutdown_Type;
import org.nimbustools.messaging.gt4_0.BaseTranslate;

import java.net.URISyntaxException;
import java.net.URI;

public class GroupTranslate extends BaseTranslate {

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public GroupTranslate(ReprFactory reprFactory) {
        super(reprFactory);
    }

    
    // -------------------------------------------------------------------------
    // TRANSLATE TO: ShutdownTasks
    // -------------------------------------------------------------------------

    public _ShutdownTasks getShutdownTasks(PostShutdown_Type post,
                                          boolean appendID)
            throws URISyntaxException {

        if (post == null) {
            return null;
        }
        final _ShutdownTasks tasks = this.repr._newShutdownTasks();
        
        final URI target =
                this.convertURI(post.getRootPartitionUnpropagationTarget());
        tasks.setBaseFileUnpropagationTarget(target);

        tasks.setAppendID(appendID);

        return tasks;
    }

    // sigh
    public URI convertURI(org.apache.axis.types.URI axisURI)
            throws URISyntaxException {
        return axisURI == null ? null : new URI(axisURI.toString());
    }
}
