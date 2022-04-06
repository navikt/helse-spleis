# Spleis
![Bygg og deploy app](https://github.com/navikt/helse-spleis/workflows/Bygg%20og%20deploy%20app/badge.svg)
![Bygg og deploy api](https://github.com/navikt/helse-spleis/workflows/Bygg%20og%20deploy%20api/badge.svg)

## Beskrivelse

Tar inn søknader og inntektsmeldinger for en person og foreslår utbetalinger.

## Regler
Dagturnering: https://github.com/navikt/helse-spleis/blob/master/sykepenger-model/src/main/resources/dagturnering.csv

## Oppgradering av gradle wrapper
Finn nyeste versjon av gradle her: https://gradle.org/releases/

```./gradlew wrapper --gradle-version $gradleVersjon```

Husk å oppdater gradle versjon i build.gradle.kts filen
```gradleVersion = "$gradleVersjon"```

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen ![#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).
