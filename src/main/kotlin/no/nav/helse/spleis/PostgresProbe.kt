package no.nav.helse.spleis

import io.prometheus.client.Counter

object PostgresProbe {
    private val personLestFraDbCounter = Counter.build("person_lest_fra_db_totals", "Antall ganger vi har lest en person fra db")
                .register()

    private val personSkrevetFraDbCounter = Counter.build("person_skrevet_fra_db_totals", "Antall ganger vi har skrevet en person til db")
            .register()

    fun personLestFraDb() {
        personLestFraDbCounter.inc()
    }

    fun personSkrevetTilDb() {
        personSkrevetFraDbCounter.inc()
    }
}
