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
package org.nimbustools.messaging.rest;

import org.nimbustools.messaging.rest.repr.ErrorMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.core.Response;

public class NimbusWebExceptionMapper
        implements ExceptionMapper<NimbusWebException>, InitializingBean {

    private static final Log logger =
            LogFactory.getLog(NimbusWebExceptionMapper.class.getName());

    private ResponseUtil responseUtil;

    public Response toResponse(NimbusWebException e) {

        final ErrorMessage err = new ErrorMessage(e.getMessage(), e.getRequestId());

        int statusCode = e.getStatus();
        final Response.Status status;
        if (statusCode >= 400 && statusCode < 600) {
            // client expects 4xx for request error or 5xx for server error.
            // anything else is not an error response
            status = Response.Status.fromStatusCode(statusCode);
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        logger.info("Mapping exception to error response. RequestId="+
                e.getRequestId(), e);

        return responseUtil.createErrorResponse(err, status);
    }

    public ResponseUtil getResponseUtil() {
        return responseUtil;
    }

    public void setResponseUtil(ResponseUtil responseUtil) {
        this.responseUtil = responseUtil;
    }

    public void afterPropertiesSet() throws Exception {
        if (responseUtil == null) {
            throw new IllegalArgumentException("responseUtil may not be null");
        }
    }
}
