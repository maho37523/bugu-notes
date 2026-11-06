package com.dspro.android.data.notes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        NoteLinkEntity::class,
        NoteRevisionEntity::class,
        ProjectEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        MemoryEntity::class,
        AttachmentEntity::class,
        ChatDraftEntity::class,
        TemporaryChatDraftEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
    abstract fun projectsDao(): ProjectsDao
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val SCHEMA_VERSION = 8
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN projectId TEXT")
                db.execSQL("UPDATE notes SET projectId = 'inbox'")
                db.execSQL("ALTER TABLE note_revisions ADD COLUMN name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE note_revisions ADD COLUMN version INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE note_revisions ADD COLUMN kind TEXT NOT NULL DEFAULT 'auto'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS projects (" +
                        "id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, icon TEXT NOT NULL, " +
                        "color INTEGER NOT NULL, instructions TEXT NOT NULL, memoryMode TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chat_sessions (" +
                        "id INTEGER NOT NULL PRIMARY KEY, projectId TEXT, title TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, archivedAt INTEGER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chat_messages (" +
                        "id INTEGER NOT NULL PRIMARY KEY, sessionId INTEGER NOT NULL, role TEXT NOT NULL, " +
                        "text TEXT NOT NULL, reasoning TEXT, status TEXT NOT NULL, time TEXT NOT NULL, " +
                        "attachment TEXT, noteRef TEXT, quoteMessageId INTEGER, createdAt INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId ON chat_messages(sessionId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS memories (" +
                        "id TEXT NOT NULL PRIMARY KEY, projectId TEXT, content TEXT NOT NULL, " +
                        "sourceType TEXT NOT NULL, sourceId TEXT, createdAt INTEGER NOT NULL, enabled INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_projectId ON memories(projectId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS attachments (" +
                        "id TEXT NOT NULL PRIMARY KEY, ownerType TEXT NOT NULL, ownerId TEXT NOT NULL, " +
                        "name TEXT NOT NULL, mime TEXT NOT NULL, localPath TEXT NOT NULL, size INTEGER NOT NULL, " +
                        "textContent TEXT, createdAt INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_ownerId ON attachments(ownerId)")
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT OR IGNORE INTO projects (id, name, icon, color, instructions, memoryMode, createdAt, updatedAt) " +
                        "VALUES ('inbox', '收件箱', '📥', 0, '', 'project_only', $now, $now)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN quoteText TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("projects", "folders", "tags")) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN archivedAt INTEGER")
                }
                for (table in listOf("notes", "chat_sessions", "memories")) {
                    db.execSQL("UPDATE $table SET projectId = NULL WHERE projectId = 'inbox'")
                }
                db.execSQL("DELETE FROM projects WHERE id = 'inbox'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_sessions ADD COLUMN titleManuallySet INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN contextSnapshot TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_drafts (sessionId INTEGER NOT NULL PRIMARY KEY, text TEXT NOT NULL, quoteMessageId INTEGER, quoteText TEXT, selectedNoteIdsJson TEXT NOT NULL DEFAULT '[]', pendingAttachmentsJson TEXT NOT NULL DEFAULT '[]', updatedAt INTEGER NOT NULL)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN archivedAt INTEGER")
                db.execSQL("ALTER TABLE chat_sessions ADD COLUMN folderId TEXT")
                db.execSQL("ALTER TABLE chat_sessions ADD COLUMN deletedAt INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_archivedAt ON notes(archivedAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS temporary_chat_drafts (projectScope TEXT NOT NULL PRIMARY KEY, projectId TEXT, folderId TEXT, text TEXT NOT NULL, quoteMessageId INTEGER, quoteText TEXT, selectedNoteIdsJson TEXT NOT NULL DEFAULT '[]', pendingAttachmentsJson TEXT NOT NULL DEFAULT '[]', updatedAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN sourcesJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN queriesJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        fun create(context: Context): NotesDatabase =
            Room.databaseBuilder(context, NotesDatabase::class.java, "dsh_notes.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
    }
}
