import sys
import os
try:
    import json
except ImportError:
    import simplejson as json
import socket
import logging
import traceback
from pylantorrent.ltException import LTException
import pylantorrent
import select
import zlib

class LTDataTransformZip(object):

    def incoming_data(self, data):
        return data

class LTDestConnection(object):

    def __init__(self, json_ent, output_printer, data_transform=None):
        self.ex = None
        self.read_buffer_len = 1024
        self.output_printer = output_printer
        self.data_transform = data_transform

        if json_ent == None:
            self.valid = False
            return

        try:
            self.host = json_ent['host']
            self.port = int(json_ent['port'])
            self.requests = json_ent['requests']
            self.block_size = int(json_ent['block_size'])
            self.degree = int(json_ent['degree'])
            self.data_length = int(json_ent['length'])
        except Exception, ex:
            vex = LTException(504, str(json_ent) + " :: " + str(ex))
            pylantorrent.log(logging.ERROR, str(vex), traceback)
            raise vex

        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect((self.host, self.port))
            self.socket = s
        except Exception, ex:
            vex = LTException(505, "%s:%d" % (self.host, self.port), self.host, self.port, reqs=self.requests)
            pylantorrent.log(logging.ERROR, str(vex), traceback)
            raise vex

        self.valid = True
        #self.read_thread = threading.Thread(target=self.read_output, args=())
        #self.read_thread.start()

    def get_block_size(self):
        if not self.valid:
            return 1024*128
        return self.block_size

    def send_header(self, destinations):
        if not self.valid:
            return

        header = {}
        header['requests'] = self.requests
        header['host'] = self.host
        header['port'] = self.port
        header['block_size'] = self.block_size
        header['degree'] = self.degree
        header['length'] = self.data_length
        header['destinations'] = destinations
        send_str = json.dumps(header)
        send_str = send_str + "\n"
        pylantorrent.log(logging.DEBUG, "sending header %s" % (send_str))
        signature = pylantorrent.get_auth_hash(send_str)
        self.send(send_str)
        self.send("EOH : %s\r\n" % (signature))

    def _poll(self, poll_period=0.0):
        if not self.valid:
            return
        try:
            self.socket.settimeout(poll_period)
            data = self._read_from_socket(self.read_buffer_len)
            if data:
                self.output_printer.print_results(data)
        except:
            # there may jsut be no data now
            pass
        self.socket.settimeout(None)

    def _read_from_socket(self, size):
        data = self.socket.recv(size)
        return data

    def _write_to_socket(self, data):
        self.socket.sendall(data)

    def read_to_eof(self):
        if not self.valid:
            return
        self.socket.settimeout(None)
        data = self._read_from_socket(self.read_buffer_len)
        while data:
            self.output_printer.print_results(data)
            data = self._read_from_socket(self.read_buffer_len)

    def send(self, data):
        if not self.valid:
            return
        try:
            self._write_to_socket(data)
        except Exception, ex:
            self.valid = False
            self.ex = LTException(506, "%s:%s %s" % (self.host, str(self.port), str(ex)), self.host, self.port, self.requests)
            pylantorrent.log(logging.WARNING, "send error " + str(self.ex), traceback)
            data = self._read_from_socket(self.read_buffer_len)
            while data:
                pylantorrent.log(logging.WARNING, "bad data: " + str(data))
                data = self._read_from_socket(self.read_buffer_len)
            j = self.ex.get_json()
            s = json.dumps(j)
            self.output_printer.print_results(s)
        # see if there is anything to read
        self._poll()


    def close(self, force=False):
        # reading of footer waits for eof so this is needed
        self.socket.shutdown(socket.SHUT_WR)
        self.read_to_eof()
        self.valid = False
        self.socket.close()

    def get_exception(self):
        return self.ex


class LTSourceConnection(object):

    def __init__(self, infile_obj, data_transform=None):
        self.inf = infile_obj
        self.footer = None
        self.header = None
        self.max_header_lines = 256
        self.data_transform = data_transform

    def _read(self, bs=None):
        if bs == None:
            d = self.inf.read()
        else:
            d = self.inf.read(bs)
        return d

    def _readline(self):
        l = self.inf.readline()
        return l

    def read_footer(self, md5str):
        if self.footer:
            return self.footer

        pylantorrent.log(logging.DEBUG, "begin reading the footer")
        lines = ""
        l = self._read()
        while l:
            lines = lines + l
            l = self._read()
        pylantorrent.log(logging.DEBUG, "footer is %s" % (lines))
        foot = json.loads(lines)
        if foot['md5sum'] != md5str:
            raise LTException(510, "%s != %s" % (md5str, foot['md5sum']), header['host'], int(header['port']), requests_a, md5sum=md5str)
        self.footer = foot
        return foot


    def read_header(self):
        if self.header:
            return self.header

        pylantorrent.log(logging.INFO, "reading a new header")

        count = 0
        lines = ""
        l = self._readline()
        while l:
            ndx = l.find("EOH : ")
            if ndx == 0:
                break
            lines = lines + l
            l = self._readline()
            count = count + 1
            if count == self.max_header_lines:
                raise LTException(501, "%d lines long, only %d allowed" % (count, max_header_lines))
        if l == None:
            raise LTException(501, "No signature found")
        signature = l[len("EOH : "):].strip()

        auth_hash = pylantorrent.get_auth_hash(lines)

        if auth_hash != signature:
            pylantorrent.log(logging.INFO, "ACCESS DENIED |%s| != |%s| -->%s<---" % (auth_hash, signature, lines))
            raise LTException(508, "%s is a bad signature" % (auth_hash))

        self.header = json.loads(lines)
        return self.header

        # verify the header
        try:
            reqs = self.header['requests']
            for r in reqs:
                filename = r['filename']
                rid = r['id']
                rn = r['rename']

            host = self.header['host']
            port = int(self.header['port'])
            urls = self.header['destinations']
            degree = int(self.header['degree'])
            data_length = long(self.header['length'])
        except Exception, ex:
            raise LTException(502, str(ex), traceback)

    def read_data(self, bs):
        return self._read(bs)

class LTDestConnectionZip(LTDestConnection):

    def __init__(self, json_ent, output_printer):
        LTDestConnection.__init__(self, json_ent, output_printer)

class LTSourceConnectionZip(LTSourceConnection):

    def __init__(self, infile_obj):
        LTSourceConnection.__init__(self, infile_obj)
