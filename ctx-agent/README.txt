Nimbus Context Agent
====================

http://www.nimbusproject.org

The context agent is a small program which is meant to be bundled onto VM 
images. It is invoked on boot by a distribution-specific mechanism (rc.local
for example). It retrieves context information in an IaaS-specific way and
securely contacts a Nimbus Context Broker.

Out of the box, the agent does little more than SSH key exchange but you
can customize the ctx-scripts to perform different contextualization tasks.


Supported IaaS Providers
------------------------
* Amazon EC2
* Nimbus versions 2.2+


Software Dependencies
---------------------
* Python 2.3+ (but not Python 3)
* curl command with SSL support


Installation
------------
1. Untar the package and copy the ctx/ and ctx-scripts/ directories to 
   /opt/nimbus/ on your VM image.

2. Add calls to the clean.sh and launch.sh scripts to your system boot process.
   They should be run at the end of the boot, after network and daemons are 
   running. On many systems, you can edit the /etc/rc.local file and add these
   calls. An example script is provided at ctx/sample-rc-local-entry.txt

3. Customize the ctx-scripts/ as needed to contextualize your VM.

4. Save/bundle your VM image and then try to boot it via the Nimbus cloud-client
   with a cluster document (or some other client that supports Nimbus 
   contextualization).

