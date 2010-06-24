Cumulus Authz
=============

This document describes the ACL data base used inside of cumulus and the 
command line tools that exist to manipulate it.  Most administrators will
not need this level of detail but it is provided for those that do.

Description
-----------

This module provides a ACL abstraction to data.  A SQL database is used
to track 'files' and users who have different privilege associated
with those files.

The data contained in the files themselves are stored outside of this
module.  A reference to the data (a full path name, a HDFS key, etc)
is stored in the database under a module specific name similar to how
an inode stores a filesystem name and internally references raw data.

This package contains a set of admin tools managing the objects in the
database as well as a python API for interacting with the database.


Installation
------------

In the base directory there is an install.sh script.  Change to the base
directory and run this script.  You may provide an installation directory
to have the needed files copied to an alternative location, or it can
be run without argument and thus installed to the same directory.

The install script will use sqlite to create a database file (other data
bases can be easily used as well but there is no auto configure for them).
at <base>/etc/authz.db.  This file is where all of the ACL data will be kept.

Dependencies
------------

Python 2.5
sqlite3
pysqlite3


Tools
-----

There are a few tools under the bin directory.  All the tools take a --help
parameter which will provide a brief description.  Below is some example
use:

#  Add two new users:

$ ./bin/cloudfs-adduser.sh -n BuzzTroll
User BuzzTroll added
$ ./bin/cloudfs-adduser.sh -n TimF
User TimF added

#  Add an alias (id and password) to BuzzTroll for use with s3

$ ./bin/cloudfs-adduser.sh -t s3 -a QLDVthrJ21WsWcp680PRk -k D16ZZi08lYJlLef0E2IoPKVku2qGer2A2JSM1pGhiW  BuzzTroll 
User BuzzTroll
Creating new alias s3:QLDVthrJ21WsWcp680PRk
updated the alias key

#  list all the current users and alias
$ ./bin/cloudfs-list-user.sh -a
User BuzzTroll
    s3 alias: QLDVthrJ21WsWcp680PRk
User TimF

# create a file owned by BuzzTroll
$ ./bin/cloudfs-touch.sh BuzzTroll a_file /etc/group

# This creates an 'inode' in the system that references the data contained 
# in the system file /etc/group
#
# now we check out its status
$ ./bin/cloudfs-stat.sh -a a_file
file    type    owner       perms
a_file  s3key   BuzzTroll   rwRW
        BuzzTroll   rwRW

# now lets let timf look at our file
$ ./bin/cloudfs-chmod.sh TimF a_file rw
changed a_file:s3key:None to rw for TimF

# and check the status again...
$ ./bin/cloudfs-stat.sh -a a_file
      file        type       owner        user       perms
    a_file       s3key   BuzzTroll   BuzzTroll        rwRW
                         BuzzTroll   BuzzTroll        rwRW
                         BuzzTroll        TimF        rw--

# this shows that both timf and buzztroll can access the file, but that
# buzztroll is the owner.  Lastly, there is a program to list the files:
$ ./bin/cloudfs-ls.sh 
s3key:a_file    BuzzTroll   /etc/group  None



User/Alias Relationship
=======================

In this system there is a notion of a canonical user (hearafter just 
User).  This is a single nimbus user.  In a simple sense it can be 
thought of as a 'person'.  Each user may have many different credentials 
and each credential is a way of accessing the system.  For example, a 
User may have a DOE x509 certificate, a unix account user name and 
password, and a symentric key pair (akin to those used by s3).  The User 
should be able to access their nimbus account information with anyone of 
these credentials (at least until an admin disallows some).  We 
accomplish this task with 'Aliases'.

An alias can be through of as a Users account, or better yet, as a 
User's credential to access their account.  All access is achieved via 
some Alias.  Once confirmed the canonical User associated with the alias 
is looked up and associated with that canonical user are various rights.

The canonical user table is below:

    create table users_canonical(
        id char(36) PRIMARY KEY,
        friendly_name varchar(64)
    );

The table is *very* simple because all it truely needs to be is a unique 
ID.  It is simply a way to distinguish one User from all others.  There 
is a friendly name in the table for the admins convenience.  The end 
user will probably never care about this table at all because all of 
their interactions with the system will be by way of the Alias.  
However, an admin might want a nice way to list out users.

Now lets take a look at the Alias table:

    create table user_alias(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id char(36) REFERENCES users_canonical(id) NOT NULL,
        alias_name varchar(64),
        friendly_name varchar(128),
        alias_type INTEGER REFERENCES user_alias_types(id) NOT NULL,
        alias_type_data varchar(1024),
        UNIQUE(alias_name, alias_type)
    );

Here we see that this table references the users_canonical table.  One 
users_canonical can have many user_alias associated with it.  Each 
user_alias row is uniquely identified by its alias_name, and alias_type. 
The alias_name is basically the account name for a give alias_type. an 
alias_type is an extendable enumeration known to the system. Here are a 
few examples:

    alias_name          |       alias type
    ----------------------------------------
    bresnaha            |       UNIX
    mM9II2KiZl6dlr...   |       s3
    /DC=org/DC=doeg...  |       DOE_Cert

Each row also has a friendly name which is intended to be used for some 
sort of display name.  In the case of a UNIX account the alias_name is 
friendly enough, but in the case of a s3 account their is a 'display 
name' associated with the account (typically an email address).  The 
friendly name is used to support such things.

The final field is 'alias_type_data'.  This field is used to store type 
specific data.  Its format is defined by the specific alias type. In the 
case of the UNIX type this is a password hash.  For s3 it is a secret 
key.





