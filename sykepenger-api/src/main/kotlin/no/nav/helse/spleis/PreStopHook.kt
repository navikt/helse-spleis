package no.nav.helse.spleis

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.routing.*
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
