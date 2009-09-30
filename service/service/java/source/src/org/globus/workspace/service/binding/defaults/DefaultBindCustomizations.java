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

package org.globus.workspace.service.binding.defaults;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nimbustools.api.repr.CustomizationRequest;
import org.nimbustools.api.services.rm.CreationException;
import org.nimbustools.api.services.rm.ResourceRequestDeniedException;
import org.globus.workspace.PathConfigs;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.service.binding.BindCustomizations;
import org.globus.workspace.service.binding.vm.CustomizationNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.safehaus.uuid.UUIDGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DefaultBindCustomizations implements BindCustomizations {
    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
            LogFactory.getLog(DefaultBindCustomizations.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected final PathConfigs paths;
    protected final UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    // Paths are validated once as working.  If the values are changed after
    // that, the failures will be seen as FileNotFoundException etc. instead of
    // nice messages.  Changing values during deployment is not supported.
    protected boolean workingPathConfigs;
    protected String localDirPath;

    
    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public DefaultBindCustomizations(PathConfigs paths) {
        if (paths == null) {
            throw new IllegalArgumentException("paths may not be null");
        }
        this.paths = paths;
    }
    

    // -------------------------------------------------------------------------
    // IoC INIT METHOD
    // -------------------------------------------------------------------------

    public synchronized void validate() throws Exception {
        if (!this.workingPathConfigs) {
            this.validatePaths();
            // see note at variable declaration, races not expected:
            this.workingPathConfigs = true;
        }

        if (this.localDirPath == null) {
            final String srcDir = this.paths.getLocalTempDirPath();
            final File srcDirFile = new File(srcDir);
            // using getPath() not getAbsolutePath() because the basePath does
            // not need to be absolute, just the right base path for the env.
            this.localDirPath = srcDirFile.getPath();
        }
    }
    
    

    // -------------------------------------------------------------------------
    // implements BindCustomizations
    // -------------------------------------------------------------------------

    public void consume(VirtualMachine vm,
                        CustomizationRequest[] reqs)
            throws CreationException, ResourceRequestDeniedException {

        if (reqs == null || reqs.length == 0) {
            return; // *** EARLY RETURN ***
        }

        try {
            this.validate();
            this.bindFileWrites(vm, reqs);
        } catch (Exception e) {
            throw new CreationException(e.getMessage(), e);
        }
    }

    public CustomizationNeed newCustomizationNeedImpl(String srcContent,
                                                      String dstPath)
            throws WorkspaceException {

        try {
            this.validate();
            return this.newNeed(srcContent, dstPath);
        } catch (Exception e) {
            throw new WorkspaceException(e.getMessage(), e);
        }
    }

    
    // -------------------------------------------------------------------------
    // VALIDATE-IMPL
    // -------------------------------------------------------------------------

    protected void validatePaths() throws Exception {

        final String srcDir = this.paths.getLocalTempDirPath();
        final String destDir = this.paths.getBackendTempDirPath();

        _validatePathsNull(srcDir, "localTempDirPath");
        _validatePathsNull(destDir, "backendTempDirPath");

        final File srcDirFile = new File(srcDir);
        _validatePathsExists(srcDirFile, "localTempDirPath", srcDir);
        _validatePathsDir(srcDirFile, "localTempDirPath", srcDir);

        // can't validate anything about backendTempDir, it's on another node
    }

    private static void _validatePathsNull(String path, String name)

            throws Exception {

        if (path == null) {
            throw new Exception("Customization needed but " + name +
                    " configuration is missing");
        }
    }

    private static void _validatePathsExists(File dir, String name, String value)

            throws Exception {

        if (!dir.exists()) {
            throw new Exception("Customization needed but " + name +
                    " configuration does not exist: '" + value + "'");
        }
    }

    private static void _validatePathsDir(File dir, String name, String value)

            throws Exception {

        if (!dir.isDirectory()) {
            throw new Exception("Customization needed but " + name +
                    " configuration is not for a directory: '" + value + "'");
        }
    }

    
    // -------------------------------------------------------------------------
    // CONSUME-IMPL
    // -------------------------------------------------------------------------

    protected void bindFileWrites(VirtualMachine vm,
                                  CustomizationRequest[] filewrites)
            throws Exception {

        for (int i = 0; i < filewrites.length; i++) {

            final String src = filewrites[i].getContent();
            final String dstPath = filewrites[i].getPathOnVM();
            vm.addCustomizationNeed(this.newNeed(src, dstPath));
        }

    }

    protected CustomizationNeed newNeed(String src,
                                        String dstPath) throws Exception {

        // already checked, this is for object extenders
        if (this.localDirPath == null) {
            throw new CreationException("cannot perform customization " +
                    "without local tmp directory");
        }

        final int maxContentLength = 40960; // embedded policy for now
        
        if (src == null) {
                throw new Exception("customize content may not be missing");
        }
        if (dstPath == null) {
            throw new Exception("customize path-on-VM may not be missing");
        }

        if (src.length() > maxContentLength) {
            logger.error(
                    "customize source string too long: " + src.length());
            final String err = "customization task source string is too long";
            throw new Exception(err);
        }

        final String srcPath = this.newSrcPath();

        final CustomizationNeed need;
        try {
            need = new CustomizationNeed(srcPath, dstPath);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            final String err = "customization task request is invalid " +
                    "(the one with given destination path: '" + dstPath + "')";
            throw new Exception(err);
        }

        final File f = new File(this.localDirPath, srcPath);

        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            fw.write(src);
            fw.flush();
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.error(e.getMessage(), e);
            } else {
                logger.error(e.getMessage());
            }
            final String err =
                    "customization task failed unexpectedly: " + e.getMessage();
            throw new Exception(err);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Successfully added customization task. " +
                    "srcPath = '" + srcPath +
                    "', dstPath = '" + dstPath + "'");
        }

        return need;
    }

    protected String newSrcPath() throws Exception {

        // already checked, this is for object extenders
        if (this.localDirPath == null) {
            throw new Exception("cannot perform customization " +
                    "without local tmp directory");
        }

        final String uuid = this.uuidGen.generateRandomBasedUUID().toString();

        if (uuid.length() != 36) {
            final String err = "customization task failed unexpectedly: UUID " +
                    "is not 36 characters?";
            throw new Exception(err);
        }

        final String srcPath = uuid.substring(0,23);

        final File f = new File(this.localDirPath, srcPath);

        // highly unlikely:
        if (f.exists()) {
            final String uuid2 =
                    this.uuidGen.generateRandomBasedUUID().toString();

            if (uuid2.length() != 36) {
                final String err = "customization task failed unexpectedly: UUID " +
                    "is not 36 characters?";
                throw new Exception(err);
            }
            final String srcPath2 = uuid2.substring(0,23);

            final File f2 = new File(this.localDirPath, srcPath2);

            // highly unlikely:
            if (f2.exists()) {
                final String err = "customization task failed unexpectedly: " +
                        "cannot obtain unique task name";
                throw new Exception(err);
            } else {
                return srcPath2;
            }
        } else {
            return srcPath;
        }
    }
}
