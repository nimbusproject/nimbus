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

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;

/**
 * See the Print class comments for basic usage in conjunction with Print.
 * Read this API documentation only to learn advanced features.
 *
 * @see Print
 */
public class PrintOpts {

    public static final int PRCODE_NOT_SET = -1;
    public static final PrintOpts USE_COMMONS_LOGGING = new PrintOpts(true);
    public static final PrintOpts DISABLE_ENTIRELY = new PrintOpts(false);

    private final boolean disableEntirely;
    private final boolean useLogging;
    private final boolean printAll;
    private int[] codes;

    private final Object infoErrLock = new Object();
    private File infoErrFile;
    private StringBuffer infoErrTxt;
    private FileWriter infoErrFileWriter;

    private final Object allOutLock = new Object();
    private File allOutFile;
    private StringBuffer allOutTxt;
    private FileWriter allOutFileWriter;

    // used for USE_COMMONS_LOGGING and DISABLE_ENTIRELY only
    private PrintOpts(boolean logging) {
        this.printAll = false;
        this.codes = new int[0];
        this.useLogging = logging;
        this.disableEntirely = !logging;
    }

    /**
     * <p>Constructor to use for stdout/stderr and/or archiving logfiles</p>
     *
     * <p><i>Before reading this comment</i>: if you just want typical print
     * messages don't read this comment.  Pass null to this constructor and
     * use System.out and System.err streams in the Print constructor (and
     * pass a non-null debug stream to the Print constructor if you want
     * debug messages).</p>
     *
     * <p>If you want fancy things, read on.</p>
     *
     * <p>Two features provided that are not in straight logging are:</p>
     *
     * <ul>
     *  <li>Fine grained control of what particular messages are printed to
     *      users and files</li>
     *  <li>Both of the streams (info, err and debug) can be stored up in
     *      buffers until a file is available to dump into (once available,
     *      if ever, archiving will switch to this file).
     *  </li>
     * </ul>
     *
     * <p>This buffer mechanism allows clients to run normally until such a time
     * that a file is created or even allow for no file to be created.  When
     * the archiving method is switched to using files, all previous messages
     * will be written to the file.  This allows for run-specific history
     * directories to be created for example.  Also, ALL debug traffic is
     * captured into the debug system.</p>
     *
     * <p>You can do things like include a debug
     * file that is not displayed to a CLI user but written out to a history
     * directory for each run.  These features are in use in the cloud client,
     * see that code for examples.</p>
     *
     * <p>The print code system allows you to control all aspects of the display
     * to clients (and any files in use).</p>
     *
     * <p>Note that this is different from info vs. debug.  That always exists.
     * This mechanism can suppress specific info messages from going to stdout.</p>
     *
     * <p>We use an INCLUSIVE array of codes so that more messages may be added
     * to the code over time without API users needing to constantly opt-out
     * of codes when updating the API version.  See the PrCodes class for
     * helper methods to create partial code arrays.  The only documentation
     * for what is printed where is looking at the source code.</p>
     *
     * @param printThese array of print codes to INCLUDE, if null, means "all"
     * @see org.globus.workspace.client_core.print.PrCodes
     */
    public PrintOpts(int[] printThese) {
        this.useLogging = false;
        this.disableEntirely = false;

        this.printAll = printThese == null;
        this.codes = printThese;

        this.infoErrFile = null;
        this.infoErrTxt = new StringBuffer(1024);
        this.infoErrFileWriter = null;

        this.allOutFile = null;
        this.allOutTxt = new StringBuffer(1024);
        this.allOutFileWriter = null;
    }

    boolean disabledEntirely() {
        return this.disableEntirely;
    }

    boolean useLogging() {
        if (this.disableEntirely) {
            return false;
        }
        return this.useLogging;
    }

    boolean isPrintAll() {
        if (this.disableEntirely) {
            return false;
        }
        return this.printAll;
    }

    public boolean printThis(int code) {
        if (this.disableEntirely) {
            return false;
        }

        if (this.printAll) {
            return true;
        }
        if (this.codes == null) {
            return true;
        }
        for (int i = 0; i < this.codes.length; i++) {
            if (this.codes[i] == code) {
                return true;
            }
        }
        return false;
    }

    private static void checkFile(File f) throws IOException {
        if (f == null) {
            throw new IllegalArgumentException("input 'f' may not be null");
        }

        final String msg;
        if (f.exists()) {
            msg = null;
        } else {
            f.createNewFile();
            msg = ", even after creation attempt.";
        }

        if (!f.exists()) {
            String err = "File does not exist @ '" + f.getAbsolutePath() + "'";
            if (msg != null) {
                err += msg;
            }
            throw new IOException(err);
        }

        if (!f.canWrite()) {
            throw new IOException(
                    "Cannot write to file ('" + f.getAbsolutePath() + "')");
        }
    }

    /**
     * Remove a print code if it is present.
     * Does nothing if in print-all, logging, or disabled mode.
     * @param code prcode
     */
    public void codeRemove(int code) {

        if (this.disableEntirely || this.useLogging ||
                this.printAll || this.codes == null) {
            return;
        }

        synchronized (DISABLE_ENTIRELY) {
            for (int i = 0; i < this.codes.length; i++) {
                if (this.codes[i] == code) {
                    this.codes[i] = PRCODE_NOT_SET;
                }
            }
        }
    }

    /**
     * Add a print code.
     * Does nothing if in print-all, logging, or disabled mode.
     * @param code prcode
     */
    public synchronized void codeAdd(int code) {

        if (this.disableEntirely || this.useLogging ||
                this.printAll || this.codes == null) {
            return;
        }

        final int[] newcodes = new int[this.codes.length + 1];

        synchronized (DISABLE_ENTIRELY) {
            System.arraycopy(this.codes, 0, newcodes, 0, this.codes.length);
            newcodes[this.codes.length] = code;
            this.codes = newcodes;
        }
    }

    /**
     * Does nothing if output file is already configured.
     * Does nothing if in logging mode or in disabled mode.
     * @param outFilePath may not be null
     * @throws IOException if there is a problem with outFilePath
     */
    public synchronized void setInfoErrFile(String outFilePath)

            throws Exception {

        if (this.disableEntirely || this.useLogging) {
            return;
        }

        if (outFilePath == null) {
            throw new IllegalArgumentException("outFilePath may not be null");
        }

        if (this.infoErrFile != null) {
            return;
        }

        final File f = new File(outFilePath);
        checkFile(f);

        synchronized (this.infoErrLock) {
            this.infoErrFile = f;
            this.switchInfoErr();
        }
    }

    /**
     * Does nothing if output file is already configured.
     * Does nothing if in logging mode or in disabled mode.
     * @param outFilePath may not be null
     * @throws IOException if there is a problem with outFilePath
     */
    public synchronized void setAllOutFile(String outFilePath)

            throws Exception {

        if (this.disableEntirely || this.useLogging) {
            return;
        }

        if (outFilePath == null) {
            throw new IllegalArgumentException("outFilePath may not be null");
        }

        if (this.allOutFile != null) {
            return;
        }

        final File f = new File(outFilePath);
        checkFile(f);

        synchronized (this.allOutLock) {
            this.allOutFile = f;
            this.switchAllOut();
        }
    }

    private void switchInfoErr() throws Exception {
        if (this.infoErrFile == null) {
            throw new IllegalAccessError("no infoErrFile");
        }
        if (this.infoErrTxt == null) {
            throw new IllegalAccessError("no in-RAM infoErrTxt");
        }
        if (this.infoErrTxt.length() > 0) {
            this.archiveInfoErr(this.infoErrTxt.toString());
        }
        this.infoErrTxt = null;
    }

    private void switchAllOut() throws Exception {
        synchronized (this.allOutLock) {
            if (this.allOutFile == null) {
                throw new IllegalAccessError("no allOutFile");
            }
            if (this.allOutTxt == null) {
                throw new IllegalAccessError("no in-RAM allOutTxt");
            }
            if (this.allOutTxt.length() > 0) {
                this.archiveAllOut(this.allOutTxt.toString());
            }
            this.allOutTxt = null;
        }
    }

    public void archiveAllOut(String text) throws Exception {
        
        if (this.disableEntirely || this.useLogging) {
            return;
        }
        
        synchronized (this.allOutLock) {
            if (this.allOutFile != null) {
                if (this.allOutFileWriter == null) {
                    this.allOutFileWriter = new FileWriter(this.allOutFile);
                }
                this.allOutFileWriter.write(text);
                this.allOutFileWriter.flush();
            } else if (this.allOutTxt == null) {
                throw new Exception("no in-RAM allOutTxt?");
            } else {
                this.allOutTxt.append(text);
            }
        }
    }

    public void archiveInfoErr(String text) throws Exception {
        
        if (this.disableEntirely || this.useLogging) {
            return;
        }

        synchronized (this.infoErrLock) {
            if (this.infoErrFile != null) {
                if (this.infoErrFileWriter == null) {
                    this.infoErrFileWriter = new FileWriter(this.infoErrFile);
                }
                this.infoErrFileWriter.write(text);
                this.infoErrFileWriter.flush();
            } else if (this.infoErrTxt == null) {
                throw new Exception("no in-RAM infoErrTxt?");
            } else {
                this.infoErrTxt.append(text);
            }
        }
    }

    public void closeArchivers() throws IOException {
        
        if (this.disableEntirely || this.useLogging) {
            return;
        }

        synchronized (this.allOutLock) {
            if (this.allOutFileWriter != null) {
                this.allOutFileWriter.close();
                this.allOutFileWriter = null;
            }
        }
        synchronized (this.infoErrLock) {
            if (this.infoErrFileWriter != null) {
                this.infoErrFileWriter.close();
                this.infoErrFileWriter = null;
            }
        }
    }

    public void flushArchivers() throws IOException {
        
        if (this.disableEntirely || this.useLogging) {
            return;
        }

        synchronized (this.allOutLock) {
            if (this.allOutFileWriter != null) {
                this.allOutFileWriter.flush();
            }
        }
        synchronized (this.infoErrLock) {
            if (this.infoErrFileWriter != null) {
                this.infoErrFileWriter.flush();
            }
        }
    }
}
