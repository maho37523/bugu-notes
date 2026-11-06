"""Exercise the exact pure-JDK ZIP codec used by BackupService."""
import io
import os
import shutil
import subprocess
import tempfile
import warnings
import zipfile
from pathlib import Path

ANDROID = Path(__file__).resolve().parents[2]
CODEC = ANDROID / "app/src/main/java/com/dspro/android/data/notes/BackupZipCodec.java"
HARNESS = Path(__file__).with_name("BackupZipCodecHarness.java")
java_home = Path(os.environ.get("JAVA_HOME", "/opt/homebrew/opt/openjdk@21"))
javac = str(java_home / "bin/javac") if (java_home / "bin/javac").exists() else shutil.which("javac")
java = str(java_home / "bin/java") if (java_home / "bin/java").exists() else shutil.which("java")
if not javac or not java:
    raise SystemExit("JDK not found; set JAVA_HOME to a JDK 21 installation")

with tempfile.TemporaryDirectory(prefix="ds018-zip-check-") as tmp:
    classes = Path(tmp) / "classes"
    classes.mkdir()
    subprocess.run([javac, "-d", str(classes), str(CODEC), str(HARNESS)], check=True)
    subprocess.run([java, "-cp", str(classes), "BackupZipCodecHarness", "roundtrip"], check=True)

    def make_zip(entries):
        data = io.BytesIO()
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            with zipfile.ZipFile(data, "w", zipfile.ZIP_DEFLATED) as archive:
                for name, payload in entries:
                    archive.writestr(name, payload)
        return data.getvalue()

    bad_cases = {
        "traversal": [("../outside", b"x"), ("manifest.json", b"{}")],
        "absolute": [("/manifest.json", b"{}")],
        "unexpected": [("manifest.json", b"{}"), ("files/a.bin", b"x")],
        "missing-manifest": [("files/f000000.bin", b"x")],
        "duplicate": [("manifest.json", b"{}"), ("manifest.json", b"{}")],
        "oversized-entry": [("manifest.json", b"{}"), ("files/f000000.bin", b"0" * (25 * 1024 * 1024 + 1))],
    }
    for label, entries in bad_cases.items():
        archive_path = Path(tmp) / f"{label}.zip"
        archive_path.write_bytes(make_zip(entries))
        result = subprocess.run([java, "-cp", str(classes), "BackupZipCodecHarness", "read", str(archive_path)], capture_output=True)
        assert result.returncode != 0, f"codec accepted malformed case: {label}"

print("PASS: BackupZipCodec roundtrip and malformed archive rejection")
