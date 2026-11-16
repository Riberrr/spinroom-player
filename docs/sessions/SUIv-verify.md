# SUIv — Przegląd całości na emulatorze
**Model:** Sonnet (niski effort) · **Gałąź:** `suiv-verify`, bez zmian w kodzie (zmienia się tylko ten plik). Zależy od: wszystkich sesji SUI.

## Zadania
1. Zainstaluj najnowszy `main` na emulatorze (z blokadą `emu-lock.sh`) i przejdź D-padem:
   1. Start;
   2. Radio, szukanie „jaz”;
   3. Playlisty;
   4. Biblioteka: lista i filtry;
   5. Szukaj: dodanie wyniku do ulubionych;
   6. wysunięcie odtwarzacza z Biblioteki;
   7. pełny odtwarzacz: utwór, potem stacja i `CH+`;
   8. Ustawienia: Wygląd (zmiana akcentu i tła), nazwa odtwarzacza, przyciski pilota.
2. Maksymalnie 10 zrzutów, każdy porównaj z odpowiednim PNG z `design/concepts/png/`.
3. Sprawdź:
   - fokus jest zawsze widoczny;
   - `Wstecz` działa wszędzie;
   - w logcat nie ma crashy;
   - „Jednolite tło” wyłącza rozmycie.

## Gotowe, gdy
„Wynik” zawiera listę różnic i błędów: jeden wiersz = jeden przyszły SFix (ekran, objaw, kroki). PR zawiera tylko ten plik.

## Wynik
_(uzupełnia sesja)_
