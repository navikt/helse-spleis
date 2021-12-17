package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.SøknadArbeidsgiver.Sykdom
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

// Understands a JSON message representing a Søknad that is only sent to the employer
internal class SendtSøknadArbeidsgiverMessage(packet: JsonMessage) : SøknadMessage(packet, NySøknadBuilder()) {
    private val søknadTom = packet["tom"].asLocalDate()
    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val sykdomsperioder = packet["soknadsperioder"].map { periode ->
        val arbeidshelse = periode.path("faktiskGrad")
            .takeIf(JsonNode::isIntegralNumber)
            ?.asInt()
            ?.coerceIn(0, 100)
            ?.prosent
        Sykdom(
            fom = periode.path("fom").asLocalDate(),
            tom = periode.path("tom").asLocalDate(),
            sykmeldingsgrad = periode.path("sykmeldingsgrad").asInt().prosent,
            arbeidshelse = arbeidshelse
        )
    }
    private val arbeidsperiode = packet["arbeidGjenopptatt"].asOptionalLocalDate()?.let { listOf(SøknadArbeidsgiver.Arbeid(it, søknadTom)) } ?: emptyList()

    private val søknad get() = SøknadArbeidsgiver(
        meldingsreferanseId = this.id,
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykdomsperioder = sykdomsperioder,
        arbeidsperiode = arbeidsperiode,
        sykmeldingSkrevet = sykmeldingSkrevet
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, søknad)
    }
}
