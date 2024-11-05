package no.nav.helse.spleis.dao

import io.micrometer.core.instrument.Counter.*
import no.nav.helse.spleis.meterRegistry

object PostgresProbe {
    fun personLestFraDb() {
        builder("person_lest_fra_db_totals")
            .description("Antall ganger vi har lest en person fra db")
            .register(meterRegistry)
            .increment()
    }

    fun hendelseLestFraDb() {
        builder("hendelse_lest_fra_db_totals")
            .description("Antall ganger vi har lest en hendelse fra db")
            .register(meterRegistry)
            .increment()
    }
}
