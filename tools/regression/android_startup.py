#!/usr/bin/env python3
"""Install/start APKs only in an explicitly named disposable BuguSmoke AVD.

Not part of the offline JDBC baseline: requires a booted Android emulator.
Never points at a phone. All installed application data must be synthetic.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time


PACKAGE = 'com.dspro.android'
SEED = """
INSERT INTO projects VALUES('A','Synthetic project A','folder',0,'','manual',NULL,1,1);
INSERT INTO projects VALUES('B','Synthetic project B','folder',0,'','manual',NULL,1,1);
INSERT INTO folders VALUES('D','Shared synthetic folder',0,NULL);
INSERT INTO notes VALUES('NG','Synthetic global','body global','D',NULL,0,1,1,NULL,NULL);
INSERT INTO notes VALUES('NA','Synthetic A','body A','D','A',0,1,1,NULL,NULL);
INSERT INTO notes VALUES('NB','Synthetic B','body B','D','B',0,1,1,NULL,NULL);
INSERT INTO temporary_chat_drafts(projectScope,projectId,folderId,text,updatedAt)
VALUES('global',NULL,'D','Synthetic global draft',1);
INSERT INTO temporary_chat_drafts(projectScope,projectId,folderId,text,updatedAt)
VALUES('project:gone','gone','D','Synthetic orphan draft',1);
INSERT INTO chat_sessions(id,projectId,title,createdAt,updatedAt,folderId)
VALUES(1,'A','Synthetic chat',1,1,'D');
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--avd', required=True, help='Exact BuguSmoke* AVD name')
    parser.add_argument('--apk', required=True, type=Path)
    parser.add_argument('--previous', type=Path, help='Schema 8 APK for upgrade test')
    parser.add_argument('--dangling-draft-folder', action='store_true',
                        help='Reproduce legacy temporary drafts pointing to deleted folders')
    parser.add_argument('--report', required=True, type=Path)
    parser.add_argument('--reset-synthetic', action='store_true',
                        help='Delete this disposable AVD application test data first')
    args = parser.parse_args()
    assert re.fullmatch(r'emulator-\d+', args.serial), 'Only emulator serials allowed'
    assert re.fullmatch(r'BuguSmoke[A-Za-z0-9_-]+', args.avd), 'Disposable AVD required'
    report = args.report.resolve()
    report.mkdir(parents=True, exist_ok=False)
    summary = {'status': 'running', 'avd': args.avd, 'serial': args.serial, 'steps': []}

    def adb(*command, input=None, check=True):
        result = subprocess.run([args.adb, '-s', args.serial, *command], input=input,
                                text=True, capture_output=True, timeout=60)
        if check and result.returncode:
            raise RuntimeError(f'{command}: {result.stdout} {result.stderr}')
        return result.stdout.strip()

    def sql(statement):
        return adb('shell', 'run-as', PACKAGE, 'sqlite3', 'databases/dsh_notes.db',
                   input=statement + '\n')

    def launch(label, legacy=False):
        adb('shell', 'am', 'force-stop', PACKAGE)
        adb('logcat', '-c')
        result = adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
        (report / (label + '-launch.txt')).write_text(result + '\n')
        assert 'Status: ok' in result, result
        # am start can report success before a Compose/Room asynchronous crash.
        time.sleep(5)
        pid = adb('shell', 'pidof', PACKAGE, check=False)
        crash = adb('logcat', '-d', '-b', 'crash')
        (report / (label + '-crash.txt')).write_text(crash + '\n')
        assert pid, f'{label}: process exited; inspect crash log'
        assert PACKAGE not in crash, f'{label}: crash log contains application'
        activities = adb('shell', 'dumpsys', 'activity', 'activities')
        assert any(PACKAGE + '/.MainActivity' in line and
                   ('topResumedActivity' in line or 'mResumedActivity' in line)
                   for line in activities.splitlines()), 'MainActivity is not resumed'
        adb('shell', 'uiautomator', 'dump', '/sdcard/bugu-smoke-window.xml')
        xml = adb('shell', 'cat', '/sdcard/bugu-smoke-window.xml')
        (report / (label + '-ui.xml')).write_text(xml + '\n')
        # v0.3 has the same library filters but no v0.4 "资料库" heading.
        assert '笔记' in xml and '对话' in xml, 'Library filters did not render'
        if not legacy:
            assert '资料库' in xml, 'Library heading did not render'
        shot = subprocess.run([args.adb, '-s', args.serial, 'exec-out', 'screencap', '-p'],
                              capture_output=True, check=True, timeout=30)
        (report / (label + '.png')).write_bytes(shot.stdout)
        summary['steps'].append({'name': label, 'status': 'passed', 'pid': pid})
        print('PASS', label, flush=True)

    try:
        assert adb('emu', 'avd', 'name').splitlines()[0] == args.avd, 'AVD name mismatch'
        assert adb('shell', 'getprop', 'ro.kernel.qemu') == '1', 'Not a virtual device'
        assert adb('shell', 'getprop', 'sys.boot_completed') == '1', 'Boot not complete'
        summary['android'] = adb('shell', 'getprop', 'ro.build.version.release')
        summary['api'] = adb('shell', 'getprop', 'ro.build.version.sdk')
        summary['page_size'] = adb('shell', 'getconf', 'PAGE_SIZE')
        summary['apk_sha256'] = hashlib.sha256(args.apk.read_bytes()).hexdigest()
        existing = adb('shell', 'pm', 'path', PACKAGE, check=False)
        if existing:
            assert args.reset_synthetic, 'Application exists; refusing to delete unknown data'
            adb('uninstall', PACKAGE)
        adb('install', str(args.apk.resolve()))
        launch('fresh')
        launch('fresh-restart')
        if args.previous:
            summary['previous_sha256'] = hashlib.sha256(args.previous.read_bytes()).hexdigest()
            # This installation and its data were created by this invocation.
            adb('uninstall', PACKAGE)
            adb('install', str(args.previous.resolve()))
            launch('previous', legacy=True)
            adb('shell', 'am', 'force-stop', PACKAGE)
            assert sql('PRAGMA user_version;') == '8', 'Expected old schema 8'
            sql(SEED)
            (report / 'seed.sql').write_text(SEED)
            if args.dangling_draft_folder:
                sql("UPDATE temporary_chat_drafts SET folderId='deleted-folder';"
                    "INSERT INTO temporary_chat_drafts(projectScope,projectId,folderId,text,"
                    "quoteMessageId,quoteText,selectedNoteIdsJson,pendingAttachmentsJson,updatedAt) "
                    "VALUES('project:A','A','deleted-project-folder','Synthetic stale draft',"
                    "42,'Synthetic quote','[\"NA\"]','[]',123);")
                adb('shell', 'run-as', PACKAGE, 'sh', input=
                    "mkdir -p files/attachments\nprintf %s 'synthetic attachment' > files/attachments/startup-fixture.txt\n")
                attachment = json.dumps([{'id': 'synthetic', 'name': 'fixture.txt',
                                         'mime': 'text/plain', 'size': 20,
                                         'localPath': adb('shell', 'run-as', PACKAGE, 'pwd') +
                                         '/files/attachments/startup-fixture.txt'}])
                sql("UPDATE temporary_chat_drafts SET pendingAttachmentsJson='" +
                    attachment.replace("'", "''") + "' WHERE projectScope='project:A';")
                fields = ('projectScope,projectId,text,quoteMessageId,quoteText,'
                          'selectedNoteIdsJson,pendingAttachmentsJson')
                original_drafts = sql('SELECT ' + fields + ' FROM temporary_chat_drafts ORDER BY projectScope;')
                # Startup hydrates/autosaves the active global draft. Inactive timestamps
                # must stay exact; the JDBC migration test checks every timestamp too.
                inactive_query = ('SELECT ' + fields + ",updatedAt FROM temporary_chat_drafts "
                                  "WHERE projectScope!='global' ORDER BY projectScope;")
                original_inactive = sql(inactive_query)
            adb('install', '-r', str(args.apk.resolve()))
            launch('upgrade')
            assert sql('PRAGMA user_version;') == '9', 'Room did not migrate to schema 9'
            rows = sql('SELECT id,content FROM notes ORDER BY id;')
            assert rows == 'NA|body A\nNB|body B\nNG|body global', rows
            folders = sql('SELECT count(*) FROM folders;')
            assert folders == '3', folders
            invalid = sql('SELECT count(*) FROM notes n JOIN folders f ON n.folderId=f.id '
                          'WHERE n.projectId IS NOT f.projectId;')
            assert invalid == '0', invalid
            if args.dangling_draft_folder:
                assert sql('SELECT ' + fields + ' FROM temporary_chat_drafts ORDER BY projectScope;') == original_drafts
                assert sql(inactive_query) == original_inactive
                assert sql('SELECT count(*) FROM temporary_chat_drafts WHERE folderId IS NOT NULL;') == '0'
                assert adb('shell', 'run-as', PACKAGE, 'cat',
                           'files/attachments/startup-fixture.txt') == 'synthetic attachment'
                (report / 'preserved-drafts.txt').write_text(original_drafts + '\n')
                summary['steps'].append({'name': 'dangling-draft-preservation', 'status': 'passed'})
            (report / 'migrated-notes.txt').write_text(rows + '\n')
            summary['steps'].append({'name': 'migration-data', 'status': 'passed'})
            launch('upgrade-restart')
            launch('upgrade-restart-2')
        summary['status'] = 'passed'
    except Exception as error:
        summary.update(status='failed', error=str(error))
        raise
    finally:
        (report / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
