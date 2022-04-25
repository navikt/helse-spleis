package no.nav.helse.spleis


import io.ktor.server.application.Application
import io.ktor.server.request.path
import io.ktor.server.routing.Routing
import java.util.concurrent.atomic.AtomicInteger

internal fun Application.preStopHook(teller: AtomicInteger) {
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
