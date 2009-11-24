import workspacecontrol.api

class KernelProcurement(workspacecontrol.api.WCModule):
    """KernelProcurement is the wcmodule responsible for picking the proper
    kernel file(s) and making them accessible to the current VMM node before
    the deployment.
    """
    
    def kernel_files(local_file_set, parameters):
        """
        local_file_set -- instance of LocalFileSet
        
        parameters -- instance of Parameters
        
        Return a Kernel instance appropriate to the inputs.
        """
        