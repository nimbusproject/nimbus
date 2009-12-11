import zope.interface
import workspacecontrol.api

class IParameters(workspacecontrol.api.IWCObject):
    """Parameters is the systemwide mechanism for propagating arguments and
    conf file settings to wcmodules.
    
    It maintains a two level hierarchy for the wcmodule author.  Key/values are
    stored by source: command line or conf file.
    
    Conf files require there to be a specified section qualifier before the key
    is used.  Only the values from the configured conf file will be included,
    not the union of all sections across all conf files related to wcmodules.
    
    See "etc/workspace-control/main.conf" which is where the wcmodule
    implementations and conf files are configured.
    """
    
    def __init__(allconfigs, opts):
        """
        allconfigs -- config object with all section+key data
        
        opts -- parsed argument opts
        """
  
    def get_arg_or_none(key):
        """Get string value of an argument if it existed.
        All values are stripped of extraneous spaces (string.strip()).
        Return None if it did not exist.  Empty string is impossible.
        """
    
    def get_conf_or_none(section, key):
        """Get string value of a configuration setting if it existed.
        All values are stripped of extraneous spaces (string.strip()).
        Return None if it did not exist.  Empty string is impossible, there
        is not distinguishing between "key not present" in the conf file vs.
        "key present with empty value," both cause None to be returned.
        """

    def all_confs_in_section(section):
        """Get list of all keyword and values of a configuration section.
        Return empty list if it did not exist or if it is empty.
        If a keyword is present in the section with no assigned value, it is not
        returned in the list.
        Return list of tuples, (keyword, value)
        """
        