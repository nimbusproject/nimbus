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

package org.nimbustools.metadataserver.defaults;

import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.HttpConnection;
import org.nimbustools.api.services.metadata.MetadataServer;
import org.nimbustools.api.services.metadata.MetadataServerException;
import org.nimbustools.api.services.metadata.MetadataServerUnauthorizedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

public class MetadataRequestHandler extends AbstractHandler {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final MetadataServer metadataServer;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public MetadataRequestHandler(MetadataServer server) {
        if (server == null) {
            throw new IllegalArgumentException("metadata server may not be null");
        }
        this.metadataServer = server;
    }


    // -------------------------------------------------------------------------
    // implements MetadataRequestHandler
    // -------------------------------------------------------------------------

    public void handle(String target,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       int dispatch) throws IOException,
                                            ServletException {

        if (target == null) {
            throw new IllegalArgumentException("target may not be null");
        }

        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null) {
            throw new IllegalArgumentException("remoteAddress may not be null");
        }

        final Request base_request = request instanceof Request?
                (Request)request :
                           HttpConnection.getCurrentConnection().getRequest();
        
        base_request.setHandled(true);

        final String responseString;
        try {
            responseString =
                    this.metadataServer.getResponse(target, remoteAddress);
        } catch (MetadataServerException e) {
            notok(response, e.getClientVisibleMessage());
            return;
        } catch (MetadataServerUnauthorizedException e) {
            noauthz(response);
            return;
        }

        ok(response, responseString);
    }


    // -------------------------------------------------------------------------
    // PRIVATE
    // -------------------------------------------------------------------------

    private static void ok(HttpServletResponse response,
                           String msg)
            throws IOException {

        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
        if (msg != null) {
            response.getWriter().println(msg);
        } else {
            response.getWriter().println();
        }
    }

    private static void notok(HttpServletResponse response,
                              String msg)
            throws IOException {

        if (msg != null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                               msg);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                               "Unknown error.");
        }
    }

    private static void noauthz(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
