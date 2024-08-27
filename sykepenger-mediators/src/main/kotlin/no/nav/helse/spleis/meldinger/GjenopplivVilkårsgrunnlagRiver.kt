package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.util.UUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.GjenopplivVilkårsgrunnlagMessage

internal class GjenopplivVilkårsgrunnlagRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "gjenoppliv_vilkårsgrunnlag"
    override val riverName = "gjenoppliv_vilkårsgrunnlag"

    override fun validate(message: JsonMessage) {
        message.requireKey(
            "@id",
            "aktørId",
            "fødselsnummer"
        )
        message.require("vilkårsgrunnlagId") { UUID.fromString(it.asText()) }
        message.interestedIn("nyttSkjæringstidspunkt", JsonNode::asLocalDate)
        message.interestedIn("arbeidsgivere") {
            message.requireArray("arbeidsgivere") {
                require("organisasjonsnummer") { it.isTextual }
                require("månedligInntekt") { it.isNumber }
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = GjenopplivVilkårsgrunnlagMessage(packet)


}
