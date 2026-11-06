set -eu
project_dir=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$project_dir"
fixture_dir="$project_dir/tools/regression/backup018"
root=$(mktemp -d "${TMPDIR:-/tmp}/dspro-backup-check.XXXXXX")
trap 'rm -rf "$root"' EXIT
python3 - "$root/schema.sql" <<'SCHEMA'
import re,sys
from pathlib import Path
source=Path('android/app/build/generated/ksp/debug/java/com/dspro/android/data/notes/NotesDatabase_Impl.java').read_text()
Path(sys.argv[1]).write_text('\n'.join(sql+';' for sql in re.findall(r'db.execSQL\("(CREATE [^"]+)"\)',source)))
SCHEMA
java_dir="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}/bin"
cache="$HOME/.gradle/caches/modules-2/files-2.1"
lookup() { find "$cache" -name "$1" | head -1; }
stdlib=$(lookup kotlin-stdlib-2.0.21.jar)
coroutines=$(lookup kotlinx-coroutines-core-jvm-1.9.0.jar)
gson=$(lookup gson-2.10.1.jar)
sqlite=$(lookup sqlite-jdbc-3.41.2.2.jar)
compiler="$(lookup kotlin-compiler-embeddable-2.0.21.jar):$stdlib:$(lookup trove4j-*.jar):$coroutines:$(lookup annotations-13.0.jar)"
mkdir -p "$root/classes"
"$java_dir/javac" -cp "$gson" -d "$root/classes" "$fixture_dir/JSONObject.java" "$fixture_dir/JSONArray.java" android/app/src/main/java/com/dspro/android/data/notes/BackupZipCodec.java
source_dir=android/app/src/main/java/com/dspro/android/data/notes
codec_arg=""
if [ -f "$source_dir/BackupZipCodec.kt" ]; then codec_arg="$source_dir/BackupZipCodec.kt"; fi
"$java_dir/java" -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -classpath "$root/classes:$stdlib:$coroutines:$gson:$sqlite" -d "$root/classes" "$fixture_dir"/*.kt "$source_dir/BackupService.kt" "$source_dir/ContextSnapshotService.kt" $codec_arg
"$java_dir/java" -cp "$root/classes:$stdlib:$coroutines:$gson:$sqlite" com.dspro.android.data.notes.TestKt "$root/schema.sql"
