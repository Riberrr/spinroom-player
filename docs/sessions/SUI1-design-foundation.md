# SUI1 — Fundament stylu: szkło, rozmyte tło, motywy, wspólne kafle
**Model:** Opus · **Gałąź:** `sui1-design` · **Moduły:** `:core:design`, `:core:data` (tylko nowy plik z preferencjami wyglądu).
**Makiety:** `png/09A-styl-biblioteka.png`, `png/10D-wyglad.png`, `png/03B-start.png` (kafle).

## Zadania
1. **Motywy:** `SpinroomTheme(theme, accent, solidBackground)`. Motywy: Grafit (obecny), Granat, Czerń OLED. Akcent: bursztyn `#F5A524` (domyślny), miętowy `#3DD68C`, błękitny `#4FC3F7`, koralowy `#F07178`, lawendowy `#B39DFF`. Wartości domyślne dają obecny wygląd.
2. **Szkło:** tokeny `glass` (biel ~6%), `glassHigh` (~12%), `glassBorder` oraz `GlassSurface` do kart, menu i paska odtwarzacza.
3. **Tło z treści:** `ContentBackdrop(imageUrl | seedColor)` rysuje na całym ekranie rozmytą, przyciemnioną (~60%) wersję obrazu. Crossfade ~300 ms, z opóźnieniem (debounce) przy szybkim przewijaniu. Rozmycie z **małej bitmapy liczonej raz**, np. transformacja Coil: zmniejszenie do ~32 px + rozmycie, cache po URL. Nie używaj `Modifier.blur` (API 31+, a minSdk to 26). Przy `solidBackground = true` jednolite tło motywu.
4. **Wspólne kafle:** `StationTile` (kwadrat, logo albo inicjały na kolorze wyliczonym z nazwy), `PlaylistMosaic` (2×2 z pierwszych okładek), `ResultRow` (miniatura, tytuł, podtytuł, opcjonalne serce), `FavoriteHeart` (wypełnione/puste). Fokus jak dziś.
5. Preferencje wyglądu jako `Flow` w nowym `AppearanceSettings` w `:core:data` (DataStore): motyw, akcent, tło, wygaszacz (domyślnie **wyłączony**). UI ustawień zrobi SUI8a.
6. Testy JVM: mapowanie preferencji, kolor z nazwy stacji, debounce tła (logika poza Compose).

## Gotowe, gdy
Testy zielone, `check.sh` czyste. Jeden zrzut z emulatora pokazuje rozmyte tło na istniejącym ekranie; jeśli to tylko tymczasowe miejsce, usuń je przed PR. README `:core:design` opisuje nowe komponenty i sposób użycia.

## Wynik
- Działa: motywy Grafit/Granat/OLED + 5 akcentów (`SpinroomTheme(theme, accent, solidBackground)`, domyślnie dawny wygląd), tokeny szkła + `GlassSurface`, `ContentBackdrop` (rozmycie 32 px przez Coil, dim 60%, crossfade 300 ms, debounce 250 ms), `StationTile` (kwadrat), `PlaylistMosaic`, `ResultRow`, `FavoriteHeart`.
- `:core:data`: `AppearanceSettings.appearance: Flow<Appearance(theme, accent, solidBackground, screensaver=false)>` + settery, `AppearanceModule` (Hilt). UI ustawień: SUI8a.
- Pułapka: `SpinroomColors.Accent/Bg/Surface*/Outline` są teraz `@Composable` getterami (wszystko się skompilowało bez zmian w feature'ach).
- Pułapka: stary `StationTile(name, provider, initials, …)` zostaje (Home go używa) — przepiąć w SUI2/3 i usunąć.
- `:app` musi wywołać `SpinroomTheme(...)` z `AppearanceSettings` (nie zrobione — poza zakresem); `ContentBackdrop` w ekranach dodają kolejne sesje.
- Zrzut z emulatora: tymczasowy backdrop na Home działał (usunięty przed PR). Debug build ma `applicationId` z sufiksem `.debug`.
- Testy JVM: `DesignLogicTest` (kolor/inicjały stacji, debounce, blur, fallback id), `AppearanceSettingsTest`. `check.sh`: kompilacja i testy OK; lint pada tylko na moim lokalnym `local.properties` (ścieżka bez `\:`), nie na kodzie.
