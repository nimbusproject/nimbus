/*
 * Copyright 1999-2010 University of Chicago
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FailAuthenticationEntryPoint
        implements AuthenticationEntryPoint, InitializingBean {

    private static final Log logger = LogFactory.getLog(FailAuthenticationEntryPoint.class);

    private ResponseUtil responseUtil;

    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {


        ErrorMessage error = new ErrorMessage("Unauthorized: "+ authException.getMessage());
        if (logger.isDebugEnabled()) {
            logger.debug("Sending authentication failure response: " + error.toString(), authException);
        }

        this.responseUtil.sendServletError(response, error, HttpServletResponse.SC_UNAUTHORIZED);
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(responseUtil);
    }

    public void setResponseUtil(ResponseUtil responseUtil) {
        this.responseUtil = responseUtil;
    }


}
