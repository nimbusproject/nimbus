#!/usr/bin/env python

import logging, sys, os, signal, time, errno
from socket import gethostname
from django.core.management.base import BaseCommand


CPSERVER_HELP = r"""
  Run this project in a CherryPy webserver. To do this, CherryPy from
  http://www.cherrypy.org/ is required.

   runcpserver [options] [cpserver settings] [stop]

Optional CherryPy server settings: (setting=value)
  host=HOSTNAME         hostname to listen on
                        Defaults to localhost
  port=PORTNUM          port to listen on
                        Defaults to 8088
  server_name=STRING    CherryPy's SERVER_NAME environ entry
                        Defaults to localhost
  daemonize=BOOL        whether to detach from terminal
                        Defaults to False
  pidfile=FILE          write the spawned process-id to this file
  workdir=DIRECTORY     change to this directory when daemonizing
  threads=NUMBER        Number of threads for server to use
  ssl_certificate=FILE  SSL certificate file
  ssl_private_key=FILE  SSL private key file
  server_user=STRING    user to run daemonized process
                        Defaults to www-data
  server_group=STRING   group to daemonized process
                        Defaults to www-data

Examples:
  Run a "standard" CherryPy server server
    $ manage.py runcpserver

  Run a CherryPy server on port 80
    $ manage.py runcpserver port=80

  Run a CherryPy server as a daemon and write the spawned PID in a file
    $ manage.py runcpserver daemonize=true pidfile=/var/run/django-cpserver.pid

"""

CPSERVER_OPTIONS = {
'host': 'localhost',
'port': 8088,
'server_name': 'localhost',
'threads': 10, 
'daemonize': False,
'workdir': None,
'pidfile': None,
'server_user': 'www-data',
'server_group': 'www-data',
'ssl_certificate': None,
'ssl_private_key': None,
}


class Command(BaseCommand):
    help = "CherryPy Server for project. Requires CherryPy."
    args = "[various KEY=val options, use `runcpserver help` for help]"

    def handle(self, *args, **options):
        from django.conf import settings
        from django.utils import translation
        # Activate the current language, because it won't get activated later.
        try:
            translation.activate(settings.LANGUAGE_CODE)
        except AttributeError:
            pass
        runcpserver(args)
        
    def usage(self, subcommand):
        return CPSERVER_HELP

def change_uid_gid(uid, gid=None):
    """Try to change UID and GID to the provided values.
    UID and GID are given as names like 'nobody' not integer.

    Src: http://mail.mems-exchange.org/durusmail/quixote-users/4940/1/
    """
    if not os.geteuid() == 0:
        # Do not try to change the gid/uid if not root.
        return
    (uid, gid) = get_uid_gid(uid, gid)
    os.setgid(gid)
    os.setuid(uid)

def get_uid_gid(uid, gid=None):
    """Try to change UID and GID to the provided values.
    UID and GID are given as names like 'nobody' not integer.

    Src: http://mail.mems-exchange.org/durusmail/quixote-users/4940/1/
    """
    import pwd, grp
    uid, default_grp = pwd.getpwnam(uid)[2:4]
    if gid is None:
        gid = default_grp
    else:
        try:
            gid = grp.getgrnam(gid)[2]            
        except KeyError:
            gid = default_grp
    return (uid, gid)
    
    
def poll_process(pid):
    """
    Poll for process with given pid up to 10 times waiting .25 seconds in between each poll. 
    Returns False if the process no longer exists otherwise, True.
    """
    for n in range(10):
        time.sleep(0.25)
        try:
            # poll the process state
            os.kill(pid, 0)
        except OSError, e:
            if e[0] == errno.ESRCH:
                # process has died
                return False
            else:
                raise Exception
    return True

def stop_server(pidfile):
    """
    Stop process whose pid was written to supplied pidfile. 
    First try SIGTERM and if it fails, SIGKILL. If process is still running, an exception is raised.
    """
    if os.path.exists(pidfile):
        pid = int(open(pidfile).read())
        try:
            os.kill(pid, signal.SIGTERM)
        except OSError: #process does not exist
            os.remove(pidfile)
            return
        if poll_process(pid):
            #process didn't exit cleanly, make one last effort to kill it
            os.kill(pid, signal.SIGKILL)
            if poll_process(pid):
                raise OSError, "Process %s did not stop."
        os.remove(pidfile)

def start_server(options):
    """
    Start CherryPy server
    """
    
    if options['daemonize'] and options['server_user'] and options['server_group']:
        #ensure the that the daemon runs as specified user
        change_uid_gid(options['server_user'], options['server_group'])
    
    from cherrypy.wsgiserver import CherryPyWSGIServer as Server
    from django.core.handlers.wsgi import WSGIHandler
    server = Server(
        (options['host'], int(options['port'])),
        WSGIHandler(), 
        int(options['threads']), 
        options['server_name']
    )
    if options['ssl_certificate'] and options['ssl_private_key']:
        server.ssl_certificate = options['ssl_certificate']
        server.ssl_private_key = options['ssl_private_key']  
    try:
        server.start()
    except KeyboardInterrupt:
        server.stop()


def runcpserver(argset=[], **kwargs):
    # Get the options
    options = CPSERVER_OPTIONS.copy()
    options.update(kwargs)
    for x in argset:
        if "=" in x:
            k, v = x.split('=', 1)
        else:
            k, v = x, True
        options[k.lower()] = v
    
    if "help" in options:
        print CPSERVER_HELP
        return
        
    if "stop" in options:
        stop_server(options['pidfile'])
        return True
    
    if options['daemonize']:
        if not options['pidfile']:
            options['pidfile'] = '/var/run/cpserver_%s.pid' % options['port']
        stop_server(options['pidfile'])     
       
        from django.utils.daemonize import become_daemon
        if options['workdir']:
            become_daemon(our_home_dir=options['workdir'])
        else:
            become_daemon()

        fp = open(options['pidfile'], 'w')
        fp.write("%d\n" % os.getpid())
        fp.close()
    
    # Start the webserver
    print 'starting server with options %s' % options
    start_server(options)


if __name__ == '__main__':
    runcpserver(sys.argv[1:])
