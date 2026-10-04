$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Brak Javy. Zainstaluj JDK 17 i dodaj bin do PATH albo użyj JBR z Android Studio.'
}
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT -and -not (Test-Path 'local.properties')) {
    $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $sdk) { $env:ANDROID_HOME = $sdk }
    else { throw 'Brak Android SDK. Otwórz README.md i skonfiguruj Android Studio.' }
}
$version = '8.9'
$tools = Join-Path $PSScriptRoot '.tools'
$gradle = Join-Path $tools "gradle-$version\bin\gradle.bat"
if (-not (Test-Path $gradle)) {
    New-Item -ItemType Directory -Force -Path $tools | Out-Null
    $zip = Join-Path $tools "gradle-$version-bin.zip"
    $url = "https://services.gradle.org/distributions/gradle-$version-bin.zip"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    $expected = (Invoke-WebRequest -Uri "$url.sha256" -UseBasicParsing).Content.Trim()
    $actual = (Get-FileHash -Path $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected.ToLowerInvariant()) { Remove-Item $zip; throw 'Błędna suma SHA256 Gradle.' }
    Expand-Archive -Path $zip -DestinationPath $tools -Force
    Remove-Item $zip
}
& $gradle --no-daemon :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Kompilacja nie powiodła się (kod $LASTEXITCODE)." }
$apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { throw 'Brak pliku APK mimo zakończenia Gradle.' }
Write-Host "APK utworzony: $apk"
