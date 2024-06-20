package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.MinimumSykdomsgradVurdertMessage

internal class MinimumSykdomsgradVurdertRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "minimum_sykdomsgrad_vurdert"
    override val riverName = "minimum_sykdomsgrad_vurdert"

    override fun validate(message: JsonMessage) {
        message.requireKey("@id", "fødselsnummer", "aktørId")
        message.requireArray("har_tapt_tilstrekkelig_arbeidstid")
        message.requireArray("har_ikke_tapt_tilstrekkelig_arbeidstid")
    }

    override fun createMessage(packet: JsonMessage) = MinimumSykdomsgradVurdertMessage(packet)
}

