import sys
sys.path.append(".")
try:
    #import elementtree.ElementTree as ET
    from xml.etree import ElementTree as ET #XXX what Python version support this?
except ImportError:
    print "elementtree not installed on the system, trying our backup copy"
    import embeddedET.ElementTree as ET #FIXME this is now broken

#local imports
from ctx_types import RetrieveResult, ResponseRole, OpaqueData, Identity
from ctx_exceptions import UnexpectedError
from ctx_logging import getlog
from conf import NS_CTX, NS_CTXTYPES, NS_CTXDESC


# ############################################################
# Response consumption v2 (XML)
# #########################################################{{{

def response2_parse_file(path, trace=False, responsetype=2, log_override=None):
    """Return RetrieveResult object if <retrieveResponse> was in
    the response.  Return partial response (i.e., not locked or
    not complete), let caller decide what behavior is appropriate
    in that situation.
    
    Raise nothing.
    
    """
    log = getlog(override=log_override)
    try:
        tree = ET.parse(path)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "Problem parsing '%s': %s: %s\n" % (path, name, err)
        log.error(errmsg)
        return None
        
    if responsetype == 2:
        namespace_context = NS_CTX
        namespace_types = NS_CTXTYPES
        namespace_desc = NS_CTXDESC
    else:
        raise UnexpectedError("unknown response type '%s'" % str(responsetype))
        
    # <retrieveResponse> is all we care about
    retrv =  tree.find('*/{%s}retrieveResponse' % namespace_context)
    
    if retrv == None:
        if trace:
            log.debug("retrieveResponse not found in response file")
        return None
        
    cancelled = retrv.find('{%s}cancelled' % namespace_types)
    
    locked = retrv.find('{%s}noMoreInjections' % namespace_types)
    
    if locked == None:
        # incomplete response, just return
        if trace:
            log.debug("noMoreInjections element not found in response?")
        return None
        
    complete = retrv.find('{%s}complete' % namespace_types)
    
    if complete == None:
        # incomplete response, just return
        if trace:
            log.debug("complete element not found in response?")
        return None


    # Going to return a RetrieveResult from now on
    result = RetrieveResult()
    
    if cancelled != None:
        if cancelled.text != None:
            if cancelled.text.strip().lower() == "true":
                raise UnexpectedError("broker reported that the context was cancelled")
                
    isLocked = False
    if locked.text != None:
        if locked.text.strip().lower() == "true":
            isLocked = True
    result.locked = isLocked
    
    if trace:
        if isLocked:
            log.debug("resource is locked")
        else:
            log.debug("resource is not locked")
            
    isComplete = False
    if complete.text != None:
        if complete.text.strip().lower() == "true":
            isComplete = True
    result.complete = isComplete
    
    if trace:
        if isComplete:
            log.debug("resource is complete")
        else:
            log.debug("resource is not complete")
    
    requires_array = retrv.find('{%s}requires' % namespace_types)
    
    if requires_array == None:
        if trace:
            log.debug("no requires found in response")
        return result

    all_identities = requires_array.findall('{%s}identity' % namespace_desc)

    if trace:
        log.debug("Found %d identities" % len(all_identities))
        
    for x,ident in enumerate(all_identities):
        if trace:
            log.debug("Examining identity #%d" % x)
        identity = response2_parse_one_identity(ident, trace=trace)
        if identity != None:
            result.identities.append(identity)
            
    all_roles = requires_array.findall('{%s}role' % namespace_desc)
    
    if trace:
        log.debug("Found %d roles" % len(all_roles))
        
    for x,role in enumerate(all_roles):
        if trace:
            log.debug("Examining role #%d" % x)
        resprole = response2_parse_one_role(role, trace)
        if resprole != None:
            result.roles.append(resprole)
            
    all_data = requires_array.findall('{%s}data' % namespace_desc)
    
    if trace:
        log.debug("Found %d data elements" % len(all_data))
        
    for x,data in enumerate(all_data):
        if trace:
            log.debug("Examining data #%d" % x)
        respdata = response2_parse_one_data(data, trace)
        if respdata != None:
            result.data.append(respdata)
    
    return result
    
def response2_parse_one_role(role, trace=False, log_override=None):
    log = getlog(override=log_override)
    if role == None:
        if trace:
            log.debug("  - role is null?")
        return None
        
    resprole = ResponseRole()
    
    if len(role.items()) > 1:
        # ok to continue here
        log.error("unexpected, role has more than one attr")
        
    if len(role.items()) < 1:
        log.error("error, role has zero attrs?")
        return None
        
    attrtuple = role.items()[0]
    
    if attrtuple == None:
        log.error("error, role has null in items list?")
        return None
        
    if len(attrtuple) != 2:
        log.error("error, role has object in item list not length 2?")
        return None
        
    namekey = "{%s}name" % NS_CTXDESC
    if attrtuple[0] == namekey:
        resprole.name = attrtuple[1]
        if trace:
            log.debug("  - name: '%s'" % attrtuple[1])
    else:
        log.error("error, role has attr not named 'name'?")
        return None
        
    resprole.ip = role.text
    if trace:
        log.debug("  - ip: '%s'" % role.text)
    
    return resprole
    
def response2_parse_one_data(data, trace=False, log_override=None):
    log = getlog(override=log_override)
    if data == None:
        if trace:
            log.debug("  - data is null?")
        return None
        
    respdata = OpaqueData()
    
    if len(data.items()) > 1:
        # ok to continue here
        log.error("unexpected, data has more than one attr")
        
    if len(data.items()) < 1:
        log.error("error, data has zero attrs?")
        return None
        
    attrtuple = data.items()[0]
    
    if attrtuple == None:
        log.error("error, data has null in items list?")
        return None
        
    if len(attrtuple) != 2:
        log.error("error, data has object in item list not length 2?")
        return None
    
    namekey = "{%s}name" % NS_CTXDESC
    if attrtuple[0] == namekey:
        respdata.name = attrtuple[1]
        if trace:
            log.debug("  - data name: '%s'" % attrtuple[1])
    else:
        log.error("error, role has attr not named 'name'?")
        return None
        
    respdata.data = data.text
    if trace:
        log.debug("  - first 32 of data: '%s'" % data.text[:32])
    
    return respdata

def response2_parse_one_identity(ident, trace=False, responsetype=2, log_override=None):
    log = getlog(override=log_override)
    if ident == None:
        if trace:
            log.debug("  - ident is null?")
        return None
        
    identity = Identity()
    
    if responsetype == 2:
        namespace_desc = NS_CTXDESC
    else:
        raise UnexpectedError("unknown response type '%s'" % str(responsetype))
    
    ip = ident.find('{%s}ip' % namespace_desc)
    if ip != None:
        identity.ip = ip.text
        if trace:
            log.debug("  - found ip: %s" % ip.text)

    host = ident.find('{%s}hostname' % namespace_desc)
    if host != None:
        identity.host = host.text
        if trace:
            log.debug("  - found host: %s" % host.text)

    pubkey = ident.find('{%s}pubkey' % namespace_desc)
    if pubkey != None:
        identity.pubkey = pubkey.text
        if trace:
            log.debug("  - found pubkey: %s" % pubkey.text)
    
    return identity

def response2_parse_for_fatal(path, trace=False, log_override=None):
    log = getlog(override=log_override)
    if path == None:
        return
    
    f = None
    try:
        try:
            f = open(path)
            for line in f:
                if line.rfind("NoSuchResourceException") >= 0:
                    msg = "Response contained NoSuchResourceException"
                    raise UnexpectedError(msg)
        except UnexpectedError:
            raise
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem looking at '%s': %s: %s\n" \
                     % (path, name, err)
            log.error(errmsg)
    finally:
        if f:
            f.close()

# }}} END: Response consumption v2 (XML)


