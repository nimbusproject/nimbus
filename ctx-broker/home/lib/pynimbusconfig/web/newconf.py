from random import Random
import string

from pynimbusconfig import pathutil
from pynimbusconfig.setuperrors import *

def run(basedir, timezone, accountprompt, log, debug, insecuremode, printurl, 
        expire_hours, cadir):
    log.debug("Installing new configurations to django and cherrypy")
    
    if not accountprompt:
        accountprompt = "contact the administrator."
    
    if not timezone:
        raise IncompatibleEnvironment("There is no 'timezone' configuration")
        
    # --------------------------------------------------------------------------
    # The generated_settings.py file is created and replaced at will by this
    # newconf system.
    
    # sanity check:
    real_settings = pathutil.pathjoin(basedir, "src/python/nimbusweb/portal/settings.py")
    pathutil.ensure_file_exists(real_settings, "web settings")
    log.debug("file exists: %s" % real_settings)
    
    generated_settings = pathutil.pathjoin(basedir, "src/python/nimbusweb/portal/generated_settings.py")
    if pathutil.check_path_exists(generated_settings):
        log.debug("Going to overwrite previously written generated_settings.py")
    
    lines = []
    
    # sqlite DB
    db_path = pathutil.pathjoin(basedir, "var/nimbus.sqlite")
    lines.append("DATABASE_ENGINE = 'sqlite3'")
    lines.append("DATABASE_NAME = '%s'" % db_path)
    
    lines.append("TIME_ZONE = '%s'" % timezone)
    lines.append("NIMBUS_ACCOUNT_PROMPT = '%s'" % accountprompt)

    cadir_path = pathutil.pathjoin(basedir, cadir)
    lines.append("NIMBUS_CADIR = '%s'" % cadir_path)
    
    if debug:
        lines.append("DEBUG = True")
        lines.append("TEMPLATE_DEBUG = True")
    else:
        lines.append("DEBUG = False")
        lines.append("TEMPLATE_DEBUG = False")
        
    if insecuremode:
        lines.append("SESSION_COOKIE_SECURE = False")
    else:
        lines.append("SESSION_COOKIE_SECURE = True")
        
    lines.append("NIMBUS_PRINT_URL = '%s'" % printurl)
    lines.append("NIMBUS_TOKEN_EXPIRE_HOURS = %d" % expire_hours)

    generated_text = "\n"
    for line in lines:
        generated_text += line
        generated_text += "\n"
        
    log.debug("Going to write this to generated_settings:\n%s" % generated_text)

    f = open(generated_settings, 'w')
    f.write(generated_text)
    f.close()
    pathutil.ensure_file_exists(generated_settings, "generated web settings")
    print "Wrote generated_settings: %s" % generated_settings
    
    # --------------------------------------------------------------------------
    
    generated_secrets = pathutil.pathjoin(basedir, "src/python/nimbusweb/portal/generated_secrets.py")
    if not pathutil.check_path_exists(generated_secrets):
    
        # Creating secret each newconf would mean that people's sessions won't
        # work after webapp reboot and they would need to login again.
        # Instead, it is only written when nonexistent (clean-slate script will
        # remove it).
        lines = []
        okchars = string.letters + string.digits + "!@%^_&*+-"
        okchars += okchars
        secret = ''.join( Random().sample(okchars, 50) )
        lines.append("SECRET_KEY = '%s'" % secret)
        
        generated_text = "\n"
        for line in lines:
            generated_text += line
            generated_text += "\n"
            
        f = open(generated_secrets, 'w')
        f.write(generated_text)
        f.close()
        pathutil.ensure_file_exists(generated_secrets, "generated web secrets")
        print "Wrote generated_secrets: %s" % generated_secrets
