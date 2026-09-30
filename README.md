# TV Gids voor Google TV

Een live tv-gids met tijdlijn voor de Google TV Streamer, met de Vlaamse
zenders (VRT, VTM, Play) en de Nederlandse publieke omroep (NPO).

## APK bouwen zonder iets te installeren (GitHub)

1. Maak een nieuwe repository op github.com (publiek is het makkelijkst, zie stap 4).
2. Upload de volledige inhoud van deze map, inclusief de verborgen map `.github`.
   Via de website: "Add file" > "Upload files" en sleep alles erin.
3. Open het tabblad **Actions**. De workflow "APK bouwen" start vanzelf en duurt
   een vijftal minuten. Is hij groen, dan staat `TVGids.apk` onder **Releases**.
4. De vaste downloadlink is:
   `https://github.com/<jouw-naam>/<repo>/releases/latest/download/TVGids.apk`

Elke keer dat je iets aanpast en opslaat in de repository wordt er een nieuwe APK gebouwd.

## APK bouwen met Android Studio

Open de map in Android Studio (Ladybug of nieuwer), laat Gradle synchroniseren en
kies Build > Build App Bundle(s) / APK(s) > Build APK(s). Het bestand komt in
`app/build/outputs/apk/`.

## Installeren op de Google TV Streamer

**Via de app Downloader (geen computer nodig)**

1. Installeer "Downloader" (van AFTVnews) uit de Play Store op de Streamer.
2. Ga naar Instellingen > Systeem > Over, en klik 7 keer op "Android TV OS-build"
   om de ontwikkelaarsopties aan te zetten.
3. Instellingen > Apps > Beveiliging en beperkingen > Onbekende bronnen: zet Downloader aan.
4. Open Downloader, typ de downloadlink van hierboven en kies Installeren.

**Via ADB vanaf je computer**

1. Zet de ontwikkelaarsopties aan (zie hierboven) en daarin "Draadloze foutopsporing".
2. Kies "Apparaat koppelen met koppelingscode" en voer uit:
   ```
   adb pair <ip>:<koppelpoort>
   adb connect <ip>:<poort>
   adb install -r TVGids.apk
   ```

De app verschijnt daarna bij je apps op Google TV.

## Waar komt de programmagegevens vandaan?

De gids leest XMLTV-bestanden in. De standaardbronnen staan in
`app/src/main/java/be/tvgids/app/Config.kt` (`EPG_BRONNEN`). Werkt een bron niet
of ontbreekt een zender, open dan in de app het scherm **Status**: daar zie je per
bron of ze gelukt is, hoeveel programma's elke zender heeft en onder welke namen
de overige zenders in de bron staan. Voeg zo'n naam toe aan de `aliassen` van de
juiste zender en bouw opnieuw.

Je kan ook een eigen bron gebruiken, bijvoorbeeld een bestand dat je dagelijks
genereert met https://github.com/iptv-org/epg en ergens online zet.

## Bediening

- Pijltjes: door zenders en tijd bladeren; de tijdlijn schuift mee.
- OK: details van het programma, met een knop om VRT MAX, VTM GO of NPO Start te openen.
- Omhoog vanaf de eerste zender: filter (Alle, Vlaams, Nederlands), Vernieuwen en Status.

De gids ververst zichzelf automatisch als de gegevens ouder zijn dan 6 uur.
