package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.rest.HendelseDTO.NySøknadDTO
import java.time.LocalDateTime
import java.util.*

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(
    originalMessage: String,
    problems: MessageProblems
) :
    SøknadMessage(originalMessage, problems) {
    init {
        requireValue("status", "NY")
        requireKey("sykmeldingId", "fom", "tom")
    }

    override val id: UUID
        get() = UUID.fromString(this["sykmeldingId"].asText())

    private val fnr get() = this["fnr"].asText()
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
        fnr = fnr,
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
