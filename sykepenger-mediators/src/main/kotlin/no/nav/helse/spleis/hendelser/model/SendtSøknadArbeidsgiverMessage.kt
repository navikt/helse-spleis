package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.SøknadArbeidsgiver.Periode.Sykdom
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Søknad that is only sent to the employer
internal class SendtSøknadArbeidsgiverMessage(packet: JsonMessage) : SøknadMessage(packet) {
    private val aktørId = packet["aktorId"].asText()
    private val orgnummer = packet["arbeidsgiver.orgnummer"].asText()
    private val perioder = packet["soknadsperioder"].map {
        Sykdom(
            fom = it.path("fom").asLocalDate(),
            tom = it.path("tom").asLocalDate(),
            gradFraSykmelding = it.path("sykmeldingsgrad").asInt(),
            faktiskGrad = it.path("faktiskGrad").takeIf(JsonNode::isIntegralNumber)?.asInt()
        )
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asSøknadArbeidsgiver(): SøknadArbeidsgiver {
        return SøknadArbeidsgiver(
            meldingsreferanseId = this.id,
            fnr = fødselsnummer,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = perioder
        )
    }
}
