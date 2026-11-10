from pathlib import Path
from lxml import etree
from PIL import Image
root=Path(__file__).resolve().parents[1]
manifest=etree.parse(str(root/'app/src/main/AndroidManifest.xml'))
a='{http://schemas.android.com/apk/res/android}'
permissions={p.get(a+'name') for p in manifest.findall('.//uses-permission')}
for p in ('android.permission.READ_PHONE_STATE','android.permission.SEND_SMS','android.permission.SYSTEM_ALERT_WINDOW','android.permission.FOREGROUND_SERVICE_SPECIAL_USE'):
    assert p in permissions,p
services=manifest.findall('.//service')
assert any(s.get(a+'name')=='.CallMonitorService' and s.get(a+'foregroundServiceType')=='specialUse' for s in services)
assert any(s.get(a+'name')=='.OutgoingCallScreeningService' for s in services)
base=root/'app/src/main/java/kr/co/namulsan/smartvisit'
assert 'CallMonitorService.showOnTop' in (base/'PhoneStateReceiver.java').read_text()
assert 'TYPE_APPLICATION_OVERLAY' in (base/'CallMonitorService.java').read_text()
assert 'sendMultimediaMessage' in (base/'DirectMmsSender.java').read_text()
assert 'Intent.ACTION_SEND' not in (base/'DirectMmsSender.java').read_text()
assert 'sendMultimediaMessage' in (base/'DirectMmsSender.java').read_text()
assert Image.open(root/'app/src/main/assets/guide_blank.png').size==(941,1278)
print('V3_STATIC_CHECK_PASS: permissions, service, direct-MMS API, image dimensions')
print('ANDROID_APK_COMPILE_AND_REAL_DEVICE_MMS_TEST: NOT RUN')
