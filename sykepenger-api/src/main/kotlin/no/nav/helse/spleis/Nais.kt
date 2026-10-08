package no.nav.helse.spleis

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

internal fun Application.nais(collectorRegistry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        registry = collectorRegistry
        meterBinders =
            listOf(
                ClassLoaderMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
                JvmThreadMetrics(),
            )
    }

    routing {
        get("/isalive") {
            call.respondText("ALIVE", ContentType.Text.Plain)
        }
        get("/isready") {
            call.respondText("READY", ContentType.Text.Plain)
        }
        get("/metrics") {
            call.respond(collectorRegistry.scrape())
        }
    }
}
