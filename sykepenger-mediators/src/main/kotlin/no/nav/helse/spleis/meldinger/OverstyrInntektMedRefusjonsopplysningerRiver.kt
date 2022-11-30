package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.OverstyrInntektMedRefusjonsopplysningerMessage

internal class OverstyrInntektMedRefusjonsopplysningerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "overstyr_inntekt"
    override val riverName = "Overstyr inntekt"

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer", "månedligInntekt", "skjæringstidspunkt", "forklaring")
        message.interestedIn("subsumsjon") { require(it.path("paragraf").isTextual) }
        message.interestedIn("subsumsjon.paragraf", "subsumsjon.ledd", "subsumsjon.bokstav")
        message.requireArray("refusjonsopplysninger") {
            require("fom", JsonNode::asLocalDate)
            interestedIn("tom", JsonNode::asLocalDate)
            requireKey("beløp")
        }
    }

    override fun createMessage(packet: JsonMessage) = OverstyrInntektMedRefusjonsopplysningerMessage(packet)
}
