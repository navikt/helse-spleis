package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.rest.HendelseDTO.NySøknadDTO
import java.time.LocalDateTime

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(
    originalMessage: String,
    problems: MessageProblems
) :
    SøknadMessage(originalMessage, problems) {
    init {
        requireValue("@event_name", "ny_søknad")
        requireValue("status", "NY")
        requireKey("sykmeldingId")
        require("fom", JsonNode::asLocalDate)
        require("tom", JsonNode::asLocalDate)
    }

    private val aktørId get() = this["aktorId"].asText()
    private val orgnummer get() = this["arbeidsgiver.orgnummer"].asText()
    private val søknadFom get() = this["fom"].asLocalDate()
    private val søknadTom get() = this["tom"].asLocalDate()
    private val rapportertdato get() = this["opprettet"].asText().let { LocalDateTime.parse(it) }
    private val sykeperioder
        get() = this["soknadsperioder"].map {
            Triple(
                first = it.path("fom").asLocalDate(),
                second = it.path("tom").asLocalDate(),
                third = it.path("sykmeldingsgrad").asInt()
            )
        }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asSykmelding() = Sykmelding(
        meldingsreferanseId = this.id,
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = sykeperioder
    )

    internal fun asSpeilDTO() = NySøknadDTO(
        rapportertdato = rapportertdato,
        fom = søknadFom,
        tom = søknadTom
    )

    object Factory : MessageFactory<NySøknadMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = NySøknadMessage(message, problems)
    }
}
