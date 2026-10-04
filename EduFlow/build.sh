#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v java >/dev/null || { echo 'Zainstaluj JDK 17.' >&2; exit 1; }
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" && ! -f local.properties ]]; then
  echo 'Ustaw ANDROID_HOME albo local.properties (sdk.dir).' >&2
  exit 1
fi
version=8.9
mkdir -p .tools
if [[ ! -x ".tools/gradle-$version/bin/gradle" ]]; then
  url="https://services.gradle.org/distributions/gradle-$version-bin.zip"
  archive=".tools/gradle-$version-bin.zip"
  curl -fL "$url" -o "$archive"
  expected="$(curl -fsSL "$url.sha256")"
  if command -v sha256sum >/dev/null; then actual="$(sha256sum "$archive" | cut -d' ' -f1)"; else actual="$(shasum -a 256 "$archive" | cut -d' ' -f1)"; fi
  [[ "$actual" == "$expected" ]] || { rm -f "$archive"; echo 'Błędna suma SHA256.' >&2; exit 1; }
  unzip -q "$archive" -d .tools
  rm "$archive"
fi
".tools/gradle-$version/bin/gradle" --no-daemon :app:assembleDebug
test -s app/build/outputs/apk/debug/app-debug.apk
printf '\nAPK: app/build/outputs/apk/debug/app-debug.apk\n'
