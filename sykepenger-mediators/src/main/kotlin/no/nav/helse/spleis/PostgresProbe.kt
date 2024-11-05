package no.nav.helse.spleis

import io.micrometer.core.instrument.Counter
import no.nav.helse.meterRegistry

object PostgresProbe {
    private val counter = Counter.builder("hendelse_skrevet_til_db_totals")
        .description("Antall ganger vi har skrevet en hendelse til db")
        .register(meterRegistry)

    fun hendelseSkrevetTilDb() {
            counter.increment()
    }
}
