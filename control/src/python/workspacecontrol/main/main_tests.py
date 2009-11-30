import optparse
import os
import sys

import wc_optparse
import workspacecontrol.api.modules as modules
import workspacecontrol.api.objects as objects
from workspacecontrol.main import get_class_by_keyword
from workspacecontrol.api.exceptions import *
from main_tests_util import mockconfigs, get_pc, realconfigs

# -----------------------------------------------------------------------------

def test_getallconfigs():
    """Test if the configuration setup is sane"""
    platform_cls = get_class_by_keyword("Platform", allconfigs=mockconfigs())
    assert modules.IPlatform.implementedBy(platform_cls)
    
def test_logging():
    """Test if the logging setup is sane"""
    p,c = get_pc(None, mockconfigs())
    c.log.debug("test_logging()")
    
def test_cmdline_parameters():
    """Test cmdline parameter intake error"""
    parser = wc_optparse.parsersetup()
    exc_thrown = False
    tmp = sys.stderr
    sys.stderr = sys.stdout
    try:
        # requires argument, should fail:
        (opts, args) = parser.parse_args(["--action"])
    except SystemExit:
        exc_thrown = True
    assert exc_thrown
    sys.stderr = tmp
    
def test_cmdline_parameters2():
    """Test cmdline parameter intake success"""
    parser = wc_optparse.parsersetup()
    (opts, args) = parser.parse_args(["-a", "create"])
    assert opts.action != None
    
def test_cmdline_parameters3():
    """Test cmdline parameter intake booleans"""
    parser = wc_optparse.parsersetup()
    (opts, args) = parser.parse_args(["-a", "create", "--startpaused"])
    assert opts.startpaused == True
    
    (opts, args) = parser.parse_args(["-a", "create"])
    assert opts.startpaused == False
    
def test_mock_procurement():
    """Test the procurement test adapter interaction"""
    
    p,c = get_pc(None, mockconfigs())
    c.log.debug("test_mock_procurement()")
    
    procure_cls = c.get_class_by_keyword("ImageProcurement")
    procure = procure_cls(p, c)
    procure.validate()
    local_file_set = procure.obtain()
    
    for lf in local_file_set.flist():
        assert lf.path
        assert lf.mountpoint
        assert os.access(lf.path, os.F_OK)
        
    procure.process_after_destroy(local_file_set)
    
def test_mock_network_lease():
    """Test the network lease test adapter interaction"""
    
    p,c = get_pc(None, mockconfigs())
    c.log.debug("test_mock_network_lease()")
    
    netlease_cls = c.get_class_by_keyword("NetworkLease")
    netlease = netlease_cls(p, c)
    netlease.validate()
    nic = netlease.obtain("public")
    
    assert nic.network == "public"
    
    nicset_cls = c.get_class_by_keyword("NICSet")
    nic_set = nicset_cls([nic])
    
    netlease.release(nic_set)
    
def test_kernel1():
    """Test the kernel adapter basics"""
    
    p,c = get_pc(None, mockconfigs())
    c.log.debug("test_kernel1()")
    
    kernelpr_cls = c.get_class_by_keyword("KernelProcurement")
    kernelprocure = kernelpr_cls(p,c)
    kernelprocure.validate()
    
def test_kernel2():
    """Test the kernel adapter obtain() with mock images"""
    
    p,c = get_pc(None, mockconfigs())
    c.log.debug("test_kernel2()")
    
    kernelpr_cls = c.get_class_by_keyword("KernelProcurement")
    kernelprocure = kernelpr_cls(p,c)
    kernelprocure.validate()
    
    procure_cls = c.get_class_by_keyword("ImageProcurement")
    procure = procure_cls(p, c)
    procure.validate()
    local_file_set = procure.obtain()
    
    kernel = kernelprocure.kernel_files(local_file_set)
    assert kernel
    
    # use providedBy on instances (implementedBy is for classes)
    assert objects.IKernel.providedBy(kernel)
    
    
def test_mock_create():
    """Test the create command with the libvirt mock adapter"""
    
    handle = "wrksp-393"
    createargs = ["--action", "create", 
                  "--name", handle,
                  "--memory", "256",
                 ]
    parser = wc_optparse.parsersetup()
    (opts, args) = parser.parse_args(createargs)
    
    p,c = get_pc(opts, mockconfigs())
    c.log.debug("test_mock_create()")
    
    procure_cls = c.get_class_by_keyword("ImageProcurement")
    procure = procure_cls(p, c)
    procure.validate()
    local_file_set = procure.obtain()
    
    kernelpr_cls = c.get_class_by_keyword("KernelProcurement")
    kernelprocure = kernelpr_cls(p,c)
    kernelprocure.validate()
    kernel = kernelprocure.kernel_files(local_file_set)
    
    netlease_cls = c.get_class_by_keyword("NetworkLease")
    netlease = netlease_cls(p, c)
    netlease.validate()
    nic = netlease.obtain("public")
    nicset_cls = c.get_class_by_keyword("NICSet")
    nic_set = nicset_cls([nic])
    
    platform_cls = c.get_class_by_keyword("Platform")
    platform = platform_cls(p, c)
    platform.validate()
    platform.create(local_file_set, nic_set, kernel)

    running_vm = platform.info(handle)
    assert running_vm
    
    assert running_vm.wchandle == handle
    assert running_vm.maxmem == 256
    assert running_vm.curmem == 256
    assert running_vm.running
    assert not running_vm.blocked
    assert not running_vm.paused
    assert not running_vm.shutting_down
    assert not running_vm.shutoff
    assert not running_vm.crashed
    
    platform.destroy(running_vm)
    
    tmp = sys.stderr
    sys.stderr = sys.stdout
    running_vm = platform.info(handle)
    assert not running_vm
    sys.stderr = tmp
    