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

package org.nimbustools.auto_config.confmgr;

import org.nimbustools.auto_config.TextFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

/**
 * This class lets you find and replace values in the *.conf files.
 * Comments preserved with changes.
 */
public class GenericConfUtil {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final File nimbusConfDir;
    protected final File workspConfDir;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public GenericConfUtil(String nimbusConfLocationPath) throws Exception {
        if (nimbusConfLocationPath == null) {
            throw new IllegalArgumentException(
                    "nimbusConfLocationPath may not be null");
        }
        this.nimbusConfDir = new File(nimbusConfLocationPath);
        if (!this.nimbusConfDir.exists()) {
            throw new Exception("Nimbus configuration directory does not " +
                    "exist: " + this.nimbusConfDir.getAbsolutePath());
        }

        this.workspConfDir = new File(this.nimbusConfDir, "workspace-service");
        if (!this.workspConfDir.exists()) {
            throw new Exception("workspace-service configuration directory " +
                    "does not exist: " + this.workspConfDir.getAbsolutePath());
        }
    }


    // -------------------------------------------------------------------------
    // INFO
    // -------------------------------------------------------------------------

    public String getNimbusConfDirAbsolutePath() {
        return nimbusConfDir.getAbsolutePath();
    }

    public String getWorkspaceServiceConfDirAbsolutePath() {
        return workspConfDir.getAbsolutePath();
    }


    // -------------------------------------------------------------------------
    // LOCATE PARTICULAR CONF FILE
    // -------------------------------------------------------------------------

    public File getWorkspConfFile(String localName)
            throws Exception {
        
        final File conf =
                new File(this.getWorkspaceServiceConfDirAbsolutePath() +
                         File.separator + localName);
        this.checkFile(conf);
        return conf;
    }

    protected void checkFile(File conf) throws Exception {

        if (conf == null) {
            throw new FileNotFoundException("conf may not be null");
        }

        final String abspath = conf.getAbsolutePath();

        if (!conf.exists()) {
            throw new FileNotFoundException("Cannot find file: " + abspath);
        }

        if (!conf.isFile()) {
            throw new FileNotFoundException("Is not a file: " + abspath);
        }

        if (!conf.canRead()) {
            throw new Exception("Can not read file: " + abspath);
        }

        if (!conf.canWrite()) {
            throw new Exception("Can not write to file: " + abspath);
        }
    }
    
    // -------------------------------------------------------------------------
    // GET PROPERTY
    // -------------------------------------------------------------------------

    /**
     * @param conf configuration file
     * @param keyword name of property
     * @return null if not found, empty if present but empty
     * @throws Exception problem with conf file or input
     */
    public String getProperty(File conf, String keyword) throws Exception {
        
        if (keyword == null || keyword.trim().length() == 0) {
            throw new Exception("no keyword provided");
        }

        this.checkFile(conf);

        final Properties props = new Properties();
        props.load(new FileInputStream(conf));
        final String value = props.getProperty(keyword);
        if (value == null) {
            return null;
        } else {
            return value.trim();
        }
    }


    // -------------------------------------------------------------------------
    // SET PROPERTY
    // -------------------------------------------------------------------------

    /*
     * Several constraints: won't work if key is not present, undefined behavior
     * if multiple keys present.  This is done in an effort to maintain comments
     * and all formatting.
     */
    public void setProperty(File conf,
                            String keyword,
                            String newvalue) throws Exception {

        final String oldvalue = this.getProperty(conf, keyword);

        // conf and keyword checked non-null via getProperty
        
        if (oldvalue == null) {
            throw new Exception("Can not alter the configuration file, no '" +
                    keyword + "' setting present in the file '" +
                    conf.getAbsolutePath() + "'");
        }

        final String compare;
        if (newvalue != null) {
            compare = newvalue.trim();
        } else {
            compare = null;
        }

        if (oldvalue.equals(compare)) {
            System.out.println("[*] The '" + keyword +
                    "' configuration does not need to be changed.");

            final String setString;
            if (oldvalue.trim().length() == 0) {
                setString = "be blank";
            } else {
                setString = "'" + oldvalue + "'";
            }

            System.out.println("    ... already set to " + setString);
            System.out.println("    ... in the file '" + conf.getCanonicalPath() + "'");
            return; // *** EARLY RETURN ***
        }

        final TextFile textFile = new TextFile(conf.getAbsolutePath());
        if (textFile.isEmpty()) {
            throw new Exception("File is empty? '" +
                    conf.getAbsolutePath() + "'");
        }

        boolean replaced = false;
        final int size = textFile.size();
        for (int i = 0; i < size; i++) {
            final String line = (String) textFile.get(i);
            if (line != null && line.trim().length() > 0) {
                if (line.trim().startsWith(keyword)) {
                    final String newline;
                    if (newvalue == null) {
                        newline = "#" + keyword + "=";
                    } else {
                        newline = keyword + "=" + newvalue.trim();
                    }
                    textFile.set(i, newline);
                    replaced = true;
                    break;
                }
            }
        }

        if (!replaced) {
            // race condition with getProperty() on file contents?
            throw new Exception("Could not alter the configuration file, no '" +
                    keyword + "' setting present in the file '" +
                    conf.getAbsolutePath() + "' (?)");
        }

        textFile.writeFile(conf);

        System.out.println("[*] The '" + keyword + "' configuration was:");
        if (newvalue == null) {
            System.out.println("    ... commented out");
        } else if (newvalue.trim().length() == 0) {
            System.out.print("    ... set to be blank");
        } else {
            System.out.println("    ... set to '" + newvalue.trim() + "'");
        }
        if (oldvalue.length() == 0) {
            System.out.println("    ... (it used to be set blank)");
        } else {
            System.out.println("    ... (it used to be set to '" + oldvalue + "')");
        }
        System.out.println("    ... in the file '" + conf.getCanonicalPath() + "'");
    }

}
