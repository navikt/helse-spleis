package no.nav.helse.spleis.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.pipeline.PipelineContext

internal fun Route.person(personRestInterface: PersonRestInterface) {
    get("/api/person/{aktørId}") {
        finnPerson(personRestInterface)
    }
    // TODO: fjern route når speil er oppdatert med ny url
    get("/api/sak/{aktørId}") {
        finnPerson(personRestInterface)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.finnPerson(personRestInterface: PersonRestInterface) {
    personRestInterface.hentSak(call.parameters["aktørId"]!!)
        ?.let {
            call.respond(TODO())
        } ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
}
