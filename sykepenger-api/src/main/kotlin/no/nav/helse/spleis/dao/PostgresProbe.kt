package no.nav.helse.spleis.dao

import io.prometheus.client.Counter

object PostgresProbe {
    private val personLestFraDbCounter =
        Counter.build("person_lest_fra_db_totals", "Antall ganger vi har lest en person fra db")
            .register()

    private val utbetalingLestFraDbCounter =
        Counter.build("utbetaling_lest_fra_db_totals", "Antall ganger vi har lest en utbetaling fra db")
            .register()

    private val hendelseLestFraDbCounter =
        Counter.build("hendelse_lest_fra_db_totals", "Antall ganger vi har lest en hendelse fra db")
            .register()

    fun personLestFraDb() {
        personLestFraDbCounter.inc()
    }

    fun utbetalingLestFraDb() {
        utbetalingLestFraDbCounter.inc()
    }

    fun hendelseLestFraDb() {
        hendelseLestFraDbCounter.inc()
    }
}
