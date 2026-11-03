PLAYER TRACKER v2 - mod Fabric (klient) dla Minecraft 1.21.1
=============================================================

WYMAGANIA
- Minecraft Java 1.21.1 + Fabric Loader 0.16+ + Fabric API (wersja 1.21.1)
- JDK 21 (do zbudowania moda)

BUDOWANIE PRZEZ GITHUB (bez instalowania Javy i IntelliJ)
1. Załóż repozytorium na github.com (New repository, np. "playertracker").
2. Wgraj całą zawartość folderu PlayerTrackerMod (Add file -> Upload files), razem z
   folderem .github. Jeśli .github się nie wgra, utwórz plik ręcznie:
   Add file -> Create new file -> nazwa: .github/workflows/build.yml -> wklej treść z tego pliku.
3. Zakładka Actions -> workflow "Build mod" ruszy sam po wgraniu (lub Run workflow).
4. Po ~2-4 min (zielony znaczek) kliknij ostatni przebieg -> na dole "Artifacts" ->
   PlayerTracker-mod -> pobierz zip, w środku jest playertracker-1.0.0.jar.
5. Jar + Fabric API do .minecraft/mods.
Wydanie z linkiem: Releases -> Create new release -> tag np. v1.0.0 -> po zbudowaniu jar
dołączy się sam do wydania.

JAK ZBUDOWAĆ LOKALNIE (alternatywa - IntelliJ IDEA Community, darmowe)
1. Zainstaluj JDK 21 (np. Temurin 21) i IntelliJ IDEA.
2. Rozpakuj ten folder. W IntelliJ: File -> Open -> wskaż folder PlayerTrackerMod.
3. Poczekaj aż Gradle pobierze zależności (pasek na dole). Jeśli pyta o JDK, wybierz 21.
   (Brak plików gradlew? Otwórz Terminal w IntelliJ i wpisz:  gradle wrapper --gradle-version 8.8
    - albo skopiuj gradlew, gradlew.bat i folder gradle/ z oficjalnego szablonu Fabric:
    https://github.com/FabricMC/fabric-example-mod )
4. Po prawej panel Gradle -> Tasks -> build -> build (dwuklik).
5. Gotowy plik: build/libs/playertracker-1.0.0.jar  (NIE bierz pliku z "-sources").

JAK ZAINSTALOWAĆ
1. Zainstaluj Fabric Loader dla 1.21.1 (fabricmc.net/use) i uruchom grę raz.
2. Wrzuć do .minecraft/mods: playertracker-1.0.0.jar oraz Fabric API.
3. Wejdź na serwer / do świata.

OBSŁUGA
- PRAWY SHIFT  - otwiera / zamyka panel (zmiana: Opcje -> Sterowanie -> Player Tracker)
- H            - śledź / przestań śledzić gracza, na którego celujesz
- (nieprzypisany) - szybkie włącz/wyłącz tracery
- Kółko myszy przewija listy.

PANEL - 4 ZAKŁADKI
- Gracze: wyszukiwarka po nicku (podgląd z głową i statusem, Enter = dodaj), lista obserwowanych
          z próbnikiem koloru (klik = zmiana), przełącznikiem linii ON/OFF i usuwaniem.
- Online: lista wszystkich graczy z serwera z filtrem, ping, dystans i przyciskiem Dodaj/Usuń.
- Tracery: styl (linia+obrys / linia / obrys), start (celownik / dół ekranu), grubość,
           przezroczystość, maks. dystans, przez ściany, tęczowe kolory.
- Ustawienia: HUD (róg, HP, dystans), alerty, reset ustawień, wyczyść listę.

UWAGA: serwer wysyła pozycje graczy tylko w zasięgu renderowania - poza nim nie ma linii.
Config: .minecraft/config/playertracker.json
