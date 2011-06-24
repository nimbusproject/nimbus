#!/usr/bin/env python

import unittest
import tempfile
import shutil
import uuid
import os

from pynimbusconfig.iaas import groupauthz

class GroupauthzTests(unittest.TestCase):
    def setUp(self):
        self._dir = tempfile.mkdtemp()

    def write_groups(self, groups):
        for group_id, members in groups.iteritems():
            base = os.path.join(self._dir, 
                    groupauthz._GROUP_FILE_PREFIX+ '%02d' % group_id)
            props_file = file(base+groupauthz._GROUP_PROPS_EXT, 'w')
            props_file.close()
            members_file = file(base+groupauthz._GROUP_MEMBERS_EXT, 'w')
            for member in members:
                members_file.write(member+'\n')
            members_file.close()

    def test_basic_ops(self):

        groups = {
                1:[new_dn() for i in range(5)],
                2:[new_dn() for i in range(3)],
                3:[],
                4:[new_dn() for i in range(1)]}
        self.write_groups(groups)

        #and throw in an unrelated file
        file(os.path.join(self._dir,'README'),'w').close()

        found_groups = groupauthz.all_groups(self._dir)
        self.assertEqual(len(groups),len(found_groups))

        ids = set([g.group_id for g in found_groups])
        self.assertEqual(len(groups),len(ids))
        for id in ids:
            self.assertTrue(id in groups)

        members = groupauthz.group_members(self._dir, 1)
        self.assertEqual(len(groups[1]), len(members))
        self.assertFalse(groupauthz.add_member(self._dir, groups[1][0]))
        new_member = new_dn()
        self.assertTrue(groupauthz.add_member(self._dir, new_member))
        groups[1] = groupauthz.group_members(self._dir, 1)
        self.assertEqual(len(groups[1]), len(members)+1)
        self.assertFalse(groupauthz.add_member(self._dir, new_member))
        self.assertEqual(len(groups[1]), 
                len(groupauthz.group_members(self._dir, 1)))

        to_remove = groups[1][2]
        self.assertTrue(groupauthz.remove_member(self._dir, to_remove))
        members = groupauthz.group_members(self._dir, 1)
        self.assertFalse(to_remove in members)
        self.assertEqual(len(members), len(groups[1])-1)
                
    def tearDown(self):
        if self._dir:
            shutil.rmtree(self._dir)


def new_dn():
    return str(uuid.uuid4())

if __name__ == '__main__':
    unittest.main()

