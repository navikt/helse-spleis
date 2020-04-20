package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Periode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.hendelser.MessageProcessor
import kotlin.math.max

// Understands a JSON message representing a Søknad that is sent to NAV
internal class SendtSøknadNavMessage(packet: JsonMessage) : SøknadMessage(packet) {
    private val søknadTom = packet["tom"].asLocalDate()
    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val sendtNav = packet["sendtNav"].asLocalDateTime()
    private val harAndreInntektskilder = packet["andreInntektskilder"].isArray && !packet["andreInntektskilder"].isEmpty
    private val permittert = packet["permitteringer"].takeIf(JsonNode::isArray)?.takeUnless { it.isEmpty }?.let { true } ?: false
    private val perioder = packet["soknadsperioder"].map {
        Periode.Sykdom(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate(),
            gradFraSykmelding = it.path("sykmeldingsgrad").asInt(),
            faktiskSykdomsgrad = it.path("faktiskGrad").takeIf(JsonNode::isIntegralNumber)?.asInt()?.let {
                max(100 - it, 0)
            }
        )
    } + packet["egenmeldinger"].map {
        Periode.Egenmelding(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate()
        )
    } + packet["fravar"].mapNotNull {
        val fraværstype = it["type"].asText()
        val fom = it.path("fom").asLocalDate()
        when (fraværstype) {
            "UTDANNING_FULLTID", "UTDANNING_DELTID" -> Periode.Utdanning(fom, søknadTom)
            "PERMISJON" -> Periode.Permisjon(fom, it.path("tom").asLocalDate())
            "FERIE" -> Periode.Ferie(fom, it.path("tom").asLocalDate())
            "UTLANDSOPPHOLD" -> Periode.Utlandsopphold(fom, it.path("tom").asLocalDate())
            else -> null // is filtered away in SendtNavSøknader river
        }
    } + (packet["arbeidGjenopptatt"].asOptionalLocalDate()?.let { listOf(Periode.Arbeid(it, søknadTom)) }
        ?: emptyList())

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asSøknad(): Søknad {
        return Søknad(
            meldingsreferanseId = this.id,
            fnr = fødselsnummer,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = perioder,
            harAndreInntektskilder = harAndreInntektskilder,
            sendtTilNAV = sendtNav,
            permittert = permittert
        )
    }
}
