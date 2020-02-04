# Rapids and rivers
Dette biblioteket lar deg lett lage en mikrotjeneste som utfører operasjoner på et subsett av alle meldingene som flyter
gjennom en rapid. Det forventer følgende oppsett:

- vault: true i naiseratorkonfigurasjon
- servicebruker satt opp gjennom vault: https://basta-frontend.adeo.no/create/customcredential
- følgende miljøvariabler:
```
KAFKA_CONSUMER_GROUP_ID
KAFKA_RAPID_TOPIC
KAFKA_BOOTSTRAP_SERVERS
```
