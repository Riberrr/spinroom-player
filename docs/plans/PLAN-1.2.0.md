Jesteś koordynatorem projektu Spinroom Player (Android TV, klient Music Assistant). Pracuj autonomicznie według zasad z pamięci "coordinator-workflow" i CLAUDE.md. Nie czytaj kodu, makiet ani PNG — deleguj do sesji. Czytaj tylko docs/STATUS.md (jest nieaktualny, v1.1.0; zaktualizuj go na końcu, max 40 linii) i "Wynik" z plików sesji. Testy tylko na emulatorach (patrz niżej), TV (10.0.0.6) nie ruszaj do mojego polecenia. Max 3 sesje naraz, modele wg tabeli. Konflikty: git merge origin/main, bez rebase/force-push. Docelowo wydanie v1.2.0 (CHANGELOG po polsku, tag na zmergowanym main po zielonym CI); na TV nie instaluj, dopóki nie napiszę.

PIERWSZY KROK: ten prompt został zapisany w pliku C:\Users\lukas\AppData\Local\Temp\claude\156ffce19645bcf7\577efdaf-1808-4284-839f-0e63bca64dab\scratchpad\PLAN-1.2.0-prompt.md. Skopiuj go do repo jako docs/plans/PLAN-1.2.0.md i wyślij w pierwszym, małym PR-ze dokumentacyjnym (razem z odświeżonym STATUS.md na końcu).

Uwagi użytkownika (UI po polsku, kod/commity po angielsku):

SESJA B (Opus high; moduły :app, :core:design; wspólna powłoka, nikt inny ich nie rusza):
1. Dolny panel odtwarzacza (zwinięty i rozwinięty) pokazuje tylko wykonawcę, brakuje tytułu utworu. W głównym odtwarzaczu jest OK. Napraw mapowanie.
2. Zminimalizowany odtwarzacz ma zawsze pokazywać przyciski poprzedni / play-pauza / następny: przygaszone, ale czytelne; ma być jasne, że stąd można zatrzymać granie.
3. Przyciski menu bocznego mają ZAWSZE prowadzić do głównego widoku danej funkcji, nawet gdy jestem już głębiej (np. Start -> wszystkie playlisty -> klik "Start" dziś nic nie robi). Czyść stos nawigacji.
4. Panel odtwarzacza przenosimy z dołu na GÓRĘ ekranu (zamiast chipa podłączonego odtwarzacza po prawej od "Dzień dobry"). Strzałka w górę z dowolnego miejsca przenosi fokus do panelu. Panel jest ZAWSZE widoczny, także przy przewijaniu (stały, ok. 56 dp). Zwinięty: okładka, tytuł (stacja pod spodem dla radia), wykonawca; "Na żywo" tylko dla radia; dla utworów z playlisty cienki pasek postępu + czas; przyciski prev / play-pauza / next (dla radia prev/next przełączają stacje). BEZ pastylki "Teraz odtwarzane". Po najechaniu rozwija się i NAKŁADA na treść (jak dotychczasowy dock: duża okładka, pasek, przyciski, ulubione, lista "Dalej"/stacje). Makieta: ciemny panel, akcent #F5A524, obrys akcentu przy fokusie.
   Podpowiedź klawisza: bardzo dyskretna, po prawej stronie zwiniętego panelu: mała szara prostokątna pastylka o zaokrąglonych rogach, naśladująca przycisk pilota, z treścią taką jak przypisany klawisz z Ustawień (np. Play/Pauza, czerwony, info, 0), a obok niej ikona łapki ze wskazującym palcem. Etykieta wynika z aktualnie skonfigurowanego skrótu "otwórz odtwarzacz".
6. Góra menu bocznego: zamień "dziwną nutkę" na małą ikonę Spinroom Player + obok pastylka z nazwą podłączonego odtwarzacza i zieloną kropką połączenia; klik otwiera listę odtwarzaczy (dziś wybór jest tylko w wersji debug). Przy zwiniętym menu zostaje ikona + kropka.
7. Usuń z menu sekcję "Przypięte". Pozycja "Odtwarzacz" od razu otwiera pełnoekranowy odtwarzacz (Wstecz wraca).

SESJA C (Sonnet medium; :feature:radio + nowe pliki w :core:data):
5. "Wszystkie stacje" ma pokazywać nie tylko ulubione, ale wszystkie stacje radiowe zapisane w Music Assistant + przeglądanie innych źródeł jak dziś. Ulubione na górze; duże listy ładuj porcjami (paging) lub grupuj A-Z; D-pad bez zacięć.

SESJA A (Sonnet medium; :feature:search):
9. Wyszukuję playlisty, wchodzę w playlistę i wracam Wstecz do wyników: kursor ma wrócić na poprzednio wybraną pozycję (ten sam mechanizm co poprawka w Ustawieniach).

SESJA D (Sonnet medium; :feature:nowplaying; start dopiero po scaleniu B):
8. Przy zmianie utworu (zwł. radio) między okładką A i B pojawia się trzecia, systemowa okładka z inicjałami na jednolitym tle. Ma jej nie być: okładka A zostaje, dopóki nowa nie załaduje się (płynne przejście, bez placeholdera); MA przy utworze B sam zwraca domyślną okładkę stacji, więc nie trzeba timera. Awatar z inicjałami tylko gdy stacja nie ma żadnego logo.

EMULATORY: dwa, oba AOSP TV; nazwy odtwarzaczy w MA muszą się różnić. emulator-5554 = API 31 (główny, bliższy TV z API 30), emulator-5556 = API 34 (kontrola nowszego systemu przed wydaniem). Zawsze adb -s <numer>. Blokada per emulator: SPINROOM_EMU_LOCK=C:/Users/lukas/AppData/Local/Temp/spinroom-emu-<numer>.lock bash scripts/emu-lock.sh acquire <SESSION_ID>. Przydział: sesja B = 5554, C i A dzielą 5556 (blokada), D po scaleniu B = 5554. Przed testem sprawdź `wm density` (docelowo 1280x720 dp = gęstość 480 na 4K / 240 na 1080p, jak w makietach); jeśli inna, zapisz to w "Wynik", a nie zmieniaj ustawień emulatora bez zgody. Przed wydaniem v1.2.0 smoke test na obu: start, granie, panel, klawisze.

Kolejność: fala 1 = B, C, A równolegle; fala 2 = D po scaleniu B; potem bookkeeping (STATUS, DECISIONS), CHANGELOG 1.2.0, tag v1.2.0. Do każdej sesji: plik sesji docs/sessions/SFix-UI<n>-<temat>.md z szablonu SFix-template.md, krótki prompt z objawami, zakresem modułów i zakazem ruszania modułów innych sesji. Login testowy MA jest w pamięci "ma-test-server" — nie zapisuj go w plikach ani logach. Zacznij od planu w 5 linijkach i startuj sesje bez dalszych pytań; pytaj mnie tylko o decyzje produktowe.
