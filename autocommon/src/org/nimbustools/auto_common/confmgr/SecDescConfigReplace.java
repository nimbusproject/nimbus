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

public class SecDescConfigReplace {

    public void replace(String secDescPath,
                        String paramName,
                        String paramValue) throws Exception {

        if (secDescPath == null) {
            throw new IllegalArgumentException("secDescPath may not be null");
        }
        if (paramName == null) {
            throw new IllegalArgumentException("paramName may not be null");
        }
        if (paramValue == null) {
            throw new IllegalArgumentException("paramValue may not be null");
        }

        final DocumentBuilderFactory docBuilderFactory =
                DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder =
                docBuilderFactory.newDocumentBuilder();
        final Document doc = docBuilder.parse(new File(secDescPath));

        doc.normalizeDocument();

        final Node node = this.getNodeByNameOnly(paramName, doc);

        if (node == null) {
            throw new Exception("Can not find '" + paramName + "' element");
        }

        this.replaceValue(node, paramValue, paramName);

        new DomWriter().writeDOM(doc, secDescPath);
    }

    protected void replaceValue(Node node, String newvalue, String paramName)
            throws Exception {

        final NamedNodeMap attrs = node.getAttributes();
        final Node valNode = attrs.getNamedItem("value");
        if (valNode == null) {
            throw new Exception("Found '" + paramName +
                    "' element, but no value attribute (?)");
        }
        valNode.setNodeValue(newvalue);
    }

    protected Node getNodeByNameOnly(String name,
                                     Node topnode) {

        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }

        if (topnode == null) {
            return null;
        }

        final String thisname = topnode.getNodeName();
        if (thisname != null && thisname.equals(name)) {
            return topnode;
        }

        final NodeList nodes = topnode.getChildNodes();
        if (nodes == null || nodes.getLength() < 1) {
            return null;
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = this.getNodeByNameOnly(name, nodes.item(i));
            if (node != null) {
                return node;
            }
        }

        return null;
    }
}
