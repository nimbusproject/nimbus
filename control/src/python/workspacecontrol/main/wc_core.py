from workspacecontrol.api.exceptions import *

def core(opts):
    """Run workspace-control.
    
    From here 'down' there is no concept of a commandline program, only
    'args' which could be coming from any kind of protocol based request.
    
    To make such a thing, construct an opts objects with the expected
    member names and values and pass it in to this method.
    
    See the 'wc_args' module and the defaults 'Parameters' implementations to
    fully understand arg intake.  See the 'cmdline' module to see how args
    are taken in and how the result of the program (no exception or exception)
    is translated into a return code.
    """
    
    if not opts:
        raise InvalidInput("No arguments")


