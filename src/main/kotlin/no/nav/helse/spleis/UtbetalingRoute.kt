package no.nav.helse.spleis

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

internal fun Route.utbetaling(personMediator: PersonMediator) {
    get("/api/utbetaling/{utbetalingsreferanse}") {
        personMediator.hentPersonForUtbetaling(call.parameters["utbetalingsreferanse"]!!)?.let { call.respond(it.toString()) }
                ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
    }
}
