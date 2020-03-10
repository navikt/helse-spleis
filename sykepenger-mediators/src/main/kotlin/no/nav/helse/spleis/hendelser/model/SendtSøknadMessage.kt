package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Periode
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.rest.HendelseDTO
import java.time.LocalDateTime
import java.util.*

// Understands a JSON message representing a Søknad
internal class SendtSøknadMessage(originalMessage: String, private val problems: MessageProblems) :
    SøknadMessage(originalMessage, problems) {
    init {
        requireValue("status", "SENDT")
        requireKey("id", "sendtNav", "fom", "tom", "egenmeldinger", "fravar")
        interestedIn("arbeidGjenopptatt")
        interestedIn("andreInntektskilder")
    }

    override val id: UUID
        get() = UUID.fromString(this["id"].asText())

    private val søknadFom get() = this["fom"].asLocalDate()
    private val søknadTom get() = this["tom"].asLocalDate()
    private val fnr get() = this["fnr"].asText()
    private val aktørId get() = this["aktorId"].asText()
    private val orgnummer get() = this["arbeidsgiver.orgnummer"].asText()
    private val rapportertdato get() = this["opprettet"].asText().let { LocalDateTime.parse(it) }
    private val sendtNav get() = this["sendtNav"].asText().let { LocalDateTime.parse(it) }
    private val perioder get() = this["soknadsperioder"].map {
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
    } + this["fravar"].map {
        val fraværstype = it["type"].asText()
        val fom = it.path("fom").asLocalDate()
        when (fraværstype) {
            in listOf("UTDANNING_FULLTID", "UTDANNING_DELTID") -> Periode.Utdanning(fom, søknadTom)
            "PERMISJON" -> Periode.Permisjon(fom, it.path("tom").asLocalDate())
            "FERIE" -> Periode.Ferie(fom, it.path("tom").asLocalDate())
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
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = perioder,
            harAndreInntektskilder = harAndreInntektskilder(),
            rapportertdato = rapportertdato
        )
    }

    private fun harAndreInntektskilder() = this["andreInntektskilder"].isArray && !this["andreInntektskilder"].isEmpty

    fun asSpeilDTO(): HendelseDTO = HendelseDTO.SendtSøknadDTO(
        rapportertdato = rapportertdato,
        sendtNav = sendtNav,
        fom = søknadFom,
        tom = søknadTom
    )

    object Factory : MessageFactory<SendtSøknadMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = SendtSøknadMessage(message, problems)
    }
}
