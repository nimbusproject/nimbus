-- access types
-- ============
-- This is a small table consiting of just 5 rows (ripped off from s3 doc):
--  r: read object 
--  w: write object
--  x: execute object (?)
--  R: read acl about object 
--  W: write acl about object (full control except changing owner)
create table access_types(
    mod char PRIMARY KEY,
    description varchar(64)
);

insert into access_types(mod, description) values ('r', 'read data');
insert into access_types(mod, description) values ('w', 'write data');
insert into access_types(mod, description) values ('R', 'read ACL');
insert into access_types(mod, description) values ('W', 'write ACL');


-- object_types
-- ===========
-- Objects are like files.  Each has a 'type' which is an access method 
-- presented to the user.  This will be needed if we are to expose both
-- a GridFTP interface and a s3 interface.
--
--  gridftp
--  hdfs
--  s3 bucket
--  s3 key
create table object_types(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name varchar(64) UNIQUE NOT NULL
);
insert into object_types(name) values ('s3');
insert into object_types(name) values ('gridftp');
insert into object_types(name) values ('hdfs');

-- users_canonical
-- ==============
-- This table is the canonical user description.  A user may have many
-- IDs and credentials and such that references this single ID
create table users_canonical(
    id char(36) PRIMARY KEY,
    friendly_name varchar(64) UNIQUE NOT NULL
);
insert into users_canonical(id, friendly_name) values ('CumulusAuthenticatedUser', 'CumulusAuthenticatedUser');
insert into users_canonical(id, friendly_name) values ('CumulusPublicUser', 'CumulusPublicUser');

-- insert into users_canonical(id, friendly_name) values ('CumulusPublicUser', 'CumulusPublicUser');
-- insert into users_canonical(id, friendly_name) values ('CumulusAuthenticatedUser', 'CumulusAuthenticatedUser');
-- user_alias_types
-- ================
-- vairous types of user identifications mechanisms:
--  auth_tokens: S3
--  x509: gsi
--  ssh:  public key
--  unix: password hash
create table user_alias_types(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name varchar(64) UNIQUE NOT NULL
);
insert into user_alias_types(name) values ('s3');
insert into user_alias_types(name) values ('x509');
insert into user_alias_types(name) values ('ssh');
insert into user_alias_types(name) values ('unix');

-- user_alias
-- ==========
-- this table references the canonical user.  it allows us to have many
-- means of identifying a single user.  For example auth tokens, ssh, gsi.
-- The format of the type_data is defined by the alias_type
--
-- i think we can be this generic but i am not 100% sure
create table user_alias(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id char(36) REFERENCES users_canonical(id) NOT NULL,
    alias_name varchar(256) NOT NULL,
    friendly_name varchar(256) NOT NULL,
    alias_type INTEGER REFERENCES user_alias_types(id) NOT NULL,
    alias_type_data varchar(1024),
    UNIQUE(alias_name, alias_type),
    UNIQUE(friendly_name, alias_type)
);

insert into user_alias(user_id, alias_name, friendly_name, alias_type) values ('CumulusAuthenticatedUser', 'CumulusAuthenticatedUser', 'CumulusAuthenticatedUser', 1);
insert into user_alias(user_id, alias_name, friendly_name, alias_type) values ('CumulusPublicUser', 'CumulusPublicUser', 'CumulusPublicUser', 1);
-- the actual data.
-- this can be a file, a dhfs file key, or a gridftp url (?)
-- it is names speced by the url spec
--
-- this is broken out because there could be many objects that
-- reference the same physical data.
--
-- For example, we provide 2 access mechanisms to a single VM:
--  GridFTP and s3.  The s3 object might present its clients
--  with a different path to the physical data than the GridFTP
--  server does
-- create table physical_data(
---     id INTEGER PRIMARY KEY AUTOINCREMENT,
--    data_key varchar(1024)
--);
-- ditching this for now.  it seems over engineered.


-- parent id may only be useful for s3
-- data key is some sort of reference to where it actually is
-- name is its name in the given object_type names space
--
-- for any given object_type the name an parent id must be null
-- for s3:
--      if the object is a bucket it will have a NULL parent
--      and thus all buckets will be unique over s3 space
--      if it is a key the key will be unique in the bucket
--      this should meet s3 requirements
--
-- if it is a gridftp file, name can just be a full path
-- with a null parent_id.  this will ensure a unique full path
-- to a file which should be consistant with unix file systems
create table objects(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name varchar(1024) NOT NULL,
    friendly_name varchar(1024),
    owner_id char(36) REFERENCES users_canonical(id) NOT NULL,
    data_key varchar(1024) NOT NULL,
    object_type INTEGER REFERENCES object_types(id) NOT NULL,
    parent_id INTEGER REFERENCES objects(id) DEFAULT NULL,

    md5sum CHAR(32),
    object_size INTEGER DEFAULT 0,
    creation_time DATETIME,
    UNIQUE(object_type, name, parent_id)
);

-- object_acl
-- ==========
--  This is a join table for descovering acl permissions associated with 
--  a file
create table object_acl(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id char(36) REFERENCES users_canonical(id) NOT NULL,
    object_id INTEGER REFERENCES objects(id) NOT NULL,
    access_type_id CHAR REFERENCES access_types(mod) NOT NULL,
    unique(user_id, object_id, access_type_id)
);


create table object_quota(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id char(36) REFERENCES users_canonical(id) NOT NULL,
    object_type INTEGER REFERENCES object_types(id) NOT NULL,
    quota INTEGER NOT NULL,
    UNIQUE(user_id, object_type)
);


