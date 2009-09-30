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

package org.nimbustools.messaging.gt4_0;

import org.globus.wsrf.impl.BaseResourceProperty;
import org.globus.wsrf.impl.SimpleResourcePropertyMetaData;
import org.globus.wsrf.ResourcePropertyMetaData;
import org.globus.wsrf.encoding.SerializationException;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Abstract base class for hardcoded callback mechanism to do basic RPs
 * (simple, one-value) with no hassles.
 */
public abstract class BasicRP extends BaseResourceProperty {

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public BasicRP(QName name) {
        this(new SimpleResourcePropertyMetaData(name));
    }

    public BasicRP(ResourcePropertyMetaData metaData) {
        super(metaData);
    }


    // -------------------------------------------------------------------------
    // DISPATCH
    // -------------------------------------------------------------------------

    public abstract Object getValue();

    public abstract void setValue(Object value);


    // -------------------------------------------------------------------------
    // implements ResourceProperty
    // -------------------------------------------------------------------------

    /**
     * NOT USED.
     *
     * Adds a value.
     *
     * @param value the value to add.
     */
    public void add(Object value) {
        // does nothing
    }

    /**
     * NOT USED.
     *
     * Removes a specific value. If the resource property contains multiple of
     * the same value, only the first one is removed.
     *
     * @param value value to remove.
     *
     * @return true if the value was removed. False otherwise.
     */
    public boolean remove(Object value) {
        return false;  // does nothing
    }

    /**
     * INDEX IS ALWAYS IGNORED (ALWAYS TREATED AS ZERO)
     *
     * Retrieves a value at a specific index.
     *
     * @param index the index of value to retrieve.
     *
     * @return the value at the given index. This operation might fail if the
     *         index is out of bounds.
     */
    public Object get(int index) {
        return this.getValue();
    }

    /**
     * INDEX IS ALWAYS IGNORED (ALWAYS TREATED AS ZERO)
     * 
     * Sets a value at a specific index.
     *
     * @param index the index to set value at.
     * @param value the new value
     */
    public void set(int index, Object value) {
        this.setValue(value);
    }

    /**
     * NOT USED.
     * 
     * Removes all values.
     */
    public void clear() {
        // does nothing
    }

    /**
     * ALWAYS ONE
     * 
     * Returns the number of values in the resource property.
     *
     * @return the number of values.
     */
    public int size() {
        return 1;
    }

    /**
     * ALWAYS FALSE
     *
     * Returns true if the resource property has any values.
     *
     * @return true if the resource property has any values. False, otherwise.
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * Returns iterator over the values of this resource property.
     *
     * @return iterator over the values of this resource property.
     */
    public Iterator iterator() {
        final List list = new ArrayList(1);
        list.add(this.get(0));
        return list.iterator();
    }

    /**
     * @return the resource property as a SOAPElement array.
     * @throws SerializationException if conversion fails.
     */
    public SOAPElement[] toSOAPElements() throws SerializationException {
        final boolean nillable = this.metaData.isNillable();
        final QName name = getMetaData().getName();
        final SOAPElement[] values = new SOAPElement[1];
        values[0] = ObjectSerializer.toSOAPElement(this.get(0), name, nillable);
        return values;
    }

    /**
     * @return the resource property as a DOM Element array.
     * @throws SerializationException if conversion fails.
     */
    public Element[] toElements() throws SerializationException {
        final boolean nillable = this.metaData.isNillable();
        final QName name = getMetaData().getName();
        final Element[] values = new Element[1];
        values[0] = ObjectSerializer.toElement(this.get(0), name, nillable);
        return values;
    }
}
