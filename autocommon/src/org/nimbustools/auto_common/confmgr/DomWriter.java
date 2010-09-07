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

import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.File;

public class DomWriter {

    public void writeDOM(Document doc, String path) throws Exception {

        final Source source = new DOMSource(doc);
        final File docfile = new File(path);

        // unfortunately the following simple solution breaks on JDK1.5+
        // because of this bug: http://issues.apache.org/jira/browse/XALANJ-1978
        // and so the implementation is overriden to fix the bug
        System.setProperty("javax.xml.transform.TransformerFactory",
                           "org.nimbustools.auto_common.confmgr.TransformerFactory");

        final Result xformresult = new StreamResult(docfile);
        final Transformer xform =
                TransformerFactory.newInstance().newTransformer();
        xform.transform(source, xformresult);
    }

}
