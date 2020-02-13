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
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
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
        val aktivitetslogger = Aktivitetslogger()
        val aktivitetslogg = Aktivitetslogg()
        personRestInterface.hentSak(call.parameters["aktørId"]!!)
            ?.let {
                val (serialisertPerson, hendelseReferanser) = serializePersonForSpeil(it)
                val hendelser = hendelseRecorder.hentHendelser(hendelseReferanser)
                call.respond(serialisertPerson.apply {
                    putArray("hendelser").addAll(hendelser.map { hendelse ->
                        objectMapper.valueToTree<JsonNode>(tilDTO(hendelse.first, hendelse.second, aktivitetslogger, aktivitetslogg))
                    })
                })
            } ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
    }
}

internal fun tilDTO(
    meldingstype: Meldingstype,
    hendelse: String,
    aktivitetslogger: Aktivitetslogger,
    aktivitetslogg: Aktivitetslogg
): HendelseDTO =
    when (meldingstype) {
        Meldingstype.NY_SØKNAD -> NySøknadMessage(hendelse, aktivitetslogger, aktivitetslogg).asSpeilDTO()
        Meldingstype.SENDT_SØKNAD -> SendtSøknadMessage(hendelse, aktivitetslogger, aktivitetslogg).asSpeilDTO()
        Meldingstype.INNTEKTSMELDING -> InntektsmeldingMessage(hendelse, aktivitetslogger, aktivitetslogg).asSpeilDTO()
        Meldingstype.PÅMINNELSE -> TODO()
        Meldingstype.YTELSER -> TODO()
        Meldingstype.VILKÅRSGRUNNLAG -> TODO()
        Meldingstype.MANUELL_SAKSBEHANDLING -> TODO()
        Meldingstype.UKJENT -> TODO()
    }
