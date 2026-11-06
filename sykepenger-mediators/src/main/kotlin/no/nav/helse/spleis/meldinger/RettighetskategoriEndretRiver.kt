package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.RettighetskategoriEndretMessage
import tools.jackson.databind.JsonNode
import java.util.UUID

internal class RettighetskategoriEndretRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator,
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventNames = setOf("rettighetskategori_endret")
    override val riverName = "Rettighetskategori endret"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer")
        message.require("skjæringstidspunkt", JsonNode::asLocalDate)
        message.require("rettighetskategoriId") { UUID.fromString(it.asText()) }
        message.require("hovedkategori") { Hovedkategori.valueOf(it.asText()) }
    }

    override fun createMessage(packet: JsonMessage) =
        RettighetskategoriEndretMessage(
            packet = packet,
            meldingsporing =
                Meldingsporing(
                    id = packet.meldingsreferanseId(),
                    fødselsnummer = packet["fødselsnummer"].asText(),
                ),
        )
}
