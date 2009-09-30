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

package org.nimbustools.auto_common.confmgr;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * TODO: modularize more
 */
public class ServerConfigGlobalAdjust {

    public void addOrReplace(String serverConfigPath,
                             String globalParamName,
                             String globalParamValue) throws Exception {

        if (serverConfigPath == null) {
            throw new IllegalArgumentException("serverConfigPath may not be null");
        }
        if (globalParamName == null) {
            throw new IllegalArgumentException("globalParamName may not be null");
        }
        if (globalParamValue == null) {
            throw new IllegalArgumentException("globalParamValue may not be null");
        }

        final DocumentBuilderFactory docBuilderFactory =
                DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder =
                docBuilderFactory.newDocumentBuilder();
        final Document doc = docBuilder.parse(new File(serverConfigPath));

        doc.normalizeDocument();

        final NodeList topnodes = doc.getDocumentElement().
                getElementsByTagName("globalConfiguration");

        if (topnodes == null || topnodes.item(0) == null) {
            throw new Exception("Can not find <globalConfiguration> section " +
                    "in this XML file (?)");
        }

        final Node globalConfiguration = topnodes.item(0);

        final NodeList children = globalConfiguration.getChildNodes();

        boolean previousSetting = false;

        for(int i = 0; i < children.getLength() ; i++) {

            final Node aGlobalNode = children.item(i);
            if (aGlobalNode.getNodeName().equals("parameter")) {

                final NamedNodeMap attrs = aGlobalNode.getAttributes();
                final Node logNode = attrs.getNamedItem("name");
                if (logNode != null &&
                        globalParamName.equals(logNode.getNodeValue())) {
                    final Node valNode = attrs.getNamedItem("value");
                    if (valNode == null) {
                        throw new Exception("Found '" + globalParamName +
                                "' <parameter> with no value attribute (?)");
                    }
                    valNode.setNodeValue(globalParamValue);
                    previousSetting = true;
                }
            }
        }

        if (!previousSetting) {

            Node text = null;

            for(int i = 0; i < children.getLength() ; i++) {

                final Node aGlobalNode = children.item(i);

                if (text == null &&
                        aGlobalNode.getNodeValue().startsWith("\n")) {
                    text = aGlobalNode.cloneNode(false);
                }

                if (aGlobalNode.getNodeName().equals("parameter")) {

                    final Node newnode = aGlobalNode.cloneNode(true);
                    final NamedNodeMap attrs = newnode.getAttributes();
                    attrs.item(0).setNodeValue(globalParamName);
                    attrs.item(1).setNodeValue(globalParamValue);

                    globalConfiguration.insertBefore(newnode, aGlobalNode);
                    globalConfiguration.insertBefore(text, aGlobalNode);
                    break;
                }
            }
        }

        new DomWriter().writeDOM(doc, serverConfigPath);
    }
}
