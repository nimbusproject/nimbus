import os
import nose.tools
import unittest
import tempfile
from  pynimbusauthz.db import * 
from  pynimbusauthz.user import * 
import nimbus_remove_user
import nimbus_new_user
import nimbus_list_users
import nimbus_edit_user
import nimbus_import_users

class TestUsers(unittest.TestCase):

    def setUp(self):
        self.users = []    

    def tearDown(self):
        for f in self.users:
            nimbus_remove_user.main([f])

    def get_user_name(self, friendly_name=None):
        if friendly_name == None:
            friendly_name = str(uuid.uuid1())
        self.users.append(friendly_name)
        return friendly_name    

    def test_make_remove_canid_user(self):
        uu = str(uuid.uuid1())
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main(["--canonical-id", uu, friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)
        rc = nimbus_list_users.main(["-b", "-r", "canonical_id", "-O", outFileName, '%'])
        rc = self.find_in_file(outFileName, uu)
        self.assertTrue(rc)


        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_make_remove_user(self):
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_make_user_twice(self):
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_new_user.main([friendly_name])
        self.assertNotEqual(rc, 0, "should be 0 %d" % (rc))

        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_remove_user_twice(self):
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_remove_user.main([friendly_name])
        self.assertNotEqual(rc, 0, "should not be 0 %d" % (rc))

    def test_add_remove_add_remove(self):
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_remove_unknown_user(self):
        friendly_name = self.get_user_name()
        rc = nimbus_remove_user.main([friendly_name])
        self.assertNotEqual(rc, 0, "should be 0 %d" % (rc))

    def find_in_file(self, fname, needle):
        found = False
        f = open(fname)
        l = f.readline()
        while l:
            print "#### " + l
            x = l.find(needle)
            if x >= 0:
                found = True
            l = f.readline()
        f.close()
        return found

    def test_new_user_s3ids(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        s3id = str(uuid.uuid1())
        s3pw = str(uuid.uuid1())
        rc = nimbus_new_user.main(["-a", s3id, "-p", s3pw, "-b", "-r", "access_id,access_secret", "-O", outFileName, friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        needle = "%s,%s" % (s3id, s3pw)
        print needle
        rc = self.find_in_file(outFileName, needle)
        os.unlink(outFileName)
        self.assertTrue(rc)

        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_no_cert(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        s3id = str(uuid.uuid1())
        s3pw = str(uuid.uuid1())
        rc = nimbus_new_user.main(["--noaccess", "-b", "-r", "access_id,access_secret", "-O", outFileName, friendly_name])
        needle = "None,None"
        rc = self.find_in_file(outFileName, needle)
        os.unlink(outFileName)
        self.assertTrue(rc)
        
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_no_s3(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        s3id = str(uuid.uuid1())
        s3pw = str(uuid.uuid1())
        rc = nimbus_new_user.main(["--nocert", "-b", "-r", "cert,key,dn", "-O", outFileName, friendly_name])
        needle = "None,None,None" 
        rc = self.find_in_file(outFileName, needle)
        os.unlink(outFileName)
        self.assertTrue(rc)
        
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_edit_user(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        s3id = str(uuid.uuid1())
        s3pw = str(uuid.uuid1())
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_edit_user.main(["-b", "-a", s3id, "-p", s3pw, "-r", "access_id,access_secret", "-O", outFileName, friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        needle = "%s,%s" % (s3id, s3pw)
        print "--> %s <--" % (needle)
        rc = self.find_in_file(outFileName, needle)
        os.unlink(outFileName)
        self.assertTrue(rc)
        
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_list_user(self):
        name1 = self.get_user_name()
        rc = nimbus_new_user.main([name1])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        name2 = self.get_user_name()
        rc = nimbus_new_user.main([name2])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        name3 = self.get_user_name()
        rc = nimbus_new_user.main([name3])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)
        rc = nimbus_list_users.main(["-b", "-r", "display_name", "-O", outFileName, '%'])
        rc = self.find_in_file(outFileName, name1)
        self.assertTrue(rc)

        rc = self.find_in_file(outFileName, name2)
        self.assertTrue(rc)

        rc = self.find_in_file(outFileName, name3)
        self.assertTrue(rc)

        os.unlink(outFileName)

    def test_db_commit_user(self):
        # insert a new user with an error
        friendly_name = self.get_user_name(friendly_name="test1@nimbus.test")
        rc = nimbus_new_user.main(["--cert", "none", "--key", "none", friendly_name])
        self.assertNotEqual(rc, 0, "we expect this one to fail %d" % (rc))

        # insert the user without the error to make sure the previous was rolled back
        friendly_name = self.get_user_name(friendly_name="test1@nimbus.test")
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "but then this clarification should succeed %d" % (rc))

    def test_import_users_add(self):
        """nimbus-import-users: Users are imported
        """
        self._test_import_users_add(dryrun=False)

    def test_import_users_add_dryrun(self):
        """nimbus-import-users: Users are imported - dryrun
        """
        self._test_import_users_add(dryrun=True)
    
    def _test_import_users_add(self, dryrun=False):
        before_users = self._get_users()
        new_users = [self._new_user_dict(), self._new_user_dict()]

        states = _import_users(itertools.chain(before_users.itervalues(), new_users), 
                dryrun=dryrun)
        after_users = self._get_users()

        for u in before_users:
            assert u in after_users
            assert states[u] == 'UNCHANGED'

        if dryrun:
            assert len(after_users) == len(before_users)
            return

        assert len(after_users) == len(before_users) + len(new_users)

        for u in new_users:
            added = after_users[u['display_name']]
            assert _user_compare(u, added)

    def test_import_users_extra(self):
        """nimbus-import-users: Extra users are not removed"""
        self._test_import_users_extra(dryrun=False, remove=False)

    def test_import_users_extra_dryrun(self):
        """nimbus-import-users: Extra users are not removed - dryrun"""
        self._test_import_users_extra(dryrun=True, remove=False)

    def test_import_users_remove(self):
        """nimbus-import-users: Extra users are removed"""
        self._test_import_users_extra(dryrun=False, remove=True)

    def test_import_users_remove_dryrun(self):
        """nimbus-import-users: Extra users are removed - dryrun"""
        self._test_import_users_extra(dryrun=True, remove=True)

    def _test_import_users_extra(self, dryrun=False, remove=False):
        self._add_users(2) # make sure there are some existing users
        before_users = self._get_users()
        
        # add some extra users
        extra_users = self._add_users(2)
        all_users = self._get_users()
        total_count = len(before_users) + len(extra_users)
        assert len(all_users) == total_count

        # now run nimbus-import-users with the original user set
        states = _import_users(before_users.itervalues(), dryrun=dryrun, remove=remove)
        all_users = self._get_users()
        if not remove or dryrun:
            assert len(all_users) == total_count
        elif remove:
            assert len(all_users) == len(before_users)

        # now check the state output
        for user in before_users:
            assert states[user] == 'UNCHANGED'
        for user in extra_users:
            user = user['display_name']
            if remove:
                assert states[user] == 'REMOVED'
            else:
                assert states[user] == 'EXTRA'

    def test_import_users_mismatch(self):
        """nimbus-import-users: Mismatched users are not updated"""
        self._test_import_users_update(dryrun=False, update=False)

    def test_import_users_mismatch_dryrun(self):
        """nimbus-import-users: Mismatched users are not updated"""
        self._test_import_users_update(dryrun=True, update=False)

    def test_import_users_update(self):
        """nimbus-import-users: Mismatched users are updated"""
        self._test_import_users_update(dryrun=False, update=True)

    def test_import_users_update_dryrun(self):
        """nimbus-import-users: Mismatched users are updated - dryrun"""
        self._test_import_users_update(dryrun=True, update=True)

    def _test_import_users_update(self, dryrun=False, update=True):
        before_users = self._get_users()
        new_users = self._add_users(3)
        new_users_dict = dict([(user['display_name'], user) for user in new_users])

        # change the users
        new_users[0]['group'] = '2'
        new_users[1]['dn'] = str(uuid.uuid4())
        new_users[2]['access_id'] = str(uuid.uuid4())
        new_users[2]['access_secret'] = str(uuid.uuid4())
        new_users[2]['group'] = '2'

        states = _import_users(itertools.chain(before_users.itervalues(), new_users),
                dryrun=dryrun, update=update)
        
        all_users = self._get_users()
        assert len(all_users) == len(before_users) + len(new_users)
        for user in all_users:
            if user in before_users:
                assert states[user] == 'UNCHANGED'
            else:
                match = _user_compare(all_users[user], new_users_dict[user])
                if update:
                    assert states[user] == 'UPDATED'
                    if dryrun:
                        assert not match
                    else:
                        print all_users[user]
                        print new_users_dict[user]
                        assert match
                else:
                    assert states[user] == 'MISMATCHED'
                    assert not match
    
    # adding tags to tests so I can easily run only this group
    test_import_users_add.importtests = 1
    test_import_users_add_dryrun.importtests = 1
    test_import_users_extra.importtests = 1
    test_import_users_extra_dryrun.importtests = 1
    test_import_users_remove.importtests = 1
    test_import_users_remove_dryrun.importtests = 1
    test_import_users_mismatch.importtests = 1
    test_import_users_mismatch_dryrun.importtests = 1
    test_import_users_update.importtests = 1
    test_import_users_update_dryrun.importtests = 1

    def _add_users(self, count):
        new_users = [self._new_user_dict() for i in range(count)]
        _import_users(new_users, remove=False, update=False)
        return new_users
        
    def _new_user_dict(self):
        return {'display_name' : self.get_user_name(), 'dn' : str(uuid.uuid4()),
                'canonical_id' : str(uuid.uuid4()), 'access_id' : str(uuid.uuid4()),
                'access_secret' : str(uuid.uuid4()), 'group' : '01'}
    
    def _get_users(self):
        """Returns a dict of existing users keyed by display_name.
        Each entry is a dict of user properties.
        """
        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        try:
            os.close(tmpFD)
            rc = nimbus_list_users.main(["-b", "-O", outFileName, '%'])
            self.assertEqual(rc, 0)

            #cheating here
            return nimbus_import_users.read_users(outFileName)

        finally:
            os.unlink(outFileName)


def _import_users(users, dryrun=False, update=False, remove=False):
    """Calls nimbus-import-user with an input file containing specified users
    """
    (in_fd, in_file_name) = tempfile.mkstemp("nimbustests")
    (out_fd, out_file_name) = tempfile.mkstemp("nimbustests")
    in_file = None
    out_file = None
    try:
        in_file = os.fdopen(in_fd, "w")
        for user in users:
            in_file.write(_user_to_csv(user)+"\n")
        in_file.close()

        args = []
        if dryrun:
            args.append('-d')
        if update:
            args.append('-u')
        if remove:
            args.append('-r')
        args.extend(['-b', '-O', out_file_name, in_file_name])

        nimbus_import_users.main(args)

        out_file = os.fdopen(out_fd)
        return dict([line.strip().split(',') for line in out_file])
    finally:
        if in_file: in_file.close()
        if out_file: out_file.close()
        os.unlink(in_file_name)
        os.unlink(out_file_name)

def _user_to_csv(user, fields=nimbus_import_users._fields):
    """Formats a user dict into a csv line (like what nimbus-list-users
    outputs).
    """
    cols = [user[field] for field in fields]
    return ",".join(cols)

def _user_compare(user1, user2):
    for k in user1:
        if k == 'group':
            if not (int(user1[k]) == int(user2[k])):
                return False
        else:
            if not (user1[k] == user2[k]):
                return False
    return True
