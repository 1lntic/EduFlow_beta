# Etap 1 — zasoby własnego UI i adapter wiadomości

Ten commit przygotowuje zasoby. Nie zmienia jeszcze launchera ani zachowania działającej aplikacji.
Etap 2 musi podłączyć journal-ui.html, messages-api.js oraz AutoSyncPolicy w JournalActivity.

UI jest lokalnym interfejsem EduFlow, nie osadzoną stroną dziennika. Dane API są wyświetlane przez textContent, nie innerHTML.
CSP blokuje zewnętrzne skrypty, ramki, połączenia i formularze w UI. Własne stałe SVG służą jako ikony.
Widoczna strona VULCAN służy do logowania; dwa osobne WebView sesji będą dostarczały dane ucznia i wiadomości.

Automatyczne odświeżanie dotyczy używania aplikacji: po uwierzytelnieniu, po powrocie oraz co 5 minut.
AutoSyncPolicy blokuje nakładanie zadań, zatrzymuje pracę po pause, stosuje backoff 1/2/4/8/15 minut i blokuje automatyczne ponawianie błędów sesji do ponownego zalogowania.
To NIE jest WorkManager ani gwarantowana synchronizacja przy zamkniętej aplikacji. Taki tryb wymaga osobnego klienta HTTP i przetestowanego uwierzytelniania.

messages-api.js odczytuje webowe Odebrane (ostatnie 50) i WiadomoscSzczegoly na wiadomosci.eduvulcan.pl.
Nagłówki i wartości referencyjne sprawdzono w tumski/eduvulcan-cli; zgodność z konkretnym kontem wymaga testu.
Nie pobieramy automatycznie treści wszystkich wiadomości. Szczegóły tylko po dotknięciu wiadomości; serwis może oznaczyć ją jako przeczytaną.
Brak wysyłania, usuwania i pobierania załączników w tym etapie. Nie obchodzimy blokad API/401/403.
Skrzynka dotyczy konta, nie ucznia wybranego przez dopasowanie nazwiska. Do podłączenia zapisu potrzebna jest izolacja konta/skrzynki.

Menu odzwierciedla screen użytkownika. Sekcje bez adapterów są wyraźnie oznaczone jako niepodłączone — nie są działającymi funkcjami.
Sprawdzono składnię JS; testy adaptera używają fikcyjnych odpowiedzi. Nie testowano UI/Androida/logowania/wiadomości na koncie.

Wykonano 10 testów adaptera wiadomości na fikcyjnych odpowiedziach; oba skrypty przeszły node --check. AutoSyncPolicy nie zostało skompilowane ani uruchomione na Androidzie.
