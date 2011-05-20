#### right now this is just used to create the db

import sqlalchemy
from sqlalchemy.orm import mapper
from sqlalchemy.orm import sessionmaker
from sqlalchemy import Table
from sqlalchemy import Integer
from sqlalchemy import String, MetaData, Sequence
from sqlalchemy import Column
from sqlalchemy import types
from datetime import datetime
import sqlite3

metadata = MetaData()

request_table = Table('requests', metadata,
    Column('id', Integer, Sequence('event_id_seq')),
    Column('rid', String(36), nullable=False, unique=True),
    Column('src_filename', String(1024), nullable=False),
    Column('dst_filename', String(1024), nullable=False, primary_key = True),
    Column('xfer_id', String(64)),
    Column('hostname', String(1024), nullable=False, primary_key = True),
    Column('port', Integer, nullable=False, default=2893, primary_key = True),
    Column('state', Integer, nullable=False, default=0),
    Column('message', String(4096), nullable=True),
    Column('entry_time', types.TIMESTAMP(), default=datetime.now()),
    Column('attempt_count', Integer, nullable=False, default=0),
    )

class RequestTable(object):
    def __init__(self):
        self.id = None
        self.rid = None
        self.src_filename = None
        self.dst_filename = None
        self.xfer_id = None
        self.hostname = None
        self.port = None
        self.state = None
        self.message = None
        self.entry_time = None
        self.attempt_count = None

mapper(RequestTable, request_table)

class LantorrentDB(object):

    def __init__(self, dburl):

        # this is raw sql deal that i used
        self._engine = sqlalchemy.create_engine(dburl,
            connect_args={'detect_types': sqlite3.PARSE_DECLTYPES|sqlite3.PARSE_COLNAMES},
            native_datetime=True
            )
        
        metadata.create_all(self._engine)
        self._Session = sessionmaker(bind=self._engine)
        self._session = self._Session()


    def db_obj_add(self, obj):
        self._session.add(obj)

    def db_commit(self):
        self._session.commit()

    def db_rollback(self):
        self._session.rollback()

    def raw_sql(self, sql):
        con = self._session.connection()
        res = con.execute(sql)
        return list(res)

    def close(self):
        self._session.close()