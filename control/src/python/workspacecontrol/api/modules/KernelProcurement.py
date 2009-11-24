import workspacecontrol.api

class KernelProcurement(workspacecontrol.api.WCModule):
    """KernelProcurement is the wcmodule responsible for picking the proper
    kernel file(s) and making them accessible to the current VMM node before
    the deployment.
    """
    
    def kernel_files(local_file_set, p, c):
        """
        local_file_set -- instance of LocalFileSet
        
        p -- instance of Parameters
        
        c -- instance of Common
        
        Return a Kernel instance appropriate to the inputs.
        """
        