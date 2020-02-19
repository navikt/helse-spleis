package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.NySøknad
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.asLocalDate
import no.nav.helse.spleis.rest.HendelseDTO.NySøknadDTO
import java.time.LocalDateTime

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(
    originalMessage: String,
    private val aktivitetslogger: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) :
    SøknadMessage(originalMessage, aktivitetslogger, aktivitetslogg) {
    init {
        requiredValue("status", "NY")
        requiredKey("fom", "tom")
    }

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
        processor.process(this, aktivitetslogger)
    }

    internal fun asNySøknad() = NySøknad(
        meldingsreferanseId = this.id,
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = sykeperioder,
        aktivitetslogger = aktivitetslogger,
        aktivitetslogg = aktivitetslogg
    )

    internal fun asSpeilDTO() = NySøknadDTO(
        rapportertdato = rapportertdato,
        fom = søknadFom,
        tom = søknadTom
    )

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger, aktivitetslogg: Aktivitetslogg) =
            NySøknadMessage(message, problems, aktivitetslogg)
    }
}
