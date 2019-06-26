package no.nav.helse.nais

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.prometheus.client.*
import io.prometheus.client.exporter.common.*
import io.prometheus.client.hotspot.*
import java.util.*

private val collectorRegistry = CollectorRegistry.defaultRegistry

fun Application.nais(isAliveCheck: () -> Boolean = { true },
                     isReadyCheck: () -> Boolean = { true }) {

   DefaultExports.initialize()

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
         val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
         call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
         }
      }
   }
}
