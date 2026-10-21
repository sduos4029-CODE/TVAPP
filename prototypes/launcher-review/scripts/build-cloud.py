#!/usr/bin/env python3
"""Build using this prepared workspace; normal Android Studio users can use gradlew."""
import os, subprocess
from pathlib import Path
from urllib.parse import urlsplit
os.chdir(Path(__file__).resolve().parent.parent)
env = os.environ.copy()
env.update(JAVA_HOME='/workspace/toolchains/jdk-21.0.8+9', ANDROID_HOME='/workspace/toolchains/android-sdk', ANDROID_USER_HOME='/workspace/toolchains/home/.android', GRADLE_USER_HOME='/workspace/toolchains/gradle-home')
command = ['./gradlew', '--no-daemon', '-Duser.home=/workspace/toolchains/home']
proxy = urlsplit(env.get('HTTPS_PROXY', ''))
if proxy.hostname:
    for protocol in ['http', 'https']:
        command += [f'-D{protocol}.proxyHost={proxy.hostname}', f'-D{protocol}.proxyPort={proxy.port or 8080}']
if Path('/etc/ssl/certs/java/cacerts').exists():
    command += ['-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
raise SystemExit(subprocess.run(command + ['assembleDebug', 'assembleDebugAndroidTest', 'lintDebug'], env=env).returncode)
