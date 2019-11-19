package no.nav.helse.spleis

import io.prometheus.client.Counter

object PostgresProbe {
    private val sakLestFraDbCounter = Counter.build("sak_lest_fra_db_totals", "Antall ganger vi har lest en sak fra db")
                .register()

    private val sakSkrevetFraDbCounter = Counter.build("sak_skrevet_fra_db_totals", "Antall ganger vi har skrevet en sak til db")
            .register()

    private val utbetalingLestFraDbCounter = Counter.build("utbetaling_lest_fra_db_totals", "Antall ganger vi har lest en utbetaling fra db")
            .register()

    private val utbetalingSkrevetTilDbCounter = Counter.build("utbetaling_skrevet_fra_db_totals", "Antall ganger vi har skrevet en utbetaling til db")
            .register()

    fun sakLestFraDb() {
        sakLestFraDbCounter.inc()
    }

    fun sakSkrevetTilDb() {
        sakSkrevetFraDbCounter.inc()
    }

    fun utbetalingLestFraDb() {
        utbetalingLestFraDbCounter.inc()
    }

    fun utbetalingSkrevetTilDb() {
        utbetalingSkrevetTilDbCounter.inc()
    }
}
