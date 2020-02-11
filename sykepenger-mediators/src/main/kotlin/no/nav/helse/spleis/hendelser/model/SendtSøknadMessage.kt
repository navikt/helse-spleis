package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.asLocalDate
import no.nav.helse.spleis.hendelser.asOptionalLocalDate
import no.nav.helse.spleis.rest.HendelseDTO
import java.time.LocalDateTime

// Understands a JSON message representing a Søknad
internal class SendtSøknadMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    SøknadMessage(originalMessage, aktivitetslogger) {
    init {
        requiredValue("status", "SENDT")
        requiredKey("sendtNav", "fom", "tom", "egenmeldinger", "fravar")
        interestedIn("arbeidGjenopptatt")
        interestedIn("andreInntektskilder")
    }
    val søknadFom get() = this["fom"].asLocalDate()
    val søknadTom get() = this["tom"].asLocalDate()
    val fnr get() = this["fnr"].asText()
    val aktørId get() = this["aktorId"].asText()
    val orgnummer get() = this["arbeidsgiver.orgnummer"].asText()
    private val rapportertdato get() = this["opprettet"].asText().let { LocalDateTime.parse(it) }
    val sendtNav get() = this["sendtNav"].asText().let { LocalDateTime.parse(it) }
    val perioder get() = this["soknadsperioder"].map {
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
            in listOf("UTDANNING_FULLTID", "UTDANNING_DELTID") -> Periode.Utdanning(fom, søknadTom)
            "PERMISJON" -> Periode.Permisjon(fom, it.path("tom").asLocalDate())
            "FERIE" -> Periode.Ferie(fom, it.path("tom").asLocalDate())
            else -> {
                aktivitetslogger.warn("Ukjent fraværstype $fraværstype")
                null
            }
        }
    } + (this["arbeidGjenopptatt"].asOptionalLocalDate()?.let { listOf(Periode.Arbeid(it, søknadTom)) }
    ?: emptyList())

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelSendtSøknad(): ModelSendtSøknad {
        return ModelSendtSøknad(
            hendelseId = this.id,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            sendtNav = sendtNav,
            perioder = perioder,
            harAndreInntektskilder = harAndreInntektskilder(),
            aktivitetslogger = aktivitetslogger
        )
    }

    private fun harAndreInntektskilder() =
        !this.isKeyMissing("andreInntektskilder") && !this["andreInntektskilder"].let { it.isNull || it.isEmpty }

    fun asSpeilDTO(): HendelseDTO = HendelseDTO.SendtSøknadDTO(
        rapportertdato = rapportertdato,
        sendtNav = sendtNav,
        fom = søknadFom,
        tom = søknadTom
    )

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger) =
            SendtSøknadMessage(message, problems)
    }
}
