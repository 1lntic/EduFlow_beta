# Nowe zakładki — etap 1 z 2

Dodaje sections-api.js i sections-ui.js. Nie podłącza ich jeszcze do JournalActivity; aktualny APK nie zmieni zachowania po samym tym commicie.
Następny etap musi obsłużyć komendę section, kolejkę żądań, izolację profilu, szyfrowany zapis sekcji i wstrzyknięcie sections-ui.js wyłącznie do lokalnego UI.

## Zweryfikowane wywołania referencyjne

- Osiagniecia?key=... — osiągnięcia.
- Uwagi?key=... — pochwały i uwagi.
- Zebrania?key=... — zebrania (kształt odpowiedzi może zależeć od typu konta).
- FrekwencjaStatystyki?key=...&idPrzedmiot=-1 — tylko statystyki, bez historii pojedynczych nieobecności.
- SprawdzianyZadaniaDomowe?key=...&dataOd=...&dataDo=... — lista sprawdzianów i zadań w zakresie dat, bez szczegółów i załączników.

Źródła: htomasz/vultron (vultron.py, c449cb35) i tumski/eduvulcan-cli (src/fetch.ts). Wykorzystano nazwy zasobów i parametry; adapter napisany od nowa.
Nie zakładamy, że API ma identyczny format na każdym koncie. Zgodność wymaga testu użytkownika.

## Bezpieczeństwo i zakres

Żądania GET są same-origin, HTTPS, bez przekierowań i bez natywnego odczytu hasła/cookies.
Context jest sprawdzany przed KAŻDYM pobraniem. Dane muszą należeć do aktywnego klucza i idDziennik.
Stała lista endpointów; brak dowolnych adresów i metod przesyłanych przez UI.
Błędy HTTP, logowania, schematu i limitów nie są zamieniane na pusty sukces.
Prawidłowa pusta tablica jest sukcesem. Nie są wykonywane zapisy do dziennika.

Pierwszy renderer pokazuje faktyczne nazwy pól i wartości, bez zgadywania ich znaczenia lub obliczania nieznanych procentów.
Nie renderuje HTML z API. Identyfikatory, klucze, tokeny, loginy, PESEL, hasła i adresy URL są filtrowane po nazwach pól.
Duże lub nadmiernie zagnieżdżone odpowiedzi kończą się jawnym błędem limitu, nie cichym ucięciem rekordów.

Realizacja zajęć, Szkoła, szkolny Jadłospis, Podręczniki i Dostęp office pozostają niepodłączone. Referencyjny Jadlospis dotyczy osobnej ścieżki kont przedszkolnych — nie podłączamy go w ciemno do ucznia technikum.
Nie testowano rzeczywistego API konta, Androida ani kompilacji. Main i istniejący adapter planu nie są zmieniane.

Wykonano 14 testów z fikcyjnymi odpowiedziami i uproszczonym DOM oraz node --check dla obu skryptów. To nie potwierdza działania API ani UI na Androidzie.
