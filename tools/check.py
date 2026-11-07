#!/usr/bin/env python3
"""Offline engineering checks. No third-party Python packages or app startup."""
from __future__ import annotations

import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
import unicodedata
from urllib.parse import unquote, urlsplit
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED_PARTS = {'.git', '.engineering', '.aws', '.codex', '.agents',
                  'build', '.gradle', '.kotlin', '__pycache__'}
PRIVATE_SUFFIXES = {'.jks', '.keystore', '.p12', '.pem', '.key', '.db', '.sqlite', '.sqlite3'}
SUITES = [
    ('migration', ['python3', 'tools/regression/migration019.py']),
    ('lifecycle', ['bash', 'tools/regression/lifecycle019/run.sh']),
    ('editor', ['bash', 'tools/regression/editor018/run.sh']),
    ('network', ['bash', 'tools/regression/network021/run.sh']),
    ('zip', ['python3', 'android/tools/regression/test_backup_zip_codec.py']),
    ('legacy_migration', ['python3', 'android/tools/regression/test_room_migration_v6.py']),
]


def git(root: Path, *args: str) -> bytes:
    result = subprocess.run(['git', '-C', str(root), *args], capture_output=True)
    if result.returncode:
        raise ValueError('Git command failed: ' + ' '.join(args))
    return result.stdout


def forbidden(name: str) -> bool:
    p = Path(name)
    skill_file = (len(p.parts) in (4, 5)
                  and p.parts[:2] == ('.agents', 'skills')
                  and p.parts[2] in {'intake', 'feature', 'bugfix', 'review', 'research'}
                  and p.parts[3:] in {('SKILL.md',), ('agents', 'openai.yaml')})
    excluded = EXCLUDED_PARTS - {'.agents'} if skill_file else EXCLUDED_PARTS
    return (p.is_absolute() or '..' in p.parts
            or bool(set(p.parts) & excluded)
            or p.name == 'local.properties' or p.name.startswith('.env')
            or p.suffix.lower() in PRIVATE_SUFFIXES
            or any(x in p.name.lower() for x in ('.db-', '.sqlite-', '.sqlite3-')))


def project_files(root: Path) -> list[str]:
    names = git(root, 'ls-files', '--cached', '--others', '--exclude-standard', '-z')
    files = sorted(set(n.decode('utf-8') for n in names.split(b'\0') if n))
    for name in files:
        if forbidden(name):
            raise ValueError('Private/generated path in version scope: ' + name)
        p = root / name
        if p.is_symlink() or not p.resolve().is_relative_to(root.resolve()):
            raise ValueError('Symlink/outside project path: ' + name)
    return [n for n in files if (root / n).is_file()]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def prose(content: str) -> str:
    lines, fence = [], None
    for line in content.splitlines():
        match = re.match(r'^\s*(`{3,}|~{3,})', line)
        if match:
            if fence is None:
                fence = match[1][0]
            elif fence == match[1][0]:
                fence = None
            continue
        if fence is None:
            lines.append(line)
    return '\n'.join(lines)


def anchors(content: str) -> set[str]:
    result, counts = set(), {}
    for line in prose(content).splitlines():
        match = re.match(r'^#{1,6}\s+(.+?)\s*#*$', line)
        if not match:
            continue
        title = match[1].replace('`', '').lower()
        slug = ''.join(c for c in title if c in '-_ ' or unicodedata.category(c)[0] in 'LNM')
        slug = slug.replace(' ', '-')
        count = counts.get(slug, 0)
        counts[slug] = count + 1
        result.add(slug if count == 0 else f'{slug}-{count}')
    return result


def check_links(root: Path, names: list[str]) -> int:
    count, errors = 0, []
    pattern = r'!?\[[^\]\n]*\]\((<[^>]+>|[^\s)]+)(?:\s+"[^"]*")?\)'
    for name in names:
        source = root / name
        for match in re.finditer(pattern, prose(source.read_text(encoding='utf-8'))):
            target = match[1].strip('<>')
            url = urlsplit(target)
            if url.scheme or url.netloc:
                continue
            count += 1
            path = unquote(url.path)
            dest = (source.parent / path).resolve() if path else source.resolve()
            if not dest.is_relative_to(root.resolve()) or not dest.exists():
                errors.append(f'{name}: missing/outside link {target}')
            elif url.fragment and dest.suffix == '.md':
                if unquote(url.fragment) not in anchors(dest.read_text(encoding='utf-8')):
                    errors.append(f'{name}: missing anchor {target}')
    if errors:
        raise ValueError('\n'.join(errors))
    return count


def check_whitespace(root: Path, names: list[str]) -> None:
    for name in names:
        data = (root / name).read_bytes()
        if not data.endswith(b'\n'):
            raise ValueError(f'{name}: missing final newline')
        for number, line in enumerate(data.decode('utf-8').splitlines(), 1):
            if line.rstrip(' \t') != line:
                raise ValueError(f'{name}:{number}: trailing whitespace')


def check_assets(root: Path) -> int:
    count = 0
    for line in (root / 'design/SHA256SUMS').read_text().splitlines():
        expected, name = line.split(maxsplit=1)
        name = name.lstrip('*')
        path = root / name
        if forbidden(name) or path.is_symlink() or not path.resolve().is_relative_to(root.resolve()):
            raise ValueError('Unsafe asset path: ' + name)
        if not path.is_file() or sha256(path) != expected:
            raise ValueError('Asset missing or modified: ' + name)
        count += 1
    if not count:
        raise ValueError('Asset manifest is empty')
    return count


def quick(root: Path) -> None:
    files = project_files(root)
    docs = [n for n in files if n.endswith('.md')]
    links = check_links(root, docs)
    changed = set(n.decode() for n in git(root, 'diff', '--name-only', '-z', 'HEAD').split(b'\0') if n)
    changed.update(n.decode() for n in git(root, 'ls-files', '--others', '--exclude-standard', '-z').split(b'\0') if n)
    check_whitespace(root, [n for n in docs if n in changed])
    for args in [('diff', '--check', 'HEAD'), ('diff', '--cached', '--check')]:
        result = subprocess.run(['git', '-C', str(root), *args], capture_output=True, text=True)
        if result.returncode:
            # Do not print added line contents from a whitespace diagnostic.
            summary = '\n'.join(line for line in result.stdout.splitlines() if not line.startswith('+'))
            raise ValueError('Git whitespace check failed:\n' + summary)
    assets = check_assets(root)
    print(f'PASS quick: {len(docs)} documents, {links} local links, {assets} assets; version paths and changed whitespace checked.', flush=True)


def environment() -> dict[str, str]:
    env = os.environ.copy()
    env.setdefault('JAVA_HOME', '/opt/homebrew/opt/openjdk@21')
    env.setdefault('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk'))
    cache = Path.home() / '.gradle'
    if Path(env.get('GRADLE_USER_HOME', str(cache))).resolve() != cache.resolve():
        raise ValueError('Legacy regression scripts require the existing ~/.gradle cache')
    required = [Path(env['JAVA_HOME']) / 'bin/java', Path(env['JAVA_HOME']) / 'bin/javac',
                Path(env['ANDROID_HOME']) / 'platforms/android-35/android.jar',
                Path(env['ANDROID_HOME']) / 'build-tools/35.0.1',
                cache / 'caches/modules-2/files-2.1']
    for path in required:
        if not path.exists():
            raise ValueError('Environment missing (no installation attempted): ' + str(path))
    if not any((cache / 'wrapper/dists/gradle-8.9-bin').glob('*/gradle-8.9/lib/gradle-launcher-8.9.jar')):
        raise ValueError('Offline Gradle 8.9 distribution is missing; no download attempted')
    return env


def baseline(root: Path) -> int:
    quick(root)
    stamp = datetime.now(ZoneInfo('Asia/Shanghai')).strftime('%Y%m%d-%H%M%S-%f')
    report = root / '.engineering/reports' / stamp
    report.mkdir(parents=True)
    summary = {'source_commit': git(root, 'rev-parse', 'HEAD').decode().strip(),
               'python_version': sys.version.split()[0],
               'report': str(report), 'steps': [], 'status': 'running'}
    try:
        env = environment()
        isolated = Path(tempfile.mkdtemp(prefix='bugu-baseline-'))
        summary.update(isolated_directory=str(isolated), java_home=env['JAVA_HOME'], android_home=env['ANDROID_HOME'])
        names = [n for n in project_files(root) if not n.endswith('.apk')]
        fingerprint = {n: sha256(root / n) for n in names}
        (report / 'inputs.json').write_text(json.dumps(fingerprint, ensure_ascii=False, indent=2) + '\n')
        for name in names:
            target = isolated / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(root / name, target)
            if sha256(target) != fingerprint[name]:
                raise ValueError('Source changed during copy: ' + name)
        commands = [('check_tools', [sys.executable, '-m', 'unittest', 'discover', '-s', 'tools', '-p', 'test_engineering_checks.py']),
                    ('build', ['./gradlew', '--offline', '--no-daemon', ':app:assembleDebug', ':app:lintDebug']), *SUITES]
        build_ok = True
        for name, command in commands:
            if not build_ok:
                summary['steps'].append({'name': name, 'status': 'not_run', 'reason': 'prerequisite failed'})
                continue
            cwd = isolated / 'android' if name == 'build' else isolated
            started = time.monotonic()
            log = report / (name + '.log')
            with log.open('w') as stream:
                result = subprocess.run(command, cwd=cwd, env=env, stdout=stream, stderr=subprocess.STDOUT)
            status = 'passed' if result.returncode == 0 else 'failed'
            summary['steps'].append({'name': name, 'command': command, 'cwd': str(cwd), 'status': status,
                                     'exit': result.returncode, 'seconds': round(time.monotonic() - started, 2), 'log': str(log)})
            print(f'{status.upper()}: {name}; log: {log}', flush=True)
            if name in {'build', 'check_tools'} and result.returncode:
                build_ok = False
        if any(sha256(root / n) != digest for n, digest in fingerprint.items()):
            raise ValueError('Project inputs changed during verification; rerun the affected checks')
        apk = isolated / 'android/app/build/outputs/apk/debug/app-debug.apk'
        if apk.is_file():
            summary['apk_sha256'] = sha256(apk)
        summary['status'] = 'passed' if all(s['status'] == 'passed' for s in summary['steps']) else 'failed'
    except (ValueError, OSError) as error:
        summary.update(status='blocked', reason=str(error))
        print('BLOCKED:', error, flush=True)
    finally:
        (report / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
        print('Report:', report / 'summary.json', flush=True)
    return 0 if summary['status'] == 'passed' else 1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['quick', 'baseline'])
    args = parser.parse_args()
    try:
        if args.mode == 'quick':
            quick(ROOT)
            return 0
        return baseline(ROOT)
    except (ValueError, OSError) as error:
        print('FAILED:', error, file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
