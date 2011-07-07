import fcntl
import shutil
import os
import logging

class WSCCacheObj(object):

    def __init__(self, dir, lockfilepath, max_size=None, log=logging):
        self._dir = dir
        self._prefix = "cached"
        self._lockfilepath = lockfilepath
        self._lockfile = None
        if max_size > 0:
            self._max_size = max_size
        else:
            self._max_size = None
        self._log = log

    def _lock(self):
        self._lockfile = open(self._lockfilepath, "r")
        fcntl.flock(self._lockfile.fileno(), fcntl.LOCK_EX)

    def _unlock(self):
        self._lockfile.close()

    def _mangle_name(self, md5sum):
        name = self._prefix + "_" + md5sum
        return name

    def _unmangle_name(self, name):
        return name.replace(self._prefix + "_", "")

    def lookup(self, md5sum, newname):
        name = self._mangle_name(md5sum)
        absname = os.path.abspath(self._dir + "/" + name)
        self._lock()
        try:
            list = os.listdir(self._dir)
            if name in list:
                # touch the file
                os.utime(absname, None)
                shutil.copyfile(absname, newname)
                return True
            return False
        finally:
            self._unlock()

    def _order_dir(self, list):
        list.sort(key=lambda x : os.path.getmtime(self._dir + "/" + x), reverse=True)
        return list

    def _get_size(self, list):
        size_list = [os.path.getsize(self._dir + "/" + f) for f in list if os.path.isfile(self._dir + "/" + f)]
        return sum(size_list)

    def _make_room_for(self, list, sz):

        list = self._order_dir(list)
        current_size = self._get_size(list)
        max = self._max_size
        fsinfo = os.statvfs(self._dir)
        bytes_available = fsinfo.f_bavail * fsinfo.f_frsize

        # adjust to available storage
        if self._max_size is None or max - current_size > bytes_available:
            max = current_size + bytes_available

        remove_list = []
        new_size = sz + current_size
        while new_size > max:
            if len(list) == 0:
                return False
            short_straw = list.pop()
            remove_list.append(short_straw)
            new_size = sz + self._get_size(list)

        # if removing anything actually helps
        for f in remove_list:
            self._log.info("removing %s form the cache" % (f))
            os.remove(self._dir + "/" + f)
        return True

    def add(self, src, md5sum):
        dstname = self._mangle_name(md5sum)
        dstabsname = os.path.abspath(self._dir + "/" + dstname)
        self._lock()
        try:
            list = os.listdir(self._dir)
            if dstname in list:
                msg = "The file with md5sum %s is already in the cache" % (md5sum)
                self._log.info(msg)
                os.utime(dstabsname, None)
                return False
            sz = os.path.getsize(src)
            if not self._make_room_for(list, sz):
                msg = "Sorry there is not room for the file %s of size %d" % (src, sz)
                self._log.info(msg)
                raise Exception(msg)
            shutil.copyfile(src, dstabsname)
        finally:
            self._unlock()

        return True


    def remove(self, md5sum):
        name = self._mangle_name(md5sum)
        absname = os.path.abspath(self._dir + "/" + name)
        self._lock()
        try:
            list = os.listdir(self._dir)
            if name in list:
                os.remove(absname)
                return True
            return False
        finally:
            self._unlock()

    def list_cache(self):
        self._lock()
        try:
            list = os.listdir(self._dir)
            list = self._order_dir(list)
        finally:
            self._unlock()

        nl = [self._unmangle_name(f) for f in list]
        return nl


    def flush_cache(self):
        list = self.list_cache()
        for l in list:
            self.remove(l)

