from pathlib import Path
import xml.etree.ElementTree as ET

root = Path('android/app/src/main')
android = '{http://schemas.android.com/apk/res/android}'
app = ET.parse(root / 'AndroidManifest.xml').getroot().find('application')
assert app is not None
assert app.get(android + 'allowBackup') == 'true'
assert app.get(android + 'fullBackupContent') == '@xml/backup_rules'
assert app.get(android + 'dataExtractionRules') == '@xml/data_extraction_rules'
expected = {'ds_api_secure.xml', 'ds_api_fallback.xml'}

def excluded(node):
    return {item.get('path') for item in node.findall('exclude') if item.get('domain') == 'sharedpref'}

assert expected <= excluded(ET.parse(root / 'res/xml/backup_rules.xml').getroot())
rules = ET.parse(root / 'res/xml/data_extraction_rules.xml').getroot()
for name in ('cloud-backup', 'device-transfer'):
    child = rules.find(name)
    assert child is not None and expected <= excluded(child)
print('PASS: encrypted and fallback preference files excluded from system backup and transfer')
