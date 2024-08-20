package no.nav.helse.spleis.dao

import io.micrometer.core.instrument.Counter.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object PostgresProbe {
    private val metrics: MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun personLestFraDb() {
        builder("person_lest_fra_db_totals")
            .description("Antall ganger vi har lest en person fra db")
            .register(metrics)
            .increment()
    }

    fun hendelseLestFraDb() {
        builder("hendelse_lest_fra_db_totals")
            .description("Antall ganger vi har lest en hendelse fra db")
            .register(metrics)
            .increment()
    }
}
