package no.nav.helse.spleis.rest

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

internal fun Route.utbetaling(personRestInterface: PersonRestInterface) {
    get("/api/utbetaling/{utbetalingsreferanse}") {
        personRestInterface.hentSakForUtbetaling(call.parameters["utbetalingsreferanse"]!!)?.let {
            call.respond(TODO("B803FB72-7BC7-486B-B21A-FBE5EA84C127: Vi ønsker ikke å sende hele modellen til speil, søk opp id for disabled test"))
        }
            ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
    }
}
