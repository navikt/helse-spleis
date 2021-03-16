package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.SøknadArbeidsgiver.Søknadsperiode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

// Understands a JSON message representing a Søknad that is only sent to the employer
internal class SendtSøknadArbeidsgiverMessage(packet: JsonMessage) : SøknadMessage(packet) {
    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val perioder = packet["soknadsperioder"].map { periode ->
        val arbeidshelse = periode.path("faktiskGrad")
            .takeIf(JsonNode::isIntegralNumber)
            ?.asInt()
            ?.coerceIn(0, 100)
            ?.prosent
        Søknadsperiode(
            fom = periode.path("fom").asLocalDate(),
            tom = periode.path("tom").asLocalDate(),
            sykmeldingsgrad = periode.path("sykmeldingsgrad").asInt().prosent,
            arbeidshelse = arbeidshelse
        )
    }

    private val søknad get() = SøknadArbeidsgiver(
        meldingsreferanseId = this.id,
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = perioder
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, søknad)
    }
}
