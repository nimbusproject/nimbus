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

package org.globus.workspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to add more logging levels to the current logging
 * implementation.  In the future, if a better logging implementation
 * is used, a regex find/replace should be able to make most of the move.
 *
 * Code all 'laging' statements with the logger.trace() method.
 */
public class Lager {


    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(Lager.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    public boolean eventLog;
    public boolean perfLog;
    public boolean traceLog;
    public boolean dbLog;
    public boolean schedLog;
    public boolean pollLog;
    public boolean stateLog;
    public boolean accounting;
    public boolean ctxLog;
    public boolean ctxLogAll;


    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    public void setEvents(String s) {
        this.eventLog = s.trim().equalsIgnoreCase("on");
    }

    public void setAccounting(String s) {
        this.accounting = s.trim().equalsIgnoreCase("on");
    }

    /*
     * The following levels need the main logger's DEBUG implementation
     * to be enabled.  If it is not, it would cause wasteful processing
     * to leave these enabled internally.
     */

    public void setTrace(String s) {
        if (logger.isDebugEnabled()) {
            this.traceLog = s.trim().equalsIgnoreCase("on");
        }
    }

    public void setDB(String s) {
        if (logger.isDebugEnabled()) {
            this.dbLog = s.trim().equalsIgnoreCase("on");
        }
    }

    public void setPerf(String s) {
        if (logger.isDebugEnabled()) {
            this.perfLog = s.trim().equalsIgnoreCase("on");
        }
    }

    public void setScheduler(String s) {
        if (logger.isDebugEnabled()) {
            this.schedLog = s.trim().equalsIgnoreCase("on");
        }
    }

    public void setPoll(String s) {
        if (logger.isDebugEnabled()) {
            this.pollLog = s.trim().equalsIgnoreCase("on");
        }
    }

    public void setState(String s) {
        if (logger.isDebugEnabled()) {
            this.stateLog = s.trim().equalsIgnoreCase("on");
        }
    }

    public void setContextualization(String s) {
        if (logger.isDebugEnabled()) {
            this.ctxLog = s.trim().equalsIgnoreCase("on");
        }
    }

    public void setContextualizationLogAll(String s) {
        if (logger.isDebugEnabled()) {
            this.ctxLogAll = s.trim().equalsIgnoreCase("on");
        }
    }


    // -------------------------------------------------------------------------
    // STATIC UTILITIES
    // -------------------------------------------------------------------------
    
    // trace id# consistently and in grep-able way
    public static String id(int id) {
        return "[id-" + id + "]";
    }
    public static String id(Integer id) {
        final String str;
        if (id != null) {
            str = id.toString();
        } else {
            str = "null";
        }
        return "[id-" +  str + "]";
    }

    public static String id(String str) {
        return "[id-" +  str + "]";
    }

    public static String groupid(String str) {
        return "[groupid-" +  str + "]";
    }

    public static String ensembleid(String str) {
        return "[ensembleid-" +  str + "]";
    }

    public static String oneormanyev(int[] ids, String str) {
        return oneormanyimpl("[NIMBUS-EVENT]", ids, str);
    }

    public static String oneormanyid(int[] ids, String str) {
        return oneormanyimpl("", ids, str);
    }

    private static String oneormanyimpl(String prefix, int[] ids, String str) {
        if (str != null) {
            return prefix + "[groupid-" +  str + "]";
        } else if (ids == null || ids.length == 0) {
            return prefix + "[id-unknown]";
        } else if (ids.length == 1) {
            return prefix + "[id-" + ids[0] + "]";
        } else {
            final StringBuffer buf = new StringBuffer("[idlist");
            for (int i = 0; i < ids.length; i++) {
                buf.append("-")
                   .append(ids[i]);
            }
            buf.append("]");
            return prefix + buf.toString();
        }
    }

    public static String ev(Integer id) {
        if (id == null) {
            return ev(-1);
        } else {
            return ev(id.intValue());
        }
    }
    public static String ev(int id) {
        if (id < 0) {
            return "[NIMBUS-EVENT]: ";
        } else {
            return "[NIMBUS-EVENT][id-" + id + "]: ";
        }
    }

    public static String groupev(String id) {
        if (id == null) {
            return "[NIMBUS-EVENT]: ";
        } else {
            return "[NIMBUS-EVENT][groupid-" + id + "]: ";
        }
    }

    public static String ensembleev(String id) {
        if (id == null) {
            return "[NIMBUS-EVENT]: ";
        } else {
            return "[NIMBUS-EVENT][ensembleid-" + id + "]: ";
        }
    }
}
