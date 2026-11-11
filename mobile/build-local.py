"""Compila exclusivamente con JDK y Android SDK locales; no usa Gradle ni descargas."""
from pathlib import Path
import os, subprocess, zipfile, shutil, secrets, hashlib, argparse, ipaddress
from urllib.parse import urlparse

parser=argparse.ArgumentParser()
parser.add_argument('--server',default='http://127.0.0.1:14200/')
options=parser.parse_args()
server=urlparse(options.server)
try:
    address=ipaddress.IPv4Address(server.hostname)
    ranges=[ipaddress.ip_network(x) for x in ('10.0.0.0/8','172.16.0.0/12','192.168.0.0/16','127.0.0.1/32')]
    if server.scheme!='http' or not any(address in x for x in ranges) or not server.port or server.username or server.password or server.query or server.fragment or server.path not in ('','/'):
        raise ValueError('Origen inválido')
except (ValueError,TypeError): raise SystemExit('Usa http://IP_PRIVADA:PUERTO/')

root = Path(__file__).resolve().parent.parent
sdk = Path(os.environ.get('ANDROID_HOME', str(Path.home()/'AppData/Local/Android/Sdk')))
jdk = Path(os.environ.get('PUNTOSTOCK_JDK', str(Path.home()/'.jdks/ms-21.0.11')))
tools = sdk/'build-tools/35.0.0'
android = sdk/'platforms/android-35/android.jar'
build = root/'.cache/android-local'
build.mkdir(parents=True, exist_ok=True)
assets=build/'assets';assets.mkdir(exist_ok=True)
(assets/'server.txt').write_text(options.server,encoding='utf-8')
env = dict(os.environ, JAVA_HOME=str(jdk))
def run(*args):
    subprocess.run([str(a) for a in args], check=True, env=env, cwd=build)
for needed in [tools/'aapt.exe',tools/'lib/d8.jar',tools/'lib/apksigner.jar',android,jdk/'bin/javac.exe']:
    if not needed.is_file(): raise SystemExit('Falta herramienta local: '+str(needed))
res=build/'res'
(res/'drawable').mkdir(parents=True,exist_ok=True)
(res/'xml').mkdir(exist_ok=True)
shutil.copyfile(root/'frontend/public/assets/puntostock-icon-192.png',res/'drawable/icon.png')
shutil.copyfile(root/'mobile/res/xml/network_security_config.xml',res/'xml/network_security_config.xml')
unsigned=build/'unsigned.apk'
run(tools/'aapt.exe','package','-f','-M',root/'mobile/AndroidManifest.xml','-S',res,'-A',assets,'-I',android,'-F',unsigned)
classes=build/'classes';classes.mkdir(exist_ok=True)
sources=list((root/'mobile/src').rglob('*.java'))
testClasses=build/'test-classes';testClasses.mkdir(exist_ok=True)
run(jdk/'bin/javac.exe','-encoding','UTF-8','-d',testClasses,root/'mobile/src/sv/puntostock/local/LocalServer.java',root/'mobile/tests/LocalServerTest.java')
run(jdk/'bin/java.exe','-cp',testClasses,'LocalServerTest')
run(jdk/'bin/javac.exe','-encoding','UTF-8','-source','8','-target','8','-bootclasspath',str(android)+os.pathsep+str(tools/'core-lambda-stubs.jar'),'-d',classes,*sources)
run(jdk/'bin/java.exe','-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--min-api','26','--lib',android,'--output',build,*classes.rglob('*.class'))
with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as z: z.write(build/'classes.dex','classes.dex')
aligned=build/'aligned.apk'
run(tools/'zipalign.exe','-f','4',unsigned,aligned)
local=root/'.cache/local';local.mkdir(exist_ok=True)
key=local/'puntostock-apk-pruebas.p12';password=local/'puntostock-apk-pruebas.password'
if not key.exists():
    password.write_text(secrets.token_urlsafe(32),encoding='utf-8')
    run(jdk/'bin/keytool.exe','-genkeypair','-keystore',key,'-storetype','PKCS12','-storepass:file',password,'-alias','puntostock-local','-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=PuntoStock Pruebas Locales')
if not password.exists(): raise SystemExit('Falta la contraseña local de firma. No se reemplazó la clave existente.')
apk=build/'PuntoStock-local.apk'
run(jdk/'bin/java.exe','-jar',tools/'lib/apksigner.jar','sign','--ks',key,'--ks-pass','file:'+str(password),'--out',apk,aligned)
run(jdk/'bin/java.exe','-jar',tools/'lib/apksigner.jar','verify','--verbose',apk)
run(tools/'aapt.exe','dump','badging',apk)
(build/'SHA256.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest()+'  '+apk.name+'\n')
print('APK local:',apk)
