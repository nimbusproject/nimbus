import workspacecontrol.api

class NICSet(workspacecontrol.api.WCObject):
    """NICSet is a "bag of NIC instances".  This is not a simple Python list
    itself because it will contain its own attributes in the future.
    """
    
    def niclist():
        """Return Python list of NIC instances
        """
    