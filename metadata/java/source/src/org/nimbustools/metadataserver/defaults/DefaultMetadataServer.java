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

package org.nimbustools.metadataserver.defaults;

import org.nimbustools.api.services.rm.Manager;
import org.nimbustools.api.services.metadata.MetadataServer;
import org.nimbustools.api.services.metadata.MetadataServerException;
import org.nimbustools.api.services.metadata.MetadataServerUnauthorizedException;
import org.nimbustools.api.repr.vm.VM;
import org.nimbustools.api.repr.vm.VMFile;
import org.nimbustools.api.repr.vm.NIC;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.safehaus.uuid.UUIDGenerator;

import java.net.URL;
import java.net.URI;
import java.util.ArrayList;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * See: http://docs.amazonwebservices.com/AWSEC2/2008-08-08/DeveloperGuide/index.html?AESDG-chapter-instancedata.html
 */
@SuppressWarnings("unchecked")
public class DefaultMetadataServer implements MetadataServer {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
           LogFactory.getLog(MetadataRequestHandler.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    protected Manager manager;
    protected String customizationPath;
    protected String listenSocket = null;
    protected boolean enabled;
    protected boolean listening;
    protected HTTPListener listener;
    protected String[] localNets;
    protected String[] publicNets;

    protected final Cache cache;
    
    private final UUIDGenerator uuidGen;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    public DefaultMetadataServer(CacheManager cacheManager) {
        if (cacheManager == null) {
            throw new IllegalArgumentException("cacheManager may not be null");
        }

        this.cache = cacheManager.getCache("metadataServerCache");
        if (this.cache == null) {
            throw new IllegalArgumentException(
                    "cacheManager does not provide 'metadataServerCache'");
        }
        
        this.uuidGen = UUIDGenerator.getInstance();
    }

    
    // -------------------------------------------------------------------------
    // PROPERTIES
    // -------------------------------------------------------------------------

    public void setCustomizationPath(String path) {
        this.customizationPath = path;
    }

    public void setListenSocket(String listenSocket) {
        this.listenSocket = listenSocket;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public void setLocalNets(String localNetsStr) {
        if (localNetsStr == null || localNetsStr.trim().length() == 0) {
            this.localNets = null;
            return;
        }

        final String[] parts = localNetsStr.split(",");
        final ArrayList partsArray = new ArrayList(parts.length);
        for (String part : parts) {
            if (part != null && part.trim().length() > 0) {
                partsArray.add(part.trim());
            }
        }
        this.localNets = (String[]) partsArray.toArray(new String[0]);
    }

    public void setPublicNets(String publicNetsStr) {
        if (publicNetsStr == null || publicNetsStr.trim().length() == 0) {
            this.publicNets = null;
            return;
        }
        
        final String[] parts = publicNetsStr.split(",");
        final ArrayList partsArray = new ArrayList(parts.length);
        for (String part : parts) {
            if (part != null && part.trim().length() > 0) {
                partsArray.add(part.trim());
            }
        }
        this.publicNets = (String[]) partsArray.toArray(new String[0]);
    }

    // -------------------------------------------------------------------------
    // implements MetadataServer
    // -------------------------------------------------------------------------

    public String getResponse(String target, String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        // If developers try to access directly , these checks could trigger.
        // Possible use cases in future?  If so, the intialize method will
        // need to be changed to bring the server up even if the HTTP server
        // fails to start up for whatever reason.
        
        if (!this.enabled) {
            throw new MetadataServerException("metadata server not enabled");
        }
        if (!this.listening) {
            throw new MetadataServerException(
                    "metadata server did not initialize correctly, sorry.");
        }

        logger.debug("considering target: '" + target + "', client: " +
                         remoteAddress);

        try {
            return this.dispatch(target, remoteAddress);
        } catch (MetadataServerUnauthorizedException e) {
            logger.error("UNAUTHORIZED call to metadata server, message: " +
                    e.getMessage());
            throw e;
        } catch (MetadataServerException e) {
            logger.error("Problem with metadata server, message: " +
                    e.getMessage() + " ||| Client visible message: " +
                    e.getClientVisibleMessage());
            throw e;
        } catch (Throwable t) {
            final String err = "Unhandled problem dispatching metadata " +
                        "request: " + t.getMessage();
            if (logger.isDebugEnabled()) {
                logger.error(err, t);
            } else {
                logger.error(err);
            }
            throw new MetadataServerException(err, t);
        }
    }

    public String getCustomizationPath() {
        return this.customizationPath;
    }

    public synchronized String getContactURL() {
        
        if (!this.listening) {
            logger.warn("contact URL requested but not listening?");
            return null;
        }

        if (this.listener == null) {
            logger.warn("listening but no listener??");
            return null;
        }

        final URL url = this.listener.getURL();
        if (url == null) {
            return null;
        } else {
            return url.toString();
        }
    }

    public synchronized boolean isListening() {
        return this.listening;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    protected synchronized void initServerAndListen() throws Exception {

        if (!this.enabled) {
            logger.info("metadata server not enabled");
            return;
        }

        if (this.listening) {
            throw new Exception("already listening");
        }

        if (this.listenSocket == null ||
                this.listenSocket.trim().length() == 0) {
            throw new Exception("metadata server enabled but there " +
                    "is no 'contact.socket' configuration");
        }

        final MetadataRequestHandler handler = new MetadataRequestHandler(this);
        this.listener = new HTTPListener(this.listenSocket.trim());
        this.listener.initServer(handler);
        this.listener.start();
        this.listening = true;
    }


    // -------------------------------------------------------------------------
    // DISPATCH
    // -------------------------------------------------------------------------

    protected String dispatch(String target, String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        if (target.equals("/")) {
            return this.topIndex();
        }

        final String subtarget;
        
        if (target.startsWith("/1.0/")) {
            subtarget = target.substring(5);
        } else if (target.startsWith("/2007-01-19/") ||
                   target.startsWith("/2007-03-01/") ||
                   target.startsWith("/2008-08-08/")) {
            subtarget = target.substring(12);
        } else {
            final String err = "Unrecognized path: '" + target + "'.  " +
                    "Expected first subdirectory in path to be " +
                    "'1.0' or '2007-01-19' or '2007-03-01' or '2008-08-08'.";
            throw new MetadataServerException(err, err);
        }
        
        return this.dispatch2(target, subtarget, remoteAddress);
    }

    protected String dispatch2(String target,
                               String subtarget,
                               String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        if (subtarget.startsWith("meta-data/")) {
            return dispatchMetaData(target,
                                    subtarget.substring(10),
                                    remoteAddress);
        } else if (subtarget.startsWith("user-data")) {
            return dispatchUserData(target,
                                    subtarget.substring(9),
                                    remoteAddress);
        } else {
            final String err = "Unrecognized URL: '" + target + "'.  " +
                    "Expected second subdirectory in path to be either " +
                    "'meta-data/' or 'user-data'.";
            throw new MetadataServerException(err, err);
        }
    }

    protected String dispatchUserData(String target,
                                      String subsubtarget,
                                      String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        return this.userData(remoteAddress);
    }
    
    protected String dispatchMetaData(String target,
                                      String subsubtarget,
                                      String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        if (subsubtarget.startsWith("ami-id")) {
            return this.amiID(remoteAddress);
        } else if (subsubtarget.startsWith("ami-launch-index")) {
            return this.amiLaunchIndex(remoteAddress);
        } else if (subsubtarget.startsWith("local-hostname")) {
            return this.localHostname(remoteAddress);
        } else if (subsubtarget.startsWith("local-ipv4")) {
            return this.localIPV4(remoteAddress);
        } else if (subsubtarget.startsWith("public-hostname")) {
            return this.publicHostname(remoteAddress);
        } else if (subsubtarget.startsWith("public-ipv4")) {
            return this.publicIPV4(remoteAddress);
        } else {
            throw unimplemented(target, remoteAddress);
        }
    }

    private static String getUserErr(String method) {
        final String USER_UNIMPL_METHOD = "You tried to use ";
        final String USER_UNIMPL_METHOD2 = ", but that is not " +
            "implemented yet.  Usually these can be implemented quickly, " +
            "inquire on the mailing list.";
        return USER_UNIMPL_METHOD + method + USER_UNIMPL_METHOD2;
    }

    private static String getLogErr(String method, String remoteAddress) {
        return "USER (@ ip: '" + remoteAddress +
                "') TRIED UNIMPLEMENTED: " + method;
    }

    private static MetadataServerException unimplemented(String method, String remoteAddress) {
        return new MetadataServerException(getLogErr(method, remoteAddress),
                                           getUserErr(method));
    }


    // -------------------------------------------------------------------------
    // ASSOCIATE IP ADDRESS
    // -------------------------------------------------------------------------


    /**
     * The usual pattern for access is a burst of request at each VM's boot.
     *
     * In the future this lookup should lock on IP address and not whole
     * metadata server instance.
     *
     * @param ip remote client's IP
     * @return VM instance, never null
     * @throws MetadataServerException problem
     * @throws MetadataServerUnauthorizedException could not associate VM
     */
    public synchronized VM getCachedAndValidatedVM(String ip)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        final Element el = this.cache.get(ip);
        final VM vm;
        if (el == null) {
            vm = this.getVM(ip);
            this.validateVM(vm, ip);
            this.cache.put(new Element(ip, vm));
        } else {
            vm = (VM) el.getObjectValue();
        }
        return vm;
    }

    /**
     * @param ipAddress remote client's IP
     * @return VM instance, never null
     * @throws MetadataServerException problem
     * @throws MetadataServerUnauthorizedException could not associate VM
     */
    public VM getValidatedVM(String ipAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {
        
        final VM vm = this.getVM(ipAddress);
        this.validateVM(vm, ipAddress);
        return vm;
    }

    /**
     * @param ipAddress remote client's IP
     * @return VM instance, never null
     * @throws MetadataServerException problem
     * @throws MetadataServerUnauthorizedException could not associate VM
     */
    public VM getVM(String ipAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        final VM[] vms;
        try {
            vms = this.manager.getAllByIPAddress(ipAddress);
        } catch (Throwable t) {

            final String uuid =
                    this.uuidGen.generateRandomBasedUUID().toString();

            String userError = "There has been an internal server error.  " +
                    "Contact the administrator with this lookup key for more " +
                    "information: '" + uuid + "'";
            String fullError = "Problem querying manager for IP '" +
                    ipAddress + "', ERROR UUID='" + uuid + "': " + t.getMessage();
            throw new MetadataServerException(fullError, userError, t);
        }

        if (vms == null || vms.length == 0) {
            final String err =
                    "Could not associate IP with a VM: " + ipAddress;
            throw new MetadataServerUnauthorizedException(err);
        } else if (vms.length > 1) {
            final String err =
                    "IP is associated with more than one VM! IP: " + ipAddress;
            throw new MetadataServerException(err, err);
        }

        return vms[0];
    }

    protected void validateVM(VM vm,
                              String ip) throws MetadataServerException {
        if (vm == null) {
            throw this.validationProblem("manager implementation is invalid, " +
                    "returned a null VM", ip);
        }

        VMFile[] vmFiles = vm.getVMFiles();
        if (vmFiles == null || vmFiles.length == 0) {
            throw this.validationProblem("manager implementation is invalid, " +
                    "returned a VM with null or zero vmFiles", ip);
        }

        boolean foundRootfile = false;
        for (VMFile file: vmFiles) {
            
            if (file == null) {
                throw this.validationProblem("manager implementation is " +
                        "invalid, returned a VM with a null value in " +
                        "vmFiles array", ip);
            }
            
            final URI uri = file.getURI();
            if (uri == null && file.getBlankSpaceSize() < 1) {
                throw this.validationProblem("manager implementation is " +
                        "invalid, returned a VM with a null URI value in " +
                        "vmFiles array", ip);
            }
            if (file.isRootFile()) {
                if (uri == null) {
                    throw this.validationProblem("manager implementation is " +
                            "invalid, returned a VM with a null URI value " +
                            "for root disk in vmFiles array", ip);
                }
                foundRootfile = true;
            }
        }

        if (!foundRootfile) {
            throw this.validationProblem("manager implementation is " +
                            "invalid, returned a VM with no " +
                            "root disk file in vmFiles array", ip);
        }
    }

    private MetadataServerException validationProblem(String issue,
                                                      String ipAddress) {
        final String uuid =
                    this.uuidGen.generateRandomBasedUUID().toString();

        String userError = "There has been an internal server error.  " +
                "Contact the administrator with this lookup key for more " +
                "information: '" + uuid + "'";
        String fullError = "Problem querying manager for IP '" +
                ipAddress + "', ERROR UUID='" + uuid + "': " + issue;
        return new MetadataServerException(fullError, userError);
    }


    // -------------------------------------------------------------------------
    // 1.0 API (FLAT)
    // -------------------------------------------------------------------------

    /**
     * "/"
     * 
     * @return APIs supported.  Sending back lies so that recent tooling works.
     */
    protected String topIndex() {
        
        /* NOT actually supporting these later protocols but providing them
           as a passthrough to 1.0 */
        return "1.0\n" +
               "2007-01-19\n" +
               "2007-03-01\n" +
               "2008-08-08\n";
    }

    /*
     * "/user-data"
     */
    protected String userData(String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        VM vm = this.getCachedAndValidatedVM(remoteAddress);
        final String data = vm.getMdUserData();
        if (data == null) {
            return "";
        } else {
            return data;
        }
    }

    /*
     * "/meta-data/ami-id"
     */
    protected String amiID(String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        VM vm = this.getCachedAndValidatedVM(remoteAddress);
        VMFile[] files = vm.getVMFiles();
        for (VMFile file: files) {
            if (file.isRootFile()) {
                return this.justFilename(file.getURI().toASCIIString());
            }
        }
        throw this.validationProblem("Should be impossible because we used " +
                                        "get*ValidatedVM", remoteAddress);
    }

    protected String justFilename(String imageURI) {
        if (imageURI == null) {
            throw new IllegalArgumentException("imageURI may not be null");
        }
        final int idx = imageURI.lastIndexOf('/');
        if (idx < 0) {
            return imageURI;
        } else {
            return imageURI.substring(idx+1);
        }
    }

    /*
     * "/meta-data/ami-launch-index"
     */
    protected String amiLaunchIndex(String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {
        
        final VM vm = this.getCachedAndValidatedVM(remoteAddress);
        return Integer.toString(vm.getLaunchIndex());
    }

    /*
     * "/meta-data/local-ipv4"
     */
    protected String localIPV4(String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        final VM vm = this.getCachedAndValidatedVM(remoteAddress);
        final NIC nic = this.getLocalNIC(vm);
        if (nic != null) {
            final String ip = nic.getIpAddress();
            if (ip != null) {
                return ip.trim();
            }
        }
        return "";
    }

    /*
     * "/meta-data/public-ipv4"
     */
    protected String publicIPV4(String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        final VM vm = this.getCachedAndValidatedVM(remoteAddress);
        final NIC nic = this.getPublicNIC(vm);
        if (nic != null) {
            final String ip = nic.getIpAddress();
            if (ip != null) {
                return ip.trim();
            }
        }
        return "";
    }

    /*
     * "/meta-data/local-hostname"
     */
    protected String localHostname(String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        final VM vm = this.getCachedAndValidatedVM(remoteAddress);
        final NIC nic = this.getLocalNIC(vm);
        if (nic != null) {
            final String hostname = nic.getHostname();
            if (hostname != null) {
                return hostname.trim();
            }
        }
        return "";
    }

    /*
     * "/meta-data/public-hostname"
     */
    protected String publicHostname(String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {
        
        final VM vm = this.getCachedAndValidatedVM(remoteAddress);
        final NIC nic = this.getPublicNIC(vm);
        if (nic != null) {
            final String hostname = nic.getHostname();
            if (hostname != null) {
                return hostname.trim();
            }
        }
        return "";
    }

    private NIC getLocalNIC(VM vm) {
        return findNIC(this.localNets, vm);
    }

    private NIC getPublicNIC(VM vm) {
        return findNIC(this.publicNets, vm);
    }

    private static NIC findNIC(String[] networkNames, VM vm) {
        if (networkNames == null || networkNames.length == 0) {
            return null;
        }
        final NIC[] nics = vm.getNics();
        if (nics == null || nics.length == 0) {
            return null;
        }
        for (String netname : networkNames) {
            final NIC nic = getParticularNetworkNIC(netname, nics);
            if (nic != null) {
                return nic;
            }
        }
        return null;
    }

    private static NIC getParticularNetworkNIC(String netname, NIC[] nics) {
        if (netname == null || netname.trim().length() == 0) {
            return null;
        }
        for (NIC nic : nics) {
            if (nic != null) {
                final String oneName = nic.getNetworkName();
                if (oneName != null
                        && oneName.trim().equals(netname.trim())) {
                    return nic;
                }
            }
        }
        return null;
    }
}
