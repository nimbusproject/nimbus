import sqlite3
import pynimbusauthz
import uuid

class AuthzException(Exception):
    e_types = {}
    e_types['CLI_PARAMETER'] = 32
    e_types['FILE_EXISTS'] = 33
    e_types['USER_EXISTS'] = 34
    e_types['DB_ERROR'] = 35

    e_types['UNKNOWN'] = 254
    
    def __init__(self, error_type, details):
        if error_type not in AuthzException.e_types.keys():
            error_type = 'UNKNOWN'
        self.error_type = error_type
        self.details = details


    def __str__(self):
        return self.error_type + " : " + self.details

    def get_rc(self):
        rc = AuthzException.e_types[self.error_type]
        return rc
