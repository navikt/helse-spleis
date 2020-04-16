package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.SøknadArbeidsgiver.Periode.Sykdom
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Søknad that is only sent to the employer
internal class SendtSøknadArbeidsgiverMessage(originalMessage: String, problems: MessageProblems) :
    SøknadMessage(originalMessage, problems) {
    init {
        requireValue("@event_name", "sendt_søknad_arbeidsgiver")
        requireValue("status", "SENDT")
        requireKey("id", "egenmeldinger", "fravar")
        require("fom", JsonNode::asLocalDate)
        require("tom", JsonNode::asLocalDate)
        require("sendtArbeidsgiver", JsonNode::asLocalDateTime)
        forbid("sendtNav")
    }

    private val aktørId get() = this["aktorId"].asText()
    private val orgnummer get() = this["arbeidsgiver.orgnummer"].asText()
    private val perioder get() = this["soknadsperioder"].map {
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

    object Factory : MessageFactory<SendtSøknadArbeidsgiverMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = SendtSøknadArbeidsgiverMessage(message, problems)
    }
}
