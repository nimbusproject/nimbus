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

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class ResponseUtil {

    private static final Log logger = LogFactory.getLog(ResponseUtil.class);

    final private Gson gson;

    public ResponseUtil() {
        this.gson = new Gson();
    }

    public void sendServletError(HttpServletResponse response, ErrorMessage error, int status) {

        response.setStatus(status);
        response.setContentType("application/json");
        final String s = this.gson.toJson(error);
        try {
            response.getOutputStream().write(s.getBytes());
            response.getOutputStream().flush();
        } catch (IOException e) {
            logger.error("Failed to write error response. Trying to write '" +
                    s + "'", e);

            //can't rethrow because we'd likely just end up right back here
        }
    }
}
