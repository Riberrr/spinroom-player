# S08 — Teraz odtwarzane (ekran-wzorzec)
**Model:** Sonnet · **Zależy od:** S06, S07 · **Gałąź:** `s08-now-playing`

## Cel
Pierwszy pełny ekran i wzorzec, który kopiują wszystkie kolejne ekrany.

## Przeczytaj
`design/screens/6-teraz-odtwarzane.png`, „Wynik” w `S06-ma-api.md` i `S07-ui-shell.md`, README `:core:design` i `:core:data`.

## Zadania
1. `:feature:nowplaying`: `UiState`, `ViewModel` (Hilt) na `PlayerRepository`, ekran: okładka (Coil), rozmyte tło (API 31+, niżej przyciemnienie), tytuł, postęp liczony lokalnie między zdarzeniami, sterowanie (losowo, poprzedni, play/pauza, następny, powtarzanie, ulubione), podgląd kolejki i pełna kolejka.
2. Podłącz mini-odtwarzacz z powłoki do tego samego stanu.
3. Wybór odtwarzacza: własny odtwarzacz TV, a dopóki go nie ma — wybór z listy odtwarzaczy MA (tylko debug).
4. W README modułu sekcja **„Wzorzec ekranu”**: układ plików, nazewnictwo, jak testować VM. Kolejne sesje kopiują ten wzorzec.
5. Testy VM na fixtures.

## Gotowe, gdy
Z TV sterujesz dowolnym odtwarzaczem MA; zrzut porównany z makietą; testy zielone.

## Nie rób
Innych ekranów; zmian w `:core:design` poza drobnymi poprawkami opisanymi w PR.

## Wynik
_(uzupełnia sesja)_
