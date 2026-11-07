set -eu
project_dir=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$project_dir"
java_dir="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}/bin"
cache="$HOME/.gradle/caches/modules-2/files-2.1"
lookup() { find "$cache" -name "$1" | head -1; }
stdlib=$(lookup kotlin-stdlib-2.0.21.jar)
coroutines=$(lookup kotlinx-coroutines-core-jvm-1.9.0.jar)
gson=$(lookup gson-2.10.1.jar)
compiler="$(lookup kotlin-compiler-embeddable-2.0.21.jar):$stdlib:$(lookup trove4j-*.jar):$coroutines:$(lookup annotations-13.0.jar)"
run_dir=$(mktemp -d "${TMPDIR:-/tmp}/bugu-chatgpt-check.XXXXXX")
trap 'rm -rf "$run_dir"' EXIT
mkdir -p "$run_dir/classes"
fixtures="$project_dir/tools/regression/lifecycle019"
"$java_dir/javac" -cp "$gson" -d "$run_dir/classes" "$fixtures/JSONObject.java" "$fixtures/JSONArray.java"
app_classes="$project_dir/android/app/build/tmp/kotlin-classes/debug"
cp_all="$run_dir/classes:$app_classes:$stdlib:$coroutines:$gson"
source_dir=android/app/src/main/java/com/dspro/android/data/chatgpt
"$java_dir/java" -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -Xfriend-paths="$app_classes" -classpath "$cp_all" -d "$run_dir/classes" \
  "$source_dir/ChatGptOAuth.kt" "$source_dir/ChatGptProtocol.kt" "$source_dir/ChatGptAccounts.kt" "$source_dir/ChatGptClient.kt" \
  tools/regression/chatgpt/Test.kt
"$java_dir/java" -cp "$cp_all" com.dspro.android.data.chatgpt.TestKt
