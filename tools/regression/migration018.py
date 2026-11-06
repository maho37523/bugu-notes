from pathlib import Path
import re, sqlite3
base = Path('android/app/src/main/java/com/dspro/android/data/notes')
schema = Path('android/app/build/generated/ksp/debug/java/com/dspro/android/data/notes/NotesDatabase_Impl.java').read_text()
new = sqlite3.connect(':memory:')
old = sqlite3.connect(':memory:')
for ddl in re.findall(r'db.execSQL\("(CREATE [^"]+)"\)', schema):
    new.execute(ddl)
    if 'chat_drafts' not in ddl:
        old.execute(ddl.replace(', `contextSnapshot` TEXT', ''))
old.execute("INSERT INTO chat_sessions(id,title,createdAt,updatedAt,titleManuallySet) VALUES(1,'Manual',10,11,1)")
old.execute("INSERT INTO chat_messages(id,sessionId,role,text,status,time,createdAt) VALUES(2,1,'user','Body','done','10:00',12)")
source = (base/'NotesDatabase.kt').read_text().split('private val MIGRATION_5_6',1)[1].split('fun create',1)[0]
for ddl in re.findall(r'db.execSQL\("([^"]+)"\)', source):
    old.execute(ddl)
assert old.execute('SELECT title,titleManuallySet FROM chat_sessions').fetchone() == ('Manual',1)
assert old.execute('SELECT text,contextSnapshot FROM chat_messages').fetchone() == ('Body',None)
for table in ['chat_messages','chat_drafts','chat_sessions']:
    def columns(db): return {row[1]: row[2:6] for row in db.execute(f'PRAGMA table_info({table})')}
    assert columns(old) == columns(new), (table, columns(old), columns(new))
old.execute("INSERT INTO chat_drafts(sessionId,text,selectedNoteIdsJson,pendingAttachmentsJson,updatedAt) VALUES(1,'Unsent','[\"n1\"]','[]',100)")
assert old.execute('SELECT text,selectedNoteIdsJson FROM chat_drafts').fetchone() == ('Unsent','["n1"]')
print('PASS: real v5→6 migration matches Room schema and preserves custom titles/messages; draft JSON persists')
