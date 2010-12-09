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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.util.*;

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

    protected static final String CONTACT_SOCKET_PREFIX = "contact.socket";

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

    protected Properties properties;
    
    private final UUIDGenerator uuidGen;
    private Set<URL> listenSockets;
    private Map<String, URL> networkContacts;
    private URL defaultContact;


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

    public void setProperties(Properties properties) {
        this.properties = properties;
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

    public synchronized String getContactURL(NIC[] nics) {
        
        if (!this.listening) {
            logger.warn("contact URL requested but not listening?");
            return null;
        }

        if (this.listener == null) {
            logger.warn("listening but no listener??");
            return null;
        }

        // search for the first NIC with a matching contact address
        if (!this.networkContacts.isEmpty() && nics != null) {
            for (NIC nic : nics) {
                final String network = nic.getNetworkName();
                if (network == null) {
                    continue;
                }
                final URL url = this.networkContacts.get(network);
                if (url != null) {
                    return url.toString();
                }
            }
        }

        if (defaultContact != null) {
            return defaultContact.toString();
        }

        return null;
    }

    public synchronized boolean isListening() {
        return this.listening;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public synchronized void initServerAndListen() throws Exception {

        if (!this.enabled) {
            logger.info("metadata server not enabled");
            return;
        }

        initialize();
        start();
    }

    public synchronized void initialize() throws Exception {

        if (!this.enabled) {
            logger.info("metadata server not enabled");
            return;
        }

        if (this.listening) {
            throw new Exception("already listening");
        }

        this.intakeProperties();

        final MetadataRequestHandler handler = new MetadataRequestHandler(this);
        this.listener = new HTTPListener(this.listenSockets);
        this.listener.initServer(handler);
    }

    public synchronized void start() throws Exception {
        this.listener.start();
        this.listening = true;
    }

    public synchronized void stop() throws Exception {
        if (!this.listening) {
            return;
        }

        this.listener.stop();
        this.listening = false;
    }

    private void intakeProperties() throws Exception {
        if (this.properties == null) {
            throw new Exception("properties not provided, don't know where to listen");
        }

        URL defaultContact = null;
        final Map<String, URL> networkContacts = new HashMap();

        for (Object propNameObject : properties.keySet()) {
            final String propName = (String) propNameObject;

            if (propName.startsWith(CONTACT_SOCKET_PREFIX)) {
                String value = this.properties.getProperty(propName);

                if (value == null) {
                    continue;
                }
                value = value.trim();
                if (value.length() == 0) {
                    continue;
                }

                // hardcoding http here on purpose, to make it obvious that https is
                // not supported if someone tries to put more than a host+port in the
                // configuration
                final URL url;
                try {
                    url = new URL("http://" + value);
                } catch (MalformedURLException e) {
                    throw new Exception("Invalid host:port value in "+ propName +
                            " property: " + e.getMessage());
                }

                if (propName.length() == CONTACT_SOCKET_PREFIX.length()) {
                    defaultContact = url;
                } else if (propName.charAt(CONTACT_SOCKET_PREFIX.length()) == '.') {
                    final String network = propName.substring(CONTACT_SOCKET_PREFIX.length()+1);

                    if (network.length() == 0) {
                        throw new Exception("Missing network name in property: " + propName);
                    }
                    networkContacts.put(network, url);
                }
            }
        }

        final Set<URL> listenSockets;
        if (defaultContact != null && defaultContact.getHost().equals("0.0.0.0")) {
            if (networkContacts.isEmpty()) {
                throw new Exception("if the metadata server listens on all interfaces (0.0.0.0), " +
                        "you must specify contact addresses to be given to VMs. For example, \""+
                CONTACT_SOCKET_PREFIX + ".public\"");
            }
            listenSockets = java.util.Collections.singleton(defaultContact);

        } else {
            listenSockets = new HashSet<URL>(networkContacts.values());
            if (defaultContact != null) {
                listenSockets.add(defaultContact);
            }
        }
        this.listenSockets = listenSockets;
        this.defaultContact = defaultContact;
        this.networkContacts = networkContacts;
    }

    // -------------------------------------------------------------------------
    // DISPATCH
    // -------------------------------------------------------------------------

    protected final String[] API_VERSIONS =
            {"latest", "1.0", "2007-01-19", "2007-03-01", "2008-08-08"};

    protected String dispatch(String target, String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

        if (target.equals("/")) {
            return this.topIndex();
        }

        String subtarget = null;

        for (String version : API_VERSIONS) {
            version = "/" + version;
            if (target.startsWith(version)) {
                if (target.length() == version.length()) {
                    subtarget = "";
                    break;
                } else if (target.charAt(version.length()) == '/') {
                    subtarget = target.substring(version.length() + 1);
                    break;
                }
            }
        }
        
        if (subtarget == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Unrecognized path: '").append(target).
                    append("'. Expected first subdirectory in path to be one of: ");
            for (int i = 0; i < API_VERSIONS.length; i++) {
                String version = API_VERSIONS[i];
                if (i == API_VERSIONS.length-1 && API_VERSIONS.length > 1) {
                    sb.append(", or ");
                } else if (i > 0) {
                    sb.append(", ");
                }
                sb.append("'").append(version).append("'");
            }
            final String err = sb.toString();
            throw new MetadataServerException(err, err);
        }
        
        return this.dispatch2(target, subtarget, remoteAddress);
    }

    protected String dispatch2(String target,
                               String subtarget,
                               String remoteAddress)
            throws MetadataServerException,
                   MetadataServerUnauthorizedException {

    if (subtarget.equals("") || subtarget.equals("/")) {
        return "meta-data\nuser-data\n";
        
    } else if (subtarget.startsWith("meta-data/")) {
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

        if (subsubtarget.equals("") || subsubtarget.equals("/")) {
            return "ami-id\nami-launch-index\nlocal-hostname\nlocal-ipv4\n" +
                    "public-hostname\npublic-ipv4\n";

        } else if (subsubtarget.startsWith("ami-id")) {
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
        StringBuilder sb = new StringBuilder();
        for (String version : API_VERSIONS) {
            sb.append(version).append("\n");
        }
        return sb.toString();
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
