import workspacecontrol.api

class IKernelProcurement(workspacecontrol.api.IWCModule):
    """KernelProcurement is the wcmodule responsible for picking the proper
    kernel file(s) and making them accessible to the current VMM node before
    the deployment.
    """
    
    def __init__(params, common):
        """
        params -- instance of Parameters
        
        common -- instance of Common
        """
    
    def kernel_files(local_file_set):
        """
        local_file_set -- instance of LocalFileSet
        
        Return an IKernel instance appropriate to the inputs.
        UnexpectedError if none can be found (may not return None)
        """
        