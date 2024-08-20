package no.nav.helse.spleis

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object PostgresProbe {
    private val metrics: MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun hendelseSkrevetTilDb() {
        Counter.builder("hendelse_skrevet_til_db_totals")
            .description("Antall ganger vi har skrevet en hendelse til db")
            .register(metrics)
            .increment()
    }
}
