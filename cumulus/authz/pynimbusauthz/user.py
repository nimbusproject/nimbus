import sqlite3
import pynimbusauthz
from pynimbusauthz.authz_exception import AuthzException
from pynimbusauthz.db import DB
import uuid

# a simple wrapper around readonly
class User(object):
    UNLIMITED = None

    def __init__(self, db_obj, uu=None, friendly=None, create=False):
        if uu == None or create:
            if uu != None:
                self.uuid = str(uu)
            else:
                self.uuid = str(uuid.uuid1())
            if friendly == None:
                friendly = self.uuid
            s = "INSERT INTO users_canonical(id, friendly_name) values(?, ?)"
            data = (self.uuid,friendly,)

            db_obj._run_no_fetch(s, data)
            self.friendly_name = friendly
        else:
            self.uuid = str(uu)
            #  verify it is in DB
            s = "SELECT id,friendly_name from users_canonical where id = ?"
            data = (uu,)
            row = db_obj._run_fetch_one(s, data)
            if row == None or len(row) < 1:
                raise AuthzException('USER_EXISTS', "user id %s not found" % (uu))
            self.friendly_name = str(row[1])

        self.db_obj = db_obj
        self.valid = True

    def get_id(self):
        return self.uuid

    def get_friendly(self):
        return self.friendly_name

    def destroy(self):
        s = "DELETE FROM users_canonical WHERE id = ?"
        data = (self.uuid,)
        self.db_obj._run_no_fetch(s, data)
        valid = False

    # remove everything associated with a user
    # including files if the are completely orphaned
    def destroy_brutally(self):
        s = "DELETE FROM object_quota where user_id = ?"
        data = (self.uuid,)
        self.db_obj._run_no_fetch(s, data)
        s = "DELETE FROM user_alias where user_id = ?"
        data = (self.uuid,)
        self.db_obj._run_no_fetch(s, data)
        s = "DELETE FROM object_acl where user_id = ?"
        data = (self.uuid,)
        self.db_obj._run_no_fetch(s, data)
        s = "delete from objects where parent_id in (select id from objects where owner_id = ?)"
        data = (self.uuid,)
        self.db_obj._run_no_fetch(s, data)
        s = "DELETE FROM objects where owner_id = ?"
        data = (self.uuid,)
        self.db_obj._run_no_fetch(s, data)
        self.destroy()

    # get every alias associate with a user
    # 
    # return list of UserAlias.  A  list shoudl be ok here because for any
    # given user there should be a very small number
    def get_all_alias(self):
        # picking exact fields is where this gets a little nasty
        s = "SELECT " + UserAlias.get_select_str() + """ 
            FROM
                user_alias
            WHERE
                user_id = ?"""
        data = (self.uuid,)
        c = self.db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_UserAlias)
        return c

    # return a single alias object or None
    def get_alias(self, alias_name, alias_type=None):
        s = "SELECT " + UserAlias.get_select_str() + """ 
            FROM
                user_alias
            WHERE
                user_id = ? and alias_name = ?"""
        if alias_type == None:
            data = (self.uuid, alias_name,)
        else:
            at = pynimbusauthz.alias_types[alias_type]
            s = s + "and alias_type = ?"
            data = (self.uuid, alias_name, at,)

        row = self.db_obj._run_fetch_one(s, data)
        if row == None or len(row) == 0:
            return None
        return UserAlias(self.db_obj, row)

    # return a single alias object or None
    def get_alias_by_friendly(self, friendly_name, alias_type=None):
        s = "SELECT " + UserAlias.get_select_str() + """ 
            FROM
                user_alias
            WHERE
                user_id = ? and friendly_name = ?"""
        if alias_type == None:
            data = (self.uuid, friendly_name,)
        else:
            at = pynimbusauthz.alias_types[alias_type]
            s = s + "and alias_type = ?"
            data = (self.uuid, friendly_name, at,)

        row = self.db_obj._run_fetch_one(s, data)
        if row == None or len(row) == 0:
            return None
        return UserAlias(self.db_obj, row)


    # get a list of alias of a given type
    def get_alias_by_type(self, alias_type):
        # picking exact fields is where this gets a little nasty
        at = pynimbusauthz.alias_types[alias_type]
        s = "SELECT " + UserAlias.get_select_str() + """ 
            FROM
                user_alias
            WHERE
                user_id = ? and alias_type = ?"""
        data = (self.uuid, at,)
        c = self.db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_UserAlias)
        return c

    def create_alias(self, alias_name, alias_type, friendly_name, alias_data=None):
        at = pynimbusauthz.alias_types[alias_type]
        i = "INSERT INTO user_alias(user_id, alias_name, alias_type"
        v = "values(?, ?, ?"
        data = [self.uuid, alias_name, at]
        i = i + ", friendly_name"
        v = v + ", ?"
        data.append(friendly_name)

        if alias_data != None:
            i = i + ", alias_type_data"
            v = v + ", ?"
            data.append(alias_data)

        s = i + ')' + v + ')'

        self.db_obj._run_no_fetch(s, data)
        return self.get_alias(alias_name, alias_type)

    def __str__(self):
        return str(self.uuid)

    def __eq__(self, other):
        if other == None:
            return False
        return self.uuid == other.uuid

    def find_user(db_obj, pattern):
        if len(pattern) == 36:
            s = "select id from users_canonical where id = ?"
            data = (pattern,)
        elif len(pattern) == 0:
            s = "select id from users_canonical where id like '%%'"
            data = []
        else:
            s = "select id from users_canonical where id like '%%%s%%'" % (pattern)
            data = []
        c = db_obj._run_fetch_iterator(s, data, _convert_user_row_to_User)
        return c
    find_user = staticmethod(find_user)

    # should probably have to force in the %s
    # return all matching alias
    def find_alias(db_obj, alias_name, alias_type=None):
        s = "SELECT " + UserAlias.get_select_str() 
        s = s + " FROM user_alias WHERE alias_name LIKE ? "
        data = [alias_name,]
        if alias_type != None:
            at = pynimbusauthz.alias_types[alias_type]
            s = s + "and alias_type = " + str(at)

        c = db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_UserAlias)
        return c
    find_alias = staticmethod(find_alias)

    def get_quota(self, object_type=pynimbusauthz.object_type_s3):
        ot = pynimbusauthz.object_types[object_type]
        s="SELECT quota from object_quota where user_id = ? and object_type = ?"
        data = [self.uuid, ot]
        row = self.db_obj._run_fetch_one(s, data)
        if row == None or len(row) == 0:
            return User.UNLIMITED
        return row[0]

    def get_quota_usage(self, object_type=pynimbusauthz.object_type_s3):
        s = "SELECT SUM(object_size) FROM objects where owner_id = ? and object_type = ?"
        ot = pynimbusauthz.object_types[object_type]
        data = [self.uuid, ot]
        row = self.db_obj._run_fetch_one(s, data)
        if row == None or len(row) == 0 or row[0] == None:
            return 0
        return row[0]

    def set_quota(self, quota, object_type=pynimbusauthz.object_type_s3):
        ot = pynimbusauthz.object_types[object_type]
        if quota == User.UNLIMITED:
            d = "DELETE FROM object_quota where user_id = ? and object_type = ?"
            data = [self.uuid, ot]
            self.db_obj._run_no_fetch(d, data)
        else:
            s = "SELECT quota from object_quota where user_id = ? and object_type = ?"
            data = [self.uuid, ot]
            row = self.db_obj._run_fetch_one(s, data)
            if row == None or len(row) == 0:
                s = "INSERT into object_quota(user_id, object_type, quota) values(?, ?, ?)"
                data = [self.uuid, ot, quota]
            else:
                s = "UPDATE object_quota SET quota = ? WHERE user_id = ? and object_type = ?"
                data = [quota, self.uuid, ot]
            self.db_obj._run_no_fetch(s, data)


    def get_user(db_obj, user_id):
        s = "select id from users_canonical where id = ?"
        data = (user_id,)
        c = db_obj._run_fetch_iterator(s, data, _convert_user_row_to_User)
        ua = list(c)
        # what if it is more than 1 user?
        if len(ua) < 1:
            return None
        return ua[0]
    get_user = staticmethod(get_user)

    def get_user_by_friendly(db_obj, friendly_name):
        s = "select id from users_canonical where friendly_name = ?"
        data = (friendly_name,)
        c = db_obj._run_fetch_iterator(s, data, _convert_user_row_to_User)
        ua = list(c)
        # what if it is more than 1 user?
        if len(ua) < 1:
            return None
        return ua[0]
    get_user_by_friendly = staticmethod(get_user_by_friendly)

    def find_user_by_friendly(db_obj, friendly_pattern):
        s = "select id from users_canonical where friendly_name LIKE ? "
        data = [friendly_pattern,]
        c = db_obj._run_fetch_iterator(s, data, _convert_user_row_to_User)
        return c
    find_user_by_friendly = staticmethod(find_user_by_friendly)


class UserAlias(object):

    cols = {}
    cols['id'] = None
    cols['user_id'] = None
    cols['alias_name'] = None
    cols['alias_type'] = None
    cols['alias_type_data'] = None
    cols['friendly_name'] = None

    count = 0
    for c in cols.keys():
        cols[c] = count
        count = count + 1

    def get_select_str():
        s = ""
        delim = ""
        for x in UserAlias.cols.keys():
            s = s + delim + x
            delim = ","

        return s
    get_select_str = staticmethod(get_select_str)

    def __init__(self, db, row):
        self.db = db
        self.id = row[UserAlias.cols['id']]
        self.user = User(db, row[UserAlias.cols['user_id']])
        self.alias_name = row[UserAlias.cols['alias_name']]
        self.alias_type = row[UserAlias.cols['alias_type']]
        self.friendly_name = row[UserAlias.cols['friendly_name']]
        self.alias_type_data = row[UserAlias.cols['alias_type_data']]
        self.valid = True

        if self.alias_name != None:
            self.alias_name = str(self.alias_name)
        if self.friendly_name != None:
            self.friendly_name = str(self.friendly_name)
        if self.alias_type_data != None:
            self.alias_type_data = str(self.alias_type_data)

    # destroy just this alias, all other data about the user remains
    def remove(self):
        s = "DELETE FROM user_alias WHERE id = ?"
        data = (self.id,)
        self.db._run_no_fetch(s, data)
        valid = False

    # return the canonical user associated with this alias
    def get_canonical_user(self):
        return self.user

    def get_name(self):
        return self.alias_name

    def get_friendly_name(self):    
        return self.friendly_name

    def get_type(self):
        return pynimbusauthz.reverse_lookup_type(pynimbusauthz.alias_types, self.alias_type)

    def get_data(self):
        return self.alias_type_data

    def set_name(self, alias_name):
        s = "UPDATE user_alias set alias_name = ? where id = ?"
        data = (alias_name,self.id,)
        self.db._run_no_fetch(s, data)
        self.alias_name = alias_name

    def set_data(self, data_key):
        s = "UPDATE user_alias set alias_type_data = ? where id = ?"
        data = (data_key,self.id,)
        self.db._run_no_fetch(s, data)
        self.alias_type_data = data_key

    def find_alias(db_obj, pattern):
        s = "select "+ UserAlias.get_select_str()+" from user_alias where alias_name like '%%%s%%'" % (pattern)
        data = []
        c = db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_UserAlias)
        return c
    find_alias = staticmethod(find_alias)

    def find_alias_by_friendly(db_obj, fn, type=pynimbusauthz.alias_type_s3):
        at = pynimbusauthz.alias_types[type]
        s = "select "+ UserAlias.get_select_str()+" from user_alias where friendly_name = ? and alias_type = ?"
        data = (fn, at,)
        c = db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_UserAlias)
        return c
    find_alias_by_friendly = staticmethod(find_alias_by_friendly)

    def find_all_alias_by_friendly(db_obj, fn, type=pynimbusauthz.alias_type_s3):
        at = pynimbusauthz.alias_types[type]
        s = "select "+ UserAlias.get_select_str()+" from user_alias where friendly_name LIKE ? and alias_type = ?"
        data = (fn, at,)
        c = db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_UserAlias)
        return c
    find_all_alias_by_friendly = staticmethod(find_all_alias_by_friendly)


    def __str__(self):
        return str(self.alias_name) + ":" + str(self.alias_type)

    def __eq__(self, other):
        if other == None:
            return False
        return (self.alias_name == other.alias_name and self.alias_type == other.alias_type)


# User Functions
# ==============

# returns password and canonical info
# returns basically a JOIN of users_canonical and user_alias
# 
#  can it be many rows?  should a user ever be able to have 2 ssh keys
#  with the same user id?  i decided no.  unix account example: this is 
#  akin to having a sinle user account with many passwords.  counter
#  example is gsi DN.  you could have a single unix account that is 
#  accesed by many DNs, however i that example the unix account is the
#  canonical user and the DN is the user name
#
#  returns an alias class

def _convert_alias_row_to_UserAlias(db, row, args):
    return UserAlias(db, row)

def _convert_user_row_to_User(db, row, args):
    return User(db, row[0])

