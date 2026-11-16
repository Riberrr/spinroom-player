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
_(uzupełnia sesja)_
