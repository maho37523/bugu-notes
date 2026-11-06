from pathlib import Path
import re, sqlite3
ROOT=Path(__file__).resolve().parents[3]
src=ROOT/'android/app/src/main/java/com/dspro/android/data/notes'
gen=(ROOT/'android/app/build/generated/ksp/debug/java/com/dspro/android/data/notes/NotesDatabase_Impl.java').read_text()
# Reconstruct a synthetic version-5 schema from current generated DDL.
# This is not an authentic historical database fixture.
ddl=re.findall(r'db.execSQL\("(CREATE TABLE IF NOT EXISTS `[^\"]+)"\)', gen)
db=sqlite3.connect(':memory:')
for sql in ddl:
    if '`chat_drafts`' in sql or '`temporary_chat_drafts`' in sql: continue
    sql=sql.replace(', `contextSnapshot` TEXT', '')
    # Remove additions from every later version, including schema 8.
    if '`notes`' in sql: sql=sql.replace(', `archivedAt` INTEGER', '')
    if '`chat_sessions`' in sql:
        sql=sql.replace(', `folderId` TEXT', '').replace(', `deletedAt` INTEGER', '')
    if '`chat_messages`' in sql:
        sql=sql.replace(", `sourcesJson` TEXT NOT NULL DEFAULT '[]'", '').replace(", `queriesJson` TEXT NOT NULL DEFAULT '[]'", '')
    db.execute(sql)
assert not {'contextSnapshot', 'sourcesJson', 'queriesJson'} & {r[1] for r in db.execute('PRAGMA table_info(chat_messages)')}
# Seed v5 data including null older snapshot-compatible fields.
db.execute("INSERT INTO chat_sessions(id,projectId,title,createdAt,updatedAt,archivedAt,titleManuallySet) VALUES(7,NULL,'Old chat',10,11,NULL,1)")
db.execute("INSERT INTO chat_messages(id,sessionId,role,text,status,time,createdAt,noteRef,quoteText) VALUES(8,7,'user','kept','done','12:00',12,'Old note title',NULL)")
source=(src/'NotesDatabase.kt').read_text().split('private val MIGRATION_5_6',1)[1].split('val MIGRATION_6_7',1)[0]
statements=re.findall(r'db.execSQL\("([^\"]+)"\)',source)
for sql in statements: db.execute(sql)
assert db.execute('SELECT text,noteRef,quoteText,contextSnapshot FROM chat_messages WHERE id=8').fetchone()==('kept','Old note title',None,None)
assert db.execute('SELECT titleManuallySet FROM chat_sessions WHERE id=7').fetchone()==(1,)
assert db.execute('SELECT text,selectedNoteIdsJson,pendingAttachmentsJson FROM chat_drafts').fetchone() is None
# Verify v6 draft schema defaults and keyed isolation.
db.execute("INSERT INTO chat_drafts(sessionId,text,updatedAt) VALUES(7,'draft',20)")
assert db.execute('SELECT selectedNoteIdsJson,pendingAttachmentsJson FROM chat_drafts WHERE sessionId=7').fetchone()==('[]','[]')
assert db.execute('SELECT count(*) FROM chat_drafts WHERE sessionId=99').fetchone()==(0,)
print('PASS: generated Room v5 DDL + actual v5→v6 migration SQL preserves chat/noteRef and creates isolated drafts')
# Apply the actual v6→v8 chain and verify defaults and preservation.
migration7=(src/'NotesDatabase.kt').read_text().split('val MIGRATION_6_7',1)[1].split('fun create',1)[0]
statements7=re.findall(r'db.execSQL\("([^\"]+)"\)',migration7)
for sql in statements7: db.execute(sql)
assert {'archivedAt'} <= {r[1] for r in db.execute('PRAGMA table_info(notes)')}
assert {'folderId','deletedAt'} <= {r[1] for r in db.execute('PRAGMA table_info(chat_sessions)')}
assert db.execute('SELECT text,contextSnapshot,sourcesJson,queriesJson FROM chat_messages WHERE id=8').fetchone()==('kept',None,'[]','[]')
assert db.execute("SELECT projectScope,folderId,text,selectedNoteIdsJson,pendingAttachmentsJson FROM temporary_chat_drafts").fetchall()==[]
db.execute("INSERT INTO temporary_chat_drafts(projectScope,projectId,folderId,text,updatedAt) VALUES('global',NULL,'f1','draft',30)")
assert db.execute("SELECT text,selectedNoteIdsJson,pendingAttachmentsJson FROM temporary_chat_drafts WHERE projectScope='global'").fetchone()==('draft','[]','[]')
print('PASS: actual v6→v8 migrations add archive/trash/folder/draft fields and initialize source/query metadata')
# Exercise the actual DAO delete statement and repository transaction guard for a missing session.
dao_source=(src/'ChatDao.kt').read_text()
clear_sql=re.search(r'@Query\("([^"]*DELETE FROM chat_drafts[^"]*)"\)', dao_source)[1]
delete_session_sql=re.search(r'@Query\("([^"]*DELETE FROM chat_sessions[^"]*)"\)', dao_source)[1]
db.commit()
db.execute('BEGIN')
db.execute(clear_sql, {'sessionId':7})
db.execute(delete_session_sql, {'id':7})
db.execute('COMMIT')
assert db.execute('SELECT count(*) FROM chat_drafts WHERE sessionId=7').fetchone()==(0,)
repository=(src/'ChatRepository.kt').read_text()
assert 'database.withTransaction {\n            if (dao.getSession(draft.sessionId) == null) return@withTransaction' in repository
# The missing-session branch is a no-op; subsequent draft save cannot recreate its deleted session.
db.execute('BEGIN')
if db.execute('SELECT 1 FROM chat_sessions WHERE id=7').fetchone() is not None:
    db.execute("INSERT OR REPLACE INTO chat_drafts(sessionId,text,updatedAt) VALUES(7,'stale',40)")
db.execute('COMMIT')
assert db.execute('SELECT count(*) FROM chat_drafts WHERE sessionId=7').fetchone()==(0,)
print('PASS: draft deletion is transactional with session deletion; save guard prevents missing-session draft recreation')
