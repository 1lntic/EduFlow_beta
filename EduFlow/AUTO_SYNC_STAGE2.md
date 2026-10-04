# EduFlow 0.3 — podłączenie własnego UI i automatu

JournalActivity ładuje lokalny journal-ui.html. To własny interfejs, nie oficjalny dziennik.
Oficjalna witryna jest widoczna tylko przy logowaniu/ponownym uwierzytelnieniu.
Dwa WebView sesji są ukryte pod UI; pracują wyłącznie w aktywnej aplikacji.
Automat: po połączeniu konta, po powrocie do aplikacji i co 5 minut. Pause anuluje zadania; błędy sesji blokują automat do ponownego logowania.
Nie dodano WorkManager ani gwarantowanej pracy przy zamkniętej aplikacji.

Plan i oceny korzystają z istniejącego journal-api.js. Dane są nadal szyfrowane przez JournalStore.
OcenyTablica może zwracać podzbiór ocen. Menu niepodłączonych sekcji nie stanowi implementacji ich danych.
Profil jest sprawdzany przez Context przed pobraniem; automatycznie wybierany jest poprzedni identyfikator albo jedyny aktywny profil. Przy wielu nieznanych profilach pokazujemy wybór.

Skrzynka webowa wymaga swojej sesji na wiadomosci.eduvulcan.pl. Jeśli bezpośrednie otwarcie portalu nie ustanowi sesji, pokaże błąd i wymaga dopasowania bootstrapu, nie obchodzenia uprawnień.
Pobieramy ostatnie 50 wiadomości. Treść tylko na żądanie i tylko dla identyfikatora z bieżącej listy. Otwarcie może zmienić status przeczytania w serwisie.
Nie wysyłamy/usuwamy wiadomości i nie pobieramy załączników.
Wiadomości NIE są zapisywane na dysku w tej wersji: lista i treść są tylko w pamięci, czyszczone po pause/zmianie profilu/logowaniu, aby nie mieszać skrzynek różnych sesji.
Oceny i plan nadal dostępne offline. Usunięcie zapisu i cookies wymaga potwierdzenia w aplikacji.

Nie ma addJavascriptInterface. Komendy z eduflow:// są przyjmowane wyłącznie od głównego dokumentu lokalnego UI; jego sieć jest zablokowana. Dane są renderowane przez textContent.
Sprawdź regulamin serwisu. Nie omijamy blokad API, opłat ani CAPTCHA.
Nie testowano kompilacji Androida, działania UI, bootstrapu skrzynki ani wiadomości na koncie użytkownika. Kod wymaga builda i testu urządzenia.
Build: Actions → Build Android APK → Run workflow → feature/journal-sync. Main pozostaje niezmienione.
