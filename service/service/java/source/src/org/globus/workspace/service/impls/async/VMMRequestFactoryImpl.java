package org.globus.workspace.service.impls.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.workspace.Lager;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

public class VMMRequestFactoryImpl  implements VMMRequestFactory {

    private static final Log logger =
                        LogFactory.getLog(RequestFactoryImpl.class.getName());


    // Current command set options -- the set is just a name attached to
    // a group of control commands.  If you would like to implement a
    // multiplexing version you can (as we did for staging in the past).

    public static final String XEN_LOCAL = "xenlocal";
    public static final String XEN_SSH = "xenssh";

    private static final String sshP =
                                  "org.globus.workspace.xen.xenssh";
    private static final String locP =
                                  "org.globus.workspace.xen.xenlocal";

    private static final int numCommands = 1;

    //for now there is only query command available
    private static final Integer query = new Integer(0);


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final HashMap impls = new HashMap();
    protected final Hashtable cmdStr = new Hashtable();
    protected final Lager lager;

    protected String commandSet;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public VMMRequestFactoryImpl(Lager lagerImpl) {
        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }

    // -------------------------------------------------------------------------
    // CONFIG SET
    // -------------------------------------------------------------------------

    public void setCommandSet(String set) throws Exception {
        if (!this.init(set)) {
            throw new Exception("Unknown/invalid command set '" + set + "'");
        }
    }

    // -------------------------------------------------------------------------
    // SETUP
    // -------------------------------------------------------------------------

    // for logging:
    private void initCmdStr() {
        this.cmdStr.put(query, "query");
    }

    private VMMRequest get(Integer i) {
        try {
            Class clazz = (Class)this.impls.get(i);
            if (clazz == null) {
                return null;
            }
            return (VMMRequest)clazz.newInstance();
        } catch (Exception e) {
            logger.fatal("",e);
            return null;
        }
    }

    public VMMRequest query() {
        return get(query);
    }

    // called during init to make sure configured classes are present
    // and implement VMMRequest interface
    private boolean test() {

        String maxStr = null;
        if (lager.traceLog) {
            int max = 0;
            Enumeration en = cmdStr.elements();
            while (en.hasMoreElements()) {
                int len = ((String)en.nextElement()).length();
                if (len > max) {
                    max = len;
                }
            }

            StringBuffer maxbuf = new StringBuffer();
            for (int i = 0; i < max; i++) {
                maxbuf.append(" ");
            }
            maxStr = maxbuf.toString();
        }

        boolean valid = true;
        StringBuffer buf =
                new StringBuffer("\n\nCommand implementations:\n\n");

        for (int i = 0; i < numCommands; i++) {
            Integer key = new Integer(i);
            String keyname = (String) cmdStr.get(key);
            String keynameJus = null;
            if (maxStr != null) {
                keynameJus = justify(keyname, maxStr);
            }
            if (!impls.containsKey(key)) {

                logger.fatal("command set does not include '" +
                                keyname + "', set explicit null");
                valid = false;
                if (lager.traceLog) {
                    buf.append(keynameJus).
                        append(": no class configuration, ").
                        append("set with explicit null instead").
                        append("\n");
                }

            } else {
                try {
                    Class clazz = (Class)impls.get(key);
                    if (clazz != null) {

                        VMMRequest req =
                                (VMMRequest)clazz.newInstance();

                        if (lager.traceLog) {
                            buf.append(keynameJus).
                                append(": ").
                                append(req.getClass().getName()).
                                append("\n");
                        }

                    } else {
                        // no implementation is OK at this stage

                        if (lager.traceLog) {
                            buf.append(keynameJus).
                                append(": not implemented").
                                append("\n");
                        }
                    }
                } catch (Throwable e) {
                    logger.fatal("Problem with " + keyname +
                                        "() implementation class",e);
                    if (lager.traceLog) {
                        buf.append(keynameJus).
                            append(": exception: ").
                            append(e.getMessage()).
                            append("\n");
                    }
                    valid = false;
                }
            }
        }

        if (lager.traceLog) {
            logger.trace(buf.toString());
        }

        return valid;
    }

    private static String justify(String str, String max) {

        if (str == null || max == null) {
            return max;
        }
        final int len = str.length();
        if (len < max.length()) {
            return max.substring(len) + str + "() ";
        } else {
            return str + "() ";
        }
    }

    // -------------------------------------------------------------------------
    // INIT
    // -------------------------------------------------------------------------

    protected boolean init(String keyword) {
        if (!this.impls.isEmpty()) {
            logger.fatal("already initialized");
            return false;
        }

        this.initCmdStr();

        boolean result = false;
        if (keyword.trim().equalsIgnoreCase(XEN_SSH)) {
            result = this.loadSSH();
            this.commandSet = XEN_SSH;
        } else if (keyword.trim().equalsIgnoreCase(XEN_LOCAL)) {
            result = loadLocal();
            this.commandSet = XEN_LOCAL;
        }

        return result && test();
    }

    private boolean loadSSH() {
        try {
            loadXenCommon(sshP);
            return true;
        } catch (Throwable e) {
            logger.fatal("",e);
            return false;
        }
    }

    private boolean loadLocal() {
        try {
            loadXenCommon(locP);
            return true;
        } catch (Throwable e) {
            logger.fatal("",e);
            return false;
        }
    }

    private void loadXenCommon(String pre) throws Throwable {

        this.impls.put(query, Class.forName(pre + ".Query"));

    }


}
