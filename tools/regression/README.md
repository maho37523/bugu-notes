# Local regression checks

Build the current Android debug target first so the compiled editor and Room 7 schema are available.

## Current v0.1.9 checks

- `python3 tools/regression/migration019.py`: compare the actual 6→7 migration, columns and indexes with fresh generated Room 7 tables; preserve note lines, manual titles, archived conversations, request snapshots and drafts.
- `python3 android/tools/regression/test_room_migration_v6.py`: execute actual 5→6→7 migration and draft deletion SQL.
- `bash tools/regression/lifecycle019/run.sh`: compile the production repositories, entities, DAO SQL, context and backup services against a runtime DAO adapter and real SQLite JDBC. Verify blank-new-note cleanup, scoped temporary drafts, first-send atomic commit and forced SQL rollback, shared folder search, archive/trash restore, orphan draft recovery, attachment path rewriting and v0.1.8 backup compatibility. The adapter executes production SQL annotations and maps production entity constructors; it does not copy repository behavior.
- `bash tools/regression/editor018/run.sh`: exercise the current compiled editor/Markdown renderer, source roundtrip, cross-paragraph selection, undo/redo, live bullets, code blocks, Chinese emphasis, Unicode find/replace, single and repeated newlines, empty quote lines and 4,000 paragraphs.
- `python3 android/tools/regression/test_backup_zip_codec.py`: execute the actual Java ZIP codec against archive roundtrip, malformed paths, duplicate entries and size limits.

The JVM runners use JDK 21 and dependencies from the local Gradle cache. They cover transformation/service logic and SQLite behavior. Android lifecycle, touch performance, actual display layout and live API calls still require device or emulator testing. Fixtures do not ship in the APK.

`backup018/` and `migration018.py` retain the historical v0.1.8 harness; their schema assumptions belong to that release. Use `lifecycle019/` and `migration019.py` for the current version.
