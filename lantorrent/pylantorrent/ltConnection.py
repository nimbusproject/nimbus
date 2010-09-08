import sys
import os
import json
import socket
import logging
import traceback
from pylantorrent.ltException import LTException
import pylantorrent
import threading


class LTConnection(object):

    def __init__(self, json_ent, output_printer):
        self.ex = None
        self.read_buffer_len = 1024
        self.output_printer = output_printer

        if json_ent == None:
            self.valid = False
            return

        try:
            self.host = json_ent['host']
            self.port = int(json_ent['port'])
            self.file = json_ent['file']
            self.rid = json_ent['id']
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
            vex = LTException(505, "%s:%d" % (self.host, self.port), self.host, self.port, self.file, self.rid)
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
        header['file'] = self.file
        header['host'] = self.host
        header['port'] = self.port
        header['id'] = self.rid
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

    def send(self, data):
        if not self.valid:
            return

        try:
            self.socket.send(data)
        except Exception, ex:
            self.valid = False
            self.ex = LTException(506, "%s:%d %s" % (self.host, self.port, str(ex)), self.host, self.port, self.file, self.rid)
            pylantorrent.log(logging.WARNING, "send error " + str(self.ex), traceback)
            j = self.ex.get_json()
            s = json.dumps(j)
            self.output_printer.print_results(s)

    def read_output(self):
        line = ""
        while True:
            try:
                data = self.socket.recv(self.read_buffer_len)
            except:
                data = ""
            line = line + str(data)
            la = line.split('\n')
            while len(la) > 1:
                z = la.pop(0)
                pylantorrent.log(logging.DEBUG, "got resutls %s" % (z))
                if z.strip() == "EOD":
                    break
                self.output_printer.print_results(z)
                
            line = la.pop(0)
            if not data or data == "":
                break
            
    def close(self):
        try:
            self.valid = False
            #self.read_thread.join()
            self.socket.close()
        except:
            pass

    def close_read(self):
        if not self.valid:
            return
        self.socket.shutdown(socket.SHUT_WR)

    def get_exception(self):
        return self.ex
