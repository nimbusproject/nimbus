import filecmp
import shutil
import tempfile
import time
import os
import filecmp
from workspacecontrol.defaults.imageprocurement.propagate_cache import WSCCacheObj



import unittest

class TestCache(unittest.TestCase):

    def setUp(self):
        self._dir = tempfile.mkdtemp()
        self._work_file = "/etc/group"
        self._work_sum = "XXXXX"
        (osf, self.lockfilepath) = tempfile.mkstemp()
        os.close(osf)

    def tearDown(self):
        shutil.rmtree(self._dir)

    def test_add_twice(self):
        cache = WSCCacheObj(self._dir, self.lockfilepath)
        rc = cache.add(self._work_file, self._work_sum)
        self.assertEqual(rc, True)
        rc = cache.add(self._work_file, self._work_sum)
        self.assertEqual(rc, False)

    def test_lookup_fail(self):
        cache = WSCCacheObj(self._dir, self.lockfilepath)
        (osf, newfile) = tempfile.mkstemp()
        os.close(osf)
        rc = cache.lookup(self._work_sum, newfile)
        self.assertEqual(rc, False)

    def test_remove_fail(self):
        cache = WSCCacheObj(self._dir, self.lockfilepath)
        rc = cache.remove(self._work_sum)
        self.assertEqual(rc, False)

    def test_list_cache(self):
        work_sums = ["1", "XX", "III", "four"]

        cache = WSCCacheObj(self._dir, self.lockfilepath)
        for sum in work_sums:
            rc = cache.add(self._work_file, sum)
            self.assertEqual(rc, True)

        count = 0
        first_len = len(work_sums)
        list = cache.list_cache()
        for l in list:
            work_sums.remove(l)
            count = count + 1

        self.assertEqual(len(work_sums), 0)
        self.assertEqual(count, first_len)

    def test_flush_cache(self):
        work_sums = ["1", "XX", "III", "four"]

        cache = WSCCacheObj(self._dir, self.lockfilepath)
        for sum in work_sums:
            rc = cache.add(self._work_file, sum)
            self.assertEqual(rc, True)
        cache.flush_cache()

        list = cache.list_cache()
        self.assertEqual(len(list), 0)        

    def test_basic_sequence(self):
        cache = WSCCacheObj(self._dir, self.lockfilepath)
        rc = cache.add(self._work_file, self._work_sum)
        self.assertEqual(rc, True)

        (osf, newfile) = tempfile.mkstemp()
        os.close(osf)
        rc = cache.lookup(self._work_sum, newfile)
        self.assertEqual(rc, True)

        rc = filecmp.cmp(newfile, self._work_file)
        self.assertEqual(rc, 1)

        rc = cache.remove(self._work_sum)
        self.assertEqual(rc, True)

    def test_exceedsize(self):
        sz1 = os.path.getsize(self._work_file)
        work_sums = ["1", "XX", "III", "four"]
        max = sz1 * (len(work_sums) - 1)

        cache = WSCCacheObj(self._dir, self.lockfilepath, max_size=max)
        for i in range(0, len(work_sums)):
            sum = work_sums[i]
            rc = cache.add(self._work_file, sum)
            self.assertEqual(rc, True)

    def test_noroom(self):
        sz1 = os.path.getsize(self._work_file)
        max = sz1 - 1
        cache = WSCCacheObj(self._dir, self.lockfilepath, max_size=max)
        try:
            cache.add(self._work_file, self._work_sum)
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    def test_correct_expulsion(self):
        sz1 = os.path.getsize(self._work_file)
        work_sums = ["1", "XX", "III", "four"]
        max = sz1 * (len(work_sums) - 1)

        cache = WSCCacheObj(self._dir, self.lockfilepath, max_size=max)
        for i in range(0, len(work_sums)):
            sum = work_sums[i]
            rc = cache.add(self._work_file, sum)
            time.sleep(1.1)
            self.assertEqual(rc, True)

        list = cache.list_cache()
        self.assertEqual(len(list), 3)
        self.assertTrue(work_sums[0] not in list)

    def test_touch_expulsion(self):
        sz1 = os.path.getsize(self._work_file)
        work_sums = ["1", "XX", "III", "four"]
        max = sz1 * (len(work_sums) - 1)

        cache = WSCCacheObj(self._dir, self.lockfilepath, max_size=max)
        for i in range(0, len(work_sums) - 1):
            sum = work_sums[i]
            rc = cache.add(self._work_file, sum)
            time.sleep(1.1)
            self.assertEqual(rc, True)
        (osf, newfile) = tempfile.mkstemp()
        os.close(osf)
        print cache.list_cache()
        rc = cache.lookup(work_sums[0], newfile)
        self.assertEqual(rc, True)
        print cache.list_cache()
        sum = work_sums[i]
        rc = cache.add(self._work_file, work_sums[len(work_sums) - 1])
        self.assertEqual(rc, True)

        list = cache.list_cache()
        self.assertEqual(len(list), 3)
        print list
        print work_sums
        self.assertTrue(work_sums[0] in list)
        self.assertTrue(work_sums[1] not in list)

    def test_lookupmiss(self):
        sz1 = os.path.getsize(self._work_file)
        work_sums = ["1", "XX", "III", "four"]
        max = sz1 * (len(work_sums) - 1)

        cache = WSCCacheObj(self._dir, self.lockfilepath, max_size=max)
        for i in range(0, len(work_sums)):
            sum = work_sums[i]
            time.sleep(1.1)
            rc = cache.add(self._work_file, sum)
            self.assertEqual(rc, True)
        (osf, newfile) = tempfile.mkstemp()
        os.close(osf)
        rc = cache.lookup(work_sums[0], newfile)
        self.assertEqual(rc, False)


if __name__ == '__main__':
    unittest.main()
