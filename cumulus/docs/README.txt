cumulus : An Amazon s3 look-alike
=================================

Cumulus is an open source implementation of the Amazon S3 REST API.  It 
is packaged with the Nimbus (open source cloud computing software for 
science) however it can be used without nimbus as well.  Cumulus allows 
you to server files to users via a known and adopted REST API. Your
clients will be able to access your data storaging service with the 
Amazon S3 clients they already use.

Installation
============

Cumulus is easy to install.  Most users will be able to follow the 'Quick
Start' guide to achieve a successful installation, but more details are 
provided here for didactic purposes.

Required software:
------------------
- Python 2.5
- twisted.web
- boto 

Check prereqs
-------------
The following shows what software is needed and how to check for it on your
system:

1) python 2.5

    % python --version

2) twisted web

    % python -c "from twisted.web import server, resource"

3) sqlite

    % which sqlite3
    ...
    % python -c "import sqlite3"


Configuration
-------------

From the distribution base directory run the program ./cumulus-install.sh <target 
directory>.  
This program will create an environment setup script call 
'env.sh'.  At times this script may need to be sourced to have more 
convenient access to commands.  install.sh also creates a configuration 
file at ~/.nimbus/cumulus.ini

When the server is run it expects to find the file cumulus.ini in either:

     1) /etc/nimbus/cumulus.ini
     2) ~/.nimbus/cumulus.ini
     3) the same directory from which the program was launched
     4)  file pointed to by the environment variable CUMULUS_SETTINGS_FILE

This file is generated when the install.sh script is run, but it can be 
modified by the admin later.

Repository Location
-------------------

In the current implementation user files are stored on a locally mounted 
file system.  The reliability and performance of cumulus will thus be 
limited by the reliabilty and performance of that file system.  Because 
of this cumulus administrators will often want to specify a location for 
the repository.

Within the cumulus.ini file there is the [posix]:directory directive. 
This is the directory in which all of the files in the CB repository 
will be stored.  The names of the files in that directory will be 
obfuscated based on the bucket/key name.  In order to discover what file 
belongs to what bucket/key you must use go through the security module.  
If the authz module is in use there are a series of tools under the bin 
directory which start with cloudfs-* that can help with this.  In most 
cases there will be no need for a system administrator to use these 
tools and they are provided for expert usage for problimatic situations.

User Management
===============

NOTE: These tools are for use with cumulus only installations.  For 
information on managing users with a full Nimbus installation see 
http://www.nimbusproject.org

There are three tools that are included with cumulus that allow an 
administrator to create, remove, and see a listing of the current users. 
The tools are under the bin/ directory:

    cumulus-add-user
    cumulus-list-users
    cumulus-remove-user

Each tool has a good usage description via --help and do what would be 
expected of them based on their names.  List user allows an admin to 
query the system for user information.  The --report options can be used 
in conjunction with the -b option for scripts.

As an exmple of cumulus-list-users, lets review a situation where an an 
admin recalls that a user has a friendly name that starts with a 'b' but 
they do not know the entire name.  The admin needs to get the S3 ID and 
password for that user.  For that job cumulus-list-users.sh would be 
used in the following way:

    % ./bin/cumulus-list-users n\*
    friendly        : nimbusadmin@nimbusproject.org
    ID              : eqe0YoRAs2GT1sDvPZKAU
    password        : S9Ii7QqcCQxDecrezMn6o5frSFvXhThYWmCE4S7nAf
    quota           : None
    canonical_id    : 048db304-6b4c-11df-897b-001de0a80259

perhaps that is too much noise for the admin and they only want to see 
ID and password in a comma separate format:

    % ./bin/cumulus-list-users -r ID,password -b b\*
    eqe0YoRAs2GT1sDvPZKAU,S9Ii7QqcCQxDecrezMn6o5frSFvXhThYWmCE4S7nAf

Running the server:
===================

Running the cumulus server is very easy:

% ./bin/cumulus

If you wish to make a daemon process out of it you can do so around this 
program.  The program nimbusctl (provided with a full Nimbus 
distribution) can be looked to as an example.  Customization to the 
server are done in the cumulus.ini file (as explained above).

HTTPS
-----

In order to use HTTP the admin must have access to a match certificate and 
key file.  Such files are produced by Nimbus on installation and they are 
stored at: $NIMBUS_HOME/var/{hostkey, hostcert}.pem.  To enable https 
in cumulus the following section of cumulus.ini must be properly altered:

    [https]
    enabled=False
    key=<path to key file>
    cert=<path to certificate file>


Clients
=======

Since cumulus is protocol compliant with the S3 rest protocol all S3 
clients should be able to be used with cumulus.  We test with both 
the commandline tool s3cmd and the python API boto.

Using the s3cmd client
----------------------
Once you have the s3cmd successfully installed and configured you must 
modify the file: $HOME/.s3cfg in order to direct it at this server. 
Make sure the following key value pairs reflect the following changes:

    host_base = <hostname of service>
    host_bucket = <hostname of server>
    use_https = False

A sample s3cfg file can be found in the install directory in a file named
dot_s3cfg.


Using boto
----------
To use boto it is important to disable virtual host based buckets and
to point the client at the right server.  here is example code that
will instantiate a boto S3Connection for use with CB:

    cf = OrdinaryCallingFormat()
    hostname = "somehost.com"

    conn = S3Connection(id, pw, host=hostname, port=80, is_secure=False, calling_format=cf)


From there the S3Connection object can be used like any other.

Deficiencies
============

The following features of S3 are not currently implemented in cumulus

- Versioning
- Location
- Logging
- Object POST
- torrent

When using s3cmd it seems that all buckets must start with a capital
letter. However, some old versions of s3cmd (like the 0.9.8.3 version
shipping with Debian Lenny) will not accept a bucket starting with a
capital letter. Upgrade your copy of s3cmd to avoid this problem.
