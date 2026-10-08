# TVGids4Streamer: projectoverzicht

*Stand van zaken op 8 oktober 2026, op basis van de bestanden in de projectmap en de git-geschiedenis (13 commits).*

## Wat het is

Een **Android TV-app in Kotlin met Jetpack Compose**. Ze toont een live tv-gids met tijdlijn voor de Google TV Streamer, bedoeld om volledig met de afstandsbediening te gebruiken. De code is compact: vier bronbestanden van samen ongeveer 1.600 regels, plus een testbestand. Alle namen in de code, de commentaren en de README zijn in het Nederlands.

- Repository: `Baconfish9/TVGids4Streamer` op GitHub
- Pakketnaam: `be.tvgids.app`
- Gebruikte versies: minSdk 26, compileSdk en targetSdk 34, AGP 9.4.1, Gradle 9.6.0, JDK 21

## Wat de app doet

- **Zenders:** 13 Vlaamse en 3 Nederlandse.

  | Groep | Zenders | App bij "Kijken" |
  |---|---|---|
  | VRT | VRT 1, VRT Canvas, Ketnet | VRT MAX (`be.vrt.vrtnu`) |
  | DPG Media | VTM, VTM 2, VTM 3, VTM 4, VTM GOLD | VTM GO (`be.vmma.vtm.zenderapp`) |
  | Play | Play, Play Fictie, Play Actie, Play Crime, Play Reality | Play (`be.goplay.app`) |
  | NPO | NPO 1, NPO 2, NPO 3 | NPO Start (`nl.uitzendinggemist`) |

- **Tijdlijn:** ze loopt 30 uur, vanaf ongeveer twee uur geleden. Een rode lijn toont het huidige moment en lopende programma's krijgen een voortgangsbalk. Alle uren staan altijd in Brusselse tijd, ook als het toestel anders is ingesteld.
- **Bediening met de pijltjes:**
  - De tijdlijn schuift mee terwijl je bladert.
  - Bij omhoog en omlaag blijf je rond hetzelfde tijdstip, zodat je niet geleidelijk naar links of rechts afdrijft.
  - Heel korte opvullers (korter dan 5 minuten, zoals Lotto of Keno) worden overgeslagen.
  - Titels van lange programma's blijven zichtbaar aan de linkerrand.
- **Detailvenster:** bij een programma dat nu loopt staat de knop "Kijken in …", bij een voorbij programma "Terugkijken in …". Bij een programma dat nog moet beginnen staat er geen kijkknop.
- **Statusscherm:** toont per bron en per zender hoeveel programma's er gevonden zijn, en onder welke namen de niet-gekoppelde zenders in de bronnen staan, zodat je ze als alias kunt toevoegen.
- **Verversen en positie:**
  - De gids haalt nieuwe gegevens op zodra ze ouder zijn dan 6 uur (controle elke 15 minuten zolang de app in beeld is).
  - Je positie in de gids blijft bewaard bij het verversen en bij een bezoek aan het statusscherm.
  - Kom je terug na meer dan 5 minuten in een andere app, dan springt de gids terug naar "nu".

## Hoe de code in elkaar zit

```
app/src/main/java/be/tvgids/app/
├── Config.kt        Zenders, EPG-bronnen en het vergelijken van zendernamen (~100 regels)
├── Epg.kt           XMLTV-parser, ophalen, opschonen en cache (~420 regels)
├── GidsScherm.kt    De gids, het detailvenster en het statusscherm (~950 regels)
└── MainActivity.kt  Activity en ViewModel, automatisch verversen (~130 regels)
app/src/test/java/be/tvgids/app/KoppelenTest.kt   Unit tests
```

- **`Config.kt`:** de zenderlijst (`ZENDERS`) met naam, land, kleur, aliassen en de app die bij "Kijken" opent, plus de lijst met EPG-bronnen (`EPG_BRONNEN`). Hier staat ook `normaliseer()`: die maakt van namen als "Eén HD", "een.be" en "EEN" allemaal "een", zodat zendernamen uit verschillende bronnen te vergelijken zijn.
- **`Epg.kt`:**
  - Leest XMLTV-bestanden in, gewoon of als `.gz`, met een streaming parser zodat grote bestanden geen geheugenproblemen geven.
  - De vier bronnen van open-epg.com (`belgium1`, `belgium2`, `netherlands1`, `netherlands2`) worden tegelijk opgehaald.
  - Staat een programma in meer dan één bron, dan wint de bron die het eerst in de lijst staat.
  - Overlappende programma's worden rechtgezet.
  - De gids wordt als JSON bewaard (via een tijdelijk bestand, zodat een onderbroken schrijfactie de cache niet beschadigt). Zo start de app snel en toont ze ook zonder internet iets.
  - Levert een verversing niets op, dan blijft de vorige gids staan.
- **`GidsScherm.kt`:** de tijdlijn, de eigen pijltjesnavigatie, het infopaneel, het detailvenster en het statusscherm. Donkere vormgeving met amber als focuskleur.
- **`MainActivity.kt`:** de ViewModel met de status van de gids, het laden van de cache en het automatisch verversen.
- **`KoppelenTest.kt`:** unit tests die controleren dat namen goed genormaliseerd worden, dat twee zenders niet naar dezelfde naam luisteren, dat zendersleutels uniek zijn en dat XMLTV-tijden juist omgezet worden, ook zonder tijdzone.

## Bouwen en installeren

- Elke push naar `main` start de GitHub Actions-workflow **APK bouwen**. Die draait de unit tests, bouwt de APK en publiceert hem als release `latest`. Het versienummer is `1.0.<buildnummer>`.
- Vaste downloadlink: `https://github.com/Baconfish9/TVGids4Streamer/releases/latest/download/TVGids.apk`
- De APK wordt ondertekend met `app/tvgids-debug.keystore`, die bewust in de repository staat, zodat elke nieuwe versie over de vorige installeert. Voor een publicatie in de Play Store is die sleutel niet geschikt.
- Installeren op de Streamer kan met de app Downloader of met ADB (draadloze foutopsporing).
- Een lokale build heeft versie 1 en installeert dus niet over een build van GitHub zonder eerst te de-installeren.

## Aanpassen

- Een zender toevoegen of een alias koppelen: `ZENDERS` in `Config.kt`. De unit tests vangen botsende aliassen op.
- Een nieuwe kijk-app: ook het pakket toevoegen aan `<queries>` in `AndroidManifest.xml`, anders vindt Android 11 en nieuwer de app niet.
- Een eigen EPG-bron (bijvoorbeeld gegenereerd met iptv-org/epg): toevoegen aan `EPG_BRONNEN`.

## Hoe ver het project staat

De eerste commits waren uploads via de GitHub-website. Daarna kwamen in volgorde:

1. Workflow voor het bouwen van de APK
2. Pijltjesnavigatie in de gids, actuele EPG-bronnen, Gradle-upgrade
3. Workflow naar Gradle 9.6.0 en Java 21 (voor AGP 9.4.1)
4. Brusselse tijd, Play-app, meeschuivende titels en logischere kijkknop
5. Positie behouden bij verversen, parallel ophalen en unit tests
6. Gradle-, IDE- en buildbestanden negeren
7. README herschreven als projectpagina, met screenshot
8. Gids terugzetten naar "nu" na lang wegblijven

Het project is een werkende eerste versie die al verfijnd is.
