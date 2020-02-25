package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.asLocalDate
import no.nav.helse.spleis.hendelser.asOptionalLocalDate
import no.nav.helse.spleis.rest.HendelseDTO
import java.time.LocalDateTime
import java.util.*

// Understands a JSON message representing a Søknad
internal class SendtSøknadMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger, private val aktivitetslogg: Aktivitetslogg) :
    SøknadMessage(originalMessage, aktivitetslogger, aktivitetslogg) {
    init {
        requiredValue("status", "SENDT")
        requiredKey("id", "sendtNav", "fom", "tom", "egenmeldinger", "fravar")
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
            else -> {
                aktivitetslogger.severeOld("Ukjent fraværstype $fraværstype")
                aktivitetslogg.severe("Ukjent fraværstype $fraværstype")
            }
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
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
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

        override fun createMessage(message: String, problems: Aktivitetslogger, aktivitetslogg: Aktivitetslogg) =
            SendtSøknadMessage(message, problems, aktivitetslogg)
    }
}
