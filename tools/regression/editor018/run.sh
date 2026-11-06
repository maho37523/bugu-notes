set -eu
java_dir="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}/bin"
cache="$HOME/.gradle/caches"
project_dir=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$project_dir"
fixtures="$project_dir/tools/regression/editor018"
root=$(mktemp -d "${TMPDIR:-/tmp}/dspro-editor-check.XXXXXX")
trap 'rm -rf "$root"' EXIT
classes="$PWD/android/app/build/tmp/kotlin-classes/debug"
lookup() { find "$cache/modules-2/files-2.1" -name "$1" | head -1; }
runtime() { find "$cache/8.9/transforms" -name "$1" | head -1; }
compile_jar=$(lookup 'kotlin-compiler-embeddable-2.0.21.jar')
stdlib=$(lookup 'kotlin-stdlib-2.0.21.jar')
trove=$(lookup 'trove4j-*.jar')
coroutines=$(lookup 'kotlinx-coroutines-core-jvm-1.9.0.jar')
annotations=$(lookup 'annotations-13.0.jar')
compose="$(runtime runtime-release-runtime.jar):$(runtime runtime-saveable-release-runtime.jar)"
compose_libs=$(find "$cache/8.9/transforms" -path '*/transformed/*.jar' -print | paste -sd: -)
ui=$(runtime 'ui-release-runtime.jar')
text=$(runtime 'ui-text-release-runtime.jar')
collection=$(lookup 'collection-jvm-*.jar')
android_jar="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platforms/android-35/android.jar"
graphics="$(runtime ui-graphics-release-runtime.jar):$(runtime ui-unit-release-runtime.jar):$(runtime ui-geometry-release-runtime.jar)"
gson=$(lookup "gson-*.jar")
cp_all="$graphics:$gson:$classes:$stdlib:$coroutines:$compose:$compose_libs:$ui:$text:$collection:$android_jar"
compile_cp="$(runtime ui-text-release-api.jar):$(runtime runtime-release-api.jar):$cp_all"
"$java_dir/javac" -cp "$gson" -d "$root" "$fixtures"/android/os/*.java "$fixtures"/android/util/*.java "$fixtures"/org/json/*.java
"$java_dir/java" -cp "$compile_jar:$stdlib:$trove:$coroutines:$annotations" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -classpath "$compile_cp" -Xfriend-paths="$classes" -d "$root" "$fixtures/Editor015.kt" "$fixtures/Markdown018.kt" "$fixtures/ReleaseRegression.kt"
"$java_dir/java" -cp "$root:$cp_all" com.dspro.android.ui.notes.Editor015Kt
"$java_dir/java" -cp "$root:$cp_all" com.dspro.android.ui.notes.Markdown018Kt
"$java_dir/java" -cp "$root:$cp_all" com.dspro.android.ui.ReleaseRegressionKt "$@"
