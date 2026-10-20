#!/usr/bin/env bash
set -euo pipefail
# The prepared cloud snapshot retains these verified toolchains and caches.
export JAVA_HOME='/workspace/toolchains/jdk-21.0.8+9'
export ANDROID_HOME=/workspace/toolchains/android-sdk
export ANDROID_USER_HOME=/workspace/toolchains/home/.android
export GRADLE_USER_HOME=/workspace/toolchains/gradle-home
cd "$(dirname "$0")/.."
./gradlew --offline --no-daemon -Duser.home=/workspace/toolchains/home assembleDebug assembleDebugAndroidTest lintDebug
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose app/build/outputs/apk/debug/app-debug.apk
