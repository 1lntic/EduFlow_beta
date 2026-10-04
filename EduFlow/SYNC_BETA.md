# EduFlow 0.2 — Sync beta

Build: Actions → Build Android APK → Run workflow → feature/journal-sync.
WWW → zaloguj się → otwórz dziennik ucznia → ↻ → wybierz profil.
Plan: wybór daty ustawia kolejne pobranie; naciśnij ↻.

Zakres: Context, OcenyTablica (niekoniecznie cały semestr), PlanZajec jednego dnia w Europe/Warsaw.
Ręczna synchronizacja, do 16 profili, ostatni dzień na profil. Bez push, pracy w tle, wiadomości i frekwencji.
Zapis JSON: AES-GCM, Android Keystore, AtomicFile. Nie używa Room.
Brak własnego serwera, odczytu hasła i natywnego kopiowania cookies. API same-origin, HTTPS, bez przekierowań.

12 testów na fikcyjnych danych: node tests/journal-api.test.cjs. Nie testowano kompilacji Androida, Keystore ani rzeczywistego konta/API.
Referencja endpointów i pól: tumski/eduvulcan-cli, src/fetch.ts, src/types.ts. Adapter napisany od nowa.
Integracja nieoficjalna; sprawdź regulamin. Nie omija uprawnień ani opłat.
W razie SCHEMA_... przesyłaj tylko kod błędu, bez danych, haseł, cookies i tokenów.

MainActivity pozostaje w Trybie klasycznym; CSS i workflow bez zmian.
Do usunięcia nowego zapisu użyj Wyczyść zapis i sesję w nowym ekranie. Stare menu usuwa sesję, nie ten zapis.
Klucz debug runnera może się zmienić, wymagając odinstalowania starego APK i powodując utratę lokalnych danych.
