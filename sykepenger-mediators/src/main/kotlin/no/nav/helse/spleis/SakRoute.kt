package no.nav.helse.spleis

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

const val path = "/api/sak/"
private const val aktørId = "aktørId"
private const val parameterizedPath = "$path{$aktørId}"

internal fun Route.sak(sakMediator: SakMediator) {
    get(parameterizedPath) {
        sakMediator.hentSak(call.parameters[aktørId]!!)?.let { call.respond(it.memento().state()) }
            ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
    }
}
