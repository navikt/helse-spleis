# Manuell jobb for å avstemme x antall personer

## Rigging
1. Skru på gateway `aiven-prod` i Naisdevice
2. Hent envvars for Kafka vha. bømlo-cli, og legg disse inn som miljøvariabler til appen. (Enkelt vha. run configurations i IntelliJ). Følgende miljøvariabler trengs:
   * KAFKA_BROKERS
   * KAFKA_CREDSTORE_PASSWORD
   * KAFKA_KEYSTORE_PATH
   * KAFKA_TRUSTSTORE_PATH
3. Koble deg til Spleis-databasen vha. nais-cli: 
   * `nais postgres proxy -n tbd helse-spleis`
4. Sett brukernavn og passord i `App.kt`
   * Brukernavn: din NAV-epost
   * Passord: Du finner passord i `~/.pgpass` etter å ha koblet deg opp mot databasen
5. (Optional) sett en limit på hvor mange personer du ønsker å avstemme
6. Start `main` i `App.kt`