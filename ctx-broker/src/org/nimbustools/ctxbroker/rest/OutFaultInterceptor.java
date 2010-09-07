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
package org.nimbustools.ctxbroker.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;


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

        final int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        String requestId = UUID.randomUUID().toString();
        final ErrorMessage error = new ErrorMessage(ERROR_TEXT, requestId);

        logger.error("Caught unhandled exception! This may be a bug. " +
                "RequestID: "+requestId, t);

        HttpServletResponse response = (HttpServletResponse)message.getExchange()
            .getInMessage().get(AbstractHTTPDestination.HTTP_RESPONSE);

        this.responseUtil.sendServletError(response, error, status);

        message.getInterceptorChain().abort();
    }

    protected boolean handleMessageCalled() {
        return handleMessageCalled;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(responseUtil);
    }

    public void setResponseUtil(org.nimbustools.ctxbroker.rest.ResponseUtil responseUtil) {
        this.responseUtil = responseUtil;
    }
}

