set -eu
project_dir=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$project_dir"
fixtures="$project_dir/tools/regression/lifecycle019"
run_dir=$(mktemp -d "${TMPDIR:-/tmp}/dspro-lifecycle-check.XXXXXX")
trap 'rm -rf "$run_dir"' EXIT
java_dir="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}/bin"
cache="$HOME/.gradle/caches/modules-2/files-2.1"
lookup() { find "$cache" -name "$1" | head -1; }
stdlib=$(lookup kotlin-stdlib-2.0.21.jar)
coroutines=$(lookup kotlinx-coroutines-core-jvm-1.9.0.jar)
gson=$(lookup gson-2.10.1.jar)
sqlite=$(lookup sqlite-jdbc-3.41.2.2.jar)
compiler="$(lookup kotlin-compiler-embeddable-2.0.21.jar):$stdlib:$(lookup trove4j-*.jar):$coroutines:$(lookup annotations-13.0.jar)"
source_dir=android/app/src/main/java/com/dspro/android/data/notes
python3 - "$run_dir/schema.sql" <<'PY'
from pathlib import Path
import re,sys
source=Path('android/app/build/generated/ksp/debug/java/com/dspro/android/data/notes/NotesDatabase_Impl.java').read_text()
sqls=re.findall(r'db.execSQL\("(CREATE [^"]+)"\)',source)
if not any('temporary_chat_drafts' in s for s in sqls):
 migration=Path('android/app/src/main/java/com/dspro/android/data/notes/NotesDatabase.kt').read_text().split('val MIGRATION_6_7',1)[1].split('fun create',1)[0]
 sqls+=re.findall(r'db.execSQL\("([^"]+)"\)',migration)
Path(sys.argv[1]).write_text('\n'.join(sql+';' for sql in sqls))
PY
mkdir -p "$run_dir/classes"
"$java_dir/javac" -cp "$gson" -d "$run_dir/classes" "$fixtures/JSONObject.java" "$fixtures/JSONArray.java" "$source_dir/BackupZipCodec.java"
"$java_dir/java" -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -java-parameters -no-stdlib -no-reflect -classpath "$run_dir/classes:$stdlib:$coroutines:$gson:$sqlite" -d "$run_dir/classes" "$fixtures"/*.kt "$source_dir/FolderScopeMigration.kt" "$source_dir/NotesEntities.kt" "$source_dir/NotesDao.kt" "$source_dir/ChatDao.kt" "$source_dir/ProjectsDao.kt" "$source_dir/MemoryDao.kt" "$source_dir/NotesRepository.kt" "$source_dir/ChatRepository.kt" "$source_dir/ProjectsRepository.kt" "$source_dir/WorkspaceSearchService.kt" "$source_dir/ContextSnapshotService.kt" "$source_dir/BackupService.kt" android/app/src/main/java/com/dspro/android/data/Models.kt
"$java_dir/java" -cp "$run_dir/classes:$stdlib:$coroutines:$gson:$sqlite" com.dspro.android.data.notes.TestKt "$run_dir/schema.sql"
