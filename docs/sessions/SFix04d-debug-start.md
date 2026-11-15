# SFix04d — Debug: ANR przy starcie na emulatorze, BindException debug+release
**Model:** Sonnet · **Gałąź:** `fix-debug-start` · **Moduły:** `:core:audio` (`SpinroomPlaybackService`), `:app` (start usługi), `:core:sendspin`. Źródło: raport SFix03v (błąd 1) i uwagi SFix03a/b/c.

## Błędy
1. Wariant debug (`io.github.riberrr.spinroom.debug`) ANR-uje przy starcie na emulatorze: „startForegroundService() did not then call startForeground()” w `SpinroomPlaybackService` (emulator pod obciążeniem). Widoczne też na `main`. Wydanie (release) startuje. Sprawdź kolejność: `startForegroundService` → `startForeground` musi nastąpić od razu w `onCreate`/`onStartCommand`, przed ciężką inicjalizacją (Ktor, mDNS, Noise, DataStore).
2. SFix03v widział `BindException: Address already in use` (port Sendspin 8928) przy debug+release naraz. SFix03a (scalone w `main`) powinien to naprawić — najpierw zbuduj debug z aktualnego `main` i sprawdź, czy błąd występuje nadal (stary debug na emulatorze mógł być sprzed poprawki). Jeśli tak: napraw; jeśli nie: opisz w „Wynik” i pomiń.

## Zadania
Najpierw odtwórz na świeżym debugu z `main` (logcat `adb logcat -d -s Spinroom:* AndroidRuntime:E ActivityManager:E | tail -80`). Minimalna poprawka kolejności startu usługi; test JVM jeśli da się wydzielić logikę. Nie zmieniaj protokołu ani audio.

## Środowisko
adb: `C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Emulator emulator-5554: jedna sesja naraz — poproś koordynatora o przydział (teraz mogą go używać SFix04a–c). Instalując debug (`.debug` — inny pakiet) nie ruszasz release, więc użytkownik Tv zostaje zalogowany; release instaluj tylko `-r`. Nie wpisuj danych logowania. Nie uruchamiaj odtwarzania. Wolny emulator: klawisze pojedynczo; po restarcie Wi-Fi: `cmd wifi connect-network AndroidWifi open`.

## Gotowe, gdy
Debug startuje na emulatorze bez ANR (lub opis przyczyny, jeśli to tylko obciążenie emulatora), `bash scripts/check.sh` czyste (lint może paść na `local.properties`), PR otwarty, nie scalony.

## Wynik
_(uzupełnia sesja)_
