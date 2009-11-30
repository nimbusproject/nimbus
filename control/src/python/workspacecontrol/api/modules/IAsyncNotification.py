import workspacecontrol.api

class IAsyncNotification(workspacecontrol.api.IWCModule):
    """AsyncNotification is the wcmodule responsible for notifying the service
    of events (both successes and failures of various tasks).
    """
    
    def __init__(params, common):
        """
        params -- instance of Parameters
        
        common -- instance of Common
        """
    
    def notify(name, actiondone, code, error):
        """
        name -- handle of the VM this is about
        
        actiondone -- event name
        
        code -- status code ('exit' code essentially)
        
        error -- error text for nonzero status codes
        """

