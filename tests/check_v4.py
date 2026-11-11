from pathlib import Path
from lxml import etree
from PIL import Image,ImageDraw,ImageFont
import zipfile
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/kr/co/namulsan/smartvisit'
manifest=etree.parse(str(root/'app/src/main/AndroidManifest.xml'))
ns='{http://schemas.android.com/apk/res/android}'
activities={a.get(ns+'name') for a in manifest.findall('.//activity')}
assert '.PersonalizedImagePreviewActivity' in activities
screen=(base/'CallMonitorService.java').read_text(encoding='utf-8')
assert 'FLAG_SECURE' not in screen,'Screen capture restriction is still present'
assert 'TYPE_APPLICATION_OVERLAY' in screen,'Post call overlay lost'
assert 'FLAG_NOT_FOCUSABLE' in screen
main=(base/'MainActivity.java').read_text(encoding='utf-8')
assert 'Intent.ACTION_OPEN_DOCUMENT' in main
assert 'REQUEST_PICK_PHOTO' in main
assert 'ProfilePhoto.save(this,data.getData())' in main
assert 'ProfilePhoto.delete(this)' in main
assert 'PersonalizedImagePreviewActivity' in main
composer=(base/'MessageImageComposer.java').read_text(encoding='utf-8')
assert 'ProfilePhoto.exists(c)' in composer
assert 'drawPortrait(canvas,portrait)' in composer
assert 'if(ProfilePhoto.exists(c))' in composer
assert '1_000_000L' in composer
assert (base/'DirectMmsSender.java').read_text().count('MessageImageComposer.create(')==1
assert Image.open(root/'app/src/main/assets/guide_blank.png').size==(941,1278)
assert 'versionCode 5' in (root/'app/build.gradle').read_text()
print('V4_STATIC_CHECK_PASS: manifest, screenshot flag removal, picker/storage, photo composition, preview, MMS image pipeline, version')
print('UNTESTED: Android type checking, Gradle APK build, on-device screenshots/photo picker/MMS')
