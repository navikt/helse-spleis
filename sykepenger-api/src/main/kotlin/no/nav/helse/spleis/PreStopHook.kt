package no.nav.helse.spleis


import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.util.concurrent.atomic.AtomicInteger

internal fun Application.preStopHook(teller: AtomicInteger) {
    routing {
        get("/stop") {
            call.application.log.info("Received shutdown signal via preStopHookPath")
            call.respond(HttpStatusCode.OK)
        }
    }
    environment.monitor.subscribe(Routing.RoutingCallStarted) {
        if (it.request.path() != "/stop") {
            teller.incrementAndGet()
        }
    }
    environment.monitor.subscribe(Routing.RoutingCallFinished) {
        if (it.request.path() != "/stop") {
            teller.decrementAndGet()
        }
    }
}
