Workspace Cloud Client
----------------------

For more information, see: 

   http://www.nimbusproject.org/docs/current/clouds/cloudquickstart.html
   
And:

   http://www.scienceclouds.org/

It is recommended that you follow the quickstarts at the web site, it usually
contains a better walkthrough.

For general notes on the capabilities and syntax of the cloud client, run
the help command:

   $ ./bin/cloud-client.sh -h

You need to have Java installed in order to run this.


Security
--------

1. Obtain a credential.  

The cloud administrator should have either provided you one or authorized your
preexisting credential.

The search path the cloud client uses is as follows:

    A. props

    If "nimbus.cert" and "nimbus.key" are in the properties file and point to
    an unencrypted credential, they trump all.  These are not present by default
    but allow an advanced user to easily toggle between clouds by using the
    --conf switch. If these values are relative paths, they will be resolved
    relative to the configuration file they are read from.

    B. proxy
  
    If a normal proxy is present in the /tmp directory and is still valid, that
    is  used.  This lets the cloud work with all existing certs, tooling,
    MyProxy, etc.

    Note: if you are using an encrypted key, such as the ones typically
    provided by grid computing certificate authorities, you will need to
    generate the proxy mentioned in this step.  See the section below.

    C. ~/.nimbus/

    If ~/.nimbus/usercert.pem and ~/.nimbus/userkey.pem are present and the key
    is unencrypted, use this.  No proxy is required or created.  This is what
    most will use.

    D. ~/.globus/

    Same as #3 but with ~/.globus.  The key still needs to be unencrypted.

    
2. Test the security setup.

   $ ./bin/cloud-client.sh --security


3. If you already have a credential and have not given your DN to the cloud
   administrators, do so sending the distinguished name printed after 'Identity'

   
Encrypted Keys and Proxy Credentials
------------------------------------

You might need to go the proxy credential route.  For example, you were given
an encrypted certificate, this is typically found in grid computing.

If you do not have a proxy credential in place using some other tool (at for
example "/tmp/x509up_u1000" where "1000" is your unix account ID number), you
can use an embedded program to run grid-proxy-init like so:
   
   $ ./bin/grid-proxy-init.sh

Note that grid-proxy-init does not follow the same search path as the cloud
client does when the cloud client is looking for unencrypted keys.  Instead,
it only looks for "~/.globus/usercert.pem" and "~/.globus/userkey.pem".

But you can specify the paths exactly if that is not where you keep the cert
and encrypted key:

   $ ./bin/grid-proxy-init.sh -cert /tmp/usercert.pem -key /tmp/userkey.pem

Issues?  Try our mailing list and/or run:

   $ ./bin/grid-proxy-init.sh -help

grid-proxy-init cannot find your credential's CA files?  They are normally in 
the "lib/certs" directory of the cloud client but you can override like so:

   $ export NIMBUS_X509_TRUSTED_CERTS="/path/to/certificates_directory"



Configuring The Cloud
---------------------

If you received this tarball from the cloud administrator or cloud website,
you probably have the proper configuration file already.

Examine the "conf/cloud.properties" file.

You can put the correct settings there or override them all via commandline
(see --extrahelp).  Or use different configuration files by using the --conf
option to specify an alternate.

Note that with clouds using Nimbus 2.5 and later, there is a repository ID and
symmetric key that you will need as well.  This is a cloud specific, consult
your cloud documentation first.  Again if you received this tarball from the
cloud administrator or cloud web application, you probably have the proper
configuration file already.

There is also a "conf/clouds/" directory that is used by the meta-cloud-client.
Each file in this directory contains the settings for a single cloud and is
identified by its filename. So the sample "conf/clouds/nimbus.properties" file
provides settings of the University of Chicago nimbus cloud and can be
referenced by the name "nimbus".



Uploading A Workspace To The Cloud
----------------------------------

1. Pick a local VM image to run.  The image must conform in this manner:

   a.  DHCP broadcast at boot
   b.  Partition file that is mounted to /dev/sda1
       (NOTE: on the Teraport cloud this may need to be /dev/xvda1 instead by default)
   c.  Running SSHd so that you can login.  Your SSH key will be written
       to /root/.ssh/authorized_keys on the VM before it is booted (see
       below for how to pick which key this is).

2. You can transfer an image to the cloud and then run it in a single command.
   Here is how to just do a transfer:

   $ ./bin/cloud-client.sh --transfer --sourcefile /tmp/some_image

There are also --download (get an image in your personal directory) and
--delete (delete an image in your personal directory) options.


SSH Notes
---------

The SSH public key to configure is set in the "conf/cloud.properties" file.
This will set the policy for root SSH login inside the VM, the service will
deliver this file to the VM's "/root/.ssh/authorized_keys" file.

You can change what public key file is used by changing the relevant property.
Alternatively, you can override the configuration via commandline flag.  You
can also override the other properties defined in that file by commandline
flag.  These extra commandlines are listed in --extrahelp.

Note you can remove the property to disable the dynamic SSH policy installation
from happening at all.


Running A Workspace
-------------------

1. If you want to run the image you transferred in the previous section,
   named 'some_image', here is how:

   $ ./bin/cloud-client.sh --run --name some_image --hours 1

2. If you want to transfer the image to the cloud and run it in the same
   command, you don't need the 'name' argument.  In the absence of the
   name argument, the image to run is deduced from the sourcefile argument.

   $ ./bin/cloud-client.sh --transfer --sourcefile /tmp/some_image --run --hours 1

   (The order of commandline flags does not matter, it will always transfer
    before running.)


After sending a run command, the network address picked for the VM will be
displayed.  Note that it will only be active once the VM is reported to be
running -- it just gets picked right away.

Because the public key file was installed to /root/.ssh/authorized_keys on
the VM before it boot (see last section), after it boots you should be able
to log in by running:

    ssh root@example.com

... where "example.com" is the network address that was assigned to the
workspace and printed to your screen.


Workspace Handle
----------------

Note the short name of the workspace you created, e.g. "vm-009".

This corresponds to a directory created under the "history" directory.  Files
from the instance creation are kept there, including log files and a handle
file (an "EPR" file, EPR stands for "endpoint reference").  The EPR allows you
to run subsequent management or query operations after creation.  The following
sections discuss these.

See -h for more options (terminate, save).


Running a Multi-Cloud Workspace
-------------------------------

The meta-cloud-client supports running a cluster across multiple clouds with
a single command. It does not currently support any other operations, so you
will still need to use the cloud-client to upload and manage your images on
each cloud.

You must have a cloud properties file in place for each cloud you want to use.
This is described above. You must also provide a cloud deployment document that
describes which cloud you want each workspace to be deployed to. Sample
documents are available in the "samples" directory. Note that the cloud names
you use in this document must have corresponding cloud properties files in the
"conf/clouds" directory. So if you have references to <cloud>nimbus</cloud> and
<cloud>stratus</cloud>, you must have valid cloud configuration files
"conf/clouds/nimbus.properties" and "conf/clouds/stratus.properties".

A multi-cloud workspace can be run with the command:

    $ ./bin/meta-cloud-client.sh --run --cluster cluster.xml \
        --deploy deploy.xml --hours 1
        
