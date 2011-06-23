import os
import shutil
import sys

from pynimbusconfig import javautil
from pynimbusconfig import pathutil
from pynimbusconfig import runutil
from pynimbusconfig.setuperrors import *

# Make this False if you want to keep stuff around for examining, otherwise it
# would be in an inconsistent state after an exception during CA creation.
WIPE_NEW_CA_DIRECTORY_ON_ERRORS = True


EXE_HOSTGUESS="org.nimbustools.auto_common.HostGuess"
EXE_NEW_HOSTCERTFILE="org.nimbustools.auto_common.confmgr.ReplaceCertFile"
EXE_NEW_HOSTKEYFILE="org.nimbustools.auto_common.confmgr.ReplaceKeyFile"
EXE_CREATE_NEW_CA="org.nimbustools.auto_common.ezpz_ca.GenerateNewCA"
EXE_CREATE_CRL="org.nimbustools.auto_common.ezpz_ca.GenerateCRL"
EXE_CREATE_NEW_CERT="org.nimbustools.auto_common.ezpz_ca.GenerateNewCert"
EXE_FIND_CA_PUBPEM="org.nimbustools.auto_common.ezpz_ca.FindCAPubFile"
EXE_FIND_CA_PRIVPEM="org.nimbustools.auto_common.ezpz_ca.FindCAPrivFile"
EXE_GET_HASHED_CERT_NAME="org.nimbustools.auto_common.ezpz_ca.CertFilenameHash"
EXE_GET_CERT_DN="org.nimbustools.auto_common.ezpz_ca.CertDN"
EXE_WRITE_SIGNING_POLICY="org.nimbustools.auto_common.ezpz_ca.SigningPolicy"
EXE_KEYSTORE_FROM_PEM="org.nimbustools.auto_common.ezpz_ca.KeystoreFromPEM"

def createCert(CN, basedir, cadir, certtarget, keytarget, log, 
        allow_overwrite=False):
    
    if not allow_overwrite and pathutil.check_path_exists(certtarget):
        msg = "Certificate file present already: " + certtarget
        raise IncompatibleEnvironment(msg)
    if not allow_overwrite and pathutil.check_path_exists(keytarget):
        msg = "Key file present already: " + keytarget
        raise IncompatibleEnvironment(msg)
    
    cacert_path = findCAcert(basedir, cadir, log)
    cakey_path = findCAkey(basedir, cadir, log)
    
    # Create temp directory.
    uuid = pathutil.uuidgen()
    tempdir = pathutil.pathjoin(cadir, uuid)
    os.mkdir(tempdir)
    pathutil.ensure_dir_exists(tempdir, "temp certs directory")
    log.debug("Created %s" % tempdir)
    
    args = [tempdir, CN, "pub", "priv", cacert_path, cakey_path]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_CREATE_NEW_CERT, args=args)
    runutil.generic_bailout("Problem creating certificate.", exitcode, stdout, stderr)
    
    pub_DN = stdout.strip()
    
    temp_pub_path = pathutil.pathjoin(tempdir, "pub")
    pathutil.ensure_file_exists(temp_pub_path, "temp cert")
    log.debug("temp cert exists: " + temp_pub_path)
    
    # copy that to user-cert records
    args = [temp_pub_path]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_GET_HASHED_CERT_NAME, args=args)
    runutil.generic_bailout("Problem finding hashed cert name.", exitcode, stdout, stderr)
    usercertfilehash = stdout.strip()
    log.debug("user cert file hash is '%s'" % usercertfilehash)
    cert_records_path = pathutil.pathjoin(cadir, "user-certs")
    cert_records_path = pathutil.pathjoin(cert_records_path,
                                          usercertfilehash + ".0")
    shutil.copyfile(temp_pub_path, cert_records_path)
    pathutil.ensure_file_exists(cert_records_path, "new certificate (record)")
    log.debug("cert exists at target: " + cert_records_path)
    
    temp_priv_path = pathutil.pathjoin(tempdir, "priv")
    pathutil.ensure_file_exists(temp_priv_path, "temp key")
    log.debug("temp key exists: " + temp_priv_path)
    
    log.debug("Created certificate: %s" % pub_DN)
    
    # Those user-supplied targets still don't exist, right? :-)
    if not allow_overwrite and pathutil.check_path_exists(certtarget):
        msg = "Certificate file present already: " + certtarget
        raise IncompatibleEnvironment(msg)
    if not allow_overwrite and pathutil.check_path_exists(keytarget):
        msg = "Key file present already: " + keytarget
        raise IncompatibleEnvironment(msg)
    
    shutil.copyfile(temp_pub_path, certtarget)
    pathutil.ensure_file_exists(certtarget, "new certificate")
    log.debug("cert exists at target: " + certtarget)
    
    shutil.copyfile(temp_priv_path, keytarget)
    pathutil.ensure_file_exists(keytarget, "new key")
    log.debug("key exists at target: " + keytarget)
    
    pathutil.make_path_rw_private(keytarget)
    pathutil.ensure_path_private(keytarget, "new key")
    log.debug("file made private: %s" % keytarget)
    
    shutil.rmtree(tempdir)

    return pub_DN

class KeystoreMismatchError(Exception):
    pass

def ensureKeystore(certpath, keypath, storepath, password, basedir, log):
    """
    Creates or validates a Java keystore from PEM-encoded certificate and key
    """

    if not pathutil.check_path_exists(certpath):
        msg = "Certificate file does not exist: " + certpath
        raise IncompatibleEnvironment(msg)
    
    if not pathutil.check_path_exists(keypath):
        msg = "Private key file does not exist: " + keypath
        raise IncompatibleEnvironment(msg)

    if pathutil.check_path_exists(storepath):
        log.debug("Keystore file exists: %s." % storepath, 
                "Ensuring that it contains right cert/key")

    args = [certpath, keypath, storepath, password]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, 
            EXE_KEYSTORE_FROM_PEM, args=args)
    if exitcode == 2:
        raise KeystoreMismatchError(stderr)
    runutil.generic_bailout("Problem creating keystore", 
            exitcode, stdout, stderr)

def getCertDN(certpath, basedir, log):

    if not pathutil.check_path_exists(certpath):
        msg = "Certificate file does not exist: " + certpath
        raise IncompatibleEnvironment(msg)
    
    args = [certpath]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, 
            EXE_GET_CERT_DN, args=args)
    runutil.generic_bailout("Problem finding cert DN", 
            exitcode, stdout, stderr)

    return stdout.strip()

def findCAcert(basedir, cadir, log):
    cacertdir = pathutil.pathjoin(cadir, "ca-certs")
    args = [cacertdir]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_FIND_CA_PUBPEM, args=args)
    runutil.generic_bailout("Problem finding CA certificate.", exitcode, stdout, stderr)
    if not stdout:
        raise UnexpectedError("Path is not present for CA certificate")
    certpath = stdout.strip()
    pathutil.ensure_file_exists(certpath, "CA certificate")
    return certpath
    
def findCAkey(basedir, cadir, log):
    cacertdir = pathutil.pathjoin(cadir, "ca-certs")
    args = [cacertdir]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_FIND_CA_PRIVPEM, args=args)
    runutil.generic_bailout("Problem finding CA key.", exitcode, stdout, stderr)
    if not stdout:
        raise UnexpectedError("Path is not present for CA key")
    keypath = stdout.strip()
    pathutil.ensure_file_exists(keypath, "CA key")
    return keypath
    
def createCA(ca_name, basedir, cadir, log):
    if pathutil.check_path_exists(cadir):
        raise IncompatibleEnvironment("cannot create a CA at a directory that exists already")
    try:
        _createCA(ca_name, basedir, cadir, log)
    except:
        if not WIPE_NEW_CA_DIRECTORY_ON_ERRORS:
            raise
        # wipe the whole directory
        print >>sys.stderr, "Error, wiping the unfinished '%s' directory" % cadir
        shutil.rmtree(cadir)
        raise
        
def _createCA(ca_name, basedir, cadir, log):
    
    javautil.check(basedir, log)
    
    # mkdir $cadir
    # mkdir $cadir/ca-certs
    # mkdir $cadir/trusted-certs
    # mkdir $cadir/user-certs
    
    os.mkdir(cadir)
    pathutil.ensure_dir_exists(cadir, "New CA directory")
    log.debug("Created %s" % cadir)
    
    cacertdir = pathutil.pathjoin(cadir, "ca-certs")
    os.mkdir(cacertdir)
    pathutil.ensure_dir_exists(cacertdir, "New CA certs directory")
    log.debug("Created %s" % cacertdir)
    
    trustedcertdir = pathutil.pathjoin(cadir, "trusted-certs")
    os.mkdir(trustedcertdir)
    pathutil.ensure_dir_exists(trustedcertdir, "New CA trusted certs directory")
    log.debug("Created %s" % trustedcertdir)
    
    usercertdir = pathutil.pathjoin(cadir, "user-certs")
    os.mkdir(usercertdir)
    pathutil.ensure_dir_exists(usercertdir, "New CA user certs directory")
    log.debug("Created %s" % usercertdir)
    
    # Create the cert via autocommon
    
    args = [cacertdir, ca_name]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_CREATE_NEW_CA, args=args)
    runutil.generic_bailout("Problem creating CA.", exitcode, stdout, stderr)
    
    
    # Make the private key owner-readable only
    
    privkeyname = "private-key-" + ca_name + ".pem"
    cakeyfile = pathutil.pathjoin(cacertdir, privkeyname)
    pathutil.ensure_file_exists(cakeyfile, "New CA key")
    log.debug("file exists: %s" % cakeyfile)
    pathutil.make_path_rw_private(cakeyfile)
    pathutil.ensure_path_private(cakeyfile, "New CA key")
    log.debug("file made private: %s" % cakeyfile)
    
    
    # Copy the new certificate file to the "hash.0" version that some toolings
    # will expect.
    
    cacertfile = pathutil.pathjoin(cacertdir, ca_name + ".pem")
    pathutil.ensure_file_exists(cacertfile, "New CA cert")
    log.debug("file exists: %s" % cacertfile)
    
    args = [cacertfile]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_GET_HASHED_CERT_NAME, args=args)
    runutil.generic_bailout("Problem finding hashed cert name.", exitcode, stdout, stderr)
    cacertfilehash = stdout.strip()
    log.debug("cert file hash is '%s'" % cacertfilehash)
    
    newpath = pathutil.pathjoin(cacertdir, cacertfilehash + ".0")
    shutil.copyfile(cacertfile, newpath)
    pathutil.ensure_file_exists(newpath, "New CA cert (hashed #1)")
    log.debug("file exists: %s" % newpath)
    
    newpath = pathutil.pathjoin(trustedcertdir, cacertfilehash + ".0")
    shutil.copyfile(cacertfile, newpath)
    pathutil.ensure_file_exists(newpath, "New CA cert (hashed #2)")
    log.debug("file exists: %s" % newpath)
    
    # Signing policy
    
    signing1 = pathutil.pathjoin(cacertdir, cacertfilehash + ".signing_policy")
    args = [cacertfile, signing1]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_WRITE_SIGNING_POLICY, args=args)
    runutil.generic_bailout("Problem creating signing_policy file.", exitcode, stdout, stderr)
    pathutil.ensure_file_exists(signing1, "signing_policy file #1")
    log.debug("file exists: %s" % signing1)
    
    signing2 = pathutil.pathjoin(trustedcertdir, cacertfilehash + ".signing_policy")
    shutil.copyfile(signing1, signing2)
    pathutil.ensure_file_exists(signing2, "signing_policy file #2")
    log.debug("file exists: %s" % signing2)
        
    # CRL
    
    crl1 = pathutil.pathjoin(cacertdir, cacertfilehash + ".r0")
    args = [crl1, cacertfile, cakeyfile]
    (exitcode, stdout, stderr) = javautil.run(basedir, log, EXE_CREATE_CRL, args=args)
    runutil.generic_bailout("Problem creating revocation file.", exitcode, stdout, stderr)
    pathutil.ensure_file_exists(crl1, "revocation file #1")
    log.debug("file exists: %s" % crl1)
    
    crl2 = pathutil.pathjoin(trustedcertdir, cacertfilehash + ".r0")
    shutil.copyfile(crl1, crl2)
    pathutil.ensure_file_exists(crl2, "revocation file #2")
    log.debug("file exists: %s" % crl2)
