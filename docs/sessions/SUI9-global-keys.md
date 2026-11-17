# SUI9 — CH+/CH− poza aplikacją i karta w rogu
**Model:** Opus · **Gałąź:** `sui9-global-keys` · **Moduły:** `:app` (usługa, manifest), `:core:audio` (sterowanie odtwarzaniem). Zależy od: SUI7, SUI8b.
**Makieta:** `png/10C-poza-aplikacja.png`.

## Zadania
1. **Usługa ułatwień dostępu:** `AccessibilityService` z `canRequestFilterKeyEvents`.
   - Przechwytuje **tylko** klawisze przypisane jako „wszędzie” w SUI8b.
   - Działa tylko wtedy, gdy w Spinroom coś gra albo jest wstrzymane.
   - Wszystkie inne klawisze przepuszcza bez zmian.
   - Nie czyta treści okien (`canRetrieveWindowContent=false`). Opis usługi po polsku wyjaśnia, do czego służy.
2. **Włączanie:** przełącznik „Działaj poza aplikacją” (Ustawienia → Przyciski pilota) prowadzi do systemowych ustawień ułatwień dostępu i pokazuje instrukcję. Stan usługi odświeża się po powrocie do aplikacji.
3. **Karta w rogu (10C):** widoczna ~3 s po przełączeniu. Logo lub okładka, „CH+ · ulubiona stacja 2 z 12”, tytuł, utwór grany teraz. Nie przejmuje fokusu. Wymaga `SYSTEM_ALERT_WINDOW`; gdy zgody nie ma albo telewizor jej nie oferuje, pokaż zwykłe powiadomienie systemowe.
4. **Google Play:** w README `:app` i w „Wynik” opisz ryzyko (deklaracja użycia ułatwień dostępu) i sposób wyłączenia usługi w buildzie do sklepu (np. flaga `BuildConfig` albo flavor), bez psucia reszty aplikacji.
5. Testy JVM: decyzja „przechwyć / przepuść” dla klawiszy.

## Gotowe, gdy
Na emulatorze: usługa włączona, aplikacja w tle (`input keyevent 3`), `adb shell input keyevent 166` przełącza stację, a karta lub powiadomienie są widoczne na zrzucie. PR otwarty.

## Wynik
**Działa (build, lint, JVM):** `GlobalKeysService` + `GlobalKeyPolicy`/`GlobalKeyFilter` (11 testów: przełącznik, nic nie gra, aplikacja na wierzchu, klawisze systemowe/nieprzypisane, zasięg „w aplikacji”, klawisz z przytrzymaniem, akcje wymagające aplikacji, połknięcie powtórzeń i key-up), `GlobalKeyController` (komendy MA bez aktywności), `CornerCard`, przełącznik w Ustawieniach (prowadzi do ustawień dostępności, instrukcja, odświeża się po powrocie). `check.sh`: lint i testy przechodzą.
**NIE sprawdzone na urządzeniu:** scenariusz „Home → `input keyevent 166` → karta” nie został uruchomiony. Emulator-5554 jest przeciążony jak w SUI8b (system_server ~100–180% CPU, aplikacja ginie z `ForegroundServiceDidNotStartInTimeException`, ekran czarny). Na TV 10.0.0.6 zainstalowałem debug (`-r`), ale włączenie usługi przez `adb shell settings put secure enabled_accessibility_services` zostało zablokowane (zmiana ustawień systemowych), więc trzeba ją włączyć ręcznie: Ustawienia TV → Ułatwienia dostępu → „Spinroom — przyciski pilota”, potem w aplikacji włączyć przełącznik, puścić stację/utwór, Home, CH+/CH−.
**Odstępstwo:** karta to `TYPE_ACCESSIBILITY_OVERLAY` (bez `SYSTEM_ALERT_WINDOW`, zgoda niepotrzebna); powiadomienie tylko gdy system odrzuci okno. Postęp na karcie (pasek z makiety) pominięty.
**API:** `PlaybackActivity.active/now` (`:core:audio`, publikuje usługa odtwarzania), `AppForeground.visible`, `GlobalKeysSupport` (`:core:data`), `KeyBindingsRepository.setGlobalEnabled`.
**Google Play:** deklaracja ułatwień dostępu to realne ryzyko odrzucenia. Build bez usługi: `-PglobalKeys=false` (manifest + `BuildConfig.GLOBAL_KEYS` + ukryty przełącznik); opis w README `:app`.
**Pułapki:** klawisz z krótkim i długim skrótem nie jest przechwytywany poza aplikacją; globalnie tylko ⏭/⏮/⏯/stacja N; po ręcznym wyłączeniu usługi w systemie przełącznik pokazuje „wymaga zgody”. Zmieniłem `app/build.gradle.kts` (manifestPlaceholders + `buildConfigField GLOBAL_KEYS`), plik koordynatora.
