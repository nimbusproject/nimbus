Group based authorization plugin
================================

First, look at the contents of the "group01.properties" file to see what
policies this plugin offers you (there are detailed comments in the file).

The policies in the "group01.properties" file will be applied to any DN listed
in the "group01.txt" file (and so forth).   You can make multiple kinds of
allocations in this manner, dropping people into different authorization groups
as you add them to the cloud.

You can have 1-15 groups (and zero groups, but that is just like disabling the
plugin entirely).

Changing the policies and changing group membership can be done WHILE RUNNING.
The files' last-modified attributes are all tracked to ensure the system is
always using the freshest information.

Enabling and disabling the plugin can only be done between container restarts.
Adding and removing groups entirely (see the end of this file) can only be done
between container restarts.


================================================================================


To ENABLE the groupauthz authorization plugin, run:

    cp  ../other/authz-callout-groupauthz.xml  ../other/authz-callout-ACTIVE.xml


To DISABLE the groupauthz authorization plugin, run:

    cp  ../other/authz-callout-disabled.xml  ../other/authz-callout-ACTIVE.xml


================================================================================


If you want to add more authorization groups, you must use the numbering and
naming scheme seen in the samples:

group##.properties    <-- The group's policy properties 
group##.txt           <-- The group member DNs


================================================================================


For each group ##, you must include BOTH "group##.properties" and "group##.txt"
or NEITHER.

You can go up to group15 for a total of fifteen groups.

Valid values are {group01,group02, ... , group14,group15}

Absence of lower numbered groups is not a problem.  For example, if you want
to get rid of group01 but keep group02, there is no error (it doesn't count
up from 01, it just looks for any definitions between group01 and group15).
