package no.nav.helse.spleis

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.pipeline.PipelineContext

internal fun Route.person(personMediator: PersonMediator) {
    get("/api/person/{aktørId}") {
        finnPerson(personMediator)
    }
    // TODO: fjern route når speil er oppdatert med ny url
    get("/api/sak/{aktørId}") {
        finnPerson(personMediator)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.finnPerson(personMediator: PersonMediator) {
    personMediator.hentSak(call.parameters["aktørId"]!!)?.let {
        call.respond(it.memento().state())
    } ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
}
