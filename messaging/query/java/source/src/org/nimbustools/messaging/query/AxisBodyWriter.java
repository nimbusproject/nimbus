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

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

@Provider
@Produces({"text/xml"})
public class AxisBodyWriter implements MessageBodyWriter<Object> {
    private static final String GET_TYPE_DESC = "getTypeDesc";
    private static final String TYPE_SUFFIX = "Type";

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

        final OutputStreamWriter writer =
                new OutputStreamWriter(outputStream);

        MessageElement element = new MessageElement(qName.getNamespaceURI(), name);
        try {
            element.setObjectValue(o);

            final SerializationContext context =
                    new SerializationContext(writer);
            //context.setSendDecl(false);
            context.setPretty(true);
            element.output(context);


        } catch (SOAPException e) {
            throw new WebApplicationException(e);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
        writer.close();
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
