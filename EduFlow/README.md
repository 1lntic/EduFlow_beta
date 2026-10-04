# EduFlow 0.1 — nieoficjalny klient eduVULCAN

Projekt Androida w Java: natywny pasek nawigacji + WebView + mobilny CSS.
**To kod źródłowy prototypu, nie skompilowany ani przetestowany APK.**
Nie było dostępu do Android SDK/JDK ani do zalogowanego dziennika w środowisku przygotowania.
Nie ma gwarancji, że logowanie będzie akceptowane przez usługę w WebView.

## Co zawiera projekt

- Natywny interfejs: górny pasek z domeną i odświeżaniem oraz Start / Oceny / Plan / Wiadomości / Więcej.
- Motyw ciemny, jasny i systemowy; wariant kompaktowy.
- Wstrzykiwanie CSS na zatwierdzonych stronach przez evaluateJavascript; ponowne montowanie po zmianach DOM.
- Mobilne pola formularzy, karty, przyciski i style typowych tabel Bootstrap/Kendo.
- Własny CSS: edycja, import do 120 KB i usuwanie.
- Automatyczne wykrywanie widocznych linków/przycisków sekcji po polskich etykietach.
- Ręczne przypisanie adresów zakładek: przytrzymaj zakładkę lub użyj ustawień.
- Frekwencja dostępna z menu Więcej; również obsługuje zapis adresu.
- Sesja obsługiwana przez WebView/CookieManager, bez własnego formularza hasła.
- Przesyłanie plików przez systemowy selektor; pobieranie HTTP(S) przez DownloadManager po potwierdzeniu.
- Wyczyszczenie lokalnej sesji i danych stron po potwierdzeniu.
- Podgląd kierunku wizualnego z wyraźnie oznaczonymi danymi demonstracyjnymi.
- GitHub Actions budujący instalacyjny, podpisany debug APK.
- Skrypty build.ps1 / build.bat / build.sh z pobraniem Gradle i kontrolą SHA256.

## Ważne ograniczenia

1. To klient webowy z nakładką CSS, nie pełna implementacja API w stylu Wulkanowego.
2. Podgląd jest makietą. Jego dashboard, statystyki i karty nie są generowane z prawdziwego konta.
   Po zalogowaniu wyświetlana jest rzeczywista witryna, z własnym CSS i natywną otoczką.
3. Selektory CSS opierają się na strukturach ogólnych (HTML, Bootstrap, Kendo), nie na sprawdzonym DOM po logowaniu.
   Nie wszystkie fragmenty witryny zostaną przestylowane; możliwe są konflikty układu.
   Strony w iframe, zwłaszcza między domenami, nie dostają tej nakładki.
4. Nawigacja szuka widocznych elementów po etykietach. Jeśli menu jest zwinięte, rozwiń je ręcznie.
   Alternatywnie przypisz adres sekcji. Jeśli aplikacja WWW nie zmienia URL, zapis adresu nie wystarczy.
5. Nie ma push, pracy w tle, bazy ocen offline, automatycznych średnich ani własnego parsera danych.
6. CSS nie odblokowuje uprawnień i usług płatnych ani nie zmienia danych w dzienniku.
7. CAPTCHA, zewnętrzny SSO, wymagania przeglądarki i aktualizacje witryny mogą wymagać korekty projektu.
   Nie ma obejść zabezpieczeń. Wyłączenie mobilnego CSS pozwala porównać stronę z oryginałem.
8. Przy pobieraniu obsługiwany jest początkowy adres HTTPS w domenie VULCAN.
   Linki blob:, generowanie plików przez JS i nietypowe przekierowania mogą nie działać.
   DownloadManager zarządza dalszym pobieraniem; sprawdzaj źródło pliku przed potwierdzeniem.
9. Tryb debug pozwala na inspekcję WebView przez komputer z autoryzacją USB. Do dystrybucji zbuduj podpisany release.

## Budowanie APK — GitHub Actions

Ta metoda nie wymaga instalowania Android Studio na własnym komputerze.

1. Utwórz repozytorium GitHub i wgraj ZAWARTOŚĆ katalogu EduFlow (app, .github, build.gradle itd.) do jego głównego katalogu.
   Pamiętaj o ukrytym katalogu .github. Nie wgrywaj samego ZIP-a.
2. Otwórz Actions → Build Android APK → Run workflow.
3. Po udanym buildzie pobierz artefakt EduFlow-debug-APK. Zawiera app-debug.apk.
4. Przenieś APK na telefon z Androidem 10+ i zezwól na instalację z tego konkretnego źródła.

Workflow instaluje JDK 17, Android SDK 35, Build Tools 35.0.0 i Gradle 8.9.
Buduje zadaniem `gradle --no-daemon :app:assembleDebug`.
Actions wymaga konta i dostępnego limitu minut; nie zostało uruchomione podczas przygotowania paczki.
APK jest podpisany kluczem debug generowanym na runnerze. Klucz może zmieniać się między buildami,
więc aktualizacja starej instalacji może wymagać odinstalowania aplikacji (utrata lokalnej sesji).
Aby zachować aktualizacje, użyj własnego stałego klucza release; nigdy nie commituj go do repozytorium.

## Budowanie APK — Android Studio / Windows

Wymagania: Android Studio obsługujące AGP 8.7.3, JDK 17, platforma SDK 35, Build Tools 35.0.0, dostęp do internetu.

1. Rozpakuj ZIP i otwórz katalog EduFlow w Android Studio.
2. W SDK Manager zainstaluj Android SDK Platform 35 oraz Android SDK Build-Tools 35.0.0.
3. W ustawieniach Gradle wybierz JDK 17 oraz lokalną dystrybucję Gradle 8.9.
   Projekt celowo NIE zawiera binarnego gradle-wrapper.jar, ponieważ nie było możliwości pobrania go w środowisku.
   Skrypt build.ps1 pobierze oficjalną dystrybucję Gradle 8.9 do .tools; tę instalację można też wskazać w Studio.
4. Najprościej uruchom build.bat. Java musi być w PATH, SDK w ANDROID_HOME/ANDROID_SDK_ROOT,
   local.properties (sdk.dir), albo domyślnym katalogu %LOCALAPPDATA%\Android\Sdk.
5. Wynik po udanej kompilacji: app\build\outputs\apk\debug\app-debug.apk.

Jeśli chcesz wykorzystać JBR z Android Studio w PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\build.ps1
```

Nowszy JBR może mieć inną wersję Javy; projekt testuj przede wszystkim z JDK 17.
Pierwszy build pobiera Gradle i zależności Android Gradle Plugin z internetu.
Skrypt nie instaluje JDK ani SDK i nie ukrywa błędów kompilatora.

Linux / macOS: zainstaluj JDK 17, SDK oraz curl i unzip, ustaw ANDROID_HOME,
następnie `bash build.sh`. Mac: domyślny SDK często znajduje się w ~/Library/Android/sdk.

## Pierwsze użycie

1. Na pierwszym ekranie wybierz Otwórz eduVULCAN. Adres początkowy: https://eduvulcan.pl/.
2. Jeśli pojawi się publiczna strona, użyj jej przycisku logowania. Nie zgadujemy prywatnych endpointów logowania.
3. Zaloguj się na prawdziwej stronie serwisu. Kod aplikacji nie odczytuje loginów i haseł.
4. Jeśli witryna wymaga cookies między domenami, możesz włączyć je w Więcej po potwierdzeniu.
   Domyślnie są wyłączone; zmiana nie gwarantuje naprawy logowania.
5. Jeśli Oceny/Plan/Wiadomości nie otwierają sekcji, przejdź do niej menu witryny i przytrzymaj właściwą zakładkę.
6. Nie zapisuj jako skrótu adresów z jednorazowym kodem logowania/tokenem.
7. Zrzut ekranu i zanonimizowana struktura DOM są potrzebne do dalszego dopasowania wyglądu.
   Nigdy nie przesyłaj hasła, cookies, tokenów sesji ani danych osobowych uczniów.

## Edytowanie wyglądu

Główny arkusz: app/src/main/assets/mobile.css.
Wstrzykiwanie i obserwacja DOM: app/src/main/assets/inject.js.
Wykrywanie sekcji: app/src/main/assets/navigate.js.
Otoczka Androida, ustawienia i sesja: app/src/main/java/pl/eduflow/client/MainActivity.java.
custom-example.css pokazuje zmianę akcentu i zaokrągleń.

Przykład do wklejenia w edytorze CSS:

```css
html[data-eduflow] {
  --ef-accent: #bba5ff;
  --ef-accent-fill: #7052c6;
  --ef-radius: 22px;
}
```

Podgląd HTML dołączony obok ZIP-a można otworzyć w zwykłej przeglądarce.
W aplikacji wybierz Więcej → Podgląd wyglądu. Start powraca do prawdziwego serwisu.
Makieta nie jest kopią ekranów Wulkanowego lub Ocenowo.

## Bezpieczeństwo i prywatność

- Brak własnej telemetrii, reklamowego SDK i backendu. Sam serwis może korzystać z własnych narzędzi.
- Brak addJavascriptInterface; JavaScript nie otrzymuje mostu do Androida.
- HTTP/mixed content i dostęp WebView do file/content są wyłączone.
- Błędy certyfikatu SSL anulują połączenie; nigdy nie są ignorowane.
- Sesja i ustawienia są w danych aplikacji; backup Androida wyłączony.
- Wewnętrzna nawigacja dopuszcza HTTPS w eduvulcan.pl, vulcan.net.pl, vulcan.edu.pl i ich subdomenach.
  Jest to whitelist rodzin domen, nie gwarancja bezpieczeństwa każdego zasobu tych usług.
- Zewnętrzne odnośniki wymagają potwierdzenia i są otwierane poza WebView; nie przenoszą sesji.
- Własny CSS nie jest sandboxowany. url() / @import mogą wykonywać zapytania do zewnętrznych serwerów.
  Importuj tylko własne/zaufane pliki.
- DownloadManager może używać cookie sesji potrzebnego do pobrania załącznika; nie włączaj nieznanych linków.
- Sprawdź regulamin eduVULCAN przed użyciem. Projekt nie deklaruje zgody firmy VULCAN ani zgodności z jej regulaminem.

## Lista testów przed używaniem

- Build debug na JDK 17 / Gradle 8.9 / SDK 35.
- Android 10 oraz nowszy Android: insets, gesty, obrót i klawiatura.
- Logowanie, CAPTCHA/SSO, wybór ucznia i powrót do dziennika.
- Oceny, plan, wiadomości, frekwencja; rozwinięte i zwinięte menu.
- Ciemny/jasny/systemowy CSS; wyłączenie CSS.
- Własny CSS i import 120 KB; odrzucenie większego pliku.
- Załączniki: wybór pliku, autoryzowane pobieranie, status DownloadManager.
- Brak internetu, błędny certyfikat, link zewnętrzny i wyczyszczenie sesji.
- Brak niezamierzonego udostępniania danych przy debugowaniu.

## Źródła wykorzystane przy projektowaniu

- Android WebView: https://developer.android.com/develop/ui/views/layout/webapps/webview
- eduVULCAN: https://eduvulcan.pl/ i https://dziennik.vulcan.edu.pl/kev/
- Wulkanowy, archiwum projektu (28.06.2024): https://github.com/wulkanowy/wulkanowy
- Historia Ocenowo Dzienniczek: https://ocenowo.com/

Nie kopiowano kodu, ikon ani zasobów wizualnych tych aplikacji.
Źródła potwierdzają charakter produktów i mechanizm WebView; nie potwierdzają działania tego prototypu.
