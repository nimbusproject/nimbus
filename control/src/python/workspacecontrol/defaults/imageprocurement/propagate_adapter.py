from workspacecontrol.api.exceptions import *

# Adapters aren't actually required to be instances of PropagationAdapter,
# but here are the expected methods.

class PropagationAdapter:
    
    def __init__(self, params, common):
        if params == None:
            raise ProgrammingError("expecting params")
        if common == None:
            raise ProgrammingError("expecting common")
            
        self.p = params
        self.c = common
        
    def validate(self):
        pass
    
    def validate_propagate_source(self, imagestr):
        pass
    
    def validate_unpropagate_target(self, imagestr):
        pass
    
    def propagate(self, remote_source, local_absolute_target):
        pass
    
    def unpropagate(self, local_absolute_source, remote_target):
        pass
    