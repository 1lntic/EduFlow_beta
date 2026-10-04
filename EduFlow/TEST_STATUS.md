# Status weryfikacji

Wykonano w środowisku przygotowania:

- XML: app/src/main/AndroidManifest.xml — OK
- XML: app/src/main/res/values/styles.xml — OK
- XML: app/src/main/res/drawable/ic_launcher.xml — OK
- JS syntax: app/src/main/assets/inject.js — OK
- JS syntax: app/src/main/assets/navigate.js — OK
- Bash syntax: build.sh — OK
- JS test: cssMount — OK
- JS test: cssUpdate — OK
- JS test: cssUnmount — OK
- JS test: cssRemount — OK
- JS test: navMatch — OK
- JS test: navMissing — OK

Testy JS działają na uproszczonej imitacji DOM, nie na zalogowanej witrynie.

Nie wykonano: kompilacji Gradle/Javy, testów Android/emulator, logowania do konta eduVULCAN, testów selektorów na prywatnym DOM, workflow Actions ani testów pobierania załączników.
Nie dołączono APK.
