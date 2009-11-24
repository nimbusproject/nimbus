class WCError(Exception):
    """Generic exception; parent of all API exceptions.
    
    Every class/interface in the workspacecontrol.api package descends from
    WCModule, WCObject, or WCError.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class InvalidInput(WCError):
    """Exception for illegal/nonsensical commandline syntax/combinations.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class InvalidConfig(WCError):
    """Exception for misconfigurations.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class IncompatibleEnvironment(WCError):
    """Exception for when something has determined a problem with the
    deployment environment.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class UnexpectedError(WCError):
    """Exception for when a function/module cannot proceed.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class ProgrammingError(WCError):
    """Not listed in docstrings, should never be seen except during
    development.  An 'assert' device that can be propagated through the
    exception handling mechanisms just in case it is seen during deployment.
    """
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
