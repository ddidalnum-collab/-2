from pathlib import Path
from lxml import etree
from PIL import Image
from lxml import html
import base64,re,subprocess,tempfile
r=Path(__file__).resolve().parents[1]
m=r/'app/src/main/AndroidManifest.xml'
root=etree.parse(str(m))
a='{http://schemas.android.com/apk/res/android}'
perms={x.get(a+'name') for x in root.findall('.//uses-permission')}
assert 'android.permission.READ_PHONE_STATE' in perms
assert 'android.permission.POST_NOTIFICATIONS' in perms
assert not perms.intersection({'android.permission.READ_CALL_LOG','android.permission.READ_SMS','android.permission.SEND_SMS','android.permission.CALL_PHONE'})
services=root.findall('.//service')
assert any(x.get(a+'name')=='.OutgoingCallScreeningService' and x.get(a+'permission')=='android.permission.BIND_SCREENING_SERVICE' and x.find('.//action').get(a+'name')=='android.telecom.CallScreeningService' for x in services)
src=r/'app/src/main/java/kr/co/namulsan/smartvisit'
main=(src/'MainActivity.java').read_text()
screen=(src/'OutgoingCallScreeningService.java').read_text()
state=(src/'PhoneStateReceiver.java').read_text()
assert 'ROLE_CALL_SCREENING' in main
assert 'ACTION_DIAL' not in main and '고객에게 전화 걸기' not in main and 'startCall(' not in main
assert 'DIRECTION_OUTGOING' in screen and 'getHandle()' in screen
assert 'DIRECTION_INCOMING' in screen and 'respondToCall' in screen
assert 'EXTRA_STATE_OFFHOOK' in state and 'EXTRA_STATE_IDLE' in state
assert Image.open(r/'app/src/main/assets/guide_blank.png').size==(941,1278)
page=(r/'demo/SmartVisit_V2_통화감지_시연.html').read_text()
p=html.fromstring(page)
for name in ('company','name','enabled','dupe','simulate','preview','poster','modal','skip'):
    assert p.xpath('//*[@id="'+name+'"]'),name
match=re.search(r"const IMG_SRC='data:image/png;base64,([^']+)'",page)
assert match and base64.b64decode(match.group(1))==(r/'app/src/main/assets/guide_blank.png').read_bytes()
assert not p.xpath('//*[@id="phone"]'), 'Do not prompt for a number in mock call flow'
assert 'android.permission.READ_CONTACTS' in perms, 'optional contacts support'
assert 'getBoolean("enabled", false)' in (src/'Store.java').read_text(), 'first launch must be OFF'
print('V2_STATIC_CHECK_PASS: manifest, role, outgoing screen, no dial UI, state receiver, image, HTML UI and embedded image')
print('APK_BUILD_AND_PHONE_MMS_TEST_UNVERIFIED')
