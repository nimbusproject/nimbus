import workspacecontrol.api

class IImageEditing(workspacecontrol.api.IWCModule):
    """ImageEditing is the wcmodule responsible for taking a procured set of
    files and performing any last minute changes on them.  After a deployment,
    the teardown hook *may* be called (for example, it is not called if there
    is an immediate-destroy event because that needs no unpropagation).
    
    This includes, but is not limited to: mounting and editing images,
    compression, decompression, partition extension.
    
    """
    
    def __init__(params, common):
        """
        params -- instance of Parameters
        
        common -- instance of Common
        """
    
    def process_after_procurement(local_file_set):
        """Do any necessary work after all files are local or otherwise
        accessible but before a VM launches.
        
        local_file_set -- instance of LocalFileSet
        
        Return nothing, local_file_set will be modified as necessary.
        """
    
    def process_after_shutdown(local_file_set):
        """Do any necessary work after a VM shuts down and is being prepared
        for teardown.  Will not be called if there is an immediate-destroy
        event because that needs no unpropagation.
        
        local_file_set -- instance of LocalFileSet
        
        Return nothing, local_file_set will be modified as necessary.
        """
