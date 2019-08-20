package no.nav.helse.nais

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

fun Application.nais(isAliveCheck: () -> Boolean = { true },
                     isReadyCheck: () -> Boolean = { true },
                     collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry) {

   install(MicrometerMetrics) {
      registry = PrometheusMeterRegistry(
         PrometheusConfig.DEFAULT,
         CollectorRegistry.defaultRegistry,
         Clock.SYSTEM
      )
      meterBinders = listOf(
         ClassLoaderMetrics(),
         JvmMemoryMetrics(),
         JvmGcMetrics(),
         ProcessorMetrics(),
         JvmThreadMetrics(),
         LogbackMetrics()
      )
   }

   routing {
      get("/isalive") {
         if (!isAliveCheck()) {
            call.respondText("NOT ALIVE", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
         } else {
            call.respondText("ALIVE", ContentType.Text.Plain)
         }
      }

      get("/isready") {
         if (!isReadyCheck()) {
            call.respondText("NOT READY", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
         } else {
            call.respondText("READY", ContentType.Text.Plain)
         }
      }

      get("/metrics") {
         val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
         call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
         }
      }
   }
}
