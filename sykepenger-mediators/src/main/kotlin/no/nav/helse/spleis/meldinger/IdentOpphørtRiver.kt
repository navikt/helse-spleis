package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.IdentOpphørtMessage

internal class IdentOpphørtRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "ident_opphørt"
    override val riverName = "Ident opphørt"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "nye_identer.fødselsnummer")
        message.requireArray("gamle_identer") {
            requireAny("type", listOf("FØDSELSNUMMER", "AKTØRID", "NPID"))
            requireKey("ident")
        }
    }

    override fun createMessage(packet: JsonMessage) =
        IdentOpphørtMessage(
            packet,
            Meldingsporing(
                id = packet["@id"].asText().toUUID(),
                fødselsnummer = packet["fødselsnummer"].asText()
            )
        )
}
