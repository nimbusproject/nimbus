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

package org.globus.workspace.client.modes;

import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.StubConfigurator;
import org.globus.workspace.client.AllArguments;
import org.globus.workspace.common.client.CommonPrint;

/**
 * Base class for all client modes
 */
public abstract class Mode {

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------
    
    protected final Print pr;
    protected final AllArguments args;
    protected final StubConfigurator stubConf;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public Mode(Print print,
                AllArguments arguments,
                StubConfigurator stubConfigurator) {
        
        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("arguments may not be null");
        }
        if (stubConfigurator == null) {
            throw new IllegalArgumentException("stubConfigurator may not be null");
        }

        this.pr = print;
        this.args = arguments;
        this.stubConf = stubConfigurator;
    }

    
    // -------------------------------------------------------------------------
    // ENTRY
    // -------------------------------------------------------------------------

    public void validateOptions() throws ParameterProblem {

        final String sectionTitle = this.name().toUpperCase() + ": VALIDATION";
        CommonPrint.printDebugSection(this.pr, sectionTitle);

        this.validateOptionsImpl();

        CommonPrint.printDebugSectionEnd(this.pr, sectionTitle);
    }
    
    public void run() throws ParameterProblem,
                             ExecutionProblem,
                             ExitNow {

        final String sectionTitle = this.name().toUpperCase() + ": RUNNING";
        CommonPrint.printDebugSection(this.pr, sectionTitle);

        this.runImpl();

        CommonPrint.printDebugSectionEnd(this.pr, sectionTitle);
    }

    
    // -------------------------------------------------------------------------
    // ABSTRACT
    // -------------------------------------------------------------------------

    public abstract String name();

    protected abstract void validateOptionsImpl() throws ParameterProblem;

    protected abstract void runImpl() throws ParameterProblem,
                                             ExecutionProblem,
                                             ExitNow;

}
