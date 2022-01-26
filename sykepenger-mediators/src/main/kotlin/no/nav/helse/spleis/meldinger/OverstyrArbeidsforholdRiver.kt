package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsforholdMessage

internal class OverstyrArbeidsforholdRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
): HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "overstyr_arbeidsforhold"

    override val riverName = "Overstyr arbeidsforhold"

    override fun createMessage(packet: JsonMessage) = OverstyrArbeidsforholdMessage(packet)

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer")
        message.require("skjæringstidspunkt", JsonNode::asLocalDate)
        message.requireArray("overstyrteArbeidsforhold") {
            requireKey("orgnummer")
            require("erAktivt", JsonNode::asBoolean)
        }
        message.require("overstyrteArbeidsforhold") { require(!it.isEmpty) }
    }
}
