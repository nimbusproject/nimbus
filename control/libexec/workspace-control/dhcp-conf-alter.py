#!/usr/bin/env python

# Copyright 1999-2006 University of Chicago
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy
# of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

# For more information see: http://www.nimbusproject.org

import ConfigParser
import fcntl
import logging
import optparse
import os
import re
import stat
import sys
import time

# lockfile's default path is conf file + suffix
LOCKSUFFIX=".lock"

log = logging.getLogger("dhcp-conf-alter")
ch = logging.StreamHandler()
ch.setLevel(logging.DEBUG)
formatter = logging.Formatter("%(levelname)s: %(message)s")
# for timestamp and line no
#formatter = logging.Formatter("%(asctime)s (%(lineno)d) %(levelname)s - %(message)s")
ch.setFormatter(formatter)
log.addHandler(ch)

# sledgehammer approach
iprestr = r"^([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])$"
IPRE = re.compile(iprestr)
macrestr = r"^([A-F]|[a-f]|[0-9])([A-F]|[a-f]|[0-9])\:([A-F]|[a-f]|[0-9])([A-F]|[a-f]|[0-9])\:([A-F]|[a-f]|[0-9])([A-F]|[a-f]|[0-9])\:([A-F]|[a-f]|[0-9])([A-F]|[a-f]|[0-9])\:([A-F]|[a-f]|[0-9])([A-F]|[a-f]|[0-9])\:([A-F]|[a-f]|[0-9])([A-F]|[a-f]|[0-9])$"
MACRE = re.compile(macrestr)

class InvalidInput(Exception):
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class DHCPConf:
    def __init__(self, original_content):
        self.original_content = original_content # list of lines
        self.header = [] # list of lines to add before the hosts
        self.all_hosts = {} # dict of DHCPConfEntry instances keyed by mac addr

    def parse(self, verbose):
        pass

    def write(self, f, verbose):
        pass

    def add(self, entry):
        if not self.original_content and not self.header:
            raise Exception("parse() must be called first")
        if not entry.mac:
            raise Exception("entry has no MAC address")
        if not entry.ip:
            raise Exception("entry has IP address")
        if not entry.hostname:
            raise Exception("entry has no hostname")

        # current impl assumes mac, ip, and hostname all unique
        for ip in self.all_hosts:
            if ip == entry.ip:
                raise Exception("IP %s already present" % ip)
            if self.all_hosts[ip].mac == entry.mac:
                raise Exception("MAC %s already present" % entry.mac)
            if self.all_hosts[ip].hostname == entry.hostname:
                raise Exception("hostname %s already present" % entry.hostname)

        self.all_hosts[entry.ip] = entry
        log.info("added entry: %s" % entry.ip)

    def remove(self, entry):
        if not self.original_content and not self.header:
            raise Exception("parse() must be called first")
        if not entry.ip:
            raise Exception("entry has no IP address")
        if not self.all_hosts.has_key(entry.ip):
            log.info("entry with IP address already not present")
            return False
        else:
            del self.all_hosts[entry.ip]
            log.info("deleted entry: %s" % entry.ip)
            return True

class DHCPConfEntry:
    def __init__(self):
        self.hostname = None
        self.mac = None
        self.ip = None
        self.dns = [] # list
        self.broadcast = None
        self.gateway = None
        self.subnetmask = None
        self.default_lease = None
        self.max_lease = None
        self.comment = None

# --------------------------- PARSING -------------------------- #

def parse(opts):
    """Returns instance of DHCPConf representing current configuration"""

    f = None
    try:
        f = open(opts.policyfile, "r")
        content = f.readlines()
    finally:
        if f:
            f.close()

    # todo: when more impls come along, make it easy to switch between them
    conf = ISCDHCPConf(content)
    conf.parse(opts.veryverbose)
    return conf

# ----------------------------- ADD ---------------------------- #

def add(opts, conf):
    """Adds an instance of DHCPConfEntry to a DHCPConf object or throws
       exception"""

    entry = DHCPConfEntry()
    entry.ip = opts.ipaddress
    entry.hostname = opts.hostname
    entry.mac = opts.macaddress
    entry.comment = "Added by dhcp-conf-alter @ %s" % time.ctime()
    if opts.dns:
        entry.dns = opts.dns # list
    if opts.subnetmask:
        entry.subnetmask = opts.subnetmask
    if opts.broadcast:
        entry.broadcast = opts.broadcast
    if opts.gateway:
        entry.gateway = opts.gateway
    if opts.leasetime:
        entry.default_lease = str(opts.leasetime[0])
        entry.max_lease = str(opts.leasetime[1])

    conf.add(entry)

# --------------------------- REMOVE --------------------------- #

def remove(opts, conf):
    """Removes an instance of DHCPConfEntry from a DHCPConf object, returns
       True if entry existed or False if already not present"""

    # NOTE: in this implementation, hostname, MAC and IP are assumed unique
    # TODO: could therefore make --remove able to take any one of those, not
    #       just IP address.
    # Currently, IP address is most convenient for caller...

    entry = DHCPConfEntry()
    entry.ip = opts.ipaddress
    return conf.remove(entry)

# --------------------------- WRITE --------------------------- #

def write(opts, conf):
    """Writes out a DHCPConf object"""
    f = None
    try:
        f = open(opts.policyfile, "w")
        conf.write(f, opts.veryverbose)
    finally:
        if f:
            f.close()

# -------------------------- ISC DHCP ------------------------- #

class ISCDHCPConf(DHCPConf):
    def __init__(self, original_content):
        DHCPConf.__init__(self, original_content)
        self.hostmatch_re = re.compile(r"^host (.*) {$")
        self.mac_re = re.compile(r"[ ]*hardware ethernet (.*);")
        self.ip_re = re.compile(r"[ ]*fixed-address (.*);")
        self.defaultlease_re = re.compile(r"[ ]*default-lease-time (.*);")
        self.maxlease_re = re.compile(r"[ ]*max-lease-time (.*);")
        self.gateway_re = re.compile(r"[ ]*option routers (.*);")
        self.broadcast_re = re.compile(r"[ ]*option broadcast-address (.*);")
        self.subnet_re = re.compile(r"[ ]*option subnet-mask (.*);")
        self.dns_re = re.compile(r"[ ]*option domain-name-servers (.*);")
        self.comment_re = re.compile(r"[ ]*# (.*)")

    def parse(self, verbose):
        # find edit point
        lineno = 0
        lineidx = -1
        for line in self.original_content:
            self.header.append(line)
            if line.find("DHCP-CONFIG-AUTOMATIC-BEGINS") != -1:
                lineidx = lineno
                self.header.append("\n")
                break
            lineno += 1

        if lineidx == -1:
            raise Exception("cannot find edit marker in dhcp conf contents")

        curr = None
        for line in self.original_content[lineidx:]:
            if line.startswith("host"):
                curr = DHCPConfEntry()

            if curr:
                # Not checking validity of previously written entries.
                # Items do not need to be ordered in entry (though they
                #     are as a side effect of the alter implementation).
                match = self.hostmatch_re.match(line)
                if match:
                    # option host-name generated from this as well
                    curr.hostname = match.group(1)
                    if verbose:
                        log.debug("found hostname = %s" % curr.hostname)
                    continue
                match = self.mac_re.match(line)
                if match:
                    curr.mac = match.group(1)
                    if verbose:
                        log.debug("           mac = %s" % curr.mac)
                    continue
                match = self.ip_re.match(line)
                if match:
                    curr.ip = match.group(1)
                    if verbose:
                        log.debug("            ip = %s" % curr.ip)
                    continue
                match = self.defaultlease_re.match(line)
                if match:
                    curr.default_lease = match.group(1)
                    if verbose:
                        log.debug("     def lease = %s" % curr.default_lease)
                    continue
                match = self.maxlease_re.match(line)
                if match:
                    curr.max_lease = match.group(1)
                    if verbose:
                        log.debug("     max lease = %s" % curr.max_lease)
                    continue
                match = self.gateway_re.match(line)
                if match:
                    curr.gateway = match.group(1)
                    if verbose:
                        log.debug("       gateway = %s" % curr.gateway)
                    continue
                match = self.broadcast_re.match(line)
                if match:
                    curr.broadcast = match.group(1)
                    if verbose:
                        log.debug("     broadcast = %s" % curr.broadcast)
                    continue
                match = self.subnet_re.match(line)
                if match:
                    curr.subnet = match.group(1)
                    if verbose:
                        log.debug("        subnet = %s" % curr.subnet)
                    continue
                match = self.comment_re.match(line)
                if match:
                    curr.comment = match.group(1)
                    if verbose:
                        log.debug("       comment = %s" % curr.comment)
                    continue
                match = self.dns_re.match(line)
                if match:
                    dnsstr = match.group(1)
                    curr.dns = dnsstr.split(" ")
                    if verbose:
                        log.debug("           dns = %s" % curr.dns)
                    continue

            if line.startswith("}"):
                if self.all_hosts.has_key(curr.ip):
                    raise Exception("duplicate IP found in conf file, aborting")
                self.all_hosts[curr.ip] = curr
                log.debug("found entry with IP = %s" % curr.ip)
                curr = None

    def write(self, f, verbose):
        if not f:
            raise Exception("no file object to write to")
        for line in self.header:
            f.write(line)
        for ip in self.all_hosts:
            self.write_entry(f, self.all_hosts[ip], verbose)

    def write_entry(self, f, entry, verbose):
        entrystr = ""
        if not entry.hostname:
            raise Exception("hostname required")
        else:
            entrystr += "host %s {\n" % entry.hostname

        if entry.comment:
            entrystr += "  # %s\n" % entry.comment

        if not entry.mac:
            raise Exception("mac required")
        else:
            entrystr += "  hardware ethernet %s;\n" % entry.mac

        if not entry.ip:
            raise Exception("ip required")
        else:
            entrystr += "  fixed-address %s;\n" % entry.ip

        if entry.default_lease:
            entrystr += "  default-lease-time %s;\n" % entry.default_lease

        if entry.default_lease:
            entrystr += "  max-lease-time %s;\n" % entry.max_lease

        # entry.hostname existence already checked above
        entrystr += '  option host-name "%s";\n' % entry.hostname
        
        if entry.subnetmask:
            entrystr += "  option subnet-mask %s;\n" % entry.subnetmask

        if entry.gateway:
            entrystr += "  option routers %s;\n" % entry.gateway

        if entry.broadcast:
            entrystr += "  option broadcast-address %s;\n" % entry.broadcast

        if entry.dns:
            entrystr += "  option domain-name-servers"
            for server in entry.dns:
                entrystr += " %s" % server
            entrystr += ";\n"

        entrystr += "}\n\n"

        if verbose:
            log.debug("\n\nWRITING:\n%s" % entrystr)

        f.write(entrystr)

# ---------------------- VALIDATE INPUTS ---------------------- #

def validate(opts):

    actions = [opts.add, opts.remove]

    count = 0
    for action in actions:
        if action:
            count += 1

    if not count:
        raise InvalidInput("You must supply an action, see help (-h).")

    if count != 1:
        raise InvalidInput("You may only supply one action, see help (-h).")

    if opts.add:
        log.debug("---   action = add")
    elif opts.remove:
        log.debug("---   action = remove")

    if not opts.ipaddress:
        raise InvalidInput("You must supply an IP address, see help (-h).")

    if not IPRE.match(opts.ipaddress):
        raise InvalidInput("IP is not a valid address: %s" % opts.ipaddress)

    log.debug("   ipaddress = %s" % opts.ipaddress)

    if not opts.policyfile:
        raise InvalidInput("You must supply a DHCP configuration file to alter, see help (-h).")

    if not os.path.isabs(opts.policyfile):
        raise InvalidInput("You must specify the DHCP configuration file with an absolute path.")
    else:
        opts.policyfile = os.path.realpath(opts.policyfile)
        log.debug("  policyfile = %s" % opts.policyfile)

    if opts.add:
        if not opts.macaddress:
            raise InvalidInput("Add requires a MAC address, see help (-h).")
        else:
            if not MACRE.match(opts.macaddress):
                raise InvalidInput("MAC is not a valid address: %s" % opts.macaddress)
            log.debug("  macaddress = %s" % opts.macaddress)
        if not opts.hostname or opts.hostname.lower().strip() == 'none':
            raise InvalidInput("Add requires a hostname, see help (-h).")
        else:
            log.debug("    hostname = %s" % opts.hostname)


    if opts.lockfilepath:
        if not os.path.isabs(opts.lockfilepath):
            raise InvalidInput("You must specify the lockfile with an absolute path.")
        else:
            opts.lockfilepath = os.path.realpath(opts.lockfilepath)
            log.debug("    lockfile = %s" % opts.lockfilepath)

    if opts.subnetmask:
        if not IPRE.match(opts.subnetmask):
            raise InvalidInput("subnet is not a valid address (note that /# addresses are not supported yet): %s" % opts.subnetmask)
        log.debug("  subnetmask = %s" % opts.subnetmask)

    if opts.broadcast:
        if not IPRE.match(opts.broadcast):
            raise InvalidInput("broadcast is not a valid address: %s" % opts.broadcast)
        log.debug("   broadcast = %s" % opts.broadcast)

    if opts.gateway:
        if not IPRE.match(opts.gateway):
            raise InvalidInput("gateway is not a valid address: %s" % opts.gateway)
        log.debug("    gateway  = %s" % opts.gateway)

    # list of dns IPs
    opts.dns = []
    if opts.dnsstr:
        log.debug("    supplied dns servers: %s" % opts.dnsstr)
        dnsservers = opts.dnsstr.split(",")
        allvalid = True
        for server in dnsservers:
            if not IPRE.match(server):
                allvalid = False
                log.error("DNS server is not a valid address: %s" % server)
            else:
                opts.dns.append(server)
        if not allvalid:
            raise InvalidInput("all DNS servers must be valid IPs")
        log.debug("    validated dns servers = %s" % opts.dns)

    # [default_lease_time, max_lease_time]
    opts.leasetime = []
    if opts.leasetimestr:
        log.debug("    supplied leasetimes: %s" % opts.leasetimestr)
        times = opts.leasetimestr.split(",")
        if len(times) != 2:
            raise InvalidInput("only supply two lease times: default and max")
        try:
            x = int(times[0])
            if x < 1:
                raise InvalidInput("default lease time is less than one")
            y = int(times[1])
            if y < 1:
                raise InvalidInput("max lease time is less than one")
            if x > y:
                raise InvalidInput("max lease time is greater than default...")
            opts.leasetime.append(x)
            opts.leasetime.append(y)
        except ValueError:
            raise InvalidInput("a lease time is not an integer")

        log.debug("    default lease time = %d" % opts.leasetime[0])
        log.debug("    max lease time     = %d" % opts.leasetime[1])

# --------------------------- LOCKING -------------------------- #

def lock(opts):

    opts.lockfile = None
    lockfilepath = None

    # validate() checked if abs path already
    if opts.lockfilepath:
        lockfilepath = opts.lockfilepath
    else:
        (confdir, f) = os.path.split(opts.policyfile)
        lockfilepath = os.path.join(confdir, "%s%s" % (f, LOCKSUFFIX))

    log.debug("about to lock, lockfile = %s" % lockfilepath)

    if not os.path.exists(lockfilepath):
        f = open(lockfilepath, "w")
        f.write("\n")
        f.close()

    opts.lockfile = open(lockfilepath, "r+")
    fcntl.flock(opts.lockfile.fileno(), fcntl.LOCK_EX)
    log.debug("acquired lock")

def unlock(opts):
    if not opts.lockfile:
        raise Exception("unlocking without a lock")

    opts.lockfile.close()
    log.debug("lock released")



# ----------------------- ARGPARSE SETUP ----------------------- #

def setup():
    ver="DHCP configuration tool: %prog\nhttp://www.nimbusproject.org"
    parser = optparse.OptionParser(version=ver)

    parser.add_option("-q", "--quiet",
                  action="store_true", dest="quiet", default=False,
                  help="don't print any messages (unless error occurs).")

    parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="print debug messages")

    parser.add_option("-y", "--veryverbose",
                  action="store_true", dest="veryverbose", default=False,
                  help="print all debug messages")

    # ----

    group = optparse.OptionGroup(parser, "Required action",
                 "One of these actions is required:")

    group.add_option("-a", "--add",
                  action="store_true", dest="add", default=False,
                  help="Add a policy to the DHCP configuration.")

    group.add_option("-r", "--remove",
                  action="store_true", dest="remove", default=False,
                  help="Remove a policy from the DHCP configuration.")

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser, "Required arguments",
                 "These arguments are required for both --add and --remove")

    group.add_option("-p", "--policyfile", dest="policyfile", metavar="FILE",
                  help="Absolute path to the DHCP configuration file, where "
                       "the lease policies are stored")

    # NOTE: in this implementation, hostname, MAC and IP are assumed unique
    # TODO: could therefore make --remove able to take any one of those, not
    #       just IP address.
    # Currently, IP address is most convenient for caller...
    group.add_option("-i", "--ipaddress", dest="ipaddress", metavar="IP",
                  help="The IP address of the lease.  If adding, must not "
                       "be in the policy already.")

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser, "Required argument for --add")

    group.add_option("-m", "--macaddress", dest="macaddress", metavar="MAC",
                  help="The MAC address to respond to.  Must not be in the "
                       "policy already.")

    group.add_option("-n", "--hostname", dest="hostname", metavar="HOST",
                  help="The hostname.  Must not be in the policy already.")

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser, "Optional arguments for --add",
                 "These arguments are optional for --add")

    group.add_option("-s", "--subnetmask", dest="subnetmask", metavar="MASK",
                  help="The subnet mask (doesn't support /# form yet)")

    group.add_option("-b", "--broadcast", dest="broadcast", metavar="IP",
                  help="The broadcast address")

    group.add_option("-g", "--gateway", dest="gateway", metavar="IP",
                  help="The gateway address")

    group.add_option("-t", "--timeforlease", dest="leasetimestr",
                  metavar="TIME,TIME",
                  help="The default,max lease time for this lease, if not "
                       "provided, the defaults in dhcpd.conf will be used.")

    group.add_option("-d", "--dns", dest="dnsstr", metavar="IP[,IP,...]",
                  help="Comma separated list of DNS IPs")

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser, "Optional arguments")

    group.add_option("-l", "--lockfile", dest="lockfilepath", metavar="FILE",
                  help="Optionally override absolute path to the default "
                       "lockfile (used to ensure only one process is editing "
                       "the DHCP configuration file at a time).  The default "
                       "lockfile is the supplied --policyfile file with a "
                       "suffix of '.lock'")

    parser.add_option_group(group)

    return parser


# ----------------------------- RUN ----------------------------- #

def run(argv=None):

    parser = setup()

    if argv:
        (opts, args) = parser.parse_args(argv[1:])
    else:
        (opts, args) = parser.parse_args()

    if opts.verbose or opts.veryverbose:
        log.setLevel(logging.DEBUG)
    elif opts.quiet:
        log.setLevel(logging.ERROR)
    else:
        log.setLevel(logging.INFO)

    try:
        # Validate all input
        # only other source of InvalidInput exceptions
        validate(opts)

        # Make sure this is the only process editing the policy file
        lock(opts)

        conf = parse(opts)

        if opts.add:
            add(opts, conf)
            write(opts, conf)
        elif opts.remove:
            changes = remove(opts, conf)
            if changes:
                pass
                write(opts, conf)

        unlock(opts)

    except InvalidInput, e:
        sys.exit("FATAL: %s" % e.msg)
        return 1

    except Exception, e:
        # a problem we did not case for
        log.exception("Problem:")
        try:
            unlock(opts)
        except:
            pass
        return 2

def main():
    if os.name != 'posix':
        sys.exit("only runs on posix systems") # because of locking mechanism
    return run()

if __name__ == "__main__":
    sys.exit(main())

