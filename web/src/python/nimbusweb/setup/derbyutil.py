import os
import re
import sys
import time
import shutil
import tempfile
from subprocess import Popen, PIPE

def update_db(ij_path, old_db, new_db):
    """
    ij_path -- assumes that this exists
    old_db -- path to 2.2 or 2.3 accounting db
    new_db -- path to 2.4 accounting db
    """

    def _run_sql_on_db(ij_path, database, sql, user=None, password=None):
        script = "connect 'jdbc:derby:%s' " % database
        if user and password:
            script += "user '%s' " % user
            script += "password '%s' " % password
        script += ";"
        script += sql
        script += "disconnect; exit;"
        
        (script_file, script_filename) = tempfile.mkstemp()
        os.write(script_file, script)
        os.close(script_file)

        ij_args = ij_path.split()
        ij_args.append(script_filename)
        output = Popen(ij_args, stdout=PIPE).communicate()[0]
        os.remove(script_filename)
        
        return output

    # Check that the databases exist
    if not os.path.exists(old_db):
        print >> sys.stderr, "Error in db-update: %s doesn't exist" % old_db
        return 1

    if not os.path.exists(new_db):
        print >> sys.stderr, "Error in db-update: %s doesn't exist" % new_db
        return 1

    # determine schema of deployments table
    tables_sql = "SHOW TABLES;"
    output = _run_sql_on_db(ij_path, old_db, tables_sql)
    find_schema = re.compile(".*(APP|NIMBUS)\s*\|DEPLOYMENTS", re.DOTALL)
    schema = find_schema.match(output).group(1)

    # Pull data out of old database
    select_sql = """
        SELECT uuid, workspaceid, creator_dn, creation_time,\
               requested_duration, active, elapsed_minutes\
        FROM %s.deployments;
    """ % schema
    output = _run_sql_on_db(ij_path, old_db, select_sql)
    if re.match(".*error", output, re.IGNORECASE | re.DOTALL):
        print >> sys.stderr, "Error in db-update: Problem getting old data"
        print >> sys.stderr, output
        return 1
        

    insert_sql = ""
    for line in output.splitlines():
        # disgard lines that aren't data
        if line.startswith("ij") or line.startswith("UUID ") \
           or line.startswith("-------") or line.endswith("selected")\
           or line == "":
            continue

        elements = line.split("|")
        elements = [element.strip() for element in elements]
        
        insert_sql += "INSERT INTO nimbus.deployments (uuid, workspaceid, creator_dn,"\
              "creation_time, requested_duration, active, elapsed_minutes) "\
               "VALUES ('%s', %s, '%s', %s, %s, %s, %s);\n" %\
               (elements[0], elements[1], elements[2], elements[3],
                elements[4], elements[5], elements[6])

    output = _run_sql_on_db(ij_path, new_db, insert_sql)
    if re.match(".*error", output, re.IGNORECASE | re.DOTALL):
        print >> sys.stderr, "Error in db-update: Problem inserting data"
        print >> sys.stderr, output
        return 1
