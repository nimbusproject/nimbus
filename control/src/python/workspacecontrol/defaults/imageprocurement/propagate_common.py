from commands import getstatusoutput
import os
import shutil
import stat
import string
import sys
import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
from workspacecontrol.main import ACTIONS
import workspacecontrol.main.wc_args as wc_args
from propagate_adapter import PropagationAdapter
from workspacecontrol.defaults.imageprocurement.propagate_cache import WSCCacheObj


# keywords for 'adapters' dict as well as the expected URL schemes
PROP_ADAPTER_CP = "cp"
PROP_ADAPTER_SCP = "scp"
PROP_ADAPTER_GUC = "gsiftp"
PROP_ADAPTER_HDFS = "hdfs"
PROP_ADAPTER_HTTP = "http"
PROP_ADAPTER_HTTPS = "https"
PROP_ADAPTER_LANTORRENT = "lantorrent"

class DefaultImageProcurement:
    """ImageProcurement is the wcmodule responsible for making files accessible
    to the current VMM node before the deployment.  As well as processing files
    after deployment.  The typical pattern is propagation before running and
    unpropagation afterwards.  If there is an immediate-destroy event, this
    module will destroy all local files.
    
    That is what we assume most implementations would do, they don't necessarily
    have to.  Perhaps in some future implementation, this module will archive
    destroyed VMs to some storage array for auditing.
    
    """
    
    zope.interface.implements(workspacecontrol.api.modules.IImageProcurement)
    
    def __init__(self, params, common):
        self.p = params
        self.c = common
        self.localdir = None
        self.securelocaldir = None
        self.blankspacedir = None
        self.adapters = None # dict: {keyword: instance of PropagationAdapter}
        
            
    # --------------------------------------------------------------------------
    # validate(), from ImageProcurement interface
    # --------------------------------------------------------------------------
    
    def validate(self):
        self._validate_localdir()
        self.securelocaldir = self._validate_securelocaldir("securelocaldir")
        
        self.blankcreate_path = None
        self._validate_blankspacecreate()
        self.blankspacedir = self._validate_blankspacedir("blankspacedir")
 
        self.adapters = {}

        cp_path = self.p.get_conf_or_none("propagation", "cp")
        if cp_path:
            try:
                import propagate_cp
                self.adapters[PROP_ADAPTER_CP] = propagate_cp.cp_propadapter(self.p, self.c)
            except Exception, ex:
                msg = "CP configuration present (propagation->cp) but cannot load a suitable CP implementation in the code | %s" % (str(ex))
                self.c.log.exception(msg + ": ")
                raise InvalidConfig(msg)

        
        scp_path = self.p.get_conf_or_none("propagation", "scp")
        if scp_path:
            try:
                import propagate_scp
                self.adapters[PROP_ADAPTER_SCP] = propagate_scp.propadapter(self.p, self.c)
            except:
                msg = "SCP configuration present (propagation->scp) but cannot load a suitable SCP implementation in the code"
                self.c.log.exception(msg + ": ")
                raise InvalidConfig(msg)
            
        guc_path = self.p.get_conf_or_none("propagation", "guc")
        if guc_path:
            try:
                import propagate_guc
                self.adapters[PROP_ADAPTER_GUC] = propagate_guc.propadapter(self.p, self.c)
            except:
                msg = "GridFTP configuration present (propagation->guc) but cannot load a suitable GridFTP implementation in the code"
                self.c.log.exception(msg + ": ")
                raise InvalidConfig(msg)
        
        hdfs_path = self.p.get_conf_or_none("propagation", "hdfs")
        if hdfs_path:
            try:
                import propagate_hdfs
                self.adapters[PROP_ADAPTER_HDFS] = propagate_hdfs.propadapter(self.p, self.c)
            except:
                msg = "HDFS configuration present (propagation->hdfs) but cannot load a suitable HDFS implementation in the code"
                self.c.log.exception(msg + ": ")
                raise InvalidConfig(msg)    
        
        http_enabled = self.p.get_conf_or_none("propagation", "http")
        if http_enabled and http_enabled.strip().lower() == "true":
            import propagate_http
            self.adapters[PROP_ADAPTER_HTTP] = propagate_http.propadapter(self.p, self.c)

        https_enabled = self.p.get_conf_or_none("propagation", "https")
        if https_enabled and https_enabled.strip().lower() == "true":
            import propagate_https
            self.adapters[PROP_ADAPTER_HTTPS] = propagate_https.propadapter(self.p, self.c)

        if len(self.adapters) == 0:
            self.c.log.warn("There are no propagation adapters configured, propagation is disabled")
            return


        lt_enabled = self.p.get_conf_or_none("propagation", "lantorrent")
        if lt_enabled and lt_enabled.strip().lower() == "true":
            try:
                import propagate_lantorrent
                self.adapters[PROP_ADAPTER_LANTORRENT] = propagate_lantorrent.LantorrentPropadapter(self.p, self.c)
            except Exception, ex:
                msg = "lantorrent configuration present (propagation->lantorrent) but cannot load a suitable lantorrent implementation in the code: " + str(ex)
                self.c.log.exception(msg + ": ")
                raise InvalidConfig(msg)
            
        for keyword in self.adapters.keys():
            adapter = self.adapters[keyword]
            adapter.validate()
            
            
    # --------------------------------------------------------------------------
    # validate() IMPL
    # --------------------------------------------------------------------------
    
    def _validate_localdir(self):
        localdir = self.p.get_conf_or_none("images", "localdir")
        if not localdir:
            raise InvalidConfig("no images->localdir configuration")
        
        if not os.path.isabs(localdir):
            localdir = self.c.resolve_var_dir(localdir)
        
        if not os.path.isdir(localdir):
            raise InvalidConfig("localdir is not a directory: %s" % localdir)
            
        self.localdir = localdir
        self.c.log.debug("local image directory (localdir): %s" % self.localdir)
        
    def _validate_securelocaldir(self, confname):
        securelocaldir = self.p.get_conf_or_none("images", confname)
        if not securelocaldir:
            raise InvalidConfig("no images->%s configuration" % (confname))
            
        if not os.path.isabs(securelocaldir):
            securelocaldir = self.c.resolve_var_dir(securelocaldir)
        
        if not os.path.exists(securelocaldir):
            self.c.log.warn("%s is configured, but '%s' does not"
                       " exist on the filesystem, attemping to create "
                       " it" % (confname, securelocaldir))
            try:
                os.mkdir(securelocaldir)
                os.chmod(securelocaldir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                raise InvalidConfig("Problem creating %s: %s: %s" 
                           % (confname, str(exceptname), str(sys.exc_value)))

            self.c.log.warn("created secure localdir '%s'" % securelocaldir)

        x = os.access(securelocaldir, os.W_OK | os.X_OK | os.R_OK)
        if x:
            self.c.log.debug("'%s' exists on the filesystem and is "
                       "rwx-able" % securelocaldir)
        else:
            raise InvalidConfig("'%s' exists on the filesystem but is not rwx" 
                       % securelocaldir)
            
        self.c.log.debug("secure image directory (per-instance images): %s" % self.securelocaldir)
        return securelocaldir

    def _validate_blankspacedir(self, confname):
        blankspacedir = self.p.get_conf_or_none("images", confname)
        if not blankspacedir:
            raise InvalidConfig("no images->%s configuration" % (confname))
            # Alternatively
            #blankspacedir = self.securelocaldir

        if not os.path.isabs(blankspacedir):
            blankspacedir = self.c.resolve_var_dir(blankspacedir)

        if not os.path.exists(blankspacedir):
            self.c.log.warn("%s is configured, but '%s' does not"
                       " exist on the filesystem, attemping to create "
                       " it" % (confname, blankspacedir))
            try:
                os.mkdir(blankspacedir)
                os.chmod(blankspacedir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__
                except AttributeError:
                    exceptname = exception_type
                raise InvalidConfig("Problem creating %s: %s: %s"
                           % (confname, str(exceptname), str(sys.exc_value)))
            self.c.log.warn("created blankspace dir '%s'" % blankspacedir)

        x = os.access(blankspacedir, os.W_OK | os.X_OK | os.R_OK)
        if x:
            self.c.log.debug("'%s' exists on the filesystem and is "
                       "rwx-able" % blankspacedir)
        else:
            raise InvalidConfig("'%s' exists on the filesystem but is not rwx"
                       % blankspacedir)
        self.c.log.debug("blankspace partition directory (per-instance blankspace): %s" % self.blankspacedir)
        return blankspacedir
    
    def _validate_blankspacecreate(self):
        blankcreate_path = self.p.get_conf_or_none("images", "blankcreate")
        if not blankcreate_path:
            self.c.log.warn("No images->blankcreate configuration, blankspace creation is disabled")
            return
        
        if not os.path.isabs(blankcreate_path):
            blankcreate_path = self.c.resolve_libexec_dir(blankcreate_path)
        
        if not os.access(blankcreate_path, os.X_OK):
            raise InvalidConfig("images->blankcreate configuration '%s' is not executable" % blankcreate_path)
        
        self.blankcreate_path = blankcreate_path
        
    
    # --------------------------------------------------------------------------
    # lengthy_obtain(), from ImageProcurement interface
    # --------------------------------------------------------------------------
    
    def lengthy_obtain(self):
        """Given a set of deployment parameters, return True if it is determined
        that the 'obtain' operation should be called in a daemonized mode with
        an asynchronous 'propagated' message being sent afterwards.
        """
        
        l_files = self._process_image_args()
        return self._is_propagation_needed(l_files)
     
    # --------------------------------------------------------------------------
    # lengthy_shutdown(), from ImageProcurement interface
    # --------------------------------------------------------------------------
    
    def lengthy_shutdown(self):
        """Given a set of parameters, return True if it is determined that the
        'process_after_shutdown' operation should be called in a daemonized mode
        with an asynchronous 'unpropagated' message being sent afterwards.
        """
        
        l_files = self._process_image_args(unprop=True)
        return self._is_unpropagation_needed(l_files)
        
    # --------------------------------------------------------------------------
    # obtain(), from ImageProcurement interface
    # --------------------------------------------------------------------------
    
    def obtain(self):
        """Given a set of deployment parameters, bring this VMM node into a
        state where it can operate on a set of local files.
        
        Return an instance of LocalFileSet
        """
        
        action = self.p.get_arg_or_none(wc_args.ACTION)
        
        if action in [ACTIONS.CREATE, ACTIONS.PROPAGATE]:
            
            self._ensure_instance_dir()
            
            l_files = self._process_image_args()
            if self._is_propagation_needed(l_files):
                self._propagate(l_files)
                
        elif action in [ACTIONS.PRINTXML]:
            
            l_files = self._process_image_args()
                
        elif action in [ACTIONS.UNPROPAGATE]:
            
            self._ensure_instance_dir()
            
            l_files = self._process_image_args(unprop=True)
            
            # client requesting new names
            unproptargets_arg = self.p.get_arg_or_none(wc_args.UNPROPTARGETS)
            if unproptargets_arg:
                self._validate_new_unproptargets(l_files, unproptargets_arg)
            
        elif action in [ACTIONS.REMOVE]:
            
            l_files = []
            
        else:
            raise ProgrammingError("do not know how to handle obtain request for action '%s'" % action)
            
        if action == ACTIONS.CREATE:
            if self._is_blankspace_needed(l_files):
                self._ensure_blankspace_dir()
                self._blankspace(l_files)
            
        local_file_set_cls = self.c.get_class_by_keyword("LocalFileSet")
        local_file_set = local_file_set_cls(l_files)
        return local_file_set
    
    # --------------------------------------------------------------------------
    # process_after_shutdown(), from ImageProcurement interface
    # --------------------------------------------------------------------------
    
    def process_after_shutdown(self, local_file_set):
        """Do any necessary work after a VM shuts down and is being prepared
        for teardown.  Will not be called if there is an immediate-destroy
        event because that needs no unpropagation.
        
        local_file_set -- instance of LocalFileSet
        
        Return nothing, local_file_set will be modified as necessary.
        """
        
        l_files = local_file_set.flist()
        if self._is_unpropagation_needed(l_files):
            self._unpropagate(l_files)
            
        self._destroy_instance_dir()
        self._destroy_blankspace_dir()
        
        
    # --------------------------------------------------------------------------
    # process_after_destroy(), from ImageProcurement interface
    # --------------------------------------------------------------------------
    
    def process_after_destroy(self, local_file_set):
        """Do any necessary work after a VM is forcibly shut down.  This is the
        alternative teardown hook to "process_after_shutdown()" and is called if
        there is an immediate-destroy event vs. a shutdown + unpropagate 
        pattern.
        
        local_file_set -- instance of LocalFileSet
        
        Return nothing, local_file_set will be modified as necessary.
        """
        
        self._destroy_instance_dir()
        self._destroy_blankspace_dir()


    # --------------------------------------------------------------------------
    # IMPLs for common query functionality
    # --------------------------------------------------------------------------
        
    def _is_propagation_needed(self, l_files):
        propneeded = 0
        for lf in l_files:
            if lf._propagate_needed:
                propneeded += 1
                
        if self.c.trace:
            self.c.log.debug("%d propagation tasks" % propneeded)
            
        return propneeded > 0
        
    def _is_blankspace_needed(self, l_files):
        blankneeded = 0
        for lf in l_files:
            if lf._blankspace > 0:
                blankneeded += 1
                
        if self.c.trace:
            self.c.log.debug("%d blankspace tasks" % blankneeded)
            
        return blankneeded > 0
        
    def _is_unpropagation_needed(self, l_files):
        unpropneeded = 0
        for lf in l_files:
            if lf._unpropagate_needed:
                unpropneeded += 1
                
        if self.c.trace:
            self.c.log.debug("%d unpropagation tasks" % unpropneeded)
            
        return unpropneeded > 0
        
    def _derive_instance_dir(self):
        vm_name = self.p.get_arg_or_none(wc_args.NAME)
        if not vm_name:
            raise InvalidInput("The %s argument is required." % wc_args.NAME.long_syntax)
        vm_securedir = os.path.join(self.securelocaldir, vm_name)
        return vm_securedir

    def _derive_blankspace_dir(self):
        vm_name = self.p.get_arg_or_none(sc_args.NAME)
        if not vm_name:
            raise InvalidInput("The %s argument is required." % wc_args.NAME.long_syntax)
        vm_blankspacedir = os.path.join(self.blankspacedir, vm_name)
        return vm_blankspacedir
        
    # --------------------------------------------------------------------------
    # IMPLs for actual actions the module takes
    # --------------------------------------------------------------------------
        
    def _ensure_instance_dir(self):
        vmdir = self._derive_instance_dir()
        if os.path.exists(vmdir):
            return
        self.c.log.info("Creating VM's unique directory: %s" % vmdir)
        os.mkdir(vmdir)
        os.chmod(vmdir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
        self.c.log.debug("created %s" % vmdir)
        
    def _destroy_instance_dir(self):
        vmdir = self._derive_instance_dir()
        if not os.path.exists(vmdir):
            return
        self.c.log.debug("Destroying %s" % vmdir)
        shutil.rmtree(vmdir)
        self.c.log.info("Destroyed VM's unique directory: %s" % vmdir)

    def _ensure_blankspace_dir(self):
        blankspacedir = self._derive_blankspacedir()
        if os.path.exists(vmdir):
            return
        self.c.log.info("Creating VM's blankspace directory: %s" % blankspacedir)
        os.mkdir(blankspacedir)
        os.chmod(vmdir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
        self.c.log.debug("created %s" % blankspacedir)

    def _destroy_blankspace_dir(self):
        blankspacedir = self._derive_blankspace_dir()
        if not os.path.exists(blankspacedir):
            return
        self.c.log.debug("Destroying %s" % blankspacedir)
        shutuil.rmtree(blankspacedir)
        self.c.log.info("Destroyed VM's unique blankspace directory: %s" % blankspacedir)
        
    def _propagate(self, l_files):
        cache_key = self.p.get_arg_or_none(wc_args.CACHECKSUM)
        try:
            cache_path = self._validate_securelocaldir("cachedir")
        except Exception, ex:
            self.c.log.warn("failure validating cache path | %s" % (str(ex)))
            cache_path = None
        cache = None
        if cache_key and cache_path:
            try:
                max_size = self.p.get_conf_or_none("images", "cache_size")
                if max_size is not None:
                    max_size = int(max_size)
                lockfile = self.p.get_conf_or_none("images", "cachelockfile")
                if lockfile is None:
                    lockfile = os.path.join(cache_path, "lock")
                cache = WSCCacheObj(cache_path, lockfile, max_size=max_size, log=self.c.log)
            except ValueError:
                raise InvalidConfig("The images->cache_size configuration is not an integer: '%s'" % max_size)
            except Exception, ex:
                self.c.log.warn("failed to create the cache at %s | %s" % (cache_path, str(ex)))
                
        for l_file in l_files:
            
            if not l_file._propagate_needed:
                continue

            if cache:
                try:
                    self.c.log.debug("cache lookup %s" % (cache_key))
                    rc = cache.lookup(cache_key, l_file.path)
                    if rc:
                        self.c.log.info("The file was found in the cache and copied to %s" % (l_file.path))
                        return
                    self.c.log.info("cache miss for %s" % (cache_key))
                except Exception, ex:
                    self.c.log.warn("an exception occured while performing a cache lookup on %s | %s" % (cache_key, str(ex)))
                
            for keyword in self.adapters.keys():
                schemestring = keyword + "://"
                scheme_len = len(schemestring)
                target_scheme = l_file._propagation_source[:scheme_len]
                    
                if target_scheme == schemestring:
                    adapter = self.adapters[keyword]
                    
                    self.c.log.debug("propagating from '%s' to '%s'" % (l_file._propagation_source, l_file.path))
                    
                    if self.c.dryrun:
                        self.c.log.debug("dryrun, not propagating")
                        return
                    
                    adapter.propagate(l_file._propagation_source, l_file.path)
                    
                    if not os.path.exists(l_file.path):
                        raise UnexpectedError("propagated from '%s' to '%s' but the file does not exist" % (l_file._propagation_source, l_file.path))
                    
                    if cache:
                        try:
                            self.c.log.debug("adding %s" % (cache_key))
                            cache.add(l_file.path, cache_key)
                        except Exception, ex:
                            self.c.log.warn("an exception occured while adding a file to the cache %s %s | %s" % (cache_key, l_file.path, str(ex)))

                    return
        
    def _unpropagate(self, l_files):
        for l_file in l_files:
            
            if not l_file._unpropagate_needed:
                continue
            for keyword in self.adapters.keys():
                schemestring = keyword + "://"
                scheme_len = len(schemestring)
                target_scheme = l_file._unpropagation_target[:scheme_len]
                    
                if target_scheme == schemestring:
                    adapter = self.adapters[keyword]
                    
                    self.c.log.debug("unpropagating from '%s' to '%s'" % (l_file.path, l_file._unpropagation_target))
                    
                    if self.c.dryrun:
                        self.c.log.debug("dryrun, not unpropagating")
                        return
                    
                    adapter.unpropagate(l_file.path, l_file._unpropagation_target)

                    return
        
    def _blankspace(self, l_files):
        for l_file in l_files:
            
            if l_file._blankspace == 0:
                continue

            cmd = "%s %s %s" % (self.blankcreate_path, l_file.blankspacepath, l_file._blankspace)
            self.c.log.debug("running '%s'" % cmd)
            if self.c.dryrun:
                self.c.log.debug("(dryrun, not running that)")
                return

            try:
                ret,output = getstatusoutput(cmd)
            except:
                raise

            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                raise EnvironmentProblem(errmsg)
            else:
                self.c.log.info("blank partition of size %dMB created at '%s'" % (l_file._blankspace, l_file.blankspacepath))

    # --------------------------------------------------------------------------
    # _process_image_args(), IMPL of common validation/preparation functionality
    # --------------------------------------------------------------------------
        
    def _process_image_args(self, unprop=False):
        """Return list of files to process.  Validate in the process.
        """
        
        action = self.p.get_arg_or_none(wc_args.ACTION)
        if not action:
            raise InvalidInput("No action, cannot process images argument")
        
        images = self.p.get_arg_or_none(wc_args.IMAGES)
        if not images:
            raise InvalidInput("The %s action requires the %s argument" % (action, wc_args.IMAGES.long_syntax))
        
        
        # ---------------------------------------------------------------
        
        # Parse images string to group of images

        # The given input string might be quoted to escape semicolons for
        # certain delivery methods (e.g., sh over ssh) and some methods may
        # not strip quotes (e.g., localhost, direct exe invocation).
        # So strip extra quotes if present, but don't expect quotes.
        
        if images[0] == "'":
            images = images[1:]
        # (there is a pathological case where input was only a single quote)
        if images and images[-1] == "'":
            images = images[:-1]

        imgstrs = images.split(';;')

        unproptargets_arg = self.p.get_arg_or_none(wc_args.UNPROPTARGETS)
        new_unprops = None
        if unprop and unproptargets_arg:
            new_unprops = self._get_new_unprop_targets(imgstrs, unproptargets_arg) 

        i = 0
        l_files = []
        for imgstr in imgstrs:
            i += 1
            logstr = "image #%d" % i
            if unprop and new_unprops:
                try:
                    unprop_path = new_unprops[i-1]
                except IndexError:
                    unprop_path = None
                lf = self._one_imagestr(logstr, imgstr, unprop, unprop_path)
            else:
                lf = self._one_imagestr(logstr, imgstr, unprop)
            self.c.log.debug("%s is valid" % logstr)
            
            # convention in the past is that first is rootdisk, a new arg
            # scheme might come about but for now we must match the old
            # workspace-control syntax
            if i == 1:
                lf.rootdisk = True
            l_files.append(lf)

        self.c.log.debug("found %d valid partitions/HD images" % len(l_files))
        
        if action != ACTIONS.CREATE and action != ACTIONS.PRINTXML:
            # only creation requires mountpoints which is checked below
            return l_files
            
        # ---------------------------------------------------------------
        
        imagemounts = self.p.get_arg_or_none(wc_args.IMAGEMOUNTS)
        if not imagemounts:
            raise InvalidInput("The %s action requires the %s argument" % (action, wc_args.IMAGEMOUNTS.long_syntax))

        self.c.log.debug("analyzing mountpoints '%s'" % imagemounts)

        # Parse imagemounts string to group of mountpoints

        # The given input string might be quoted to escape semicolons for
        # certain delivery methods (e.g., sh over ssh) and some methods may
        # not strip quotes (e.g., localhost, direct exe invocation).
        # So strip extra quotes if present, but don't expect quotes.

        if imagemounts[0] == "'":
            imagemounts = imagemounts[1:]
        # (there is a pathological case where input was only a single quote)
        if imagemounts and imagemounts[-1] == "'":
            imagemounts = imagemounts[:-1]

        imagemountlist = []
        
        mountstrs = imagemounts.split(';;')
        for mountstr in mountstrs:
            if mountstr[:5] == "/dev/":
                prev = mountstr
                mountstr = mountstr[5:]
                self.c.log.debug("stripped '/dev' from '%s', now = '%s'" % (prev, mountstr))
            imagemountlist.append(mountstr)

        if len(imagemountlist) < 1:
            raise InvalidInput("The %s action requires a valid %s argument, received '%s'" % (action, wc_args.IMAGEMOUNTS.long_syntax, imagemounts))
            
        nummts = len(imagemountlist)
        numparts = len(l_files)
        
        if nummts != numparts:
            raise InvalidInput("fatal, number of mountpoints (%d) does not match number of valid partitions/HD images (%d)" % (nummts, numparts))

        for i,l_file in enumerate(l_files):
            l_file.mountpoint = imagemountlist[i]

        return l_files
        
        
    # --------------------------------------------------------------------------
    # _one_imagestr() supports _process_image_args() above
    # --------------------------------------------------------------------------
       
    def _one_imagestr(self, logstr, imgstr, unprop=False, new_unprop=None):
        """Convert given imagestr from arguments into LocalFile list.
        """
        lf_cls = self.c.get_class_by_keyword("LocalFile")
        lf = lf_cls()
        
        # Introduce 'hidden' fields into the LocalFile instance for use by
        # this module only.
        lf._blankspace = 0
        lf._propagate_needed = False
        lf._propagation_source = None
        lf._unpropagate_needed = False
        lf._unpropagation_target = None
        
        # These are the fields the LocalFile interface expects:
        lf.path = None
        lf.blankspacepath = None
        lf.mountpoint = None # not set in this method
        lf.rootdisk = False # not set in this method
        lf.editable = True
        lf.read_write = True
        lf.physical = False
        
        # if 'ro' is not a field, assumed to be 'rw'
        parts = imgstr.split(';')
        if len(parts) > 1:
            if parts[1] == 'ro':
                lf.read_write = False
        imgstr = parts[0]
        
        self.c.log.debug("Examining file (partition/HD image): '%s'" % imgstr)
        
        # ---------------------------------------------------------------
        
        if ".." in imgstr:
            raise InvalidInput("'%s' contains '..', that is not allowed" % imgstr)
        
        # ---------------------------------------------------------------
        
        prop = not unprop # peering suspiciously
        
        if prop and imgstr[0] == "/":
            
            lf.path = imgstr
            self.c.log.debug("partition/HD is absolute path")
            
            if self.securelocaldir in lf.path:
                raise InvalidInput("Attempt to specify absolute path ('%s') that lies in secure image directory.  This is not allowed because that secure image directory is managed by workspace control, it is for per-instance files that the program retrieves." % lf.path)
            
            if not os.path.exists(lf.path):
                raise InvalidInput("File specified by absolute path ('%s') but it does not exist." % lf.path)
            
        # ---------------------------------------------------------------
            
        elif prop and imgstr[:8] == "file:///":
            lf.path = imgstr[8:]
            self.c.log.debug("partition/HD is absolute path w/ file://")
            
            if self.securelocaldir in lf.path:
                raise InvalidInput("Attempt to specify absolute path ('%s') that lies in secure image directory.  This is not allowed because that secure image directory is managed by workspace control, it is for per-instance files that the program retrieves." % lf.path)
                
            if not os.path.exists(lf.path):
                raise InvalidInput("File specified by absolute path ('%s') but it does not exist." % lf.path)
                
        # ---------------------------------------------------------------
                
        elif prop and imgstr[:7] == "file://":
            original = imgstr[7:]
            self.c.log.debug("partition/HD is relative path w/ file://")
            
            securedir_try = self._derive_instance_dir()
            securedir_try = os.path.join(securedir_try, original)

            # We need to check that the image hasn't previously been unzipped
            gz_parts = original.rsplit(".gz",1)
            securedir_lessgz_try = self._derive_instance_dir()
            securedir_lessgz_try = os.path.join(securedir_lessgz_try, gz_parts[0])
            
            localdir_try = os.path.join(self.localdir, original)
            
            # important: try securedir first, it takes precedence
            if os.path.exists(securedir_try):
                lf.path = securedir_try
            elif os.path.exists(securedir_lessgz_try):
                lf.path = securedir_lessgz_try
            elif os.path.exists(localdir_try):
                lf.path = localdir_try
            else:
                raise InvalidInput("File specified by relative path ('%s' could resolve to either '%s', '%s', or '%s') but it does not exist" % (original, securedir_try, securedir_lessgz_try, localdir_try))
            
        # ---------------------------------------------------------------
                
        elif imgstr[:14] == "blankcreate://":
            self._one_imagestr_blankcreate(lf, imgstr)
            
        # ---------------------------------------------------------------
            
        else:
            self._one_imagestr_propagation(lf, imgstr, unprop, new_unprop)
            
        # ---------------------------------------------------------------
            
        if prop and not lf.path:
            raise InvalidInput("image specified is not an absolute path and uses an unknown URL scheme, or perhaps no scheme at all: '%s'" % imgstr)
            
        if unprop and not lf._unpropagate_needed:
            raise InvalidInput("Received images argument for an unpropagation request that makes no sense: %s" % imgstr)
            
        if not lf.path:
            raise InvalidInput("image specified is not an absolute path and uses an unknown URL scheme, or perhaps no scheme at all: '%s'" % imgstr)
            
        return lf


    # --------------------------------------------------------------------------
    # support methods for _one_imagestr()
    # --------------------------------------------------------------------------
       
    def _one_imagestr_blankcreate(self, lf, imgstr):
    
        if not self.blankcreate_path:
            raise InvalidInput("blankspace creation requested ('%s') but that functionality is not enabled" % imgstr)
    
        blank_filename = imgstr[14:]
        if len(blank_filename) > 0:
            if blank_filename[0] == "/":
                raise InvalidInput("blank partition creation can only happen in secure workspace-specific local directory (absolute path used: '%s')" % blank_filename)
        else:
            raise InvalidInput("blank partition has no name ('%s' given)" % imgstr)
    
        try:
            size = blank_filename.split("-size-")[1]
            lf._blankspace = int(size)
        except:
            raise InvalidInput("blank partition name is expected to have embedded size")
    
        lf.blankspacepath = self._derive_blankspace_dir()
        lf.blankspacepath = os.path.join(lf.blankspacepath, blank_filename)
        
        if os.path.exists(lf.blankspacepath):
            raise InvalidInput("blank partition is going to be created but the file exists already: '%s'" % lf.blankspacepath)
        
        if self.c.trace:
            self.c.log.debug("partition of size %dM is going to be created (blankcreate) at '%s'" % (lf._blankspace, lf.blankspacepath))

    def _one_imagestr_propagation(self, lf, imgstr, unprop, new_unprop):

        if unprop and new_unprop:
            validate = new_unprop
        else:
            validate = imgstr

        for keyword in self.adapters.keys():
            schemestring = keyword + "://"
            schemestring_len = len(schemestring)
            self.c.log.debug("schemestring: %s" % schemestring)
            
            # note: relative paths for local files require "file://"
            if len(validate) <= schemestring_len:
                raise InvalidInput("image specified is not an absolute path and uses an unknown URL scheme, or perhaps no scheme at all: %s" % imgstr)
                
            if validate[:schemestring_len] == schemestring:
                self.c.log.debug("partition/HD is specified w/ %s" % schemestring)
                
                adapter = self.adapters[keyword]
                if unprop:
                    adapter.validate_unpropagate_target(validate)
                else:
                    adapter.validate_propagate_source(imgstr)
                
                if unprop:
                    lf._unpropagate_needed = True
                    lf._unpropagation_target = imgstr
                else:
                    lf._propagate_needed = True
                    lf._propagation_source = imgstr
                
                fnameindex = string.rfind(imgstr, '/')
                local_filename = imgstr[fnameindex+1:]


                # lf.path is propagation target while in the module ... if this
                # object is returned by the module, it is assumed to exist
                lf.path = self._derive_instance_dir()
                lf.path = os.path.join(lf.path, local_filename)
                    
                pathexists = os.path.exists(lf.path)
                if pathexists and lf._propagate_needed:
                    raise InvalidInput("file is going to be transferred to this host but the target exists already: '%s'" % lf.path)
                    
                # You would think you can make the following commented-out check
                # here, but you can't.  Things that are leaving the host can't
                # be checked for ahead of time before the transfer because
                # filenames can be changed (consider *.gz).
                ###if not pathexists and lf._unpropagate_needed:
                ###    raise InvalidInput("file is going to be transferred from 
                ###this host but it does not exist: '%s'" % lf.path)
                
                return
 
    def _get_new_unprop_targets(self, original_images, unproptargets_arg):
        
        images = list(original_images)

        if unproptargets_arg[0] == "'":
            unproptargets_arg = unproptargets_arg[1:]
        # (there is a pathological case where input was only a single quote)
        if unproptargets_arg and unproptargets_arg[-1] == "'":
            unproptargets_arg = unproptargets_arg[:-1]

        unproptargets = unproptargets_arg.split(';;')

        for i, image in enumerate(images):
            try:
                old_image = images[i]
                images[i] = unproptargets[i]
            except IndexError:
                # No unprop target with to match
                break
            self.c.log.debug("old unpropagation target '%s' is now '%s'" % (old_image, images[i]))

        return images

               
    def _validate_new_unproptargets(self, l_files, unproptargets_arg):
        
        # The given input string might be quoted to escape semicolons for
        # certain delivery methods (e.g., sh over ssh) and some methods may
        # not strip quotes (e.g., localhost, direct exe invocation).
        # So strip extra quotes if present, but don't expect quotes.
        
        if unproptargets_arg[0] == "'":
            unproptargets_arg = unproptargets_arg[1:]
        # (there is a pathological case where input was only a single quote)
        if unproptargets_arg and unproptargets_arg[-1] == "'":
            unproptargets_arg = unproptargets_arg[:-1]

        unproptargets = unproptargets_arg.split(';;')
        
        argname = wc_args.UNPROPTARGETS.long_syntax
        
        if len(unproptargets) == 0:
            raise InvalidInput("received %s argument but there are no targets" % argname)
        
        num_needs = 0
        for lf in l_files:
            if lf._unpropagate_needed:
                num_needs += 1
        
        if num_needs == 0:
            raise InvalidInput("received %s argument but there are no unpropagations scheduled" % argname)
            
        if len(unproptargets) != num_needs:
            raise InvalidInput("received %s argument but cannot match unpropagations scheduled with targets.  There are %d unprop-targets and %d unpropagation needs" % (argname, len(unproptargets), num_needs))

        # note how the order is assumed to match -- this is a precarious side
        # effect of the commandline based syntax
        counter = -1
        for lf in l_files:
            if lf._unpropagate_needed:
                counter += 1
                old = lf._unpropagation_target
                lf._unpropagation_target = unproptargets[counter]
                self.c.log.debug("old unpropagation target '%s' is now '%s'" % (old, lf._unpropagation_target))
         

def url_parse(url):
    parts = url.split('://', 1)
    scheme = parts[0]
    rest = parts[1]

    parts = rest.split('@', 1)
    if len(parts) == 1:
        user = None
        password = None
    else:
        rest = parts[1]
        u_parts = parts[0].split(':')
        user = parts[0]
        if len(u_parts) == 1:
            password = None
        else:
            password = parts[1]

    parts = rest.split('/', 1)
    contact_string = parts[0]
    if len(parts) > 1:
        path = '/' + parts[1]
    else:
        path = None

    parts = contact_string.split(':')
    hostname = parts[0]
    if len(parts) == 1:
        port = None
    else:
        port = int(parts[1])

    return (scheme, user, password, hostname, port, path)

