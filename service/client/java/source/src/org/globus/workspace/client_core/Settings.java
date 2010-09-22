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

package org.globus.workspace.client_core;

import org.globus.workspace.common.print.Print;

/**
 * Contains esoteric running preferences.  API user can pass in their own
 * preferences object (new objects contain the embedded defaults, use set
 * methods to override specific preferences).
 */
public class Settings {

    // default is "disabled entirely" mode. this field may not ever be set null
    private Print printImpl = new Print();

    private int groupSuffixMinCharacters = 3;

    private String generatedEprElementName = "WORKSPACE_EPR";

    private String generatedGroupEprElementName = "WORKSPACE_GROUP_EPR";

    private String generatedEnsembleEprElementName = "WORKSPACE_ENSEMBLE_EPR";

    private String generatedContextEprElementName = "NIMBUS_CONTEXT_EPR";

    private String generatedContextBrokerContactElementName = "NIMBUS_CONTEXT_BROKER";


    /**
     * Never null.
     * @return desired print implementation, default is "disabled entirely"
     * @see org.globus.workspace.common.print.Print
     * @see org.globus.workspace.common.print.PrintOpts
     */
    public Print getPrintImpl() {
        return this.printImpl;
    }

    /**
     * @param print desired print implementation, may not be null
     * @see org.globus.workspace.common.print.Print
     * @see org.globus.workspace.common.print.PrintOpts
     */
    public void setPrintImpl(Print print) {
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        this.printImpl = print;
    }

    /**
     * @return minimum number of suffix characters for group prefix creations
     */
    public int getGroupSuffixMinCharacters() {
        return this.groupSuffixMinCharacters;
    }

    /**
     * @param numChars minimum number of suffix characters for group prefix creations
     * @throws IllegalArgumentException numChars may not be less than one
     */
    public void setGroupSuffixMinCharacters(int numChars) {
        if (numChars < 1) {
            throw new IllegalArgumentException(
                            "numChars may not be less than one");
        }
        this.groupSuffixMinCharacters = numChars;
    }

    /**
     * If generating workspace EPR files, what is the enclosing element?
     * @return element name
     */
    public String getGeneratedEprElementName() {
        return this.generatedEprElementName;
    }

    /**
     * @param elementName enclosing element name for generated workspace EPR files
     * @throws IllegalArgumentException elementName may not be null
     */
    public void setGeneratedEprElementName(String elementName) {
        if (elementName == null) {
            throw new IllegalArgumentException("elementName may not be null");
        }
        this.generatedEprElementName = elementName;
    }

    /**
     * If generating group EPR files, what is the enclosing element?
     * @return element name
     */
    public String getGeneratedGroupEprElementName() {
        return this.generatedGroupEprElementName;
    }

    /**
     * @param elementName enclosing element name for generated group EPR files
     * @throws IllegalArgumentException elementName may not be null
     */
    public void setGeneratedGroupEprElementName(String elementName) {
        if (elementName == null) {
            throw new IllegalArgumentException("elementName may not be null");
        }
        this.generatedGroupEprElementName = elementName;
    }

    /**
     * If generating ensemble EPR files, what is the enclosing element?
     * @return element name
     */
    public String getGeneratedEnsembleEprElementName() {
        return this.generatedEnsembleEprElementName;
    }

    /**
     * @param elementName enclosing element name for generated ensemble EPR files
     * @throws IllegalArgumentException elementName may not be null
     */
    public void setGeneratedEnsembleEprElementName(String elementName) {
        if (elementName == null) {
            throw new IllegalArgumentException("elementName may not be null");
        }
        this.generatedEnsembleEprElementName = elementName;
    }

    /**
     * If generating context EPR files, what is the enclosing element?
     * @return element name
     */
    public String getGeneratedContextEprElementName() {
        return this.generatedContextEprElementName;
    }

    /**
     * @param elementName enclosing element name for generated context EPR files
     * @throws IllegalArgumentException elementName may not be null
     */
    public void setGeneratedContextEprElementName(String elementName) {
        if (elementName == null) {
            throw new IllegalArgumentException("elementName may not be null");
        }
        this.generatedContextEprElementName = elementName;
    }

    /**
     * If generating context broker contact files, what is the enclosing
     * element?
     * @return element name
     */
    public String getGeneratedContextBrokerContactElementName() {
        return generatedContextBrokerContactElementName;
    }

    /**
     * @param elementName enclosing element name for generated context EPR files
     * @throws IllegalArgumentException elementName may not be null
     */
    public void setGeneratedContextBrokerContactElementName(String elementName) {
        this.generatedContextBrokerContactElementName = elementName;
    }
}
