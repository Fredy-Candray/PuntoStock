"""Descarga de APK solo en una interfaz privada, sin servicios externos."""
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import argparse, ipaddress, hashlib
from urllib.parse import urlparse

parser=argparse.ArgumentParser()
parser.add_argument('--host',required=True)
parser.add_argument('--port',type=int,default=14202)
args=parser.parse_args()
address=ipaddress.IPv4Address(args.host)
if not any(address in ipaddress.ip_network(net) for net in ('10.0.0.0/8','172.16.0.0/12','192.168.0.0/16')):
    raise SystemExit('Selecciona la IP de tu red privada.')
apk=Path(__file__).resolve().parent.parent/'.cache/android-local/PuntoStock-local.apk'
if not apk.is_file(): raise SystemExit('Primero compila la APK local.')
class Handler(BaseHTTPRequestHandler):
    def do_HEAD(self): self.send_apk(False)
    def do_GET(self): self.send_apk(True)
    def send_apk(self,body):
        if urlparse(self.path).path!='/PuntoStock-WiFi.apk':
            self.send_error(404);return
        data=apk.read_bytes()
        self.send_response(200)
        self.send_header('Content-Type','application/vnd.android.package-archive')
        self.send_header('Content-Disposition','attachment; filename="PuntoStock-WiFi.apk"')
        self.send_header('Content-Length',str(len(data)))
        self.send_header('Cache-Control','no-store')
        self.send_header('X-Content-Type-Options','nosniff')
        self.end_headers()
        if body: self.wfile.write(data)
print(f'Descarga local: http://{args.host}:{args.port}/PuntoStock-WiFi.apk',flush=True)
print('SHA256: '+hashlib.sha256(apk.read_bytes()).hexdigest(),flush=True)
ThreadingHTTPServer((args.host,args.port),Handler).serve_forever()
