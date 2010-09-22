
# Only works if IPython is installed.
# To use, place the following line wherever in the program you want to drop to
# an interpreter (can be always forced or can be more strategic like after a
# conditional statement, etc.)

"""
from workspacecontrol import ipshell; ipshell("")
"""

# Do not *ever* commit a line like that to the repository!  Workspace control
# is meant to be driven by programs, not humans.

ipshell = None
try:
    from IPython.Shell import IPShellEmbed
    ipshell = IPShellEmbed('', "Dropping into IPython", "Leaving interpreter")
except:
    pass
