package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.asLocalDate
import no.nav.helse.spleis.hendelser.asOptionalLocalDate
import java.time.LocalDateTime
import java.util.*

// Understands a JSON message representing a Søknad
internal class SendtSøknadMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    SøknadMessage(originalMessage, aktivitetslogger) {
    init {
        requiredValue("status", "SENDT")
        requiredKey("sendtNav", "fom", "tom", "egenmeldinger", "fravar")
        interestedIn("arbeidGjenopptatt")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelSendtSøknad(): ModelSendtSøknad {
        val søknadFom = this["fom"].asLocalDate()
        val søknadTom = this["tom"].asLocalDate()
        return ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = this["fnr"].asText(),
            aktørId = this["aktorId"].asText(),
            orgnummer = this["arbeidsgiver.orgnummer"].asText(),
            rapportertdato = this["opprettet"].asText().let { LocalDateTime.parse(it) },
            perioder = this["soknadsperioder"].map {
                Periode.Sykdom(
                    fom = it.path("fom").asLocalDate(),
                    tom = it.path("tom").asLocalDate(),
                    grad = it.path("sykmeldingsgrad").asInt(),
                    faktiskGrad = it.path("faktiskGrad").asDouble(it.path("sykmeldingsgrad").asDouble())
                )
            } + this["egenmeldinger"].map {
                Periode.Egenmelding(
                    fom = it.path("fom").asLocalDate(),
                    tom = it.path("tom").asLocalDate()
                )
            } + this["fravar"].mapNotNull {
                val fraværstype = it["type"].asText()
                val fom = it.path("fom").asLocalDate()
                when (fraværstype) {
                    in listOf("UTDANNING_FULLTID", "UTDANNING_DELTID") -> Periode.Utdanning(søknadFom, søknadTom, fom)
                    "PERMISJON" -> Periode.Permisjon(fom, it.path("tom").asLocalDate())
                    "FERIE" -> Periode.Ferie(fom, it.path("tom").asLocalDate())
                    else -> {
                        aktivitetslogger.warn("Ukjent fraværstype $fraværstype")
                        null
                    }
                }
            } + (this["arbeidGjenopptatt"].asOptionalLocalDate()?.let { listOf(Periode.Arbeid(it, søknadTom)) }
                ?: emptyList()),
            aktivitetslogger = aktivitetslogger,
            originalJson = this.toJson()
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger) =
            SendtSøknadMessage(message, problems)
    }
}
