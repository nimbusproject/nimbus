/*
 * Copyright 1999-2009 University of Chicago
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
package org.nimbustools.messaging.query;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.util.Map;

@Path("/")
public class ElasticQuery {

    private static final Log logger =
            LogFactory.getLog(ElasticQuery.class.getName());

    Map<String, ElasticVersion> versions;

    public Map<String, ElasticVersion> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, ElasticVersion> versions) {
        this.versions = versions;
    }

    @Path("/")
    @Produces("text/xml")
    public ElasticVersion handle(@FormParam("Action") String action,
                       @FormParam("Version") String version,
                       @Context MessageContext ctx) {

        logger.info("Got "+action+ " request for version "+ version+". Agent: "+
                ctx.getHttpHeaders().getRequestHeader("User-Agent"));


        // get appropriate action
        final ElasticVersion theVersion = versions.get(version);

        if (theVersion == null) {
            throw new QueryException(QueryError.NoSuchVersion);
        }

        return theVersion;
    }
}
