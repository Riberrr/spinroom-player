# Changelog

## 1.2.0
- Odtwarzacz przeniesiony na górę ekranu: stały pasek (okładka, tytuł, wykonawca, przyciski poprzedni/play/następny, „Na żywo” dla radia), strzałka w górę daje mu fokus i rozwija go nad treścią; dyskretna podpowiedź przycisku otwierającego odtwarzacz.
- Menu boczne: przyciski zawsze wracają do głównego widoku funkcji; u góry ikona aplikacji i nazwa podłączonego odtwarzacza (OK otwiera listę odtwarzaczy); usunięto „Przypięte”, „Odtwarzacz” otwiera pełny ekran.
- Radio: „Moje stacje” pokazują wszystkie stacje radiowe z Music Assistant (A–Z, ładowane porcjami), ulubione na górze.
- Szukaj: po powrocie z playlisty kursor wraca na wybraną pozycję.
- Okładki: przy zmianie utworu poprzednia okładka zostaje, aż nowa się załaduje (bez okładki z inicjałami).

## 1.1.4
- Naprawa: pełnoekranowy odtwarzacz znów pokazuje rozmyte tło z okładki (lub kolor) zgodnie z ustawieniem, zamiast czarnego.

## 1.1.3
- Naprawa: gdy wybrany odtwarzacz jest offline (MA nadało telewizorowi nowe id po aktualizacji), aplikacja sama przełącza się na własny odtwarzacz TV, więc stacje znów grają.

## 1.1.2
- Dolny odtwarzacz: ulubione stacje przewijają się w dół; animacja ładowania w pasku, panelu i pełnym ekranie.
- Ustawienia: przypisywanie przycisków nie zamyka już ekranu, można zmienić przycisk istniejącego skrótu; Wstecz z podekranu wraca na właściwą pozycję.
- Ulubione stacje: mniejsze kafelki (więcej naraz) na Starcie i w Radiu, przewijane w poziomie.

## 1.1.1
- Radio: wykonawca i tytuł granego utworu oraz jego okładka (z metadanych stacji); okładki stacji z własnych źródeł nie są już pomijane.

## 1.1.0
- Nowy wygląd: motywy Grafit/Granat/OLED i akcenty, szklane panele, rozmyte tło z okładki.
- Nowa powłoka: menu boczne z przypiętymi, wysuwany odtwarzacz na dole (przytrzymaj Play/Pause), CH+/CH− przełączają utwór lub ulubioną stację.
- Nowe ekrany: Start, Radio (z szukaniem stacji), Playlisty, Biblioteka z filtrami, Szukaj, odtwarzacz 40:60.
- Ustawienia: nazwa odtwarzacza, Wygląd, przyciski pilota; opcjonalna usługa ułatwień dostępu dla CH± poza aplikacją (włączana ręcznie w Ustawieniach TV).

## 1.0.0
- Wydanie 1.0: ekran „O aplikacji” (wersja, licencje), podpisane wydanie, README ze zrzutami.

## 0.3.1
- Naprawa awarii przy starcie (Hilt ViewModel), reguły R8, pamięć podręczna Coil, baseline profile, lint w CI.

## 0.3.0
- Menu pod przytrzymaniem OK (Odtwórz następny, Do kolejki, Ulubione), wiersz „Ulubione” na Start.
- Kalibracja opóźnienia dźwięku (−500…+500 ms) i sygnał testowy.
- Wygaszacz po 60 s i autostart po włączeniu TV.
- Uwaga: ta wersja wysypuje się przy starcie, użyj 0.3.1.

## 0.2.1
- Naprawa budowania wydania (release).

## 0.2.0
- Ekrany: Teraz odtwarzane, Start, Biblioteka, Szczegóły, Źródła, Szukaj, Ustawienia.
- Usługa odtwarzania w tle i klawisze multimedialne.
- Szkielet: protokół Sendspin, silnik audio, klient API Music Assistant (prace sprzed pierwszego tagu).

