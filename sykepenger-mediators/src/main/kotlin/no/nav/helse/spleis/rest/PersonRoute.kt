package no.nav.helse.spleis.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.Meldingstype
import no.nav.helse.spleis.hendelser.model.InntektsmeldingMessage
import no.nav.helse.spleis.hendelser.model.NySøknadMessage
import no.nav.helse.spleis.hendelser.model.SendtSøknadMessage

internal fun Route.person(personRestInterface: PersonRestInterface, hendelseRecorder: HendelseRecorder) {
    val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    get("/api/person/{aktørId}") {
        personRestInterface.hentSak(call.parameters["aktørId"]!!)
            ?.let {
                val (serialisertPerson, hendelseReferanser) = serializePersonForSpeil(it)
                val hendelser = hendelseRecorder.hentHendelser(hendelseReferanser)
                call.respond(serialisertPerson.apply {
                    putArray("hendelser").addAll(hendelser.map { hendelse ->
                        objectMapper.valueToTree<JsonNode>(tilDTO(hendelse.first, hendelse.second, MessageProblems(hendelse.second)))
                    })
                })
            } ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
    }
}

internal fun tilDTO(
    meldingstype: Meldingstype,
    hendelse: String,
    problems: MessageProblems
): HendelseDTO =
    when (meldingstype) {
        Meldingstype.NY_SØKNAD -> NySøknadMessage(hendelse, problems).asSpeilDTO()
        Meldingstype.SENDT_SØKNAD -> SendtSøknadMessage(hendelse, problems).asSpeilDTO()
        Meldingstype.INNTEKTSMELDING -> InntektsmeldingMessage(hendelse, problems).asSpeilDTO()
        Meldingstype.PÅMINNELSE -> TODO()
        Meldingstype.YTELSER -> TODO()
        Meldingstype.VILKÅRSGRUNNLAG -> TODO()
        Meldingstype.MANUELL_SAKSBEHANDLING -> TODO()
        Meldingstype.UTBETALING -> TODO()
        Meldingstype.UKJENT -> TODO()
    }
