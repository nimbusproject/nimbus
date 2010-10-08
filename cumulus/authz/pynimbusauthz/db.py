import sqlite3
import os
import itertools
import urlparse
import pynimbusauthz
from pynimbusauthz.authz_exception import AuthzException

def make_test_database(db_str=":memory:"):
    f = open(os.environ['CUMULUS_AUTHZ_DDL'], "r")
    s = ""
    for l in f.readlines():
        s = s + " " + l
    f.close()
    commands = s.split(";")
    conn = sqlite3.connect(db_str)
    c = conn.cursor()
    for e in commands:
        c.execute(e)
    conn.commit()
    return conn



# a simple wrapper around readonly
class DB(object):
    def __init__(self, con_str=None, con=None):
        self.replace_char = None

        if con_str != None:
            parts_a = con_str.split("://", 1)

            if len(parts_a) > 1:
                scheme = parts_a[0]
                rest = parts_a[1]
            else:
                rest = con_str
                scheme = ""
            url = urlparse.urlparse("http://" + rest)
            if scheme == "sqlite" or scheme == '':
                self.con = sqlite3.connect(url.path)
            elif scheme == "psycopg2":
                import psycopg2

                self.replace_char = '%s'
                db = url.path[1:]
                self.con = psycopg2.connect(user=url.username, password=url.password, host=url.hostname, database=db, port=url.port)
            else:
                raise AuthzException('DB_ERROR', "unsupport db url %s" % (con_str))
        else:
            self.con = con
        #self.con.isolation_level = "EXCLUSIVE"

    def _run_no_fetch(self, s, data):
        if self.replace_char:
            s = s.replace('?', self.replace_char)
        c = self.con.cursor()
        c.execute(s, data)
        c.close()

    def _run_fetch_iterator(self, s, data, convert_func, args=None):
        if self.replace_char:
            s = s.replace('?', self.replace_char)
        c = self.con.cursor()
        c.execute(s, data)
        new_it = itertools.imap(lambda r: convert_func(self, r, args), c) 
        return new_it

    def _run_fetch_all(self, s, data):
        if self.replace_char:
            s = s.replace('?', self.replace_char)
        c = self.con.cursor()
        c.execute(s, data)
        r = c.fetchall()
        c.close()
        return r


    def _run_fetch_one(self, s, data):
        if self.replace_char:
            s = s.replace('?', self.replace_char)
        c = self.con.cursor()
        c.execute(s, data)
        r = c.fetchone()
        c.close()
        return r
        
    def commit(self):
        self.con.commit()

    def rollback(self):
        self.con.rollback()

    def close(self):
        self.con.close()
