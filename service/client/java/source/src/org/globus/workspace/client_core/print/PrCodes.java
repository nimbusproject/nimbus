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

package org.globus.workspace.client_core.print;

import org.globus.workspace.common.print.PrintOpts;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * The core API should be strongly typed enough to use directly without needing
 * any of these codes.  But there are situations where the base client's modes
 * and some other printing facilities are easily re-used, but perhaps with
 * different output requirements.  Instead of "enable" vs. "disable", one
 * can finely tune.
 *
 * Only applies to info and err.  The only way to know what controls what
 * is to get the source and an IDE and have at it (usage search etc.).  This
 * is here so that developers can finely control the underlying print system
 * without needing to support their own virtually identical version of the
 * package(s).  If you need some message to be more fine grained, want a new
 * one, etc., makes your requests via the workspace developer's list (that
 * goes for pretty anything else also).
 * 
 * ASSUME NOTHING about the number values, always references via member name,
 * including NOT_SET.  Always clean compile with new versions of the API...
 * (final primitives and Strings go directly into .class files).
 */
public class PrCodes {

    public static int[] getAllCodes() {
        final Class clazz = PrCodes.class;
        final Field[] fields = clazz.getDeclaredFields();
        final int[] all = new int[fields.length];
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().getName().equals("int")) {
                try {
                    all[i] = fields[i].getInt(fields[i]);
                } catch (IllegalAccessException e) {
                    // will not happen because of int check
                    all[i] = PrintOpts.PRCODE_NOT_SET;
                }
            } else {
                all[i] = PrintOpts.PRCODE_NOT_SET;
            }
        }
        return all;
    }

    public static int[] getAllCodesExcept(int[] excepting) {
        final int[] all = getAllCodes();

        if (excepting == null || excepting.length == 0) {
            return all;
        }

        for (int i = 0; i < excepting.length; i++) {
            for (int j = 0; j < all.length; j++) {
                if (all[j] == excepting[i]) {
                    all[j] = PrintOpts.PRCODE_NOT_SET;
                }
            }
        }

        // not after any speed records
        final ArrayList intList = new ArrayList(all.length);
        for (int i = 0; i < all.length; i++) {
            if (all[i] != PrintOpts.PRCODE_NOT_SET) {
                intList.add(new Integer(all[i]));
            }
        }

        final int[] subset = new int[intList.size()];

        for (int i = 0; i < subset.length; i++) {
            subset[i] = ((Number)intList.get(i)).intValue();
        }

        return subset;
    }

    public static void main(String[] args) {
        final int[] testing = getAllCodes();
        System.out.println("testing length " + testing.length);

        final int[] allout = getAllCodesExcept(testing);
        System.out.println("allout length " + allout.length);

        final int[] someouts = {CREATE__DRYRUN, DESTROY__DRYRUN, PAUSE__DRYRUN};
        final int[] someout = getAllCodesExcept(someouts);
        System.out.println("someout length " + someout.length);
    }

    public static final int ANY_ERROR_CATCH_ALL = 1;
    
    public static final int CREATE__DRYRUN = 21;
    public static final int DESTROY__DRYRUN = 22;
    public static final int FACTORYRPQUERY__DRYRUN = 23;
    public static final int INSTANCERPQUERY__DRYRUN = 24;
    public static final int PAUSE__DRYRUN = 25;
    public static final int REBOOT__DRYRUN = 26;
    public static final int SHUTDOWN__DRYRUN = 27;
    public static final int SHUTDOWNSAVE__DRYRUN = 28;
    public static final int START__DRYRUN = 29;
    public static final int ENSEMBLEDONE__DRYRUN = 30;
    public static final int ENSEMBLEREPORT__DRYRUN = 31;
    public static final int CTXLOCK__DRYRUN = 32;
    public static final int CTXDATA__DRYRUN = 33;
    public static final int CTXMONITOR__DRYRUN = 34;
    public static final int CTXPRINTSTATUS__DRYRUN = 35;

    public static final int CREATE__EXTRALINES = 100;

    public static final int CREATE__FACTORY_ENDPOINT = 101;
    public static final int CREATE__EPRFILE_WRITES = 102;
    public static final int CREATE__INSTANCE_EPRFILE_WRITES_OVERFLOW = 103;

    public static final int CREATE__INSTANCE_ID_PRINT = 104;
    public static final int CREATE__GROUP_ID_PRINT = 105;
    public static final int CREATE__ENSEMBLE_ID_PRINT = 106;
    public static final int CREATE__CONTEXT_ID_PRINT = 111;
    
    public static final int CREATE__CTXBROKER_ENDPOINT = 113;
    public static final int CREATE__CTXBROKER_CONTACTINF = 114;

    public static final int CREATE__INSTANCE_CREATING_PRINT_WAITING_DOTS = 107;
    public static final int CREATE__GROUP_CREATING_PRINT_WAITING_DOTS = 108;
    public static final int CREATE__ENSEMBLE_CREATING_PRINT_WAITING_DOTS = 109;
    public static final int CREATE__CONTEXT_CREATING_PRINT_WAITING_DOTS = 110;

    public static final int CREATE__SUBSCRIPTION_CREATING_PRINT_WAITING = 120;
    public static final int CREATE__SUBSCRIPTION_CREATING_PRINT_WAITING_DOTS = 121;

    public static final int CREATE__INSTANCE_CREATING_ALL_INIITIAL_SCHEDULE_STRINGS = 131;
    public static final int CREATE__INSTANCE_CREATING_INIITIAL_START_TIME = 132;
    public static final int CREATE__INSTANCE_CREATING_INIITIAL_DURATION = 133;
    public static final int CREATE__INSTANCE_CREATING_INIITIAL_SHUTDOWN_TIME = 134;
    public static final int CREATE__INSTANCE_CREATING_INIITIAL_TERMINATION_TIME = 135;

    public static final int CREATE__INSTANCE_CREATING_NET_NONE = 200;
    public static final int CREATE__INSTANCE_CREATING_NET_NAME = 201;
    public static final int CREATE__INSTANCE_CREATING_NET_MAC = 202;
    public static final int CREATE__INSTANCE_CREATING_NET_ASSOCIATION = 203;
    public static final int CREATE__INSTANCE_CREATING_NET_IP = 204;
    public static final int CREATE__INSTANCE_CREATING_NET_HOST = 205;
    public static final int CREATE__INSTANCE_CREATING_NET_GATEWAY = 206;
    public static final int CREATE__INSTANCE_CREATING_NET_MASK = 207;
    public static final int CREATE__INSTANCE_CREATING_NET_BROADCAST = 208;
    public static final int CREATE__INSTANCE_CREATING_NET_NETWORK = 209;

    public static final int CREATE__GROUP_CREATING_NET_ONELINE = 501;

    public static final int DELEGATE__ALLMESSAGES = 1000;

    public static final int STATE__STAGING = 2001;
    public static final int STATE__PROPAGATION = 2002;
    public static final int STATE__CREATION = 2003;
    public static final int STATE__DESERIALIZATION = 2004;

    public static final int METADATA__FILE_READ = 3001;
    
    public static final int DEPREQ__FILE_READ = 3002;
    public static final int DEPREQ__FILE_OVERRIDE = 3003;
    public static final int DEPREQ__USING_ARGS = 3004;

    public static final int OPTIONALPARAM__FILE_READ = 3005;
    public static final int OPTIONALPARAM__FILE_OVERRIDE = 3006;
    public static final int OPTIONALPARAM__CUSTOMIZATION_OVERRIDE = 3007;
    public static final int OPTIONALPARAM__THEREST = 3008;

    public static final int SSH__FILE_READ = 3100;
    public static final int MD_USERDATA__FILE_READ = 3102;
    public static final int MD_SSH__FILE_READ = 3103;
    public static final int CTX_DOC__FILE_READ = 3104;

    public static final int LISTENER_TERMINATION__INSTANCE_ID_PRINT = 5000;
    public static final int LISTENER_TERMINATION__ERRORS = 5001;

    public static final int LISTENER_STATECHANGE__INSTANCE_STATE_CHANGE = 5002;
    public static final int LISTENER_STATECHANGE__INSTANCE_STATE_PROBLEMS = 5003;
    public static final int LISTENER_STATECHANGE__ERRORS = 5004;

    public static final int LISTENER_AUTODESTROY = 5005;
    public static final int LISTENER_AUTODESTROY__ERRORS = 5006;
	public static final int LISTENER_AUTODESTROY_CLOUD_UNPROPAGATE = 5007;

    public static final int LISTENER_LOGISTICSQUERY__ERRORS = 5012;

    public static final int SUBSCRIPTIONS_STATECHANGE__TARGET_STATE_ALL_REACHED = 6000;
    public static final int SUBSCRIPTIONS_STATECHANGE__TARGET_STATE_NOT_ALL_REACHED = 6001;
    public static final int SUBSCRIPTIONS_STATECHANGE__EXTRALINES = 6002;
    public static final int SUBSCRIPTIONS_STATECHANGE__EXTRALINES_ALLENDED = 6003;

    public static final int SUBSCRIPTIONS_TERMINATED__ALL_TERMINATED = 6010;
    public static final int SUBSCRIPTIONS_TERMINATED__NOT_ALL_TERMINATED = 6011;

    public static final int WSLISTEN__INFO = 7000;
    public static final int WSLISTEN__ERRORS = 7001;

    public static final int ALL_SINGLESHOT_MODES__PRINT_WAITING_DOTS = 8000;
    public static final int ALL_SINGLESHOT_MODES__EXTRALINES = 8001;

    public static final int FACTORYRPQUERY__DEFAULT_MINUTES = 9000;
    public static final int FACTORYRPQUERY__MAX_MINUTES = 9001;
    public static final int FACTORYRPQUERY__ASSOCS = 9002;
    public static final int FACTORYRPQUERY__VMM = 9003;
    public static final int FACTORYRPQUERY__VMM_VERSIONS = 9004;
    public static final int FACTORYRPQUERY__CPU_ARCH = 9005;

    public static final int INSTANCERPQUERY__STATE = 10000;
    public static final int INSTANCERPQUERY__STATE_ERROR = 10001;
    public static final int INSTANCERPQUERY__NETWORK_NICNAME = 10002;
    public static final int INSTANCERPQUERY__NETWORK_IP = 10003;
    public static final int INSTANCERPQUERY__NETWORK_HOSTNAME = 10004;
    public static final int INSTANCERPQUERY__NETWORK_GATEWAY = 10005;
    public static final int INSTANCERPQUERY__NETWORK_ASSOCIATION = 10006;
    public static final int INSTANCERPQUERY__SCHEDBANNER = 10007;
    public static final int INSTANCERPQUERY__START_TIME = 10008;
    public static final int INSTANCERPQUERY__DURATION = 10009;
    public static final int INSTANCERPQUERY__SHUTDOWN_TIME = 10010;
    public static final int INSTANCERPQUERY__TERM_TIME = 10011;

    public static final int ENSMONITOR__ALL_RUNNING = 20000;
    public static final int ENSMONITOR__ONE_ERROR = 20002;
    public static final int ENSMONITOR__SINGLE_REPORT_NAMES = 20003;
    public static final int ENSMONITOR__REPORT_DIR = 20004;

    public static final int CTXMONITOR__ALL_OK = 20100;
    public static final int CTXMONITOR__ONE_ERROR = 20102;
    public static final int CTXMONITOR__SINGLE_REPORT_NAMES = 20103;
    public static final int CTXMONITOR__REPORT_DIR = 20104;
    public static final int CTXMONITOR__KNOWNHOSTS_FILE_CREATE = 20105;

    public static final int CTXPRINTSTATUS__ONE_ERROR = 30102;
    public static final int CTXPRINTSTATUS__ONE_IP = 30103;

}
