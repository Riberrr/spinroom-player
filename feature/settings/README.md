# :feature:settings

Screen "Ustawienia" (`SettingsScreen`, signature unchanged; ViewModel via Hilt). Server address + login (D-pad friendly text fields, focus outline in accent). Only the token is saved; the password is cleared after each attempt. After a successful login it asks the library for the album count to prove the token works. Shows connection status; "Wyloguj" forgets the token.

"Opóźnienie dźwięku" (S14c, `latency/`): sub-screen opened from a button at the bottom (in-screen state + BackHandler, no route). ←/→ on the slider = ±10 ms, saved in `LatencyStore` (−500…+500) and applied live by the playback service. "Graj sygnał testowy" serves a generated beep WAV from the TV (`TestSignalServer`, random port, `/spinroom-sync.wav`) and asks MA to `play_media` it (option `play`) on the TV's group queue (`CalibrationGroups.find`: synced leader, group player, or TV as leader); leaving the screen stops it. Hidden when the TV is not grouped.

Not done yet: Sendspin pairing/QR (S01a design), TV player name, discovery via mDNS. Add them as new sections in this screen.
Depends on `:core:design`, `:core:data` (Hilt).
Sub-screen "O aplikacji" (S16, `AboutScreen`): wersja, notka o nieoficjalnym kliencie, licencje. Opened from a button at the bottom, same in-screen pattern as latency.
