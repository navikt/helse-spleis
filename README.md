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

1. Endre `{{image}}` i `deploy/prod-migrate-job.yml` til å peke til siste image av [spleis-jobs imaget](https://github.com/navikt/helse-spleis/pkgs/container/helse-spleis%2Fspleis-jobs)
2. Deploy jobben til `prod-gcp` via kubectl: `k apply -f deploy/prod-migrate-job.yml`

En fullskala migrering tar omtrent 2 timer for spleis å gjennomføre.

## Oppgradering av gradle wrapper
Finn nyeste versjon av gradle her: https://gradle.org/releases/

```./gradlew wrapper --gradle-version $gradleVersjon```

Husk å oppdater gradle versjon i build.gradle.kts filen
```gradleVersion = "$gradleVersjon"```

## Protip for å kjøre tester raskere
Finn filen .testcontainers.properties, ligger ofte på hjemmeområdet ditt eks:

```~/.testcontainers.properties```

legg til denne verdien

```testcontainers.reuse.enable=true```

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen ![#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).
