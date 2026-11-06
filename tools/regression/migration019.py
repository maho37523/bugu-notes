"""Compare the actual 6→8 migration chain with fresh generated Room 8 tables."""
from pathlib import Path
import re
import sqlite3

ROOT = Path(__file__).resolve().parents[2]
base = ROOT / 'android/app/src/main/java/com/dspro/android/data/notes'
generated = (ROOT / 'android/app/build/generated/ksp/debug/java/com/dspro/android/data/notes/NotesDatabase_Impl.java').read_text()
ddls = re.findall(r'db.execSQL\("(CREATE [^"]+)"\)', generated)
assert any('temporary_chat_drafts' in ddl for ddl in ddls), 'Build Room 8 first'
fresh, upgraded = sqlite3.connect(':memory:'), sqlite3.connect(':memory:')
for ddl in ddls:
    fresh.execute(ddl)
    if 'temporary_chat_drafts' in ddl or 'index_notes_archivedAt' in ddl:
        continue
    old = ddl
    if 'CREATE TABLE' in ddl and '`notes`' in ddl:
        old = old.replace(', `archivedAt` INTEGER', '')
    if 'CREATE TABLE' in ddl and '`chat_sessions`' in ddl:
        old = old.replace(', `folderId` TEXT', '').replace(', `deletedAt` INTEGER', '')
    if 'CREATE TABLE' in ddl and '`chat_messages`' in ddl:
        old = old.replace(", `sourcesJson` TEXT NOT NULL DEFAULT '[]'", '').replace(", `queriesJson` TEXT NOT NULL DEFAULT '[]'", '')
    upgraded.execute(old)
upgraded.execute("INSERT INTO notes(id,title,content,pinned,createdAt,updatedAt) VALUES('n','标题','第一行\n第二行',0,1,2)")
upgraded.execute("INSERT INTO chat_sessions(id,title,createdAt,updatedAt,archivedAt,titleManuallySet) VALUES(8,'手动对话',1,2,3,1)")
upgraded.execute("INSERT INTO chat_messages(id,sessionId,role,text,status,time,createdAt,contextSnapshot) VALUES(9,8,'user','原始内容','done','12:00',1,'snapshot')")
upgraded.execute("INSERT INTO chat_drafts(sessionId,text,selectedNoteIdsJson,pendingAttachmentsJson,updatedAt) VALUES(8,'待发送','[\"n\"]','[]',4)")
source = (base / 'NotesDatabase.kt').read_text().split('val MIGRATION_6_7', 1)[1].split('fun create', 1)[0]
for sql in re.findall(r'db.execSQL\("([^"]+)"\)', source):
    upgraded.execute(sql)
tables = [row[0] for row in fresh.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")]
for table in tables:
    def columns(db):
        return {row[1]: row[2:6] for row in db.execute(f'PRAGMA table_info(`{table}`)')}
    assert columns(fresh) == columns(upgraded), (table, columns(fresh), columns(upgraded))
    def indexes(db):
        return {row[1]: (row[2], tuple(r[2] for r in db.execute(f'PRAGMA index_info(`{row[1]}`)'))) for row in db.execute(f'PRAGMA index_list(`{table}`)')}
    assert indexes(fresh) == indexes(upgraded), (table,indexes(fresh),indexes(upgraded))
assert upgraded.execute('SELECT title,content,archivedAt FROM notes').fetchone() == ('标题', '第一行\n第二行', None)
assert upgraded.execute('SELECT title,titleManuallySet,archivedAt,folderId,deletedAt FROM chat_sessions').fetchone() == ('手动对话',1,3,None,None)
assert upgraded.execute('SELECT text,contextSnapshot FROM chat_messages').fetchone() == ('原始内容','snapshot')
assert upgraded.execute('SELECT sourcesJson,queriesJson FROM chat_messages').fetchone() == ('[]','[]')
assert upgraded.execute('SELECT text,selectedNoteIdsJson FROM chat_drafts').fetchone() == ('待发送','["n"]')
assert upgraded.execute('SELECT COUNT(*) FROM temporary_chat_drafts').fetchone() == (0,)
print('PASS: actual Room 6→8 migration chain matches fresh Room 8 schema, preserves legacy records and initializes web source metadata')
