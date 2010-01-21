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

import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.messaging.rest.repr.ErrorMessage;
import org.springframework.beans.factory.InitializingBean;


public class OutFaultInterceptor
        extends AbstractPhaseInterceptor<Message>
        implements InitializingBean {

    private static final String ERROR_TEXT = "An internal server error occurred. Please " +
            "contact the administer and include the request ID.";

    private static final Log logger =
            LogFactory.getLog(OutFaultInterceptor.class.getName());

    private boolean handleMessageCalled;
    private ResponseUtil responseUtil;

    public OutFaultInterceptor() {
        this(Phase.PRE_STREAM);
    }

    public OutFaultInterceptor(String s) {
        super(Phase.MARSHAL);

    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public void handleMessage(Message message) throws Fault {
        handleMessageCalled = true;
        Exception ex = message.getContent(Exception.class);
        if (ex == null) {
            throw new RuntimeException("Exception is expected");
        }
        Fault fault = (Fault)ex;

        final Throwable t = fault.getCause();

        ErrorMessage error;
        int status;

        // note that these exceptions are probably handled by NimbusWebExceptionMapper.
        // leaving this logic in case that is disabled..
        if (t instanceof NimbusWebException) {
            NimbusWebException nwe = (NimbusWebException) t;

            String errorText = nwe.getMessage();
            if (errorText == null) {
                errorText = ERROR_TEXT;
            }

            String requestId = nwe.getRequestId();
            if (requestId == null) {
                requestId = UUID.randomUUID().toString();
            }

            error = new ErrorMessage(errorText, requestId);

            status = nwe.getStatus();
            if (status < 400 || status >= 600) {
                // client expects a 4xx response for request error
                // or 5xx for server error. otherwise success..
                status = 500;
            }

            logger.debug("Returning friendly error response for exception. " +
                    "RequestID: "+requestId, nwe);

        } else {

            // otherwise it is some unhandled error condition;
            // likely a bug somewhere.

            String requestId = UUID.randomUUID().toString();

            error = new ErrorMessage(ERROR_TEXT, requestId);
            status = 500;

            logger.error("Caught unhandled exception! This may be a bug. " +
                    "RequestID: "+requestId, t);
        }

        // deal with the actual exception : fault.getCause()
        HttpServletResponse response = (HttpServletResponse)message.getExchange()
            .getInMessage().get(AbstractHTTPDestination.HTTP_RESPONSE);
        response.setStatus(status);
        response.setContentType(ResponseUtil.JSON_CONTENT_TYPE);
        try {

            final String s = responseUtil.createJsonString(error);


            response.getOutputStream().write(s.getBytes());
            response.getOutputStream().flush();
            message.getInterceptorChain().abort();
        } catch (IOException ioex) {
            throw new RuntimeException("Error writing the response");
        }
    }

    protected boolean handleMessageCalled() {
        return handleMessageCalled;
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

