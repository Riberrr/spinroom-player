# SFix03e — Biblioteka: stany i pasek liter
**Model:** Haiku · **Gałąź:** `fix-library` · **Moduł:** `:feature:library` (tylko `LibraryScreen.kt`, `LibraryViewModel.kt`, strings). Zależy od SFix03b.

## Błędy (emulator)
1. Bez połączenia z serwerem ekran jest pusty (bez komunikatu), a pasek liter (#, A, D…) i tak się wyświetla i można na niego wejść fokusem.
2. Pasek liter jest widoczny także na pustej karcie („Nic tu jeszcze nie ma”, np. Albumy · 0).
3. Albumy, Wykonawcy i Utwory pokazały pustą listę bez licznika, a po OK „Albumy · 0”. Licznik ma być widoczny od razu po załadowaniu karty, nie dopiero po wybraniu.

## Zadania
1. Bez połączenia: ten sam wzorzec co Start (SFix03d): `EmptyState` „Połącz się z serwerem” albo „Łączę z serwerem…”. Teksty skopiuj dosłownie z `feature/home/.../strings.xml` po scaleniu SFix03d, a jeśli jeszcze go nie ma, użyj tych samych słów.
2. Pasek liter tylko przy `itemCount > 0`.
3. Test JVM `LibraryViewModel` dla stanu offline i pustej karty.
4. Bez emulatora: wystarczą `@Preview` + testy.

## Gotowe, gdy
Testy zielone, `bash scripts/check.sh` czyste, PR otwarty.

## Wynik
_(uzupełnia sesja)_
