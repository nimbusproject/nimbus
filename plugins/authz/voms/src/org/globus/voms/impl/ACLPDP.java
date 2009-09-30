/*
 * Copyright 1999-2007 University of Chicago
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

package org.globus.voms.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.util.QuotedStringTokenizer;

import javax.security.auth.Subject;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.namespace.QName;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;

/**
 * This PDP checks for the existence of attributes only (could be used as a
 * basis for a DN version too if GridMap.normalizeDN() were run on the
 * contents), one per line, but allows for other strings to exist to the
 * right of the attribute.
 *
 * This is mainly to allow administrators to use grid-mapfiles as straight
 * ACLs but also use a straight attribute list (GridMap class fails to parse
 * a file without username mappings).
 */
public class ACLPDP implements VomsConstants {

    private static final String COMMENT_CHARS = "#";

    private static Log logger = LogFactory.getLog(ACLPDP.class.getName());

    private File file;
    private long lastModified;
    protected Set attributeSet;

    public void initialize(HashMap configs, String name) throws Exception {

        if (configs == null) {
            throw new Exception("no configuration object");
        }

        Object aclFile = configs.get(ATTR_SECURITY_CONFIG_FILE);
        if (aclFile == null) {
            throw new Exception("no attribute based authorization " +
                    "policy (the '" + ATTR_SECURITY_CONFIG_FILE +
                    "' config key)");
        }

        String fileName = (String) aclFile;
        load(new File(fileName));
    }

    public int isPermitted(Subject peer,
                           String attr,
                           MessageContext msgCtx,
                           QName op) {


        boolean ret = false;

        try {
            refresh();
            ret = this.attributeSet.contains(attr);
        } catch (Exception e) {
            // catch all, log, and return false (DENY)
            logger.error(e);
        }

        if (ret) {
            return PDPDecision.PERMIT;
        } else {
            return PDPDecision.DENY;
        }
    }

    private void refresh() throws IOException {
        if (this.file != null &&
                this.file.lastModified() != this.lastModified) {
            
            load(this.file);
        }
    }

    private void load(File file) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            this.file = file;
            this.lastModified = file.lastModified();
            load(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch(Exception e) {
                    logger.error("", e);
                }
            }
        }
    }

    private void load(InputStream input) throws IOException {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(input));

        Set localSet = new HashSet();
        QuotedStringTokenizer tokenizer;
        String line;
        while( (line = reader.readLine()) != null) {
            line = line.trim();
            if ( (line.length() == 0) ||
                 ( COMMENT_CHARS.indexOf(line.charAt(0)) != -1) ) {
                continue;
            }

            tokenizer = new QuotedStringTokenizer(line);

            String attr;
            if (tokenizer.hasMoreTokens()) {
                attr = tokenizer.nextToken();
            } else {
                continue;
            }

            // (ignore rest of line)

            localSet.add(attr);
            logger.debug("added attribute to policy: " + attr);
        }

        this.attributeSet = localSet;
    }

}
