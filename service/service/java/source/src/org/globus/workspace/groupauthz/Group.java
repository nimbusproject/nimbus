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

package org.globus.workspace.groupauthz;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

public class Group {

    private static final Log logger = LogFactory.getLog(Group.class.getName());

    private static final String[] EMPTY_RESPONSE = new String[0];

    private final String groupNumString;

    private final File dnListFile;
    private long dnListLastModified;

    private final File rightsFile;
    private long rightsLastModified;

    private GroupRights rights;
    private String[] DNs;

    public Group(File identityListFile, File groupRightsFile, String grpNum) {

        if (identityListFile == null) {
            throw new IllegalArgumentException(
                    "identityListFile may not be null");
        }

        if (groupRightsFile == null) {
            throw new IllegalArgumentException(
                    "groupRightsFile may not be null");
        }

        this.dnListFile = identityListFile;
        this.rightsFile = groupRightsFile;
        this.DNs = null;
        this.rights = null;
        this.groupNumString = grpNum;
    }

    // main interface, if response is null, DN is not in group
    public synchronized GroupRights identityRights(String dn) {
        if (dn == null) {
            return null;
        }
        final String[] dns = this.getIdentities();
        final GroupRights groupRights = this.getRights();
        for (int i = 0; i < dns.length; i++) {
            if (dn.equals(dns[i])) {
                return groupRights;
            }
        }
        return null;
    }

    // nickname from rights file
    public synchronized String getName() {
        final GroupRights grts = this.getRights();
        if (grts == null) {
            return null;
        } else {
            return grts.getName();
        }
    }

    public synchronized GroupRights getRights() {

        try {
            if (this.rights == null) {
                this.reloadRightsFile();
            } else if (this.rightsLastModified <
                        this.rightsFile.lastModified()) {
                this.reloadRightsFile();
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            this.rights = null;
        }

        return this.rights;
    }

    public String getIdentitiesFilePath() {
        return this.dnListFile.getAbsolutePath();
    }

    public synchronized String[] getIdentities() {

        try {
            if (this.DNs == null) {
                this.reloadDNFile();
            } else if (this.dnListLastModified <
                        this.dnListFile.lastModified()) {
                this.reloadDNFile();
            }
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            this.DNs = null;
        }

        if (this.DNs != null) {
            return this.DNs;
        } else {
            return EMPTY_RESPONSE;
        }
    }

    private void reloadDNFile() throws IOException {
        checkFile(this.dnListFile, "DN policy");
        final long current = this.dnListFile.lastModified();

        final Iterator iter =
                new TextFile(this.dnListFile.getAbsolutePath()).iterator();

        final ArrayList dns = new ArrayList(16);
        while (iter.hasNext()) {
            final String dn = (String) iter.next();
            if (dn != null && dn.trim().length() > 0) {
                dns.add(dn.trim());
            }
        }

        this.DNs = (String[]) dns.toArray(new String[dns.size()]);
        this.dnListLastModified = current;

        String msg = this.groupNumString + ": Loaded " + dns.size() +
                           " identities from '" +
                           this.dnListFile.getAbsolutePath() + "'";

        if (this.rights != null && this.rights.getName() != null) {
            msg = this.rights.getName() + " -- " + msg;
        }

        logger.warn(msg);

        if (logger.isDebugEnabled()) {
            final StringBuffer buf = new StringBuffer(msg);
            buf.append(". Identities:\n");
            for (int i = 0; i < this.DNs.length; i++) {
                buf.append(this.DNs[i]);
            }
        }
    }

    private void reloadRightsFile() throws Exception {
        checkFile(this.rightsFile, "Group rights");
        final long current = this.rightsFile.lastModified();

        final Properties props = new Properties();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(this.rightsFile);
            props.load(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        this.rights =
                new GroupRights(props, this.rightsFile.getAbsolutePath());
        this.rightsLastModified = current;

        String msg = this.groupNumString + ": Loaded group definition from '" +
                     this.rightsFile.getAbsolutePath() + "'";


        if (this.rights.getName() != null) {
            msg = this.rights.getName() + " -- " + msg;
        }

        logger.warn(msg);

        if (logger.isDebugEnabled()) {
            msg += ", contents resolve to:\n";
            logger.debug(msg + this.rights.toString());
        }
    }

    private static void checkFile(File file, String type) throws IOException {
        if (!file.exists()) {
            throw new IOException(type + " file does not exist: '" +
                    file.getAbsolutePath() + "'");
        }
        if (!file.canRead()) {
            throw new IOException(type + " file can not be read: '" +
                    file.getAbsolutePath() + "'");
        }
    }

    private static class TextFile extends ArrayList {
        //via Eckel
        TextFile(String fileName) throws IOException {
            super(Arrays.asList(gRead(fileName).split("\n")));
        }
    }

    private static String gRead(String fileName) throws IOException {
        final StringBuffer sb = new StringBuffer(16);

        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(fileName);
            br = new BufferedReader(fr);
            String s = br.readLine();
            while(s != null) {
                sb.append(s);
                sb.append("\n");
                s = br.readLine();
            }
        } finally {
            if (fr != null) {
                fr.close();
            }
            if (br != null) {
                br.close();
            }
        }
        return sb.toString();
    }

    public boolean hasDN(String dn) {
        for(int i = 0; i < DNs.length; i++) {
            if(dn.equals(DNs[i]))
                return true;
        }
        return false;
    }
}
