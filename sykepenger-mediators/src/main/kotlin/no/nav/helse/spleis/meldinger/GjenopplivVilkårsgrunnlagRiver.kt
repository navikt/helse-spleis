package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
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
            "fødselsnummer",
            "vilkårsgrunnlagId"
        )
        message.interestedIn("nyttSkjæringstidspunkt")
    }

    override fun createMessage(packet: JsonMessage) = GjenopplivVilkårsgrunnlagMessage(packet)
}
