# ############################################################
# Custom Exceptions
# #########################################################{{{

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

class ProgrammingError(Exception):
    
    """Not listed in docstrings, should never be seen except during
    development."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
        
# }}} END: Exceptions


