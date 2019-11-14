package no.nav.helse.spleis

import io.prometheus.client.Counter

object PostgresProbe {
    private val personLestFraDbCounter = Counter.build("person_lest_fra_db_totals", "Antall ganger vi har lest en person fra db")
                .register()

    private val personSkrevetFraDbCounter = Counter.build("person_skrevet_fra_db_totals", "Antall ganger vi har skrevet en person til db")
            .register()

    private val utbetalingLestFraDbCounter = Counter.build("utbetaling_lest_fra_db_totals", "Antall ganger vi har lest en utbetaling fra db")
            .register()

    private val utbetalingSkrevetTilDbCounter = Counter.build("utbetaling_skrevet_fra_db_totals", "Antall ganger vi har skrevet en utbetaling til db")
            .register()

    fun personLestFraDb() {
        personLestFraDbCounter.inc()
    }

    fun personSkrevetTilDb() {
        personSkrevetFraDbCounter.inc()
    }

    fun utbetalingLestFraDb() {
        utbetalingLestFraDbCounter.inc()
    }

    fun utbetalingSkrevetTilDb() {
        utbetalingSkrevetTilDbCounter.inc()
    }
}
