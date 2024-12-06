# Spleis
![Bygg og deploy app](https://github.com/navikt/helse-spleis/workflows/Bygg%20og%20deploy%20app/badge.svg)
![Bygg og deploy api](https://github.com/navikt/helse-spleis/workflows/Bygg%20og%20deploy%20api/badge.svg)

## Beskrivelse

Tar inn søknader og inntektsmeldinger for en person og foreslår utbetalinger.

## Regler

Dagturnering: 

https://github.com/navikt/helse-spleis/blob/master/sykepenger-model/src/main/resources/dagturnering.csv

## Migrere JSON til siste skjema

JSON-migrering skjer hver gang vi henter opp en person, men noen ganger er det greit å kunne bumpe alle personer samtidig.
Da kan vi anvende `spleis-migrate`, en enkel k8s job:

1. Trykk på [Run workflow](https://github.com/navikt/helse-spleis/actions/workflows/manuell-jobb.yml) 
2. Fyll image som beskrevet & navn til `migrate_v2`

En fullskala migrering tar omtrent 2 timer for spleis å gjennomføre.

## Oppgradering av gradle wrapper
Finn nyeste versjon av gradle her: https://gradle.org/releases/

```./gradlew wrapper --gradle-version $gradleVersjon```

## Protip for å kjøre tester raskere
Finn filen .testcontainers.properties, ligger ofte på hjemmeområdet ditt eks:

```~/.testcontainers.properties```

legg til denne verdien

```testcontainers.reuse.enable=true```

## Hvorfor Spleis?

Da ytelsen sykepenger skulle automatiseres, ble det tidlig laget en skisse over tjenester som skulle inngå i dette arbeidet. Vi så for oss tre tjenester som tok imot relevante dokumenter (sykmelding, søknad, og inntektsmelding), én tjeneste som skulle spleise disse sammen til en sak, og én tjeneste som skulle behandle saken.

De tre første tjenesten rakk aldri bli navngitt.

Tjenesten som skulle behandle saken ble kalt Spa.

Tjenesten som skulle spleise dokumentene sammen til en sak ble kalt **Spleis**.

Det ble relativt fort klart at vi ikke kunne regne med en så enkel flyt som 1. Dokumenter inn, 2. Sammenstill data, 3. Behandle sak, så Spleis endte opp med å gjøre brorparten av saksbehandlingen, Spa ble aldri laget, og dokument-inntaket er i stor grad håndtert av andre team.

## Linting

Lintet med hjelp av editorconfig er brukt gjennomgående i hele repoet for kotlinfiler. For å slå det på så gjør følgende
1. `Cmd + Q`
2. Trykk på `Code style`
3. Marker `Enable editorconfig support` 

Valgfritt, men lurt er å også bare linte de steder man varit inom. 
1. `Cmd+Q`
2. Trykk på `Tools`
3. Velg `Actions on save`
4. Marker `Reformat code`
5. Hvis ønskelig endre fra whole file til changed lines for `Reformat code`
6. Klikk på `Configure autosave options...` lengst ned i fila, eller gå til `Appearance & Behavior -> System Settings` 
7. Marker alle alternativ under `Autosave` og endre default tiden for autosave vid idle til 60s. 

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen ![#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).

### For Øvrige 
Eksterne henvendelser kan sendes via E-post til tbd(at)nav.no
