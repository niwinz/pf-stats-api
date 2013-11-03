#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from subprocess import Popen, PIPE
import argparse
import shlex
import datetime
import re
import ipaddress as ip
import functools
import os
import os.path
import threading
import json
import curses
import time
import socket
import functools

@functools.lru_cache(maxsize=100000, typed=False)
def humanize_bytes(bytes, precision=1):
    """Return a humanized string representation of a number of bytes.

    Assumes `from __future__ import division`.

    >>> humanize_bytes(1)
    '1 byte'
    >>> humanize_bytes(1024)
    '1.0 kB'
    >>> humanize_bytes(1024*123)
    '123.0 kB'
    >>> humanize_bytes(1024*12342)
    '12.1 MB'
    >>> humanize_bytes(1024*12342,2)
    '12.05 MB'
    >>> humanize_bytes(1024*1234,2)
    '1.21 MB'
    >>> humanize_bytes(1024*1234*1111,2)
    '1.31 GB'
    >>> humanize_bytes(1024*1234*1111,1)
    '1.3 GB'
    """
    abbrevs = (
        (1<<50, 'PB'),
        (1<<40, 'TB'),
        (1<<30, 'GB'),
        (1<<20, 'MB'),
        (1<<10, 'kB'),
        (1, 'bytes')
    )
    if bytes == 1:
        return '1 byte'
    for factor, suffix in abbrevs:
        if bytes >= factor:
            break
    return '%.*f %s' % (precision, bytes / factor, suffix)


def auto_lock(method):
    @functools.wraps(method)
    def _decorator(*args, **kwargs):
        with threading.Lock():
            return method(*args, **kwargs)
    return _decorator


class Status(object):
    def __init__(self):
        self.running = False
        self.data = {}

    def update_stamp(self):
        self.last_updated = datetime.datetime.now()

    def set_worker_status(self, running):
        self.running = running

    def generate_new_keydata(self):
        return {
            "in": 0,        # bytes
            "out": 0,       # bytes
            "packets": 0,   # total number of packets analyzed
        }

    def update_key(self, key, dt):
        if key not in self.data:
            self.data[key] = self.generate_new_keydata()

        data = self.data[key]
        data["packets"] += 1

        # Update total trafic stats
        direction = dt["direction"]
        data[direction] += int(dt["bytes"])
        data["last_packet"] = datetime.datetime.utcnow().isoformat()


status = Status()
status.update_stamp()


class StreamService(threading.Thread):
    _file_cmd = "tcpdump -n -e -ttt -r {0}"
    _file_cmd = "cat {0}"
    _realtime_cmd = "tcpdump -n -e -ttt -i {0}"

    rx = re.compile(r"""
        ^([^\s]+)\s                         # Code part
        (rule\s[^\s]+)\s                    # Rule number
        (\w+)\s(\w+)\s\w+\s([\w\d]+)\:\s    # Rule definition
        (\d+\.\d+\.\d+\.\d+)\.(\d+)\s       # Source host and port
        [\>\<]\s                            # Direction
        (\d+\.\d+\.\d+\.\d+)\.(\d+)         # Destination host and port
        .*
        length\s(\d+)$                      # Packet length
    """, flags=re.X)

    def __init__(self, dest, stop_event):
        self.dest = dest
        self.stop_event = stop_event
        super().__init__()

    def update_status(self, data):
        global status

        *_, ract, rdir, rif, src_ip, src_port, dst_ip, dst_port, bs = data
        key_host = src_ip if rdir == "in" else dst_ip

        if key_host == "255.255.255.255":
            return

        if key_host.endswith(".255"):
            return

        if key_host == "0.0.0.0":
            return

        status.update_key(key_host, {
            "src_ip": src_ip,
            "dst_ip": dst_ip,
            "src_port": src_port,
            "dst_port": dst_port,
            "rule_if": rif,
            "bytes": bs,
            "direction": "out" if rdir == "in" else "in",
        })

    def parse_line(self, line):
        res = self.rx.search(line)
        if res:
            self.update_status(res.groups())

    def run(self):
        if os.path.exists(self.dest):
            return self.parse(self.dest, True)
        else:
            return self.parse(self.dest, False)

    def parse(self, path, is_file=True):
        global status

        if is_file:
            final_cmd = self._file_cmd.format(path)
        else:
            final_cmd = self._realtime_cmd.format(path)

        try:
            p = Popen(shlex.split(final_cmd), stdout=PIPE, stderr=PIPE, close_fds=True)
            status.set_worker_status(running=True)

            for line in p.stdout:
                self.parse_line(line.decode('utf-8'))

                if self.stop_event.is_set():
                    break
        except KeyboardInterrupt:
            p.terminate()
            raise
        finally:
            status.set_worker_status(running=False)


class CurrentSpeedService(threading.Thread):
    sleep_time = 2

    def __init__(self, stop_event):
        self.stop_event = stop_event = stop_event = stop_event = stop_event
        super().__init__()

    def run(self):
        global status
        self.cache = {}
        self.update_current_speed(status)

        while not self.stop_event.is_set():
            time.sleep(self.sleep_time)
            self.update_current_speed(status)

    def update_current_speed(self, status):
        for key, data in status.data.items():
            if key not in self.cache:
                self.cache[key] = {"in": data["in"], "out": data["out"]}
            else:
                cache = self.cache[key]
                data["speed_in"] = (data['in'] - cache["in"]) / self.sleep_time
                data["speed_out"] = (data['out'] - cache["out"]) / self.sleep_time

                cache["in"] = data['in']
                cache["out"] = data["out"]


class Ui(object):
    def __init__(self):
        self.win = curses.initscr()
        curses.noecho()
        #curses.echo()
        #curses.cbreak()
        self.n = datetime.datetime.now()

    def stop(self):
        curses.endwin()

    def draw_header(self):
        #self.win.move(0, 0)
        #self.win.hline("-", 100)

        # Draw header
        self.win.move(1,0)
        self.win.hline("=", 95)

        self.win.move(2,0)
        self.win.addstr("Host")
        self.win.move(2, 30)
        self.win.addstr("Downloaded")
        self.win.move(2, 45)
        self.win.addstr("Uploaded")
        self.win.move(2, 60)
        self.win.addstr("Download speed")
        self.win.move(2, 80)
        self.win.addstr("Upload speed")

        self.win.move(3, 0)
        self.win.hline("=", 95)

    def draw(self):
        # Draw data
        global status
        sorted_items = sorted(status.data.items(), key=lambda x: x[1]['in'], reverse=True)

        start_row = 3
        self.draw_time_running()

        for i, item in enumerate(sorted_items, 4):
            key, data = item

            self.clear_line(i)
            self.draw_host(i, key)
            self.draw_downloaded_bytes(i, data)
            self.draw_uploaded_bytes(i, data)
            self.draw_download_speed(i, data)
            self.draw_upload_speed(i, data)
            self.win.refresh()


    def ip_to_host(self, ip):
        try:
            return socket.gethostbyaddr(ip)[0]
        except socket.herror:
            return ip

    def clear_line(self, position):
        self.win.move(position, 0)
        self.win.clrtoeol()

    def draw_time_running(self):
        self.clear_line(0)
        self.win.move(0, 2)
        self.win.addstr("Time running: {0}".format(datetime.datetime.now() - self.n))

    def draw_line(self, position):
        self.win.move(position, 0)
        self.win.hline("-", 95)

    def draw_host(self, position, key):
        self.win.move(position, 0)
        self.win.addstr(self.ip_to_host(key))

    def draw_downloaded_bytes(self, position, data):
        self.win.move(position, 30)
        self.win.addstr(humanize_bytes(data["in"]))

    def draw_uploaded_bytes(self, position, data):
        self.win.move(position, 45)
        self.win.addstr(humanize_bytes(data["out"]))

    def draw_download_speed(self, position, data):
        self.win.move(position, 60)
        if "speed_in" in data and data["speed_in"] > 0:
            self.win.addstr(humanize_bytes(data["speed_in"]) + "/s")

    def draw_upload_speed(self, position, data):
        self.win.move(position, 80)
        if "speed_out" in data and data["speed_out"] > 0:
            self.win.addstr(humanize_bytes(data["speed_out"]) + "/s")




if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='pf log analyzer')
    parser.add_argument('-r', action='store', dest="dest")
    parser.add_argument('-t', action='store', dest='sleep', default="1")
    args = parser.parse_args()

    if not args.dest:
        parser.print_help()
    else:
        stop_event = threading.Event()

        stream_service = StreamService(args.dest, stop_event)
        stream_service.start()

        speed_service = CurrentSpeedService(stop_event)
        speed_service.start()

        ui = Ui()
        ui.draw_header()

        try:
            while True:
                try:
                    ui.draw()
                except Exception as e:
                    pass
                
                time.sleep(float(args.sleep))

        except KeyboardInterrupt:
            ui.stop()
            stop_event.set()
