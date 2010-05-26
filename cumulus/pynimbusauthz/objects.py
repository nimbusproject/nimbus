import sqlite3
import pynimbusauthz
from pynimbusauthz.user import User
from pynimbusauthz.db import DB
import uuid
from datetime import datetime
import itertools
import time

#
#
class File(object):

    cols = {}
    cols['id'] = None
    cols['name'] = None
    cols['owner_id'] = None
    cols['data_key'] = None
    cols['object_type'] = None
    cols['parent_id'] = None
    cols['md5sum'] = None
    cols['object_size'] = None
    cols['creation_time'] = None

    count = 0
    for c in cols.keys():
        cols[c] = count
        count = count + 1

    def get_select_str():
        s = ""
        delim = ""
        for x in File.cols.keys():
            s = s + delim + x
            delim = ","
        return s
    get_select_str = staticmethod(get_select_str)

    def __init__(self, db_obj, row):
        self.db_obj = db_obj
        self.id = row[File.cols['id']]
        self.name = row[File.cols['name']]
        user_id = row[File.cols['owner_id']]
        self.owner = User(self.db_obj, user_id)
        self.data_key = row[File.cols['data_key']]
        ot = row[File.cols['object_type']]
        self.object_type = pynimbusauthz.reverse_lookup_type(pynimbusauthz.object_types, ot)
        file_id = row[File.cols['parent_id']]
        if file_id == None:
            self.parent = None
        else:
            self.parent = File.get_file_from_db_id(db_obj, file_id)
        self.md5sum = row[File.cols['md5sum']]
        self.object_size = row[File.cols['object_size']]
        ctm = row[File.cols['creation_time']]
        if ctm != None:
            ndx = ctm.rfind(".")
            if ndx > 0:
                ctm = ctm[:ndx]
            self.creation_time = time.strptime(ctm, "%Y-%m-%d %H:%M:%S")
        else:
            self.creation_time = None

    def get_owner(self):
        return self.owner

    def get_parent(self):
        return self.parent

    def get_name(self):
        return self.name

    def get_size(self):
        return self.object_size

    def get_md5sum(self):
        return str(self.md5sum)

    def get_creation_time(self):
        return self.creation_time

    def get_id(self):
        return self.id

    def get_data_key(self):
        return str(self.data_key)

    def get_object_type(self):
        return self.object_type

    def delete(self):
        d = "DELETE FROM object_acl WHERE object_id = ?"
        d2 = "DELETE FROM objects WHERE id = ?"
        data = (self.id,)
        self.db_obj._run_no_fetch(d, data)
        self.db_obj._run_no_fetch(d2, data)

    def get_all_users(self):
        s = "SELECT DISTINCT user_id FROM object_acl WHERE object_id = ?"
        data = (self.get_id(),)
        c = self.db_obj._run_fetch_iterator(s, data, pynimbusauthz.user._convert_user_row_to_User)
        return c

    def get_all_user_files(self):
        s = "SELECT DISTINCT user_id FROM object_acl WHERE object_id = ?"
        data = (self.get_id(),)
        c = self.db_obj._run_fetch_iterator(s, data, _convert_object_row_to_UserFile, [self])
        return c

    def get_file_from_db_id(db_obj, id):
        s = "SELECT " + File.get_select_str() + """
            FROM objects
            WHERE id = ?
        """
        data = (id,)
        row = db_obj._run_fetch_one(s, data)
        if row == None or len(row) == 0:
            return None
        return File(db_obj, row)
    get_file_from_db_id = staticmethod(get_file_from_db_id)

    def get_file(db_obj, name, object_type, parent=None):
        # look it up
        ot = pynimbusauthz.object_types[object_type]
        s = "SELECT " + File.get_select_str() + """
            FROM objects
            WHERE name = ? and object_type = ?
        """
        data = (name, ot,)
        if parent != None:
            s = s + " and parent_id = ?"
            data = (name, ot, parent.get_id(),)
        else:
            s = s + " and parent_id IS NULL"

        row = db_obj._run_fetch_one(s, data)
        if row == None or len(row) == 0:
            return None
        return File(db_obj, row)
    get_file = staticmethod(get_file)

    def create_file(db_obj, name, owner, data_key, object_type, parent=None, size=None, md5sum=None):
        ot = pynimbusauthz.object_types[object_type]

        data = [name, owner.get_id(), data_key, ot]
        key_str = ""
        val_str = ""
        if parent != None:
            data.append(parent.get_id())
            key_str = ", parent_id"
            val_str = ", ?"

        if size != None:
            data.append(size)
            key_str = key_str + ", object_size"
            val_str = val_str + ", ?"

        if md5sum != None:
            data.append(md5sum)
            key_str = key_str + ", md5sum"
            val_str = val_str + ", ?"

        creation_time = datetime.now()
        data.append(creation_time)
        key_str = key_str + ", creation_time)"
        val_str = val_str + ", ?)"
            
        
        s = "INSERT INTO objects(name, owner_id, data_key, object_type"+key_str
        s = s + " values(?, ?, ?, ?" + val_str
        db_obj._run_no_fetch(s, data)
        f = File.get_file(db_obj, name, object_type, parent)
        # need to set default permissions
        uf = UserFile(f)
        uf.chmod("rwRW")
        return f
    create_file = staticmethod(create_file)

    # return all of the files children
    def get_all_children(self, limit=None, match_str=None, clause=None):

        data = []
        s = "SELECT " + File.get_select_str() + """
        FROM objects WHERE 
        """
        s = s + " parent_id = ?"
        data.append(self.id)
        if match_str != None:
            s = s + " and name LIKE '" + match_str + "'"

        if clause != None:
            s = s + clause
        if limit != None:
            s = s + " LIMIT %d" % (limit)

        c = self.db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_File, None)
        return c

    def find_files(db_obj, pattern, object_type, parent=None):
        # look it up
        ot = pynimbusauthz.object_types[object_type]
        s = "SELECT " + File.get_select_str() + """
            FROM objects
            WHERE name LIKE '%%%s%%' and object_type = %d""" % (pattern, ot)
        data = []
        if parent != None:
            s = s + " and parent_id = " + parent.get_id()

        c = db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_File)
        return c

    find_files = staticmethod(find_files)

    def __str__(self):
        return self.name + ":" + self.object_type + ":" + str(self.parent)

    def __eq__(self, other):
        if other == None:
            return False
        return str(self) == str(other)

    def get_user_files(db_obj, user, name=None, type=None, parent=None, root=False):
        s = "SELECT " + File.get_select_str() + """ 
                FROM objects where owner_id = ? """
        data = [user.get_id()]
        if root:
            s = s + " AND parent_id IS NULL"
        elif parent != None:
            s = s + " AND parent_id = ?"
            data.append(parent.get_id())
        if type != None:
            ot = pynimbusauthz.object_types[type]
            data.append(ot)
            s = s + " AND object_type = ?"
        if name != None:
            data.append(name)
            s = s + " AND name = ?"
        c = db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_File)
        return c
    get_user_files = staticmethod(get_user_files)


#
#  this is a combo of the file object and the user object but with
#  all the permissions for that user populated
class UserFile(object):

    # if no user specificed populate with the owner
    def __init__(self, fileObj, user=None):

        self.db_obj = fileObj.db_obj
        self.owner = fileObj.get_owner()
        if user == None:
            self.user = self.owner
        else:
            self.user = user

        self.file = fileObj

        self._get_acls()

    # look up all the user acl information
    def _get_acls(self):
        self.perm_list = []
        self.read = False
        self.write = False
        self.read_acl = False
        self.write_acl = False
        s = """SELECT at.mod
                FROM
                    object_acl oa,
                    access_types at
                WHERE
                    oa.user_id = '"""
        s = s + self.user.get_id()
        s = s + """' and oa.object_id = """ +str(self.file.get_id())
        s = s + """  and
                    oa.access_type_id = at.mod 
            """
#        data = (self.user.get_id(), self.file.get_id(),)
        data = ()
        rows = self.db_obj._run_fetch_all(s, data)
        if rows == None or len(rows) == 0:
            return False

        for r in rows:
            if r[0] == 'r':
                self.read = True
            elif r[0] == 'w':
                self.write = True
            elif r[0] == 'R':
                self.read_acl = True
            elif r[0] == 'W':
                self.write_acl = True
            self.perm_list.append(r[0])

        return True

    # change the file permissions if you are allowed to write to them
    def chmod(self, perms_list, user=None):
        if not self.write_acl and self.user != self.owner:
            raise Exception("The user is not allowed to change permissions")
        for p in perms_list:
            if p not in "rwRW":
                raise Exception("invalid permission %s" % (p))

        if user == None:
            user = self.user

        # remove all
        s = "DELETE FROM object_acl WHERE user_id = ? and object_id = ?"
        data = (user.get_id(), self.file.get_id(),)
        self.db_obj._run_no_fetch(s, data)
        i = """INSERT INTO 
            object_acl(user_id, object_id, access_type_id) 
            values(?, ?, ?)"""
        for p in perms_list:
            data = (user.get_id(), self.file.get_id(), p)
            self.db_obj._run_no_fetch(i, data)
        self._get_acls()

    # get the file permissions if you are allowed to read them
    def get_perms(self, force=False):
        if not self.read_acl and not force:
            raise Exception("you cant read it")
        perms_list = ""
        if self.read:
            perms_list = perms_list + "r"
        if self.write:
            perms_list = perms_list + "w"
        if self.read_acl:
            perms_list = perms_list + "R"
        if self.write_acl:
            perms_list = perms_list + "W"

        return perms_list

    # return the physical data associated with the file
    def get_file(self):
        return self.file

    # return T/F based on whether the user has the access described int the
    # parameters
    def can_access(self, perm_list):
        for p in perm_list:
            if p not in self.perm_list:
                return False
        return True

    # returns a User object
    def get_owner(self):
        return self.owner

    # returns a User object
    def get_user(self):
        return self.user

    # returns all of the UserFiles that have this file as a parent
    # the user files will be returned with this objects user
    #
    # XXX this is NOT the most efficient use of the database.  a better
    # way would be to use a join against files and permission and inflate
    # state from there.  As is this will cause a SELECT in a loop
    # for every row found.  Ulitmately it could be a perf problem
    def get_all_children(self):
        if not self.read:
            raise Exception("You cannot get a directory listing without read rights")
        s = "SELECT " + File.get_select_str() + """
        FROM objects WHERE parent_id = ?
        """
        parent_id = self.file.get_id()
        data = (parent_id,)
        c = self.db_obj._run_fetch_iterator(s, data, _convert_alias_row_to_UserFile, [self.user])
        return c

    def __str__(self):
        return str(self.user) + ":" + str(self.file)

    def __eq__(self, other):
        if other == None:
            return False
        return str(self) == str(other)


def _convert_alias_row_to_File(db, row, args):
    return File(db, row)

def _convert_alias_row_to_UserFile(db, row, args):
    user = args[0]
    f = File(db, row)
    return UserFile(f, user)

def _convert_object_row_to_UserFile(db, row, args):
    user_id = str(row[0])
    f = args[0]
    u = User(db, user_id)
    return UserFile(f, u)
