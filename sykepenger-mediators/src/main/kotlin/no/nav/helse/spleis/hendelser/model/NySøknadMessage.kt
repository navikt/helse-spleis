package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.asLocalDate
import java.time.LocalDateTime
import java.util.*

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    SøknadMessage(originalMessage, aktivitetslogger) {
    init {
        requiredValue("status", "NY")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelNySøknad() = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = this["fnr"].asText(),
        aktørId = this["aktorId"].asText(),
        orgnummer = this["arbeidsgiver.orgnummer"].asText(),
        rapportertdato = this["opprettet"].asText().let { LocalDateTime.parse(it) },
        sykeperioder = this["soknadsperioder"].map {
            Triple(
                first = it.path("fom").asLocalDate(),
                second = it.path("tom").asLocalDate(),
                third = it.path("sykmeldingsgrad").asInt()
            )
        },
        aktivitetslogger = aktivitetslogger,
        originalJson = this.toJson()
    )

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger) =
            NySøknadMessage(message, problems)
    }
}
