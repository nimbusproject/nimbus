#!/usr/bin/env python

# ================================ LICENSE ====================================
lic="""
Copyright 1999-2008 University of Chicago

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy
of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

For more information see: http://www.nimbusproject.org
"""


# ================================ TASKDEF ====================================


# ignore this part, edit below
TASKS=[]
class Task:
    def __init__(self, ip=None, network=None, broadcast=None,
                       veth=None, vif=None, bridge=None, original=None,
                       physnic=None, formac=None):
        self.ipaddress = ip # 1.2.3.4
        self.network = network # "1.2.3.0/24"
        self.broadcast = broadcast # "1.2.3.255"
        self.vethname = veth # "veth1"
        self.vifname = vif # "vif0.1"
        self.bridgename = bridge # "xenbr0"
        self.orignic = original # "eth0"
        self.physicalnic = physnic # "peth0"
        self.macdigits = formac
        self.slashpart = None # ignore


# SEE EXPLANATION BELOW BEFORE CHANGING THESE:

TASKS.append(Task(  ip="125.125.125.125",
                    network="125.125.125.0/24",
                    broadcast="125.125.125.255",
                    veth="veth1",
                    vif="vif0.1",
                    bridge="xenbr0",
                    original="eth0",
                    physnic="peth0",
                    formac="CF"           ))

TASKS.append(Task(  ip="172.20.6.125",
                    network="172.20.6.0/24",
                    broadcast="172.20.6.255",
                    veth="veth2",
                    vif="vif0.2",
                    bridge="xenbr0",
                    original="eth0",
                    physnic="peth0",
                    formac="DF"           ))

# ============================== EXPLANATION ==================================
doc="""

This script will allow you to host VMs on a foreign subnet without needing
the VMM to have a real presence on that subnet.  A presence is required
currently for DHCP delivery of an address using *non modified* DHCP
implementations.


       ======= UNIQUE FAKE IP PER VMM =======

In the case where you have subnets that can accomodate one extra address *per
VMM* you can configure an alias for that extra IP and be done.  For example, 
in domain zero run:
    
ifconfig eth0:0 10.20.0.5 netmask 255.255.0.0

And configure the DHCP conf file with:
    
    shared-network networks_ether1 {
    
          # original subnet
          subnet W.X.Y.Z netmask 255.255.255.0 {
          }
        
          # new, fake subnet, this VMM has 10.20.0.5 on it, etc.
          subnet 10.20.0.0 netmask 255.255.0.0 {
          }
    }

And you're good to go.


       ======= ONE FAKE IP FOR EVERY VMM (this script) =======
       
But that requires each VMM has a different extra IP on the subnet (otherwise
there can be IP and ARP confusion).

This script is mainly motivated by the public IP case which is normally a
scarce resource.

*** THIS IS AN ADVANCED CONFIGURATION ***

Instead of requiring an extra public IP per VMM just to host VMs with public
IPs, this script allows you to configure the SAME ALIAS IP ON EVERY VMM.

The ebtables rules this script sets up block off ALL traffic to/from the
cluster for this address, so it's as if it is really not in use.  It's only in
use on the local network stack on each VMM.

It must not be used for any hostname value, any configuration value, etc.  It
must be considered an "unused" IP for normal cluster operations and not ever
be addressed or needed for anything.

Above, in the "TASKDEF" section, for each "foreign subnet" to host you must
configure:

a) the special IP
b) the network with slash abbreviation (for example "1.2.3.0/24")
c) the broadcast (for example "1.2.3.255")
d) the virtual interface name (for example "veth1")
e) the corresponding Xen vif (for example "vif0.1")
f) the bridge to add the vif to (for example "xenbr0")
g) the original dom0 interface (for example "eth0")
h) the actual physical NIC interface (for example "peth0")
i) two hex digits to use for fake MAC (for example "FF")

If you have multiple physical NICs, take care to start at the appropriate 
"veth" interface ("eth1" and the corresponding "vif0.1" may already be in use).

Note that the value of "e", the bridge, must be the same bridge that workspace
NICs for the intended network are bridged to.  If you have just one physical
NIC, there is likely only one to choose for (for example "xenbr0").

Regarding "g", the script will create a fake MAC address for the fake NIC,
based on the original dom0 interface (for example "eth0").  It will replace
the third and fourth hex digits with something else you provide.  The strategy
here is to create MAC addresses that are unique yet different on each node,
as a contingency because the bridge might be confused by identical MACs.  So
it is based off the node's unique MAC and assumes the cluster's entire real MAC
set has different 3rd and 4th digits.  Also assumes workspace MAC prefix will
never be able to conflict either (this prefix is usually defined in the
service's network settings).


ROUTING -- ** WARNING **

If you set up a "foreign subnet" for DHCP by normal means (for example using
"ifconfig") you would typically cause a routing entry to be added for the
associated subnet.

This can not happen (since the kernel will now choose this fake address as the
source IP for anything heading to that subnet -- and traffic from this fake
address is blocked).

If your cluster node relies on the network for logins (LDAP, SSH's UseDNS,
etc.), in particular SOME SERVICE ADDRESS ON THE NEW SUBNET, there is a chance
you could cut yourself off entirely from the node if you log out and that
'veth' interface is part of the routing table.

It is hard to make sure this is done right automatically, so all this script
does is remove the net, for example "route del -net 125.125.125.0/24 veth1"

If something needs to be added back in, SCRIPT THE NECESSARY ROUTING ADDITION
after this script runs.

"""



# ================================ IMPORTS ====================================

import fcntl
import os
import socket
import struct
import sys

# ================================== MISC =====================================

# from python mailing list:
def _ifinfo(sock, addr, ifname):
    iface = struct.pack('256s', ifname[:15])
    info  = fcntl.ioctl(sock.fileno(), addr, iface)
    if addr == 0x8927:
        hwaddr = []
        for char in info[18:24]:
            hwaddr.append(hex(ord(char))[2:])
        return ':'.join(hwaddr)
    else:
        return socket.inet_ntoa(info[20:24])

# from python mailing list:
def ifconfig(ifname):
    ifreq = {'ifname': ifname}
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        ifreq['addr']    = _ifinfo(sock, 0x8915, ifname) # SIOCGIFADDR
        ifreq['brdaddr'] = _ifinfo(sock, 0x8919, ifname) # SIOCGIFBRDADDR
        ifreq['netmask'] = _ifinfo(sock, 0x891b, ifname) # SIOCGIFNETMASK
        ifreq['hwaddr']  = _ifinfo(sock, 0x8927, ifname) # SIOCSIFHWADDR
    except:
        pass
        # exceptions are normal...
    sock.close()
    return ifreq
    
    
# ================================ EBTABLES ===================================
    
def ebtables_rule1(task):
    """ebtables -A FORWARD -p IPv4 -o peth0 --ip-src 125.125.125.125 -j DROP
    
    English: "discard any packet heading out on the physical NIC to the
    cluster (bridged out on the peth0 interface) with an IP source address
    of 125.125.125.125"
    """
    return "FORWARD -p IPv4 -o %s --ip-src %s -j DROP" % (task.physicalnic, task.ipaddress)
    
def ebtables_rule2(task):
    """ebtables -A FORWARD -p IPv4 -o peth0 --ip-dst 125.125.125.125 -j DROP
    
    English: "discard any packet heading out on the physical NIC to the
    cluster (bridged out on the peth0 interface) with an IP destination address
    of 125.125.125.125"
    """
    return "FORWARD -p IPv4 -o %s --ip-dst %s -j DROP" % (task.physicalnic, task.ipaddress)
    
def ebtables_rule3(task):
    """ebtables -A FORWARD -p IPv4 -i peth0 --ip-src 125.125.125.125 -j DROP
    
    English: "discard any packet coming from the physical NIC, from the
    cluster (bridged in on the peth0 interface), with an IP source address
    of 125.125.125.125"
    """
    return "FORWARD -p IPv4 -i %s --ip-src %s -j DROP" % (task.physicalnic, task.ipaddress)
    
def ebtables_rule4(task):
    """ebtables -A FORWARD -p IPv4 -i peth0 --ip-dst 125.125.125.125 -j DROP
    
    English: "discard any packet coming from the physical NIC, from the
    cluster (bridged in on the peth0 interface), with an IP destination address
    of 125.125.125.125
    """
    return "FORWARD -p IPv4 -i %s --ip-dst %s -j DROP" % (task.physicalnic, task.ipaddress)
    
def ebtables_rule5(task):
    """ebtables -A FORWARD -p IPv4 --ip-proto udp --ip-dport 67 --ip-dst 125.125.125.125 -j ACCEPT
    
    English: "accept any UDP packet addressed to 125.125.125.125 that is to
    port 67".  This lets the VMs contact this address for the DHCP protocol.
    """
    return "FORWARD -p IPv4 --ip-proto udp --ip-dport 67 --ip-dst %s -j ACCEPT" % (task.ipaddress)
    
def ebtables_rule6(task):
    """ebtables -A FORWARD -p IPv4 --ip-dst 125.125.125.125 -j DROP
    
    English: "discard any IP packet addressed to 125.125.125.125".  This
    catches any other addressed packet from any VM on this VMM to the address.
    The previous rule (#5) accepts OK packets (DHCP) and then this one throws
    out anything else.
    """
    return "FORWARD -p IPv4 --ip-dst %s -j DROP" % (task.ipaddress)
    
def ebtables_nat_rule1(task):
    """ebtables -t nat -A PREROUTING -p ARP -i peth0 --arp-op Request --arp-ip-dst 125.125.125.125 -j DROP
    
    English: "discard any ARP packet coming from the physical NIC, from the
    cluster (bridged in on the peth0 interface), with a query address of
    125.125.125.125"
    """
    return "PREROUTING -p ARP -i %s --arp-op Request --arp-ip-dst %s  -j DROP" % (task.physicalnic, task.ipaddress)
    
def ebtables_run(rule, add, natrule=False):
    """if add is True, implements -A (append), if false, -D (delete)"""
    
    if add:
        flag = "-A"
    else:
        flag = "-D"
    
    if natrule:
        cmd = "ebtables -t nat %s %s" % (flag, rule)
    else:
        cmd = "ebtables %s %s" % (flag, rule)
        
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        if add:
            raise Exception("ERROR, command failed: '%s'" % cmd)
        else:
            print "Removal command failed: '%s'" % cmd
    
def ebtables_adjust(task, add):
    """if add is True, these implement -A (append), if false, -D (delete)"""
    
    ebtables_run(ebtables_nat_rule1(task), add, natrule=True)
    
    # The order of these rules is very important.
    ebtables_run(ebtables_rule1(task), add)
    ebtables_run(ebtables_rule2(task), add)
    ebtables_run(ebtables_rule3(task), add)
    ebtables_run(ebtables_rule4(task), add)
    ebtables_run(ebtables_rule5(task), add)
    ebtables_run(ebtables_rule6(task), add)
    
# =============================== VIF SETUP ===================================
    
def vif_setup(task):
    
    cmd = "ifconfig %s -multicast" % task.vifname
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)
    
    cmd = "ifconfig %s -arp" % task.vifname
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)
    
    cmd = "ifconfig %s up" % task.vifname
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)
        
# ============================= VIF + BRIDGE ==================================
    
def vif_bridge_add(task):
    
    cmd = "brctl addif %s %s" % (task.bridgename, task.vifname)
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)
        
def vif_bridge_del(task):
    
    cmd = "brctl delif %s %s" % (task.bridgename, task.vifname)
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)
    
    
# ================================ RUN ONE ====================================
    
def get_mac(task):
    
    interface = task.orignic
    try:
        ifreq = ifconfig(interface)
    except:
        return None
    #print ifreq
    try:
        #print " - Found '%s' address for %s" % (ifreq['addr'], interface)
        print " - Found '%s' MAC for %s" % (ifreq['hwaddr'], interface)
        return ifreq['hwaddr']
    except KeyError:
        return None
        
def get_new_mac(task, currmac):
    parts = currmac.split(":")
    if len(parts) != 6:
        msg = "unexpected, current MAC doesn't have six parts? '%s'" % currmac
        raise Exception(msg)
    parts[1] = task.macdigits
    newparts = []
    for part in parts:
        if len(part) == 1:
            newparts.append("0%s" % part.upper())
        elif len(part) == 2:
            newparts.append(part.upper())
        else:
            msg = "current MAC looks invalid '%s', part with len > 2" % currmac
            raise Exception(msg)
    return ":".join(newparts)
    
def set_mac(task, newmac):
    
    cmd = "ifconfig %s hw ether %s" % (task.vethname, newmac)
    #cmd = "ip link set ${netdev} addr fe:ff:ff:ff:ff:ff" % ()
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)

def set_ip(task):
    
    cmd = "ip addr flush %s" % (task.vethname)
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)
        
    cmd = "ip addr add %s%s broadcast %s dev %s" % (task.ipaddress, task.slashpart, task.broadcast, task.vethname)
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)
        
    cmd = "ip link set dev %s up" % (task.vethname)
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)

def remove_route(task):
    cmd = "route del -net %s %s" % (task.network, task.vethname)
    print " - Running: %s" % cmd
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("ERROR, command failed: '%s'" % cmd)
    
def do_one_task(task):
    
    ebtables_setup_started = False
    vif_added_to_bridge = False
    
    try:
        # create MAC for veth (don't set it yet though)
        currmac = get_mac(task)
        if not currmac:
            raise Exception("could not resolve interface '%s'" % task.orignic)
            
        newmac = get_new_mac(task, currmac)
        
        print " - Chose MAC '%s' for %s" % (newmac, task.vethname)
        
        # set up ebtables rules for the address
        ebtables_setup_started = True
        ebtables_adjust(task, True)
        
        # set up vif
        vif_setup(task)
        
        # add vif to bridge
        vif_bridge_add(task)
        vif_added_to_bridge = True
        
        # set mac
        set_mac(task, newmac)
        
        # set IP
        set_ip(task)
        
        # routing
        remove_route(task)

    except:
        if ebtables_setup_started:
            print "*** BACKING OUT ebtables rules ***"
            ebtables_adjust(task, False)
        if vif_added_to_bridge:
            print "*** BACKING OUT vif addition to bridge ***"
            vif_bridge_del(task)
        raise
    
# =============================== VALIDATION ==================================
    
def simple_ip_check(ip):
    
    if not ip:
        print "Problem: no IP defined"
        return False
        
    parts = ip.split(".")
    
    if len(parts) != 4:
        print "Problem: expecting four parts in IP:  W.X.Y.Z"
        return False
        
    for part in parts:
        try:
            num = int(part)
        except:
            print "Problem: '%s' is not a number" % part
            return False
            
    for part in parts:
        num = int(part)
        if num < 0:
            print "Problem: '%s' is negative? invalid IP" % part
            return False
        if num > 255:
            print "Problem: '%s' is greater than 255? invalid IP" % part
            return False

    return True
    
def check_str(something, name):
    
    if not something:
        print "No %s definition, invalid task" % name
        return False
    
    if not isinstance(something, str):
        print "%s is not a string? invalid task" % name
        return False
    
    return True
    
def check_task_dups(tasks):
    ips = []
    veths = []
    vifs = []
    macdigs = []
    
    probstr = "Exiting, found a duplicate in different task definitions:"
    for task in tasks:
        if task.ipaddress in ips:
            print "%s IP '%s'" % (probstr, task.ipaddress)
            return False
        else:
            ips.append(task.ipaddress)
        
        if task.vethname in veths:
            print "%s veth '%s'" % (probstr, task.vethname)
            return False
        else:
            veths.append(task.vethname)
            
        if task.vifname in vifs:
            print "%s vif '%s'" % (probstr, task.vifname)
            return False
        else:
            vifs.append(task.vifname)
            
        if task.macdigits in macdigs:
            print "%s mac digits '%s'" % (probstr, task.macdigits)
            return False
        else:
            macdigs.append(task.macdigits)
            
    return True

def check_task(task):
    """allowed to alter task, e.g. mac prefix --> upper()"""
    
    if not task:
        print "problem with TASKS list setup ('no task')"
        return False

    if not check_str(task.vethname, "'veth'"):
        return False
    if not check_str(task.vifname, "'vif'"):
        return False
    if not check_str(task.bridgename, "'bridge'"):
        return False
    if not check_str(task.orignic, "'original nic'"):
        return False
    if not check_str(task.physicalnic, "'physical nic'"):
        return False

    if not check_str(task.ipaddress, "'IP'"):
        return False
    if not simple_ip_check(task.ipaddress):
        return False
        
    if not check_str(task.broadcast, "'broadcast'"):
        return False
    if not simple_ip_check(task.broadcast):
        return False
        
    if not check_str(task.network, "'mask /net'"):
        return False
        
    err = "expecting /net number like '/24'"
    netparts = task.network.split("/")
    
    if len(netparts) != 2:
        print "%s, invalid task (expecting net/mask, e.g. 1.2.3.0/24)" % err
        return False
        
    if not simple_ip_check(netparts[0]):
        return False
        
    try:
        somenum = int(netparts[1])
        if somenum < 0 or somenum > 32:
            print "%s, invalid task (num=%d)" % (err, somenum)
            return False
        task.slashpart = "/%d" % somenum
    except:
        print "%s, invalid task (not an integer)" % err
        return False

    name = "'mac digits' definition (formac)"
    if not check_str(task.macdigits, name):
        return False
    if len(task.macdigits) != 2:
        print "expecting 2 digits for %s, invalid task" % name
        return False

    task.macdigits = task.macdigits.upper()
    valid = "0123456789ABCDEF"
    for char in task.macdigits:
        if char not in valid:
            print "expecting hex digit for %s, invalid task" % name
            print "offending letter is '%s'" % char
            return False

    return True

# ================================== MAIN =====================================

def main(argv=None):
    if len(TASKS) == 0:
        print "No task definition, exiting (see the explanation at the top of \
this program"
        return 1
        
    for i,task in enumerate(TASKS):
        print "** Checking task definition #%d" % (i+1)
        if not check_task(task):
            return 1
    
    if not check_task_dups(TASKS):
        return 1
            
    for i,task in enumerate(TASKS):
        print "** Running task #%d (%s: '%s')" % (i+1, task.vethname, task.ipaddress)
        do_one_task(task)

if __name__ == "__main__":
    sys.exit(main())

