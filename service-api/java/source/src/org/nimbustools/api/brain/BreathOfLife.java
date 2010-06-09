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

package org.nimbustools.api.brain;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <p><img src="http://www.nimbusproject.org/images/sh.png" /> Use to instantiate
 * an application context (via Spring IoC).</p>
 *
 * <p>This is the only concrete class in the exposed API (besides exceptions),
 * every implementation can be configured at runtime. Default implementations
 * live in the {@link org.nimbustools.api.defaults} package.</p>
 *
 * <p>The implementations in the {@link org.nimbustools.api.defaults.services}
 * package do nothing besides logging.  The workspace project supplies
 * implementations of these separately.  When distributed together in an
 * installable form, the word "default" might take on a different meaning.
 * Defaults here mean the defaults for this when it is in standalone mode.</p>
 *
 * <p>The 'real' implementations are probably also internally modularized via
 * an inversion of control container (can piggyback through this one), but this
 * API system proper stands alone as a bridge between the messaging layers and
 * implementations of VM manager and other services -- allowing everything to
 * be decoupled and independently implemented.</p>
 *
 * <p>In any messaging implementation (hosted by an application container), you
 * will be able to find a call to
 * {@link BreathOfLife#breathe(String xmlCtxPath)}
 * and all further coupling is via ModuleLocator and the representative object
 * interfaces (the {@link org.nimbustools.api.repr} package).</p>
 *
 * <p>In any services implementation, you will NOT be able to find a dependency
 * on anything beyond ("above") this API.</p>
 *
 * <p>The point of this is to bridge between remote messaging syntax (and even
 * <b>semantics</b>) and a consistent service implementation (which can also
 * be replaced/altered at will).</p>
 *
 * <p><b>See developer documentation</b>.</p>
 *
 * @see ModuleLocator
 * @see org.nimbustools.api.services.rm.Manager
 * @see org.nimbustools.api.services.ctx.ContextBroker
 * @see org.nimbustools.api.repr.ReprFactory
 */
public class BreathOfLife {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final String DEFAULT_CONFIG =
            "org/nimbustools/api/defaults/defaults.xml";

    private static final String ID_MODULE_LOCATOR =
            "nimbus-brain.ModuleLocator";


    // -------------------------------------------------------------------------
    // IMPL
    // -------------------------------------------------------------------------

    /**
     * Begin with embedded defaults.xml file
     * This will do nothing practical for you unless you're developing/debugging
     * @return locator
     * @see ModuleLocator
     */
    public ModuleLocator breathe() {
        return this.breathe(null);
    }

    /**
     * Begin with a given Spring configuration file.
     *
     * @param xmlCtxPath path to XML context/configuration file
     * @return locator
     * @see NimbusFileSystemXmlApplicationContext
     * @see ModuleLocator
     */
    public ModuleLocator breathe(String xmlCtxPath) {

        String name = "Nimbus master configuration";
        if (xmlCtxPath == null) {
            name = name + " - internal default @ " + DEFAULT_CONFIG;
        } else {
            name = name + " @ " + xmlCtxPath;
        }

        Logging.debug("Initializing " + name);

        final ApplicationContext ctx;
        if (xmlCtxPath == null) {
            ctx = new ClassPathXmlApplicationContext(DEFAULT_CONFIG);
        } else {
            ctx = new NimbusFileSystemXmlApplicationContext(xmlCtxPath);
        }
        
        final ModuleLocator locator =
                (ModuleLocator) ctx.getBean(ID_MODULE_LOCATOR);

        Logging.debug("Initialized " + name);

        /*
          final String appCTX = "APPCXT: '" + ctx.getDisplayName() + "'";
          final Manager manager = locator.getManager();
          this.reportLog("Manager", appCTX, manager.report());
          final ReprFactory repr = locator.getReprFactory();
          this.reportLog("ReprFactory", appCTX, repr.report());
        */

        return locator;
    }

    protected void reportLog(String name, String appCTX, String report) {

        if (report == null) {
            return;
        }

        final StringBuffer buf = new StringBuffer(1024);
        buf.append(appCTX).append("\n");

        final String header =
                "-----------------==== " + name + " ====--------------------";
        buf.append(header).append("\n");
        
        buf.append(report).append("\n");

        for (int i = 0; i < header.length(); i++) {
            buf.append("-");
        }
        
        Logging.debug(buf.toString());
    }
}
