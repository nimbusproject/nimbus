#!/usr/bin/env python

# If one of these gets configured with an address, the program exits.
INTERFACES_TO_CHECK = [ "eth0", "eth1" ]

import socket
import fcntl
import struct
import time

# from python mailing list:
def _ifinfo(sock, addr, ifname):
    iface = struct.pack('256s', ifname[:15])
    info = fcntl.ioctl(sock.fileno(), addr, iface)
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
        ifreq['addr'] = _ifinfo(sock, 0x8915, ifname) # SIOCGIFADDR
        ifreq['brdaddr'] = _ifinfo(sock, 0x8919, ifname) # SIOCGIFBRDADDR
        ifreq['netmask'] = _ifinfo(sock, 0x891b, ifname) # SIOCGIFNETMASK
        ifreq['hwaddr'] = _ifinfo(sock, 0x8927, ifname) # SIOCSIFHWADDR
    except:
        pass
        # exceptions are normal...
    sock.close()
    return ifreq

no_ip = True
while no_ip:
    time.sleep(0.5)
    for iface in INTERFACES_TO_CHECK:
        ifreq = ifconfig(iface)
        if ifreq.has_key("addr"):
            no_ip = False
            break

