package no.nav.helse.spleis.dao

import io.micrometer.core.instrument.Counter.builder
import io.micrometer.core.instrument.MeterRegistry

object PostgresProbe {
    fun personLestFraDb(meterRegistry: MeterRegistry) {
        builder("person_lest_fra_db_totals")
            .description("Antall ganger vi har lest en person fra db")
            .register(meterRegistry)
            .increment()
    }

    fun hendelseLestFraDb(meterRegistry: MeterRegistry) {
        builder("hendelse_lest_fra_db_totals")
            .description("Antall ganger vi har lest en hendelse fra db")
            .register(meterRegistry)
            .increment()
    }
}
