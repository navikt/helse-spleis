package no.nav.helse.spleis.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.spleis.db.HendelseRecorder

internal fun Route.person(personRestInterface: PersonRestInterface, hendelseRecorder: HendelseRecorder) {
    val objectMapper = jacksonObjectMapper()
    get("/api/person/{aktørId}") {
        personRestInterface.hentSak(call.parameters["aktørId"]!!)
            ?.let {
                val (serialisertPerson, hendelseReferanser) = serializePersonForSpeil(it)
                val hendelser = hendelseRecorder.hentHendelser(hendelseReferanser)
                call.respond(serialisertPerson.apply {
                    putArray("hendelser").addAll(hendelser.map { objectMapper.readTree(it.second) })
                })
            } ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
    }
}
