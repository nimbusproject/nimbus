import base64
import hmac
import boto
import os
import boto.utils
import traceback
import sys
import pycb
import logging
import errno
from pycb.cbException import cbException
import time
import stat
import urllib
import glob
from pycb.cbObject import cbObject

install_dir = "/usr/local"

def getPosixBaseDir():
    global install_dir
    return install_dir + "/"

def getPosixAuthDir():
    return getPosixBaseDir() + "cbauth/"

def getPosixBuckerDir():
    return getPosixBaseDir() + "buckets/"

def getPosixBuckerMetaDir():
    return getPosixBaseDir() + "buckets_acl/"

try:
    from hashlib import sha1 as sha
    from hashlib import sha256 as sha256
except ImportError:
    import sha

class cbPosixUserObject(object):
    def __init__(self, id, display_name, password):
        self.id = id
        self.display_name = display_name
        self.password = password

    def get_password(self):
        return self.password

    def hashFile(self, name):
        return urllib.quote(name, '')

    def unhashFile(self, name):
        return urllib.unquote(name)

    def isOwner(self, bucketName, objectName=None):
        return self.getOwner(bucketName, objectName)[0] == self.id

    def get_owner(self, bucketName, objectName=None):
        if objectName == None:
            f = self.getBucketPermFile(bucketName)
        else:
            f = self.getObjectFile(bucketName, objectName)
        auth_file = open(f, 'r')
        data_key = auth_file.readline().strip()
        (own_id, display_name) = self.read_owner_line(auth_file)
        auth_file.close()
        return (own_id, display_name)

    def getDataKey(self, f):
        auth_file = open(f, 'r')
        data_key = auth_file.readline().strip()
        auth_file.close()
        return data_key


    def get_id(self):
        return self.id

    def get_display_name(self):
        return self.display_name

    def getBucketPermFile(self, bucketName):
        return getPosixBuckerMetaDir() + bucketName

    def getBucketDir(self, bucketName):
        return getPosixBuckerDir() + bucketName

    def getObjectFile(self, bucketName, objectName):
        return getPosixBuckerDir()+bucketName+"/"+self.hashFile(objectName)

    def read_owner_line(self, auth_file):
        line = auth_file.readline()
        linea = line.split("::", 1)
        id = linea[0].strip()
        display_name = linea[1].strip()
        return (id, display_name)

    def read_auth_line(self, auth_file):
        line = auth_file.readline()
        if not line:
            return None
        linea = line.split("::", 2)
        id = linea[0].strip()
        display_name = linea[1].strip()
        perms = linea[2].strip()
        return (id, display_name, perms)

    # XXX should add locking
    def find_my_perms(self, f):
        auth_file = open(f, 'r')
        data_key = auth_file.readline().strip()
        owner_a = self.read_owner_line(auth_file)
        line = self.read_auth_line(auth_file)

        my_perms = ""
        while line != None:
            (user, display, perms) = line
            if user == self.getID():
                # always use the last entry
                my_perms = perms
            line = self.read_auth_line(auth_file)
        auth_file.close()
        return (my_perms, data_key)

    # XXX should add locking
    def setowner_perms(self, f, data_key, perms):
        auth_file = open(f, 'w')
        auth_file.write(data_key)
        auth_file.write('\n')
        auth_file.write(self.getID()+"::"+self.getDisplayName())
        auth_file.write('\n')
        auth_file.write(self.getID()+"::"+self.getDisplayName()+"::"+perms)
        auth_file.write('\n')
        auth_file.close()

    # XXX should add locking
    def add_perms(self, f, uid, perms):
        auth_file = open(f, 'a')
        auth_file.write(uid+"::"+self.getDisplayName()+"::"+perms)
        auth_file.write('\n')
        auth_file.close()

    def put_bucket(self, bucketName, perms="RWrw"):
        rc = 'InvalidBucketName'
        rc_msg = "Failed to delete"
        try:
            f = self.getBucketPermFile(bucketName)
            d = self.getBucketDir(bucketName)
            os.mkdir(d)
            self.setowner_perms(f, bucketName, perms)
            pycb.log(logging.INFO, "created bucket %s" %(bucketName))
            return d
        except OSError, (OsEx):
            if OsEx.errno == errno.EEXIST:
                rc = 'BucketAlreadyExists'
            elif OsEx.errno == errno.EINVAL:
                rc = 'InvalidBucketName'
            pycb.log(logging.ERROR, "%s %s %s" %(rc, d, str(OsEx)))
        except:
            rc_msg = sys.exc_info()[0]
            pycb.log(logging.ERROR, rc_msg)
            rc = 'InternalError'
        ex = cbException(rc)
        raise ex


    def getBucketPerms(self, bucketName):
        f = self.getBucketPermFile(bucketName)
        my_line = self.find_my_perms(f)
        return my_line

    def put_object(self, data_obj, bucketName, objectName, perms="RWrw"):
        data_key = data_obj.get_data_key()
        f = self.getObjectFile(bucketName, objectName)
        self.setowner_perms(f, data_key, perms)

    def delete_object(self, bucketName, objectName):
        f = self.getObjectFile(bucketName, objectName)
        os.unlink(f)

    def delete_bucket(self, bucketName):
        rc = 'InternalError'
        try:
            d = self.getBucketDir(bucketName)
            f = self.getBucketPermFile(bucketName)
            st = os.stat(d)
            if stat.S_ISDIR(st.st_mode):
                dir_list = os.listdir(d)

                if len(dir_list) == 0:
                    os.unlink(f)
                    os.rmdir(d)
                    return
                else:
                    rc = 'BucketNotEmpty'
            else:
                rc = 'InternalError'
        except OSError, (OsEx):
            if OsEx.errno == errno.ENOENT:
                rc = 'InvalidBucketName'
            rc_msg = sys.exc_info()[0]
            traceback.print_exc(file=sys.stdout)
        except Exception, (ex):
            rc = 'InternalError'
            rc_msg = sys.exc_info()[0]
            print "trouble 1 %s " % (rc_msg)
            traceback.print_exc(file=sys.stdout)

        pycb.log(logging.ERROR, "couldnt delete bucket %s, %s" % (bucketName, rc))
        ex = cbException(rc)
        raise ex

    def getObjectPerms(self, bucketName, objectName):
        f = self.getObjectFile(bucketName, objectName)
        my_line = self.find_my_perms(f)
        return my_line

    def grant(self, user_id, bucketName, objectName=None, perms="Rr"):
        (o_perms, key) = self.getPerms(bucketName, objectName)
        ndx = o_perms.find("W")
        if ndx < 0:
            raise cbException('AccessDenied')
        if objectName == None:
            f = self.getBucketPermFile(bucketName)
        else:
            f = self.getObjectFile(bucketName, objectName)
        self.add_perms(f, user_id, perms)
        pycb.log(logging.INFO, "granted: %s to %s" % (perms, user_id))

    def get_perms(self, bucketName, objectName=None):
        if objectName == None:
            perms = self.getBucketPerms(bucketName)
        else:
            perms = self.getObjectPerms(bucketName, objectName)
        return perms

    def get_acl(self, bucketName, objectName=None):
        (perms, key) = self.getPerms(bucketName, objectName)
        ndx = perms.find("R")
        if ndx < 0:
            raise cbException('AccessDenied')
        if objectName == None:
            f = self.getBucketPermFile(bucketName)
        else:
            f = self.getObjectFile(bucketName, objectName)

        auth_file = open(f, 'r')
        key = auth_file.readline()
        owner_a = self.read_owner_line(auth_file)
        line = self.read_auth_line(auth_file)
        acls = {}
        while line != None:
            (id, display_name, perms) = line
            acls[id] = line
            line = self.read_auth_line(auth_file)
        auth_file.close()
        return acls.itervalues()

    def exists(self, bucketName, objectName=None):
        if objectName == None:
            f = self.getBucketPermFile(bucketName)
        else:
            f = self.getObjectFile(bucketName, objectName)
        return os.path.exists(f)
 
    def __str__(self):
        return str(self.id+":"+self.display_name)

    def get_my_buckets(self):
        d = getPosixBuckerMetaDir()
        dir_list = os.listdir(d)

        results = []
        for f in dir_list:
            file = d + "/" + f
            st = os.stat(file)
            tm = time.gmtime(st.st_ctime)
            size = -1
            try:
                if self.isOwner(f):
                    mds = bucketIFace.getMD5(data_key)
                    obj = cbObject(tm, size, f, f, self, md5sum=mds)
                    results.append(obj)
            except:
                traceback.print_exc(file=sys.stdout)
                pycb.log(logging.WARNING, "error checking ownership of %s" % (f), tb=traceback)


        return results

    def list_bucket(self, bucketIface, bucketName, args):
        d = self.getBucketDir(bucketName)
        if not os.path.isdir(d):
            pycb.log(logging.ERROR, "did not find %s" %(d))
            raise cbException('NoSuchBucket')

        if 'prefix' in args:
            p = args['prefix'][0]
            p = self.hashFile(p)
            prefix = d + "/" + p + "*"
        else:
            prefix = d + "/" + "*"

        dir_list = glob.glob(prefix)
        if 'max-keys' in args:
            max_a = args['max-keys']
            max = int(max_a[0])
        else:
            max = len(dir_list) + 1

        results = []
        for f in dir_list:
            if len(results) >= max:
                break

            file = f.replace(d, "")
            st = os.stat(f)
            if stat.S_ISREG(st.st_mode):
                # XXX just pick the size of the first one
                dir_list = os.listdir(d)
                if len(dir_list) > 0:
                    data_key = self.getDataKey(d+"/"+dir_list[0])
                    size = bucketIface.getSize(data_key)
                    tm = bucketIface.getModTime(data_key)
                    key = self.unhashFile(file)
                    display_name = key
                    obj = cbObject(tm, size, key, display_name, self)
                    results.append(obj)

        return results

    def getBucketLocation(self, bucketName):
        ex = cbException('NotImplemented')
        raise ex

    def set_user_pw(self, password):
        raise Exception("sorry, passwords cannot be changed with this tool")

    def remove_user(self):
        etc_dir = pycb.cbPosixSecurity.getPosixAuthDir()
        pw_filename = etc_dir + self.id
        os.remove(pw_filename)


class cbPosixSec(object):

    def __init__(self, installdir):
        global install_dir

        install_dir = installdir
        # just make the dirs every time
        try:
            os.mkdir(getPosixBaseDir())
        except Exception, ex:
            pass
        try:
            os.mkdir(getPosixAuthDir())
        except Exception, ex:
            pass
        try:
            os.mkdir(getPosixBuckerDir())
        except Exception, ex:
            pass
        try:
            os.mkdir(getPosixBuckerMetaDir())
        except Exception, ex:
            pass

    def get_user(self, id):
        usr_ath_filename = getPosixAuthDir() + id
        if os.path.exists(usr_ath_filename) == 0:
            ex = cbException('InvalidAccessKeyId')
            pycb.log(logging.ERROR, "could not open: %s" % (usr_ath_filename))
            raise ex

        auth_file = open(usr_ath_filename, 'r')
        key = auth_file.readline()
        key = key.strip()
        display_name = auth_file.readline()
        display_name = display_name.strip()
        auth_file.close()
        return cbPosixUserObject(id, display_name, key)

    def get_user_id_by_display(self, display_name):
        raise Exception("sorry, the posix security module can only find users by id")

    def create_user(self, display_name, id, pw, opts):
        etc_dir = pycb.cbPosixSecurity.getPosixAuthDir()
        pw_filename = etc_dir + id
        try:
            os.mkdir(etc_dir)
        except:
            pass
        f = open(pw_filename, 'w')
        f.write(pw)
        f.write('\n')
        f.write(display_name)
        f.write('\n')
        f.close()

    def get_user_id_by_display(self, display_name):
        pycb.log(logging.WARNING, "posix security does not implement this call")

        return None
