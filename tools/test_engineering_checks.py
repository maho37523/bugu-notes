"""Negative checks for the engineering guardrails; only temporary synthetic files."""
import hashlib
import contextlib
import io
import json
from pathlib import Path
import subprocess
import shutil
import tempfile
import unittest
from unittest.mock import patch

import check


class Guardrails(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name).resolve()

    def write(self, name, content):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
        return path

    def test_links_accept_chinese_anchors_and_ignore_code_and_web(self):
        self.write('docs/a.md', '[章节](b.md#r01来源)\n[图](../image.png)\n[外链](https://example.invalid)\n```md\n[示例](absent.md)\n```\n')
        self.write('docs/b.md', '# R01：来源\n')
        self.write('image.png', 'synthetic')
        self.assertEqual(check.check_links(self.root, ['docs/a.md']), 2)

    def test_broken_link_and_anchor_fail(self):
        for content in ['[错误](missing.md)\n', '[错误](a.md#missing)\n']:
            self.write('a.md', content)
            with self.assertRaises(ValueError):
                check.check_links(self.root, ['a.md'])

    def test_outside_link_is_rejected(self):
        self.write('a.md', '[外部](../outside.md)\n')
        with self.assertRaises(ValueError):
            check.check_links(self.root, ['a.md'])

    def test_duplicate_heading_anchors(self):
        self.assertEqual(check.anchors('# 标题\n## 标题\n'), {'标题', '标题-1'})

    def test_modified_and_empty_asset_manifest_fail(self):
        content = self.write('asset.png', 'original')
        digest = hashlib.sha256(content.read_bytes()).hexdigest()
        self.write('design/SHA256SUMS', f'{digest}  asset.png\n')
        self.assertEqual(check.check_assets(self.root), 1)
        content.write_text('modified')
        with self.assertRaises(ValueError):
            check.check_assets(self.root)
        self.write('design/SHA256SUMS', '')
        with self.assertRaises(ValueError):
            check.check_assets(self.root)

    def test_whitespace_and_missing_newline_fail(self):
        for content in ['hello \n', 'hello']:
            self.write('a.md', content)
            with self.assertRaises(ValueError):
                check.check_whitespace(self.root, ['a.md'])

    def test_private_and_generated_paths(self):
        for name in ['.env', 'android/local.properties', 'x.jks', 'user.db-wal',
                     'android/app/build/a', '.engineering/private', '../outside']:
            self.assertTrue(check.forbidden(name), name)
        self.assertFalse(check.forbidden('android/Bugu-Notes-v0.2.1-debug.apk'))
        self.assertFalse(check.forbidden('android/app/src/main/java/ApiConfig.kt'))

    def test_tracked_private_file_is_rejected_before_reading(self):
        with patch('check.git', return_value=b'.env\0'):
            with self.assertRaisesRegex(ValueError, 'Private/generated'):
                check.project_files(self.root)

    def test_missing_offline_distribution_cannot_pass(self):
        for name in ['jdk/bin/java', 'jdk/bin/javac', 'sdk/platforms/android-35/android.jar']:
            self.write(name, 'synthetic')
        (self.root / 'sdk/build-tools/35.0.1').mkdir(parents=True)
        (self.root / '.gradle/caches/modules-2/files-2.1').mkdir(parents=True)
        with patch('check.Path.home', return_value=self.root):
            with patch.dict('os.environ', {'JAVA_HOME': str(self.root / 'jdk'), 'ANDROID_HOME': str(self.root / 'sdk')}, clear=True):
                with self.assertRaisesRegex(ValueError, 'Offline Gradle'):
                    check.environment()

    def test_failed_build_skips_dependent_suites_and_returns_failure(self):
        self.write('a.md', '# Synthetic\n')
        calls = []
        def fake_run(command, **kwargs):
            calls.append(command)
            return subprocess.CompletedProcess(command, 1 if './gradlew' in command else 0)
        def fake_git(root, *args):
            return b'synthetic-source\n' if args == ('rev-parse', 'HEAD') else b''
        with contextlib.redirect_stdout(io.StringIO()), patch('check.quick'), patch('check.environment', return_value={'JAVA_HOME': 'synthetic', 'ANDROID_HOME': 'synthetic'}), patch('check.project_files', return_value=['a.md']), patch('check.git', side_effect=fake_git), patch('check.subprocess.run', side_effect=fake_run):
            self.assertEqual(check.baseline(self.root), 1)
        self.assertEqual(len(calls), 2)
        summary = json.loads(next((self.root / '.engineering/reports').glob('*/summary.json')).read_text())
        self.assertEqual(summary['status'], 'failed')
        self.assertTrue(all(s['status'] == 'not_run' for s in summary['steps'][2:]))
        # This synthetic isolated directory contains only a.md; clean the fixture.
        shutil.rmtree(summary['isolated_directory'])


if __name__ == '__main__':
    unittest.main()
