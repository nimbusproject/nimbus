# ############################################################
# RetrieveResult
# #########################################################{{{

class Identity:
    """Class holding one identity result.  RetrieveResult houses
    a list of these."""
    
    def __init__(self):
        """Instantiate fields."""
        
        self.ip = None
        self.host = None
        self.pubkey = None
        
class ResponseRole:
    """Class holding one role result.  RetrieveResult houses
    a list of these."""
    
    def __init__(self):
        """Instantiate fields."""
        
        self.name = None
        self.ip = None
        
class OpaqueData:
    """Class holding one data result.  RetrieveResult houses
    a list of these."""
    
    def __init__(self):
        """Instantiate fields."""
        
        self.name = None
        self.data = None

class RetrieveResult:

    """Class holding contextualization result."""
    
    def __init__(self):
        """Instantiate fields."""
        
        self.locked = False
        self.complete = False
        
        # list of Identity objects
        self.identities = []
        
        # list of ResponseRole objects
        self.roles = []
        
        # list of OpaqueData objects
        self.data = []

    
# }}} END: RetrieveResult

