import sqlite3
import os
import itertools

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

        if con_str != None:
            self.con = sqlite3.connect(con_str)
        else:
            self.con = con
        #self.con.isolation_level = "EXCLUSIVE"

    def _run_no_fetch(self, s, data):
        c = self.con.cursor()
        c.execute(s, data)
        c.close()

    def _run_fetch_iterator(self, s, data, convert_func, args=None):
        c = self.con.cursor()
        c.execute(s, data)
        new_it = itertools.imap(lambda r: convert_func(self, r, args), c) 
        return new_it

    def _run_fetch_all(self, s, data):
        c = self.con.cursor()
        c.execute(s, data)
        r = c.fetchall()
        c.close()
        return r


    def _run_fetch_one(self, s, data):
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
