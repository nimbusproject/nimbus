cumulus : An Amazon s3 look-alike
=================================

Required software:
------------------
- Python
- twisted
- boto (for security and client side testing)

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
From the distribution base directory run the program 
./install [<target directory>].  If no target is specified it will install
to the same directory.  This program will create an environment setup 
script call 'env.sh'.  At times this script may need to be sourced to
have more convenient access to commands.  install.sh also creates
a configuration file at ~/.nimbus/cumulus.ini

When the server is run it expects to find the file cumulus.ini in either:

     1) /etc/nimbus/cumulus.ini
     2) ~/.nimbus/cumulus.ini
     3) the same directory from which the program was launched
     4)  file pointed to by the environment variable CUMULUS_SETTINGS_FILE

This file has the following format:

    [cb]

    installdir = /home/bresnaha/Dev/Nimbus/nimbus/cumulus
    port = 8888
    hostname = laptroll

    [backend]
    type=posix
    data_dir=/home/bresnaha/Dev/Nimbus/nimbus/cumulus/posixdata
    [security]
    type=authz
    security_dir=/home/bresnaha/Dev/Nimbus/nimbus/cumulus/posixauth
    authzdb=/home/bresnaha/Dev/Nimbus/nimbus/cumulus/etc/authz.db


    [log]
    level=INFO
    file=/home/bresnaha/Dev/Nimbus/nimbus/cumulus/log/cumulus.log

This file is generated when the install.sh script is run, but it can 
be modified by the admin later.  The security type is choosen based on
the presence of sqlite3.  If it is installed with the needed python
libraries then authz is used.  If not a much more basic and less 
scalable security module is used.

The [posix]:directory is the directory in which all of the files in the CB 
repository will be stored.  The names of the files in that directory 
will be obfuscated based on the bucket/key name.  In order to discover 
what file belongs to what bucket/key you must use go through the security
module.  If the authz module is in use there are a series of tools under
the bin directory that can help with this.

Creating a new user
-------------------
The program ./bin/cumulus-add-user.sh can add cumulus users to the 
configured security module.  For example:

./bin/cumulus-add-user.sh -g -n buzztroll@nimbusproject.com
Created a new user with:
ID:  5hWVNH26CpuWbQJhthCqr Key: suSZmEQEefbVN0piTM790lSadBSuf41FtZrl2SlezA

This will add a user with the display name of buzztroll@nimbusproject.com
and it will echo out the ID and Key

Running the server:
-------------------
If you are running on port 80 you will need to be root.

% ./bin/cumulus.sh

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
-----------
POST, and COPY are not implemented

When using s3cmd it seems that all buckets must start with a capital 
letter

In order to use virtual host based buckets DNS wildcards must be used.
