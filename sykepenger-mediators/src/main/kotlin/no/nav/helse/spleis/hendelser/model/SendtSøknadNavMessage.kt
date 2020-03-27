package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Periode
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Søknad that is sent to NAV
internal class SendtSøknadNavMessage(originalMessage: String, private val problems: MessageProblems) :
    SøknadMessage(originalMessage, problems) {
    init {
        requireValue("@event_name", "sendt_søknad_nav")
        requireValue("status", "SENDT")
        requireKey("id", "egenmeldinger", "fravar")
        require("fom", JsonNode::asLocalDate)
        require("tom", JsonNode::asLocalDate)
        require("sendtNav", JsonNode::asLocalDateTime)
        interestedIn("arbeidGjenopptatt")
        interestedIn("andreInntektskilder")
    }

    private val søknadTom get() = this["tom"].asLocalDate()
    private val aktørId get() = this["aktorId"].asText()
    private val orgnummer get() = this["arbeidsgiver.orgnummer"].asText()
    private val sendtNav get() = this["sendtNav"].asLocalDateTime()
    private val perioder get() = this["soknadsperioder"].map {
        Periode.Sykdom(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate(),
            gradFraSykmelding = it.path("sykmeldingsgrad").asInt(),
            faktiskGrad = it.path("faktiskGrad").takeIf(JsonNode::isIntegralNumber)?.asInt()
        )
    } + this["egenmeldinger"].map {
        Periode.Egenmelding(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate()
        )
    } + this["fravar"].map {
        val fraværstype = it["type"].asText()
        val fom = it.path("fom").asLocalDate()
        when (fraværstype) {
            in listOf("UTDANNING_FULLTID", "UTDANNING_DELTID") -> Periode.Utdanning(fom, søknadTom)
            "PERMISJON" -> Periode.Permisjon(fom, it.path("tom").asLocalDate())
            "FERIE" -> Periode.Ferie(fom, it.path("tom").asLocalDate())
            "UTLANDSOPPHOLD" -> Periode.Utlandsopphold(fom, it.path("tom").asLocalDate())
            else -> problems.severe("Ukjent fraværstype $fraværstype")
        }
    } + (this["arbeidGjenopptatt"].asOptionalLocalDate()?.let { listOf(Periode.Arbeid(it, søknadTom)) }
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
            harAndreInntektskilder = harAndreInntektskilder(),
            sendtTilNAV = sendtNav
        )
    }

    private fun harAndreInntektskilder() = this["andreInntektskilder"].isArray && !this["andreInntektskilder"].isEmpty

    object Factory : MessageFactory<SendtSøknadNavMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = SendtSøknadNavMessage(message, problems)
    }
}
