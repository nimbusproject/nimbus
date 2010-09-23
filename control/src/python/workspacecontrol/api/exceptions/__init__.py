class IWCError(Exception):
    """Generic exception; parent of all API exceptions.
    
    Every class/interface in the workspacecontrol.api package descends from
    IWCModule, IWCObject, or IWCError.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class InvalidInput(IWCError):
    """Exception for illegal/nonsensical commandline syntax/combinations.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class InvalidConfig(IWCError):
    """Exception for misconfigurations.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class IncompatibleEnvironment(IWCError):
    """Exception for when something has determined a problem with the
    deployment environment.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class UnexpectedError(IWCError):
    """Exception for when a function/module cannot proceed.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class ProgrammingError(IWCError):
    """Not listed in docstrings, should never be seen except during
    development.  An 'assert' device that can be propagated through the
    exception handling mechanisms just in case it is seen during deployment.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
