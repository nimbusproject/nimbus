
class InvalidInput(Exception):
    
    """Exception for illegal commandline syntax/combinations."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class InvalidConfig(Exception):
    
    """Exception for misconfigurations."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
        
class IncompatibleEnvironment(Exception):
    
    """Exception for when something has determined a problem with the
    deployment environment."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
        
class UnexpectedError(Exception):
    
    """Exception for when the program cannot proceed."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class CLIError(Exception):
    """An exception for nimbus command line tools"""

    e_code = {}
    e_code['EUSER'] = 1
    e_code['ENIMBUSHOME'] = 2
    e_code['EPATH'] = 3
    e_code['ECMDLINE'] = 4

    def __init__(self, type, msg):
        self.type = type
        self.msg = msg
        self.rc = CLIError.e_code[type]

    def get_rc(self):
        return self.rc

    def __str__(self):
        return self.msg
