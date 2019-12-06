[![Actions Status](https://github.com/navikt/helse-spleis/workflows/Bygg%20og%20deploy/badge.svg)](https://github.com/navikt/helse-spleis/actions)

# Sykepenger - spleis

Tar i mot hendelser knytter til sykepengesaken for en person, for eksempel søknader og inntektsmeldinger.
Håndterer vedtaksperiodeene for en person, og sørger for å innhente informasjon vi trenger for å kunne ta beslutning om 
saken skal resultere i en automatisk utbetaling eller ikke.

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.

## Regler
Dagturnering: https://github.com/navikt/helse-spleis/blob/master/sykepenger-model/src/main/resources/dagturnering.csv

## Bygge prosjektet
For å bygge trenger man å oppgi en Github-bruker med lesetilgang til Github-repoet.
Dette gjøres enklest ved å opprette et personal access token for Github-brukeren din: 

På Github: gå til Settings/Developer settings/Personal access tokens,
og opprett et nytt token med scope "read:packages"

Legg inn tokenet i din `.gradle/gradle.properties` fil slik:

```
githubUser=x-access-token
githubPassword=<tokenet ditt>
```

## Kafka (iac)

```json
{
 "topics": [
   {
     "configEntries": {},
     "members": [
       {
         "member": "srvspsak",
         "role": "PRODUCER"
       },
{
         "member": "s150563",
         "role": "MANAGER"
       },
{
         "member": "s149030",
         "role": "MANAGER"
       },
{
         "member": "srvSyfogsak",
         "role": "CONSUMER"
       }
     ],
     "numPartitions": 3,
     "topicName": "privat-helse-sykepenger-opprettGosysOppgave"
   }
 ]
}
```

