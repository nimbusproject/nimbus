#!/usr/bin/env python

"""Provides basic management of Nimbus groupauthz members.
"""

import os
import tempfile
from itertools import groupby
import shutil

_GROUP_FILE_PREFIX = 'group'
_GROUP_PROPS_EXT = '.properties'
_GROUP_MEMBERS_EXT = '.txt'

def all_groups(groupauthz_dir):
    """Returns a list of all valid groups
    """
    groups = []
    files = [f for f in os.listdir(groupauthz_dir) 
            if f.startswith(_GROUP_FILE_PREFIX) and (f.endswith(_GROUP_PROPS_EXT) or f.endswith(_GROUP_MEMBERS_EXT))]
    files.sort()
    for name, group_files in groupby(files, lambda f: os.path.splitext(f)[0]):
        members_path = None
        props_path = None
        for f in group_files:
            ext = os.path.splitext(f)[1].lower()
            if ext == _GROUP_PROPS_EXT:
                props_path = os.path.join(groupauthz_dir, f)
            elif ext == _GROUP_MEMBERS_EXT:
                members_path = os.path.join(groupauthz_dir, f)
        group_id = int(name[len(_GROUP_FILE_PREFIX):])
        
        if members_path and props_path:
            groups.append(Group(group_id, props_path, members_path))
    return groups

def one_group(groupauthz_dir, group_id):
    """Returns the specified group, or raises an InvalidGroupError
    """
    group_id = _assure_group_id(group_id)
    # inefficient, meh
    for group in all_groups(groupauthz_dir):
        if group.group_id == group_id:
            return group
    raise InvalidGroupError('group %s was not found' % group_id)

def group_members(groupauthz_dir, group_id):
    """Returns a list of DNs authorized in specified group
    """
    group_id = _assure_group_id(group_id)
    g = one_group(groupauthz_dir, group_id)
    return g.get_members()

def find_member(groupauthz_dir, dn):
    """Finds the group containing a DN, or None
    """
    for group in all_groups(groupauthz_dir):
        if group.has_member(dn):
            return group
    return None

def add_member(groupauthz_dir, dn, group_id=1):
    """Adds a DN to a group. if the group already contains DN, do nothing
    """
    group_id = _assure_group_id(group_id)
    g = one_group(groupauthz_dir, group_id)
    return g.add_member(dn)

def remove_member(groupauthz_dir, dn, group_id=None):
    """Removes a DN from all groups, or a specified group
    """

    if group_id:
        group_id = _assure_group_id(group_id)
        g = one_group(groupauthz_dir, group_id)
        return g.remove_member(dn)
    removed = False
    for group in all_groups(groupauthz_dir):
        if group.remove_member(dn):
            removed = True
    return removed

def _assure_group_id(group_id):
    """Ensures that group ID is an integer
    """
    try:
        return int(group_id)
    except (ValueError, TypeError):
        raise InvalidGroupError("Group ID is invalid, must be an integer")


class Group(object):
    """A single group definition
    """
    def __init__(self, group_id, props_path, members_path):
        self.group_id = group_id
        self.props_path = props_path
        self.members_path = members_path
        # later we could parse the properties file and grab the group info

    def __str__(self):
        return self.group_id

    def get_members(self):
        """Returns list of member DNs in this group"""
        f = None
        members = []
        try:
            f = file(self.members_path)
            for line in f:
                line = line.strip()
                if line:
                    members.append(line)
        finally:
            if f: f.close()
        return members

    def has_member(self, dn):
        """Whether specified DN is a member of group
        """
        for member in self.get_members():
            if member == dn:
                return True
        return False

    def add_member(self, dn):
        """Add specified DN to group, if it isn't already a member.
        Returns update status.
        """
        if self.has_member(dn):
            return False
        f = None
        try:
            f = file(self.members_path, 'a')
            f.write(dn+'\n')
        finally:
            if f: f.close()
        return True

    def remove_member(self, dn):
        """Removes specified DN from group. Returns update status.
        """
        f = None
        tempf = None
        try:
            removed = False
            f = file(self.members_path)
            tempfd, temppath = tempfile.mkstemp(text=True)
            tempf = os.fdopen(tempfd,'w')
            for line in f:
                if line.strip() != dn.strip():
                    tempf.write(line)
                else:
                    removed = True
            f.close()
            tempf.close()
            shutil.move(temppath, self.members_path)
            
            return removed

        finally:
            if f: f.close()
            if tempf: tempf.close()

class InvalidGroupError(Exception):
    """Group does not exist or is invalid
    """
    pass

