# Spleis utboks (KUN SKRUDD PÅ I DEV)

## Spleis utboks består av 3 deler.

### Lagring av meldinger
Alle meldinger* som produseres ved håndtering av en innkommende melding lagres ned i databasen i samme database-transaksjon som personen lagres ned med. Dette sikrer at vi er i stand til å få sendt ut alle meldinger på sikt, selv om det skulle være Kafka-problemer når den innkommende meldingen håndteres.

### Henting & utsending av meldinger 
Etter at person og meldinger er lagret ned (fortsatt ved håndtering av samme innkommende melding) henter vi opp alle usendte meldinger i utboksen for den aktuelle personen** Disse hentes i rekkefølgen de ble lagt inn i utboksen. Det vil si at det kan være at vi henter opp fler meldinger enn de den innkommende meldingen selv produserte. Dette er for å garantere rekkefølgen meldinger blir sendt ut i.

Når en melding er sendt OK har den fått et `@sendt`-tidspunkt satt på seg (UTC) som da ikke nødvendigvis er samme tidspunkt som `@opprettet/@opprettetUTC`. Dette feltet får man også i prod i dag selv om utboks ikke er enabled, men tidspunktet vil per nå nesten alltid være likt****

### Retry ved feil
Hvert `"@event_name":"minutt"` eller ved `"@event_name":"spleis_utboks_retry"` (typisk fra Spout) fiskes det opp alle personer som har usendte meldinger i utboksen***. For hver disse sendes det ut en `person_påminnelse` (Spleis sender melding til seg selv) som håndteres på lik linje med alle andre innkommende meldinger. Som beskrevet ovenfor henter vi da ut alle usendte meldinger for en person og sender dem ut i rett rekkefølge.

Grunnen til å gjøre det på denne måten fremfor en egen job el.lig. er å bevare det at spleis håndterer en person i rekkefølge basert på meldinger som legges på Kafka. Ved å følge samme mønster vet vi at ingen andre podder håndterer samme person samtidig. I tillegg er blir ikke "retry-mekanismen" en egen flyt på siden, men den samme flyten som brukes for alle innkommende meldinger. 

## Forbedringspotensiale / noe å tenke på
### Trenger vi å lagre meldingene for alltid?
Nei, det kan kanskje bli litt fullt? En veldig enkel fiks vil jo være å slette radene ettersom vi får sendt meldingene fremfor å sette `sendt`-tidspunktet. Om det fortsatt er interessant å ha alle meldingene lagret et sted kunne det vært gjort i en annen storage et annet sted? 

### Burde det vært et eget event istedenfor `person_påminnelse` ?
Ja, det hadde sikkert vært ryddig det altså.

*Noen få meldinger er `fire and forget` - de lagres ikke ned, men det er unntaket fremfor regelen.
**Det hentes også opp eventuelle meldinger som ikke er key'en på noe (som ikke gjelder enkeltpersoner)
***Man kan også sette en liste med `personidentifikatorer` i eventet om du kun vil retrye for dem istedenfor å hente opp fra databasen.
****Sett bort fra millisekunder ogsånn da..