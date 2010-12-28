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
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.InitializingBean;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

@Path("/")
public class ElasticQuery implements InitializingBean {

    /**
     * API version header field injected into request to allow knowledge of version later
     * on. Specifically in MessageBodyWriter. It is not enough to use UriInfo because that
     * does not include form-encoded POST parameters.
     */
    public static final String API_VERSION_HEADER = "nimbus-elastic-api-version";

    private static final Log logger =
            LogFactory.getLog(ElasticQuery.class.getName());

    Map<String, ElasticVersion> versions;
    ElasticVersion fallbackVersion;

    @Path("/")
    @Produces("text/xml")
    public ElasticVersion handle(@FormParam("Action") String action,
                       @FormParam("Version") String version,
                       @Context MessageContext ctx) {

        logger.info("Got "+action+ " request for version "+ version+". Agent: "+
                ctx.getHttpHeaders().getRequestHeader("User-Agent"));


        this.validateVersion(version);

        // this is fairly ugly. We need to store some context information
        // about this request for later on: the API version (used for "lying"
        // about schema URL in the message body writer). The only way I could
        // find for doing this that works is adding to HTTP headers.
        final Map map = (Map) ctx.get(Message.PROTOCOL_HEADERS);
        map.put(API_VERSION_HEADER, Collections.singleton(version));


        // get appropriate action
        final ElasticVersion theVersion = versions.get(version);

        if (theVersion == null) {

            // to start off, we only support a single API version.
            // However, there is a great deal of compatibility with
            // recent versions aschanges are mostly additive. So we
            // log the mismatch and attempt to process request
            // anyways.

            if (fallbackVersion != null) {

                logger.warn("Version "+version+" is not supported. " +
                        "Attempting to process request anyways.");

                return fallbackVersion;
            }

            throw new QueryException(QueryError.NoSuchVersion,
                    "The requested API version ("+version+
                            ") is not yet supported by this service.");
        }

        return theVersion;
    }

    private Pattern versionPattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private void validateVersion(String version) {
        if (version == null) {
            throw new QueryException(QueryError.NoSuchVersion, "The specified version is invalid");
        }
        if (!versionPattern.matcher(version).matches()) {
            throw new QueryException(QueryError.NoSuchVersion, "The specified version \""+
                    version +"\" is invalid");
        }

    }

    public void afterPropertiesSet() throws Exception {
        if ((this.versions == null || this.versions.isEmpty()) &&
                fallbackVersion == null) {
            throw new Exception("versions must contain at least one entry OR "+
                    "you must specify a fallbackVersion");
        }
    }

    public Map<String, ElasticVersion> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, ElasticVersion> versions) {
        this.versions = versions;
    }

    public ElasticVersion getFallbackVersion() {
        return fallbackVersion;
    }

    public void setFallbackVersion(ElasticVersion fallbackVersion) {
        this.fallbackVersion = fallbackVersion;
    }
}
