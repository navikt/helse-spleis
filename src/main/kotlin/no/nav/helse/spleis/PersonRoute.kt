package no.nav.helse.spleis

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

const val personPath = "/api/person/"
private const val aktørId = "aktørId"
private const val personParameterizedPath = "$personPath{$aktørId}"

internal fun Route.person(personMediator: PersonMediator) {
    get(personParameterizedPath) {
        personMediator.hentPerson(call.parameters[aktørId]!!)?.let { call.respond(it.toString()) }
            ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
    }
}
