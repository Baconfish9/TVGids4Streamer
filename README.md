# TV Gids voor Google TV

[![APK bouwen](https://github.com/Baconfish9/TVGids4Streamer/actions/workflows/apk.yml/badge.svg)](https://github.com/Baconfish9/TVGids4Streamer/actions/workflows/apk.yml)
[![Laatste release](https://img.shields.io/github/release-date/Baconfish9/TVGids4Streamer?label=laatste%20build)](https://github.com/Baconfish9/TVGids4Streamer/releases/latest)
![Platform](https://img.shields.io/badge/platform-Android%20TV%20%7C%20telefoon%20%7C%20tablet-3ddc84)
![minSdk](https://img.shields.io/badge/minSdk-26-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7f52ff)

Een live tv-gids met tijdlijn voor de **Google TV Streamer**, met de Vlaamse zenders
(VRT, VTM, Play) en de Nederlandse publieke omroep (NPO). Volledig te bedienen met de
afstandsbediening, en met één druk op de knop door naar VRT MAX, VTM GO, Play of NPO Start.
Werkt ook op Android-telefoons en -tablets, in liggende stand.

![Schermafbeelding van de gids](docs/screenshot.png)

## Inhoud

- [Functies](#functies)
- [Zenders](#zenders)
- [Installeren](#installeren)
- [Bediening](#bediening)
- [Programmagegevens](#programmagegevens)
- [Zelf bouwen](#zelf-bouwen)
- [Aanpassen](#aanpassen)
- [Projectstructuur](#projectstructuur)

## Functies

- **Tijdlijn van 30 uur** vanaf twee uur geleden, met een rode lijn op het huidige tijdstip
  en een voortgangsbalk onder elk programma dat nu loopt.
- **Gemaakt voor de afstandsbediening**: de tijdlijn schuift mee terwijl je bladert. Titels
  van lange programma's blijven zichtbaar aan de linkerrand en heel korte opvullers worden
  overgeslagen.
- **Ook op telefoon en tablet** (liggend): vegen door de tijdlijn, tikken voor details en
  een knop **Nu** om terug te springen. Hou je het toestel rechtop, dan vraagt een
  animatie om het te draaien. Op een telefoon verdwijnt het infopaneel om plaats te sparen.
- **Infopaneel en detailvenster** met uren, duur, genre en beschrijving.
- **Kijken of terugkijken**: vanuit het detailvenster open je meteen de app van de zender.
- **Automatisch verversen** zodra de gegevens ouder zijn dan 6 uur. Je positie in de gids
  blijft daarbij behouden.
- **Snel opstarten**: de laatste gids wordt bewaard, zodat de app meteen iets toont, ook
  zonder internet.
- **Statusscherm** per bron en per zender, handig om ontbrekende zenders te koppelen.
- **Belgische tijd**: alle uren in Europe/Brussels, los van de instelling van het toestel.

## Zenders

| Vlaanderen | App | Nederland | App |
|---|---|---|---|
| VRT 1, VRT Canvas, Ketnet | VRT MAX | NPO 1, NPO 2, NPO 3 | NPO Start |
| VTM, VTM 2, VTM 3, VTM 4, VTM GOLD | VTM GO | | |
| Play, Play Fictie, Play Actie, Play Crime, Play Reality | Play | | |

De lijst staat in [`Config.kt`](app/src/main/java/be/tvgids/app/Config.kt); zie
[Aanpassen](#aanpassen).

## Installeren

De laatste APK staat altijd op dezelfde plaats:

```
https://github.com/Baconfish9/TVGids4Streamer/releases/latest/download/TVGids.apk
```

Eerst eenmalig de ontwikkelaarsopties aanzetten op de Streamer: **Instellingen > Systeem >
Over** en 7 keer klikken op **Android TV OS-build**.

### Met de app Downloader (geen computer nodig)

1. Installeer **Downloader** (van AFTVnews) uit de Play Store.
2. **Instellingen > Apps > Beveiliging en beperkingen > Onbekende bronnen**: zet Downloader aan.
3. Open Downloader, typ de link hierboven en kies **Installeren**.

### Met ADB vanaf je computer

Zet in de ontwikkelaarsopties **Draadloze foutopsporing** aan en kies **Apparaat koppelen met
koppelingscode**:

```sh
adb pair <ip>:<koppelpoort>
adb connect <ip>:<poort>
adb install -r TVGids.apk
```

De app verschijnt daarna bij je apps op Google TV. Een nieuwe versie installeer je op dezelfde
manier; ze komt gewoon over de vorige.

## Bediening

| Toets | Actie |
|---|---|
| Pijltjes | Door zenders en tijd bladeren |
| OK | Details van het programma |
| Omhoog vanaf de eerste zender | Naar de knoppen **Vernieuwen** en **Status** |
| Terug | Detailvenster of statusscherm sluiten |

Op een telefoon of tablet veeg je horizontaal door de tijd en verticaal door de zenders.
Tik op een programma voor de details; **Nu** brengt je terug naar het huidige uur.

In het detailvenster zie je **Kijken in …** bij een programma dat nu loopt en
**Terugkijken in …** bij een programma dat al voorbij is. Bij een programma dat nog moet
beginnen staat er geen kijkknop.

## Programmagegevens

De gids leest [XMLTV](https://wiki.xmltv.org/)-bestanden (`.xml` of `.xml.gz`). Standaard
komen die van [open-epg.com](https://www.open-epg.com/) (twee Belgische en twee Nederlandse
bestanden). De bronnen worden tegelijk opgehaald; staat een programma in meer dan één bron,
dan wint de bron die het eerst in de lijst staat.

Je kan ook een eigen bron gebruiken, bijvoorbeeld een bestand dat je dagelijks genereert met
[iptv-org/epg](https://github.com/iptv-org/epg) en ergens online zet.

### Een zender zonder gegevens?

Open in de app **Status**. Daar zie je:

1. per bron of ze gelukt is en hoeveel programma's ze bevat,
2. per zender hoeveel programma's er gevonden zijn,
3. onder welke namen de overige zenders in de bronnen staan.

Zoek de juiste naam in die laatste lijst, voeg hem toe aan de `aliassen` van de zender in
`Config.kt` en bouw opnieuw. Namen worden vergeleken zonder hoofdletters, accenten,
leestekens en achtervoegsels zoals `HD` of `BE`, dus `Eén HD` en `een.be` zijn hetzelfde.

## Zelf bouwen

### Via GitHub Actions

Elke push naar `main` start de workflow [**APK bouwen**](.github/workflows/apk.yml). Die
draait eerst de unit tests, bouwt daarna de APK en publiceert hem als release `latest`. Dat
duurt een vijftal minuten.

Elke build krijgt het versienummer `1.0.<buildnummer>`. Je ziet het in de app bovenaan in
**Status**.

Werk je met een eigen kopie, dan werkt dat net zo: zet de bestanden in een eigen repository
(inclusief de map `.github`). De downloadlink wordt dan
`https://github.com/<gebruiker>/<repo>/releases/latest/download/TVGids.apk`.

### Lokaal

Vereisten: een recente Android Studio (het project gebruikt AGP 9.4.1, Gradle 9.6.0 en JDK 21).

Open de map in Android Studio, laat Gradle synchroniseren en kies **Build > Build App
Bundle(s) / APK(s) > Build APK(s)**. Of vanaf de commandolijn, met Gradle 9.6.0:

```sh
gradle assembleRelease       # APK in app/build/outputs/apk/release/
gradle testDebugUnitTest     # unit tests
```

> **Let op:** een lokale build heeft versie 1 en installeert dus niet over een build van
> GitHub. De-installeer eerst, of gebruik voor een debug-build `adb install -r -d`.

### Ondertekening

Er zijn twee sporen:

- **De APK op GitHub** (om zelf te installeren) wordt ondertekend met
  `app/tvgids-debug.keystore`, die bewust in de repository staat. Zo heeft elke build dezelfde
  handtekening en installeert hij over de vorige.
- **De App Bundle (AAB) voor de Play Store** wordt ondertekend met een aparte uploadsleutel
  die nooit in de repository komt. GitHub Actions bouwt die AAB alleen als de sleutel als
  secret is ingesteld, en zet hem bij de workflow-run als download `TVGids-playstore-aab`.

De twee versies hebben een andere handtekening. Wie van de GitHub-APK naar de Play
Store-versie overstapt, moet de app dus eerst één keer de-installeren.

#### Uploadsleutel instellen (eenmalig)

1. Maak de sleutel aan, **buiten** de projectmap, en bewaar hem en de wachtwoorden goed
   (bijvoorbeeld in je wachtwoordbeheerder):

   ```sh
   keytool -genkeypair -v -keystore ~/tvgids-upload.jks -alias upload \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Zet hem in GitHub onder **Settings > Secrets and variables > Actions** als vier secrets:

   | Secret | Waarde |
   |---|---|
   | `UPLOAD_KEYSTORE_BASE64` | de uitvoer van `base64 -i ~/tvgids-upload.jks` |
   | `UPLOAD_KEYSTORE_PASSWORD` | het wachtwoord van de keystore |
   | `UPLOAD_KEY_ALIAS` | `upload` |
   | `UPLOAD_KEY_PASSWORD` | het wachtwoord van de sleutel |

   Met de GitHub CLI kan dat ook: `gh secret set UPLOAD_KEYSTORE_BASE64 < <(base64 -i ~/tvgids-upload.jks)`.

3. Kies in de Play Console bij de eerste upload voor **Play App Signing**. Google bewaart
   dan de echte app-sleutel; verlies je de uploadsleutel, dan kan Google hem vervangen.

## Play Store

Het [privacybeleid](docs/privacybeleid.md) staat in deze repository; gebruik de link naar
dat bestand op GitHub in de Play Console. De app vraagt alleen de permissie `INTERNET`,
gebruikt uitsluitend HTTPS en verzamelt geen gegevens.

### Screenshots

In [`docs/playstore/`](docs/playstore) staan screenshots die aan de eisen van de Play Store
voldoen (JPEG zonder transparantie, 16:9):

| Bestand | Formaat | Voor |
|---|---|---|
| `tv-1-gids.jpg`, `tv-2-detail.jpg` | 1920×1080 | Android TV |
| `telefoon-1-gids.jpg`, `telefoon-2-detail.jpg` | 1920×1080 | Telefoon |
| `tablet-1-gids.jpg`, `tablet-2-detail.jpg` | 2560×1440 | Tablet van 7 en 10 inch |

### Stappen in de Play Console

1. Maak de app aan en upload de AAB (`TVGids-playstore-aab` bij de workflow-run).
   Kies daarbij voor **Play App Signing**.
2. Vul bij de winkelvermelding de screenshots per vormfactor in, plus het app-icoon
   (512×512) en de feature graphic (1024×500).
3. Zet bij de geavanceerde instellingen, onder **Vormfactoren**, de vormfactor
   **Android TV** aan. TV-apps krijgen een eigen beoordeling: Google controleert onder meer
   of alles met de afstandsbediening werkt en of de banner aanwezig is.
4. Vul het formulier **Gegevensveiligheid** in: er worden geen gegevens verzameld of gedeeld.
5. Geef de link naar het privacybeleid op.

### Versienummer

De `versionCode` is `1000 +` het volgnummer van de GitHub-workflow. De Play Store aanvaardt
alleen een hogere code dan de vorige upload. Begint de teller van GitHub ooit opnieuw,
bijvoorbeeld na het hernoemen van de workflow, verhoog dan `versieBasis` in
`app/build.gradle.kts` tot boven de laatst geüploade code.

## Aanpassen

Alles staat in [`Config.kt`](app/src/main/java/be/tvgids/app/Config.kt):

- **`EPG_BRONNEN`**: de lijst met XMLTV-adressen.
- **`ZENDERS`**: naam, land, kleur, aliassen en de app die bij **Kijken** geopend wordt.

Voeg je een **nieuwe kijk-app** toe, vermeld het pakket dan ook in de `<queries>` van
[`AndroidManifest.xml`](app/src/main/AndroidManifest.xml). Zonder dat vindt Android 11 en
nieuwer de app niet. Pakketnamen op je Streamer vind je met:

```sh
adb shell pm list packages | grep -i -E "vrt|vtm|npo|goplay"
```

Na het aanpassen van de zenders vangen de unit tests botsende aliassen op, dus twee zenders
die naar dezelfde naam luisteren.

## Projectstructuur

```
app/src/main/java/be/tvgids/app/
├── Config.kt        Zenders, EPG-bronnen en het vergelijken van zendernamen
├── DraaiMelding.kt  Geanimeerde melding om een telefoon of tablet liggend te houden
├── Epg.kt           XMLTV-parser, ophalen, opschonen en cache
├── GidsScherm.kt    De gids, het detailvenster en het statusscherm (Jetpack Compose)
└── MainActivity.kt  Activity en ViewModel, automatisch verversen
app/src/test/        Unit tests
```
