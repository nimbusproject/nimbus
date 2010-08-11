import pathutil
import javautil
import runutil
from setuperrors import IncompatibleEnvironment

EXE_LOGICAL_HOST = "org.nimbustools.auto_common.confmgr.AddOrReplaceLogicalHost"
EXE_PUBLISH_HOST = "org.nimbustools.auto_common.confmgr.AddOrReplacePublishHostname"
EXE_GLOBUS_SECDESC = "org.nimbustools.auto_common.confmgr.AddOrReplaceGlobalSecDesc"
EXE_NEW_GRIDMAPFILE = "org.nimbustools.auto_common.confmgr.ReplaceGridmap"
EXE_NEW_HOSTCERTFILE = "org.nimbustools.auto_common.confmgr.ReplaceCertFile"
EXE_NEW_HOSTKEYFILE = "org.nimbustools.auto_common.confmgr.ReplaceKeyFile"
EXE_SERVICE_RESOURCE = "org.nimbustools.auto_common.confmgr.ServiceResourceAdjust"

# config paths, relative to $GLOBUS_LOCATION
CONF_SERVERCONFIG = "etc/globus_wsrf_core/server-config.wsdd"
CONF_SECDESC = "etc/globus_wsrf_core/global_security_descriptor.xml"
CONF_BROKERCONFIG = "etc/nimbus-context-broker/jndi-config.xml"

def adjust_hostname(hostname, basedir, gtdir, log):
    serverconfig = get_serverconfig_path(gtdir)
    pathutil.ensure_file_exists(serverconfig, "GT server config")

    args = [hostname, serverconfig]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_LOGICAL_HOST, 
            args=args)
    runutil.generic_bailout("Problem adjusting logical host in GT container", 
            exitcode, stdout, stderr)

    log.debug("Adjusted GT container logical host to %s" % hostname)

    args = ['true', serverconfig]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_PUBLISH_HOST, 
            args=args)
    runutil.generic_bailout("Problem setting GT container to publish hostname", 
            exitcode, stdout, stderr)

    log.debug("Adjusted GT container to publish hostname in URLs")

def adjust_gridmap_file(gridmap, basedir, gtdir, log):
    if not pathutil.is_absolute_path(gridmap):
        raise IncompatibleEnvironment("gridmap path must be absolute")

    pathutil.ensure_file_exists(gridmap, "gridmap")

    secdesc = get_secdesc_path(gtdir)
    pathutil.ensure_file_exists(secdesc, "container security settings")

    args = [gridmap, secdesc]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_NEW_GRIDMAPFILE,
            args=args)
    runutil.generic_bailout("Problem setting new gridmap file location", 
            exitcode, stdout, stderr)

    log.debug("Adjusted GT container gridmap file to %s" % gridmap)

def adjust_secdesc_path(basedir, gtdir, log):

    secdesc = get_secdesc_path(gtdir)
    pathutil.ensure_file_exists(secdesc, "container security settings")

    serverconfig = get_serverconfig_path(gtdir)
    pathutil.ensure_file_exists(serverconfig, "GT server config")

    args = [secdesc, serverconfig]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_GLOBUS_SECDESC, 
            args=args)
    runutil.generic_bailout("Problem activating new security settings in GT container", 
            exitcode, stdout, stderr)

    log.debug("Activated new security settings file in GT container: %s" %
            secdesc)

def adjust_host_cert(cert, key, basedir, gtdir, log):

    pathutil.ensure_file_exists(cert, "host certificate")
    pathutil.ensure_file_exists(key, "host private key")

    secdesc = get_secdesc_path(gtdir)
    pathutil.ensure_file_exists(secdesc, "container security settings")

    args = [cert, secdesc]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_NEW_HOSTCERTFILE, 
            args=args)
    runutil.generic_bailout("Problem activating host certificate", 
            exitcode, stdout, stderr)
    log.debug("Activated host certificate file in GT container: %s" % cert)

    args = [key, secdesc]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_NEW_HOSTKEYFILE, 
            args=args)
    runutil.generic_bailout("Problem activating host key", 
            exitcode, stdout, stderr)
    log.debug("Activated host key file in GT container: %s" % cert)

def adjust_broker_config(cacert, cakey, keystore, keystore_pass, basedir, gtdir, log):
    brokerconfig = get_brokerconfig_path(gtdir)

    pathutil.ensure_file_exists(cacert, "CA certificate")
    pathutil.ensure_file_exists(cakey, "CA private key")
    pathutil.ensure_file_exists(brokerconfig, "Nimbus Context Broker config")
    pathutil.ensure_file_exists(keystore, "Java keystore")

    # is some BS
    restbroker_xml = pathutil.pathjoin(gtdir, 
            'etc/nimbus-context-broker/other/main.xml')
    pathutil.ensure_file_exists(restbroker_xml, 
            "Context Broker REST interface config")

    args = [brokerconfig, 'NimbusContextBroker', 'ctxBrokerBootstrapFactory',
            'caCertPath', cacert, 'caKeyPath', cakey]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, 
            EXE_SERVICE_RESOURCE, args=args)
    runutil.generic_bailout("Problem adjusting broker config", 
            exitcode, stdout, stderr)
    
    args = [brokerconfig, 'NimbusContextBroker', 'rest',
            'keystoreLocation', keystore, 'keystorePassword', keystore_pass,
            'springConfig', restbroker_xml]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, 
            EXE_SERVICE_RESOURCE, args=args)
    runutil.generic_bailout("Problem adjusting broker config", 
            exitcode, stdout, stderr)
    log.debug("Ensured Context Broker CA config: %s" % brokerconfig)

def get_brokerconfig_path(gtdir):
    return pathutil.pathjoin(gtdir, CONF_BROKERCONFIG)
    
def get_serverconfig_path(gtdir):
    return pathutil.pathjoin(gtdir, CONF_SERVERCONFIG)

def get_secdesc_path(gtdir):
    return pathutil.pathjoin(gtdir, CONF_SECDESC)

