package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.SøknadArbeidsgiver.Søknadsperiode
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate
import kotlin.math.max

// Understands a JSON message representing a Søknad that is only sent to the employer
internal class SendtSøknadArbeidsgiverMessage(packet: MessageDelegate) : SøknadMessage(packet) {
    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val perioder = packet["soknadsperioder"].map {
        Søknadsperiode(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate(),
            gradFraSykmelding = it.path("sykmeldingsgrad").asInt(),
            faktiskGrad = it.path("faktiskGrad").takeIf(JsonNode::isIntegralNumber)?.asInt()?.let {
                max(100 - it, 0)
            }
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
