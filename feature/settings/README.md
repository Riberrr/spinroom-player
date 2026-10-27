# :feature:settings

Screen "Ustawienia" (`SettingsScreen`, signature unchanged; ViewModel via Hilt). Server address + login (D-pad friendly text fields, focus outline in accent). Only the token is saved; the password is cleared after each attempt. After a successful login it asks the library for the album count to prove the token works. Shows connection status; "Wyloguj" forgets the token.

Not done yet: Sendspin pairing/QR (S01a design), TV player name, discovery via mDNS. Add them as new sections in this screen.
Depends on `:core:design`, `:core:data` (Hilt).
