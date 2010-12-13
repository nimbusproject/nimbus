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
package org.nimbustools.messaging.query;

import org.apache.axis.description.TypeDesc;
import org.apache.axis.message.MessageElement;
import org.apache.axis.encoding.SerializationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.xml.sax.Attributes;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

@Provider
@Produces({"text/xml"})
public class AxisBodyWriter implements MessageBodyWriter<Object> {

    private static final Log logger =
            LogFactory.getLog(AxisBodyWriter.class.getName());

    private static final String GET_TYPE_DESC = "getTypeDesc";
    private static final String TYPE_SUFFIX = "Type";

    @Context
    MessageContext messageContext;


    // this is a travesty

    public boolean isWriteable(Class<?> aClass, Type type,
                               Annotation[] annotations, MediaType mediaType) {

        return getTypeDescVerySlowly(aClass) != null;
    }

    public long getSize(Object o, Class aClass, Type type,
                        Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public void writeTo(Object o, Class aClass, Type type,
                        Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap multivaluedMap,
                        OutputStream outputStream)
            throws IOException, WebApplicationException {
        final TypeDesc typeDesc = getTypeDescVerySlowly(aClass);

        final QName qName = typeDesc.getXmlType();

        if (qName == null) {
            throw new WebApplicationException(500);
        }

        String name = qName.getLocalPart();
        // responses should not have 'Type' on end of element name. ugly.
        if (name.endsWith(TYPE_SUFFIX)) {
            name = name.substring(0, name.length() - TYPE_SUFFIX.length());
        }

        String namespace = getNamespaceUriFromContext();
        if (namespace == null) {
            namespace = qName.getNamespaceURI();
        }

        final OutputStreamWriter writer =
                new OutputStreamWriter(outputStream);

        //manually write the xml header deal
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        MessageElement element = new MessageElement(name, "", namespace);
        try {
            element.setObjectValue(o);

            final SerializationContext context =
                    new CleanSerializationContext(writer, namespace);
            context.setDoMultiRefs(false);
            context.setPretty(true);
            element.output(context);


        } catch (SOAPException e) {
            throw new WebApplicationException(e);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
        writer.close();
    }

    private String getNamespaceUriFromContext() {
        if (this.messageContext != null) {

            final HttpHeaders headers = this.messageContext.getHttpHeaders();
            if (headers != null) {
                final List<String> versionHeaders =
                        headers.getRequestHeader(ElasticQuery.API_VERSION_HEADER);

                if (versionHeaders != null && !versionHeaders.isEmpty()) {
                    final String version = versionHeaders.get(0);
                    if (version.length() > 0) {
                        return "http://ec2.amazonaws.com/doc/" + version + "/";
                    }
                }
            }
        }
        return null;
    }

    private static TypeDesc getTypeDescVerySlowly(Class aClass) {
        try {

            final Method method = aClass.getMethod(GET_TYPE_DESC);
            return (TypeDesc) method.invoke(null);
        } catch (IllegalAccessException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

}

class CleanSerializationContext extends SerializationContext {
    private final String defaultNamespace;

    public CleanSerializationContext(Writer writer, String defaultNamespace) {
        super(writer);
        this.defaultNamespace = defaultNamespace;
    }

    @Override
    public boolean shouldSendXSIType() {
        return false;
    }

    @Override
    public void startElement(QName qName, Attributes attributes) throws IOException {
        // for some reason many of the EC2 types are coming through with an empty namespace
        // override it with the one provided at constructor time
        if ("".equals(qName.getNamespaceURI())) {
            qName = new QName(defaultNamespace, qName.getLocalPart());
        }
        super.startElement(qName, attributes);
    }

    // override these to ensure that by default nil elements are left out of
    // serialized XML. Only the first one is apparently called by MessageElement, but
    // overriding both for completeness.

    @Override
    public void serialize(QName elemQName,
                          Attributes attributes,
                          Object value)
            throws IOException {
        serialize(elemQName, attributes, value, null, Boolean.FALSE, null);
    }

    @Override
    public void serialize(QName elemQName,
                          Attributes attributes,
                          Object value,
                          QName xmlType)
            throws IOException {
        serialize(elemQName, attributes, value, xmlType, Boolean.FALSE, null);
    }
}
