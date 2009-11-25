import workspacecontrol.api

class ILocalFileSet(workspacecontrol.api.IWCObject):
    """LocalFileSet is a "bag of LocalFile instances".  This is not a simple
    Python list itself because it will contain its own attributes in the future.
    """
    
    def flist():
        """Return Python list of LocalFile instances
        """
    