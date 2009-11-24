import workspacecontrol.api

class ImageProcurement(workspacecontrol.api.WCModule):
    """ImageProcurement is the wcmodule responsible for making files accessible
    to the current VMM node before the deployment.  As well as processing files
    after deployment.  The typical pattern is propagation before running and
    unpropagation afterwards.  If there is an immediate-destroy event, this
    module will destroy all local files.
    
    That is what we assume most implementations would do, they don't necessarily
    have to.  Perhaps in some future implementation, this module will archive
    destroyed VMs to some storage array for auditing.
    
    """
    
    def obtain(parameters):
        """Given a set of deployment parameters, bring this VMM node into a
        state where it can operate on a set of local files.
        
        Return an instance of LocalFileSet
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
        
    def process_after_destroy(local_file_set, parameters, common):
        """Do any necessary work after a VM is forcibly shut down.  This is the
        alternative teardown hook to "process_after_shutdown()" and is called if
        there is an immediate-destroy event vs. a shutdown + unpropagate 
        pattern.
        
        local_file_set -- instance of LocalFileSet
        
        parameters -- instance of Parameters
        
        common -- instance of Common
        
        Return nothing, local_file_set will be modified as necessary.
        """
