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

package org.globus.workspace.service.impls.site;

import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.HttpConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;

public class PilotNotificationHTTPHandler_v01 extends AbstractHandler {

    private static final Log logger =
           LogFactory.getLog(PilotNotificationHTTPHandler_v01.class.getName());

    public final static String urlPath = "/pilot_notification/v01/";

    private final SlotPollCallback slotcall;
    private final ExecutorService executorService;
    private final Lager lager;

    public PilotNotificationHTTPHandler_v01(SlotPollCallback slotcallback,
                                            ExecutorService execService,
                                            Lager lagerImpl) {
        if (slotcallback == null) {
            throw new IllegalArgumentException("slotcallback is null");
        }
        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.slotcall = slotcallback;
        this.executorService = execService;
        this.lager = lagerImpl;
    }

    public void handle(String target,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       int dispatch) throws IOException, ServletException {

        if (this.lager.pollLog) {
            logger.trace("handle invoked, target: '" + target + "', client: " +
                         request.getRemoteAddr());
        }

        if (!target.equalsIgnoreCase(urlPath)) {
            return;
        }

        final Request base_request = request instanceof Request?
                (Request)request :
                           HttpConnection.getCurrentConnection().getRequest();
        base_request.setHandled(true);

        final String[] data = getData(request);

        response.setContentType("text/html");

        if (data == null || data[0] == null) {
            logger.error("POST with no data (?)");
            notok(response, "POST with no data (?)");
            return;
        }

        try {
            consumeNotification(data);
        } catch (Exception e) {
            final String msg = "Problem consuming notification: ";
            if (logger.isDebugEnabled()) {
                logger.error(msg + e.getMessage(), e);
            } else {
                logger.error(msg + e.getMessage());
            }
            notok(response, e.getMessage());
            return;
        }

        // This will be the place to persist a notification, before an OK
        // is sent.  Because the pilot can fallback to SSH --> file mechanism,
        // the OK response should not be sent unless the container can fail
        // immediately and still everything will be OK at recovery.  Thus,
        // this is where a notification should be persisted.

        ok(response);
    }

    private static void ok(HttpServletResponse response)
            throws IOException {
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("OK");
    }

    private static void notok(HttpServletResponse response,
                              String msg)
            throws IOException {
        
        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        if (msg != null) {
            response.getWriter().println("NOTOK: " + msg);
        } else {
            response.getWriter().println("NOTOK");
        }
    }

    private void consumeNotification(String[] data) throws Exception {

        if (data == null || data.length < 4) {
            throw new Exception("this should be len 4");
        }


        final String name = data[0];
        final String state = data[1];
        final int code = Integer.parseInt(data[2]);
        final String message = data[3];

        String log_msg = "pilot http notification read, name = '" +
                         name + "', state = '" + state + "', " +
                         "code = " + code;

        if (message != null) {
            log_msg += ", message = " + message;
        } else {
            log_msg += ", no message";
        }

        if (this.lager.pollLog) {
            logger.trace(log_msg);
        }

        // true/false return just a signal if impl cared about it or not
        // in this notification consumer, we do not care

        if (this.executorService != null) {
            // get out of http response thread, ensure pilot does not hang on
            // notification processing
            final PilotNotifyTask task =
                    new PilotNotifyTask(name, state, code, message,
                                        log_msg, this.slotcall);
            this.executorService.submit(task);
        } else {
            PilotNotificationUtil.oneNotification(name, state, code, message,
                                                  log_msg, this.slotcall);
        }
    }

    private static String[] getData(HttpServletRequest request)
            throws IOException {

        if (request == null) {
            return null;
        }

        final String[] lines = new String[4];  // lines 1,2,3,4-->end
                                               // NOT null checked yet
        
        final StringBuffer messagebuf = new StringBuffer(1024);

        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            isr = new InputStreamReader(request.getInputStream());
            br = new BufferedReader(isr);
            String line = br.readLine();
            if (line != null) {
                lines[0] = line;
                while (line != null) {
                    line = br.readLine();
                    if (line != null) {
                        if (lines[1] == null) {
                            lines[1] = line;
                        } else if (lines[2] == null) {
                            lines[2] = line;
                        } else {
                            messagebuf.append(line)
                                      .append("\n");
                        }
                    }
                }
            }
        } catch (IOException e) {
            final String msg = "Problem reading (authenticated) POST: ";
            if (logger.isDebugEnabled()) {
                logger.error(msg + e.getMessage(), e);
            } else {
                logger.error(msg + e.getMessage());
            }
            throw e;
        } finally {
            if (br != null) {
                br.close();
            }
            if (isr != null) {
                isr.close();
            }
        }

        if (messagebuf.length() != 0) {
            lines[3] = messagebuf.toString();
        }

        return lines;
    }
}
