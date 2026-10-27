# Spinroom Player — branding

## Nazwa: **Spinroom Player** (zatwierdzona 27.10.2026)

- „Spinroom” = *living room* + kręcąca się płyta; „spin” subtelnie nawiązuje do protokołu Sendspin. „Player” zachowuje ciągłość z roboczą nazwą „MA TV Player”.
- Ma brzmieć naturalnie po angielsku. Na GitHubie tylko 2 puste repo „spinroom” (sprawdzone 27.10.2026). Przed publikacją w Google Play sprawdzić sklep i bazę znaków towarowych EUIPO.
- Odrzucone: „MA TV Player” (może sugerować oficjalną aplikację; Music Assistant należy do Open Home Foundation), „Sendspin…” (marka projektu MA, istnieje SendspinDroid), „Couchtune” (Couchtuner to piracki serwis), „Fermata” (popularny odtwarzacz Androida).

| Element | Wartość |
| --- | --- |
| Nazwa w launcherze TV (`app_name`) | Spinroom |
| Pełna nazwa (O aplikacji, README, Releases) | Spinroom Player — for Music Assistant |
| Podtytuł | *Your Music Assistant, from the living room* |
| Dopisek prawny (README, „O aplikacji”) | *Nieoficjalny klient Music Assistant. Nie jest powiązany z projektem Music Assistant.* |
| Repozytorium | `github.com/Riberrr/spinroom-player` |
| applicationId | `io.github.riberrr.spinroom` (nie da się go zmienić po pierwszym wydaniu) |
| Pakiet Kotlin | `io.github.riberrr.spinroom` |
| Licencja | MIT (wybrana przy zakładaniu repo). Biblioteki AndroidX/Media3 (Apache-2.0) i SendspinDroid (MIT) są z nią zgodne — przy pożyczaniu kodu zachować ich nagłówki z prawami autorskimi |

Nie używamy logo ani nazwy „Music Assistant” jako części marki — tylko w opisie („działa z Music Assistant”).

## Identyfikacja wizualna (z makiet)

| Token | Kolor | Użycie |
| --- | --- | --- |
| `bg` | `#0B0D12` | tło aplikacji |
| `surface` | `#151922` | karty, menu boczne |
| `surfaceHigh` | `#1E2430` | kafelek z fokusem, mini-odtwarzacz |
| `outline` | `#2A3140` | obramowania |
| `textPrimary` | `#F2F4F8` | tytuły |
| `textSecondary` | `#9AA3B2` | podpisy, metadane |
| `accent` | `#F5A524` | **kolor marki**: fokus, postęp, przycisk Odtwórz |
| `success` | `#3DD68C` | „połączono”, na żywo (radio) |
| `info` | `#4FC3F7` | źródła / wtyczki |

- Font: **Inter** (OFL) — dołączony do APK, wagi 400/500/600/700.
- Ikona (koncepcja nr 10 „Etykieta-dom”, zatwierdzona 27.10.2026; źródło: `design/icon-concept.svg`): zaokrąglony kwadrat `#0B0D12`, ciemna płyta `#151922` z dwoma rowkami `#2A3140`, bursztynowa etykieta `#F5A524` w kształcie domu z ciemnym otworem. Dom to „room” i delikatne nawiązanie do logo MA (dom z cyjanowymi słupkami) — bez kopiowania jego kształtu ani koloru `#18BCF2`.
- Do zrobienia w S0: ikona adaptacyjna (tło + pierwszy plan), wersja monochromatyczna (sam dom + kontur płyty), banner 320×180 (ikona po lewej, „Spinroom” w Inter 600 po prawej).
- Wymagane zasoby Android TV: banner 320×180 dp (`android:banner`), ikona adaptacyjna 108 dp, ikona monochromatyczna.
- Ton komunikatów: krótko, po polsku, bez żargonu („Nie widzę serwera Music Assistant w sieci” zamiast „mDNS discovery failed”).
