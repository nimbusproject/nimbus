import zope.interface
import workspacecontrol.api.objects

class DefaultNICSet:
    
    zope.interface.implements(workspacecontrol.api.objects.INICSet)
    
    def __init__(self, nic_list):
        self.nic_list = nic_list
        
    def niclist(self):
        return self.nic_list
        
    