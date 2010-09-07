/*
 * Copyright 1999-2009 University of Chicago
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
package org.nimbustools.auto_common.confmgr;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/***
 * Adjusts resource parameters in globus-style jndi-config.xml files.
 */
public class ServiceResourceAdjust {


    public void adjust(File jndiConfig, String serviceName, String resourceName, Map<String,String> pairs)
            throws Exception {

        if (jndiConfig == null) {
            throw new IllegalArgumentException("jndiConfig may not be null");
        }
        if (serviceName == null || serviceName.length() == 0) {
            throw new IllegalArgumentException("serviceName may not be null or empty");
        }
        if (resourceName == null || resourceName.length() == 0) {
            throw new IllegalArgumentException("resourceName may not be null or empty");
        }
        if (pairs == null || pairs.size() == 0) {
            throw new IllegalArgumentException("pairs may not be null or empty");
        }

        final DocumentBuilderFactory docBuilderFactory =
                DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder =
                docBuilderFactory.newDocumentBuilder();
        final Document doc = docBuilder.parse(jndiConfig);

        doc.normalizeDocument();

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        final Node paramsNode = (Node) xpath.evaluate(
                "//service[@name='"+serviceName+"']/resource[@name='"+resourceName+"']/resourceParams",
                doc, XPathConstants.NODE);

        if (paramsNode == null) {
            throw new Exception("Could not find resourceParams node for specified service resource");
        }

        boolean dirty = false;
        for (Map.Entry<String, String> pair : pairs.entrySet()) {
            Node node = (Node) xpath.evaluate(
                    "parameter[name='"+pair.getKey()+"']/value",
                    paramsNode, XPathConstants.NODE);
            if (node == null) {
                throw new Exception("Could not find a resource parameter named '"+pair.getKey()+"'");
            }
            if (!pair.getValue().equals(node.getTextContent())){
                node.setTextContent(pair.getValue());
                dirty = true;
            }
        }
        if (dirty) {
            new DomWriter().writeDOM(doc, jndiConfig.getAbsolutePath());
        }
    }

    public static void main(String[] args) {

        if (args == null || args.length < 5 || args.length % 2 == 0) {
            System.err.println("Needs these arguments:\n" +
                    "1 - the jndi-config.xml path\n" +
                    "2 - the <service> name\n" +
                    "3 - the <resource> name\n" +
                    "4 - the parameter name\n" +
                    "5 - the parameter value\n" +
                    "[additional parameter name/value pairs]"
            );
            System.exit(1);
        }

        try {
            final File file = new File(args[0]);
            if (! (file.exists() && file.canRead() && file.canWrite())) {
                System.err.println("the config XML file must exist and be readable/writeable");
                System.exit(1);
            }

            final HashMap<String,String> pairs = new HashMap<String, String>();
            for (int i=3; i < args.length; i+=2) {
                final String key = args[i];
                final String value = args[i+1];
                pairs.put(key, value);
            }
            new ServiceResourceAdjust().adjust(file, args[1], args[2], pairs);

        } catch (Exception e) {
            System.err.println("Problem replacing key-file value: " + e.getMessage());
            System.exit(1);
        }
    }
}
