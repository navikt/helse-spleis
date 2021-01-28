package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

// Understands a JSON message representing a Søknad that is sent to NAV
internal class SendtSøknadNavMessage(packet: MessageDelegate) : SøknadMessage(packet) {
    private val søknadTom = packet["tom"].asLocalDate()
    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val sendtNav = packet["sendtNav"].asLocalDateTime()
    private val andreInntektskilder = packet["andreInntektskilder"].map {
        Søknad.Inntektskilde(
            sykmeldt = it["sykmeldt"].asBoolean(),
            type = it["type"].asText()
        )
    }
    private val permittert = packet["permitteringer"].takeIf(JsonNode::isArray)?.takeUnless { it.isEmpty }?.let { true } ?: false
    private val papirsykmeldinger = packet["papirsykmeldinger"].map {
        Søknadsperiode.Papirsykmelding(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate()
        )
    }
    private val søknadsperioder = packet["soknadsperioder"].map { periode ->
        val arbeidshelse = periode.path("faktiskGrad")
            .takeIf(JsonNode::isIntegralNumber)
            ?.asInt()
            ?.coerceIn(0, 100)
            ?.prosent
        Søknadsperiode.Sykdom(
            fom = periode.path("fom").asLocalDate(),
            tom = periode.path("tom").asLocalDate(),
            sykmeldingsgrad = periode.path("sykmeldingsgrad").asInt().prosent,
            arbeidshelse = arbeidshelse
        )
    }
    private val egenmeldinger = packet["egenmeldinger"].map {
        Søknadsperiode.Egenmelding(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate()
        )
    }
    private val fraværsperioder = packet["fravar"].mapNotNull {
        val fraværstype = it["type"].asText()
        val fom = it.path("fom").asLocalDate()
        when (fraværstype) {
            "UTDANNING_FULLTID", "UTDANNING_DELTID" -> Søknadsperiode.Utdanning(fom, søknadTom)
            "PERMISJON" -> Søknadsperiode.Permisjon(fom, it.path("tom").asLocalDate())
            "FERIE" -> Søknadsperiode.Ferie(fom, it.path("tom").asLocalDate())
            "UTLANDSOPPHOLD" -> Søknadsperiode.Utlandsopphold(fom, it.path("tom").asLocalDate())
            else -> null // is filtered away in SendtNavSøknader river
        }
    }
    private val arbeidGjenopptatt = packet["arbeidGjenopptatt"].asOptionalLocalDate()?.let { listOf(Søknadsperiode.Arbeid(it, søknadTom)) } ?: emptyList()
    private val perioder = søknadsperioder + papirsykmeldinger + egenmeldinger + fraværsperioder + arbeidGjenopptatt

    private val søknad get() = Søknad(
        meldingsreferanseId = this.id,
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = perioder,
        andreInntektskilder = andreInntektskilder,
        sendtTilNAV = sendtNav,
        permittert = permittert
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, søknad)
    }
}
