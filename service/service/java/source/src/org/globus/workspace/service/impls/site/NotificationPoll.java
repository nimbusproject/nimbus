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

import commonj.timers.Timer;
import commonj.timers.TimerListener;
import commonj.timers.TimerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * Polls for asynchronous notifications coming from outside of the service
 * via the filesystem.
 */
public abstract class NotificationPoll implements TimerListener {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    protected static final Log logger =
        LogFactory.getLog(NotificationPoll.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final Lager lager;
    
    private final TimerManager timerManager;
    private final long delay;
    private final String path;
    private final NotificationPollCallback call;
    private final String polledObjectNoun;

    private Timer timer;
    private RandomAccessFile raf = null;
    private long filepos;    

    public NotificationPoll(TimerManager timerManager,
                            Lager lagerImpl,
                            long delay,
                            String verifiedPath,
                            long filepos,
                            NotificationPollCallback call,
                            String polledObjectNoun) throws Exception {

        if (delay < 0) {
            throw new Exception ("delay is less than zero");
        }

        if (timerManager == null) {
            throw new Exception ("timerManager is null");
        }

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;

        if (verifiedPath == null) {
            throw new Exception ("verifiedPath is null");
        }

        if (filepos < 0) {
            throw new Exception("filepos is less than zero?");
        }

        if (call == null) {
            throw new Exception ("callback is null");
        }

        if (polledObjectNoun == null) {
            throw new Exception ("polledObjectNoun is null");
        }

        this.timerManager = timerManager;
        this.delay = delay;
        this.path = verifiedPath;
        this.call = call;
        this.polledObjectNoun = polledObjectNoun;
        this.filepos = filepos;

        this.timer = null;
    }

    /**
     * @param name name
     * @param state state
     * @param code code
     * @param message message
     * @return true if this concerns the implementation and can decrease num
     */
    protected abstract boolean oneNotification(String name,
                                               String state,
                                               int code,
                                               String message);

    
    // TimerListener interface
    public void timerExpired(Timer timer) {
        
        if (lager.pollLog) {
            logger.trace("timerExpired()");
        }

        int notDoneCount = 0;
        try {
            notDoneCount = call.numPendingNotifications();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error("problem retrieving pending notification: " +
                             e.getMessage(), e);
            } else {
                logger.error("problem retrieving pending notification: " +
                             e.getMessage());
            }
            // leave notDoneCount at zero and stop poll
        }

        if (lager.pollLog) {
            if (notDoneCount > 1) {
                logger.trace("there are " + notDoneCount + " " +
                       this.polledObjectNoun + "s with pending notifications");
            } else if (notDoneCount == 1) {
                logger.trace("there is one " + this.polledObjectNoun +
                        " with pending notifications");
            } else {
                logger.trace("there are no " + this.polledObjectNoun +
                        "s with pending notifications");
            }
        }

        if (notDoneCount > 0) {
            notificationWatch();
        }

        resetNotificationWatcher();

        if (notDoneCount > 0) {
            scheduleNotificationWatcher();
        } else if (lager.pollLog) {
            logger.trace("no " + this.polledObjectNoun + "s are awaiting" +
                    " notifications: Not re-scheduling NotificationPoll");
        }
    }

    public void scheduleNotificationWatcher() {
        if (lager.pollLog) {
            logger.trace("scheduleNotificationWatcher()");
        }

        if (this.timer == null) {
            this.timer = this.timerManager.schedule(this, this.delay);
            if (lager.pollLog) {
                logger.trace("scheduled notificationWatcher");
            }
        }
    }

    private synchronized void resetNotificationWatcher() {
        if (lager.pollLog) {
            logger.trace("resetNotificationWatcher()");
        }

        this.timer = null;
    }

    private void notificationWatch() {
        if (lager.pollLog) {
            logger.trace("notificationWatch()");
        }

        final String[] notifications = this.readNotifications();
        if (notifications == null) {
            return;
        }

        // minimum 3 tokens required

        // NAME::state::code[::message][::message word #2][etc]]

        int decrease = 0;
        for (int i = 0; i < notifications.length; i++) {
            try {
                if (processOneNotification(notifications[i])) {
                    decrease += 1;
                }
            } catch (Exception e) {
                String msg =
                        "Exception in notificationWatch: " + e.getMessage();
                if (logger.isDebugEnabled()) {
                    logger.error(msg, e);
                } else {
                    logger.error(msg);
                }
            }
        }

        // some small time above where container crash will cause
        // inconsistency if numPendingNotifications implementation relies
        // on decreaseNumPending
        if (decrease > 0) {
            try {
                call.decreaseNumPending(decrease);
            } catch (Exception e) {
                logger.error("decreaseNumPending threw Exception: " +
                                 e.getMessage(), e);
            }
        }
    }

    private String[] readNotifications() {

        try {
            try {
                if (this.raf == null) {
                    this.raf = new RandomAccessFile(this.path, "r");
                }
            } catch (FileNotFoundException e) {
                if (lager.pollLog) {
                    // this could be the case if no notifications exist yet and
                    // that is OK
                    logger.trace(e.getMessage());
                }
                return null;
            }
            return readNotificationsImpl();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            this.raf = null;
            // this is as far as the problem goes:
            return null;
        }
    }

    private String[] readNotificationsImpl()
            throws Exception {

        if (this.raf == null) {
            throw new Exception("raf should be opened before calling");
        }
        
        if (this.filepos > this.raf.length()) {
            logger.warn("Notification cursor reset, file is new/truncated?");
            this.filepos = 0;
        }

        this.raf.seek(this.filepos);

        final ArrayList notifs = new ArrayList(64);
        while (true) {
            String line = this.raf.readLine();

            if (line == null) {
                break;
            }
            
            if (!line.endsWith("<eon>")) {
                logger.debug("race with a notification writer (unless " +
                        "there is a severe, odd issue, line='" + line + "'");

                // next readNotificationsImpl invocation should start this
                // line over, so filepos is not updated
                break;
            }

            this.filepos = this.raf.getFilePointer();
            // chop off <eon>
            notifs.add(line.substring(0, line.length() - 5));
        }

        if (notifs.isEmpty()) {
            return null;
        }

        this.call.cursorPosition(this.filepos);
        return (String[]) notifs.toArray(new String[notifs.size()]);
    }


    // true if a message is sent to impl and the impl cares about it,
    // see this.oneNotification()
    private boolean processOneNotification(String line) {
        final String[] notification = line.trim().split("::");

        if (notification.length < 3) {
            if (notification.length != 0) {
                logger.error("invalid notification line, less than " +
                        "three tokens: " + line);
            }
            return false;
        }

        // notification encoder will not send blankspace, trimming
        // as precaution
        String name = notification[0].trim();
        String state = notification[1].trim().toLowerCase();
        String code = notification[2].trim();
        int codeInt;
        try {
            codeInt = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            logger.error("invalid notification line, code is " +
                    "not an integer: '" + code + "', line = " + line);
            return false;
        }

        String message = null;
        if (notification.length > 3) {
            StringBuffer buf = new StringBuffer();
            for (int j = 3; j < notification.length; j++) {
                // notification encoder will not send blankspace,
                // trimming as precaution
                String token = notification[j].trim();
                if (token.equalsIgnoreCase("]eol[")) {
                    buf.append("\n");
                } else {
                    buf.append(token);
                    if (j != notification.length -1) {
                        buf.append(" ");
                    }
                }
            }
            message = buf.toString();
        }

        return oneNotification(name, state, codeInt, message);
    }
}
