set -eu
project_dir=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$project_dir"
java_dir="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}/bin"
cache="$HOME/.gradle/caches/modules-2/files-2.1"
lookup() { find "$cache" -name "$1" | head -1; }
app_classes="$project_dir/android/app/build/tmp/kotlin-classes/debug"
run_dir=$(mktemp -d "${TMPDIR:-/tmp}/dspro-network-check.XXXXXX")
trap 'rm -rf "$run_dir"' EXIT
stdlib=$(lookup kotlin-stdlib-2.0.21.jar)
coroutines=$(lookup kotlinx-coroutines-core-jvm-1.9.0.jar)
gson=$(lookup gson-2.10.1.jar)
annotations=$(lookup annotations-13.0.jar)
trove=$(lookup trove4j-*.jar)
compiler=$(lookup kotlin-compiler-embeddable-2.0.21.jar)
fixtures="$project_dir/tools/regression/lifecycle019"
mkdir -p "$run_dir/classes"
"$java_dir/javac" -cp "$gson" -d "$run_dir/classes" "$fixtures/JSONObject.java" "$fixtures/JSONArray.java"
cp_all="$run_dir/classes:$app_classes:$stdlib:$coroutines:$gson"
"$java_dir/java" -cp "$compiler:$stdlib:$trove:$coroutines:$annotations" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -Xfriend-paths="$app_classes" -classpath "$cp_all" -d "$run_dir/classes" \
  android/app/src/main/java/com/dspro/android/data/ApiProtocol.kt \
  android/app/src/main/java/com/dspro/android/data/DeepSeekClient.kt \
  android/app/src/main/java/com/dspro/android/data/DeepSeekSearchClient.kt \
  tools/regression/network021/ProtocolTest.kt
"$java_dir/java" -cp "$cp_all" com.dspro.android.data.ProtocolTestKt
mkdir -p "$run_dir/store-classes"
"$java_dir/java" -cp "$compiler:$stdlib:$trove:$coroutines:$annotations" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -Xfriend-paths="$app_classes" -classpath "$cp_all" -d "$run_dir/store-classes" \
  tools/regression/network021/store_fixture/FakeAndroid.kt \
  tools/regression/network021/store_fixture/FakeSecurity.kt \
  android/app/src/main/java/com/dspro/android/data/ApiConfig.kt \
  android/app/src/main/java/com/dspro/android/data/ApiProtocol.kt \
  android/app/src/main/java/com/dspro/android/data/chatgpt/AndroidChatGptStore.kt \
  tools/regression/network021/store_fixture/StoreTest.kt
"$java_dir/java" -cp "$run_dir/store-classes:$cp_all" com.dspro.android.data.StoreTestKt
python3 tools/regression/network021/check_backup_rules.py
bash tools/regression/chatgpt/run.sh
