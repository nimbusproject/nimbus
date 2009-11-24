"""Collection of wcmodules which are "workspace-control modules," a term used to
distinguish a strong interface used by the workspace-control logic from a
regular Python module.

*All* interactions with the code that drives the entirety of workspace-control's
functionality happen via wcmodules.

Thus, one can replace any wcmodule with another implementation in order to
change implementation behavior.

Many wcmodules end up being implemented with scripts (called directly or via
sudo).  

Thus, to change workspace-control's behavior it may only be required to replace
or edit a shell script or other program.

In order to use only *part* of workspace-control's functionality in another
context (which is encouraged, drop the project a line sometime to tell us you
are finding something useful), you may either need the wcmodule Python
implementation or really just the script that implements most of the work.  This
depends on the particular wcmodule, see the detailed developer documentation or
just ask on the mailing list what the best course of action is.

Some wcmodule implementations may run only as Python code in normal deployment
but have a script present in the libexec directory to ease independent use or
testing.

WCModule is an interface that all wcmodule interfaces inherit from.
WCObject is an interface that all helper object interfaces inherit from.

"""

# Hide the fact that there are separate files:

__all__ = ["WCModule", "WCObject"]
from WCModule import WCModule
from WCObject import WCObject
