#!/usr/bin/env python3
"""Runs the framework-only instrumentation in an explicitly disposable emulator."""
import argparse
import json
from pathlib import Path
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--avd', required=True)
    parser.add_argument('--test-apk', required=True, type=Path)
    parser.add_argument('--report', required=True, type=Path)
    args = parser.parse_args()
    assert re.fullmatch(r'emulator-\d+', args.serial)
    assert re.fullmatch(r'BuguSmoke[A-Za-z0-9_-]+', args.avd)

    def adb(*command):
        result = subprocess.run([args.adb, '-s', args.serial, *command], capture_output=True,
                                text=True, timeout=90, check=True)
        return result.stdout.strip()

    assert adb('emu', 'avd', 'name').splitlines()[0] == args.avd
    assert adb('shell', 'getprop', 'ro.kernel.qemu') == '1'
    assert adb('shell', 'getprop', 'sys.boot_completed') == '1'
    args.report.mkdir(parents=True, exist_ok=False)
    adb('install', '-r', str(args.test_apk.resolve()))
    result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'synthetic_avd', args.avd,
                 'com.dspro.android.test/com.dspro.android.testing.ChatGptInstrumentation')
    (args.report / 'instrumentation.txt').write_text(result + '\n')
    passed = 'PASS: Android loopback callback' in result and 'INSTRUMENTATION_CODE: -1' in result
    (args.report / 'summary.json').write_text(json.dumps({'status': 'passed' if passed else 'failed',
        'avd': args.avd, 'android': adb('shell', 'getprop', 'ro.build.version.release')}, indent=2) + '\n')
    assert passed, 'Instrumentation failed; inspect synthetic test report'
    print('PASS: Android local callback, real RS256 and encrypted-store isolation')


if __name__ == '__main__':
    main()
