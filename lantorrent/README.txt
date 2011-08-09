==========
Lantorrent
==========

Lantorrent is a file distribution protocol integrated into the Nimbus 
IaaS toolkit.  It works as a means to multi-cast virtual machine images 
to many backend nodes.  The protocol is optimized for propagating 
virtual machine images (typically large files) from a central repository 
across a LAN to many virtual machine monitor nodes.

Lantorrent works best for the following scenarios:

1) large file transfers (VM images are typically measured in gigabytes)
2) local area switched network (typical for data center computer racks)
3) file recipients are willing peers.  
4) many endpoints request the same file at roughly the same time

-----------------
Protocol overview
-----------------

When an endpoint wants a file it submits a request to a central agent.  
This agent aggregates request for files so that they can be sent out in 
an efficient single multi-cast session.  Each request for a source file 
is stored until either N request on that file have been made or N' 
seconds have passed since the last request on that source file has been 
made.  This allows for a user to request a single file in several 
unrelated session yet still have the file transfered in an efficient 
multi-cast session.

Once N requests for a given source file have been made or N' seconds 
have passed the destination set for the source file is determined.  A 
chain of destination endpoints is formed such that each node receives 
from and sends to one other node.  The first node receives from the 
repository and send to a peer node, that peer node sends to another, 
and so on until all receive the file.  In this way all links of the 
switch are utilized to send directly to another endpoint in the switch.  
This results in the most efficient transfer on a LAN switched network.

Often times in a IaaS system a single network endpoint (VMM) will want 
multiple copies of the same file.  Each file is booted as a virtual 
machine and that virtual machine will make distinct changes to that file 
as it runs, thus it needs its own copy of the file.  However that file 
does not need to be transfered across the network more than once.  
Lantorrent will send the file to each endpoint once and instruct that 
endpoint to write it to multiple files if needed.

-----------------------------
Enabling Lantorrent in Nimbus
-----------------------------

Lan torrent is part of the Nimbus distribution as of Nimbus 2.6.
However, due to system administrative overhead it is not enabled by
default.  To enable Lantorrent in Nimbus there are a few configurations
changes that must be made.

The following software is required on both service and VMM nodes:
  - python 2.5
  - python simplejson

Lantorrent is run out of xinetd thus it must also be installed on all VMMs.

To install LANTorrent you must take the following steps:

1) edit $NIMBUS_HOME/nimbus-setup.conf
    change lantorrent.enabled: False -> lantorrent.enabled: True

2) edit $NIMBUS_HOME/services/etc/nimbus/workspace-service/other/common.conf
    change the value of propagate.extraargs:
    propagate.extraargs=$NIMBUS_HOME/lantorrent/bin/lt-request

    be sure to expand $NIMBUS_HOME to its full and actual path.

3) install lantorrent on VMM

    - run: python setup-vmm.py install.  This will output the contents of 
      the xinetd configuration file that you will need in the next step.
      [optional] You may want to setup a python virtual environment for
      the installation.
      To do this, run: virtualenv /path_to_install_lantorrent
      Then, use the /path_to_install_lantorrent/bin/python binary
      instead of your system python.
      [optional] If you don't have Internet connectivity on the VMM, you
      will have to manually install the simplejson library from source
      (in the virtual env if you chose to create one).

4) install lantorrent into xinetd
    - the above step outputs and xinetd file like this:

    ============== START WITH THE NEXT LINE ==================
    service lantorrent
    {
        type        = UNLISTED
        disable     = no
        socket_type = stream
        protocol    = tcp
        user        = bresnaha
        wait        = no
        port        = 2893
        server      = /home/bresnaha/lt1/bin/ltserver
        env         = HOME=/opt/nimbus
    }                                                                               
    =============== END WITH THE PREVIOUS LINE =================

    note the 'user' value.  We strongly recommend that it is not 'root'.
    This user will be the owner of all received files (the nimbus user)

      Copy the output to a file called /etc/xinetd.d/lantorrent. Once 
      this is done restart xinetd (/etc/init.d/xinetd restart).

5) change the propagation method.
    - edit the file: 
        $NIMBUS_HOME/services/etc/nimbus/workspace-service/other/authz-callout-ACTIVE.xml

        and change:
            <property name="repoScheme" value="scp" />
        to:
            <property name="repoScheme" value="lantorrent" />

6) restart the service: $NIMBUS_HOME/bin/nimbusctl restart

7) [optional] if the path to Nimbus on the workspace control nodes (VMMs)
    is not /opt/nimbus you will also need to edit a configuration file on 
    all backends.

    In the file:
    <workspace control path>/control/etc/workspace-control/propagation.conf

    make sure the value of:
    
    lantorrentexe: /opt/nimbus/bin/ltclient.sh 

    points to the proper location of you ltclient.sh script.  This should
    be a simple matter of changing /opt/nimbus to the path where you chose
    to install workspace control.

----------------
Protocol details
----------------

requests
--------

transfer
--------

A transfer from one peer to another is performed for forming a TCP 
connection. A json header is first sent down the socket describing the 
transfer.  It describes the files where data must be written, and the 
other endpoints where the data must be sent.  As data comes in the 
server will store it to one or more files and forward it on to another 
endpoint.  All of this information is contained in the header.  The 
header has the following format:

{
    host
    port
    length
    requests = [ {filename, id, rename}, ]
    destinations =
     [ {
         host
         port
         requests = [ { filename, id, rename } ]
         block_size
     }, ]
}

When the server forms a connection with the next endpoint it will form 
another header by removing the first entry of the destination list and 
using the values contained in it as root level values in the header 
described above. If the destination list is empty then the server is the 
last in the chain and no forwarding is needed.

After the header is sent the binary payload (the contents of the file) 
is sent down the socket.

The 'requests' value of the json header contains destination 
information. Each filename in the list is a file where data should be 
written.  The value of id is the id of the request for that file.  The 
value of rename is a boolean which determines if the file should be 
first written to a temporary location and then moved to the final 
location once all of the data is received.  This feature is helpful in 
monitoring file completion.

security
--------

Each connection to a lantorrent peer comes with a json header.  Directly 
following that header is a line of text in the following format:
  EOH : <signature>

The value of signature is the hmac signature of everything that came 
before the literal EOH.  The password for that signature is found in the 
lt.ini file.  Every endpoint and the master repo must have the same 
value for that password.
