import workspacecontrol.api

class ImageEditing(workspacecontrol.api.WCModule):
    """ImageEditing is the wcmodule responsible for taking a procured set of
    files and performing any last minute changes on them.  After a deployment,
    the teardown hook *may* be called (for example, it is not called if there
    is an immediate-destroy event because that needs no unpropagation).
    
    This includes, but is not limited to: mounting and editing images,
    compression, decompression, partition extension.
    
    """
    
    def process_after_procurement(local_file_set, parameters, common):
        """Do any necessary work after all files are local or otherwise
        accessible but before a VM launches.
        
        local_file_set -- instance of LocalFileSet
        
        parameters -- instance of Parameters
        
        common -- instance of Common
        
        Return nothing, local_file_set will be modified as necessary.
        """
    
    def process_after_shutdown(local_file_set, parameters, common):
        """Do any necessary work after a VM shuts down and is being prepared
        for teardown.  Will not be called if there is an immediate-destroy
        event because that needs no unpropagation.
        
        local_file_set -- instance of LocalFileSet
        
        parameters -- instance of Parameters
        
        common -- instance of Common
        
        Return nothing, local_file_set will be modified as necessary.
        """
