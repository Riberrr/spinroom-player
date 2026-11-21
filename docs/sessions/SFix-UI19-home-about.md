# SFix-UI19 — Start bez nagłówka, „O aplikacji” 1.2.0
**Gałąź:** `sfix-ui19-home-about`

## Błąd
1. Start: nagłówek „Dzień dobry” i plakietka połączenia zajmują miejsce (stan pokazuje już pastylka odtwarzacza w menu).
2. „O aplikacji”: brak „Co nowego”, nazwa produktu do uporządkowania.

## Zadania
1. `:feature:home` — usunąć nagłówek i plakietkę, treść zaczyna się wyżej, fokus bez zmian.
2. `:feature:settings` — „O aplikacji”: pełna nazwa „Spinroom Player — for Music Assistant”, wersja z packageManager, „Co nowego w 1.2.0” z CHANGELOG.md.

## Gotowe, gdy
check.sh zielony, PR otwarty.

## Wynik
- Start: usunięty `Header` (powitanie + plakietka), lista zaczyna się od `contentPadding top = 8.dp`; pierwszy fokus nadal na pierwszej ulubionej stacji, linki „Wszystkie …” osiągalne D-padem. Usunięte nieużywane stringi `home_greeting/connected/offline`.
- „O aplikacji”: podtytuł zgodny z BRAND.md, wersja, sekcja „Co nowego w 1.2.0” (strings_about.xml), licencje bez zmian.
- Sprawdzone na emulatorze 5556 (Start i O aplikacji). `HomeUiState.isConnected/playerName` zostają (używa ich ViewModel).
- Pułapka: „Co nowego” jest tekstem na sztywno — przy kolejnym wydaniu zaktualizować `about_whats_new*`. Lokalny `local.properties` z `sdk.dir=C:/...` psuje lint (PropertyEscape) — użyć `C\:/...` lub ANDROID_HOME.
