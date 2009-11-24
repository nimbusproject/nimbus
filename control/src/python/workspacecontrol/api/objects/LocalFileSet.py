import workspacecontrol.api

class LocalFileSet(workspacecontrol.api.WCObject):
    """LocalFileSet is a "bag of LocalFile instances".  This is not a simple
    Python list itself because it will contain its own attributes in the future.
    """
    
    def flist():
        """Return Python list of LocalFile instances
        """
    