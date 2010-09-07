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

package org.globus.workspace.common.print;

import java.io.PrintStream;
import java.io.IOException;

/**
 * <p>Archiver/print filter.</p>
 *
 * <p>There are three modes.  You may disable all messages, have the classes
 * use commons logging, or enable a mode where print streams and possibly
 * behind the scenes files are used to do useful things (including filtering
 * out any particular message).  For explanations of advanced features, see the
 * PrintOpts constructor comments.</p>
 *
 * <p>Below are basic examples, likely all you will ever need to know.</p>
 *
 * <p>Basic use with stdout/stderr:</p>
 *
 * <pre>
 * debugEnabled = [...]
 * PrintOpts opts = new PrintOpts(null);
 * PrintStream info = System.out;
 * PrintStream err = System.err;
 * PrintStream debug = null;
 * if (debugEnabled) {
 *   debug = System.err;
 * }
 * Print pr = new Print(opts, info, err, debug);
 * </pre>
 *
 * <p>Configure API actions to use a particular Print instance via
 * Preferences.  Only actions print anything.  The default is to print/archive
 * NOTHING.</p>
 *
 * <pre>
 * // Print instance "pr" is defined
 *
 * // Action instance "someAction" is defined (all classes in the
 * // "org.globus.workspace.client_api.actions" package inherit from Action).
 *
 * someAction.getPreferences().setPrintImpl(pr);
 * </pre>
 *
 * <p>You could also use the object in your own code in order to leverage
 * advanced trickery/flexibility later...</p>
 * 
 * <pre>
 * pr.infoln("Hello info");
 * pr.errln("Hello err");
 * pr.debugln("Hello debug");
 * </pre>
 *
 * <p>Default Print setup for actions: disable all messages, even to background 
 * archivers.  You don't need to do this in normal circumstances since it is
 * the default:</p>
 *
 * <pre>
 * Print pr = new Print(PrintOpts.DISABLE_ENTIRELY);
 * someAction.getPreferences().setPrintImpl(pr);
 * </pre>
 *
 * <p>Alternative Print setup for actions: use commons logging.  Example:</p>
 *
 * <pre>
 * Print pr = new Print(PrintOpts.USE_COMMONS_LOGGING);
 * someAction.getPreferences().setPrintImpl(pr);
 * </pre>
 *
 * <p>Only three logging levels are used. INFO, ERROR, and DEBUG.  If DEBUG
 * is enabled, ERROR level messages will get exceptions (causing stacktrace
 * logs).  Note that in almost all cases there are no ERROR logs in the
 * core action APIs, just throwing exceptions is preferred there.</p>
 *
 * @see PrintOpts#PrintOpts(int[])
 * @see org.globus.workspace.client_core.Action
 * @see org.globus.workspace.client_core.Settings
 */
public class Print {

    private static final String lineSeparator =
                                    System.getProperty("line.separator");

    private final PrintOpts opts;

    private PrintStream out;
    private PrintStream err;
    private PrintStream debug;

    private ProxyInfoPrintStream proxyInfo;
    private ProxyErrPrintStream proxyErr;
    private ProxyDebugPrintStream proxyDebug;

    /**
     * Convenience constructor for DISABLE entirely mode.
     */
    public Print() {
        this(PrintOpts.DISABLE_ENTIRELY, null, null, null);
    }

    /**
     * Convenience constructor for all streams == null
     * @param printOpts may not be null
     */
    public Print(PrintOpts printOpts) {
        this(printOpts, null, null, null);
    }

    public Print(PrintOpts printOpts,
                 PrintStream outStream,
                 PrintStream errStream,
                 PrintStream debugStream) {

        if (printOpts == null) {
            throw new IllegalArgumentException("printOpts may not be null");
        }

        this.opts = printOpts;

        this.out = outStream;
        this.err = errStream;
        this.debug = debugStream;

        this.proxyInfo = null;
        this.proxyErr = null;
        this.proxyDebug = null;
    }

    /**
     * This allows for String construction to be pre-emptively skipped if it
     * is not going to be used.  Like logging.isDebugEnabled etc.
     * @return true if logging is enabled, whatever the implementation
     */
    public boolean enabled() {
        return !this.opts.disabledEntirely();
    }

    public boolean useThis() {
        if (this.opts.disabledEntirely()) {
            return false;
        }
        return !this.opts.useLogging();
    }

    public boolean useLogging() {
        if (this.opts.disabledEntirely()) {
            return false;
        }
        return this.opts.useLogging();
    }

    public PrintOpts getOpts() {
        return this.opts;
    }

    public void setOutStream(PrintStream outStream) {
        this.out = outStream;
    }

    public void setErrStream(PrintStream errStream) {
        this.err = errStream;
    }

    public void setDebugStream(PrintStream debugStream) {
        this.debug = debugStream;
    }

    public synchronized ProxyInfoPrintStream getInfoProxy() {
        if (this.proxyInfo == null) {
            this.proxyInfo = new ProxyInfoPrintStream(this);
        }
        return this.proxyInfo;
    }

    public synchronized ProxyErrPrintStream getErrProxy() {
        if (this.proxyErr == null) {
            this.proxyErr = new ProxyErrPrintStream(this);
        }
        return this.proxyErr;
    }

    public synchronized ProxyDebugPrintStream getDebugProxy() {
        if (this.proxyDebug == null) {
            this.proxyDebug = new ProxyDebugPrintStream(this);
        }
        return this.proxyDebug;
    }

    public boolean isDebugEnabled() {
        return this.debug != null;
    }

    /* ****** */
    /*  INFO  */
    /* ****** */

    public void info(String text) {
        _info(text, false);
    }

    public void infoln(String text) {
        _info(text, true);
    }

    public void infoln() {
        this.infoln("");
    }

    public void info(int code, String text) {
        if (!this.opts.printThis(code)) {
            // then try debug
            this.debug(text);
            return;
        }
        this.info(text);
    }

    public void infoln(int code, String text) {
        if (!this.opts.printThis(code)) {
            // then try debug
            this.debugln(text);
            return;
        }
        this.infoln(text);
    }

    private void _info(String text, boolean ln) {
        
        if (text == null) {
            return;
        }
        
        try {
            if (ln) {
                this.opts.archiveInfoErr(text + lineSeparator);
            } else {
                this.opts.archiveInfoErr(text);
            }
        } catch (Exception e) {
            if (this.debug != null) {
                this.debug.println(e.getMessage());
            }
        }
        
        if (this.out == null) {
            // then try debug
            if (ln) {
                this.debugln(text);
            } else {
                this.debug(text);
            }
            return;
        }

        if (ln) {
            this.out.println(text);
        } else {
            this.out.print(text);
        }

        try {
            if (ln) {
                this.opts.archiveAllOut(text + lineSeparator);
            } else {
                this.opts.archiveAllOut(text);
            }
        } catch (Exception e) {
            if (this.debug != null) {
                this.debug.println(e.getMessage());
            }
        }
    }

    /* ******* */
    /*  ERROR  */
    /* ******* */

    public void err(String text) {
        _err(text, false);
    }

    public void errln(String text) {
        _err(text, true);
    }

    public void errln() {
        this.errln("");
    }

    public void err(int code, String text) {
        if (!this.opts.printThis(code)) {
            // then try debug
            this.debug(text);
            return;
        }
        this.err(text);
    }

    public void errln(int code, String text) {
        if (!this.opts.printThis(code)) {
            // then try debug
            this.debugln(text);
            return;
        }
        this.errln(text);
    }

    private void _err(String text, boolean ln) {

        if (text == null) {
            return;
        }

        try {
            if (ln) {
                this.opts.archiveInfoErr(text + lineSeparator);
            } else {
                this.opts.archiveInfoErr(text);
            }
        } catch (Exception e) {
            if (this.debug != null) {
                this.debug.println(e.getMessage());
            }
        }
        
        if (this.err == null) {
            // then try debug
            if (ln) {
                this.debugln(text);
            } else {
                this.debug(text);
            }
            return;
        }

        if (ln) {
            this.err.println(text);
        } else {
            this.err.print(text);
        }

        try {
            if (ln) {
                this.opts.archiveAllOut(text + lineSeparator);
            } else {
                this.opts.archiveAllOut(text);
            }
        } catch (Exception e) {
            if (this.debug != null) {
                this.debug.println(e.getMessage());
            }
        }
    }

    /* ******* */
    /*  DEBUG  */
    /* ******* */

    public void debug(String text) {
        _debug(text, false);

    }

    public void debugln(String text) {
        _debug(text, true);
    }

    public void debugln() {
        this.debugln("");
    }

    /**
     * alias for debugln
     * @param text string
     */
    public void dbg(String text) {
        this.debugln(text);
    }

    private void _debug(String text, boolean ln) {

        if (text == null) {
            return;
        }

        if (this.debug != null) {
            if (ln) {
                this.debug.println(text);
            } else {
                this.debug.print(text);
            }
        }
        
        try {
            if (ln) {
                this.opts.archiveAllOut(text + lineSeparator);
            } else {
                this.opts.archiveAllOut(text);
            }
        } catch (Exception e) {
            if (this.debug != null) {
                this.debug.println(e.getMessage());
            }
        }
    }

    /* ***** */
    /*  AUX  */
    /* ***** */

    public void flush() {
        if (this.out != null) {
            this.out.flush();
        }
        if (this.err != null) {
            this.err.flush();
        }
        if (this.debug != null) {
            this.debug.flush();
        }
        try {
            this.opts.flushArchivers();
        } catch (IOException e) {
            if (this.debug != null) {
                this.debug.println(e.getMessage());
            }
        }
    }

    public void close() {
        if (this.out != null) {
            this.out.close();
        }
        if (this.err != null) {
            this.err.close();
        }
        if (this.debug != null) {
            this.debug.close();
        }
        try {
            this.opts.closeArchivers();
        } catch (IOException e) {
            if (this.debug != null) {
                this.debug.println(e.getMessage());
            }
        }
    }
}
