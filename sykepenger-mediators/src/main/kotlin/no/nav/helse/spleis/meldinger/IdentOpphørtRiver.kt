package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.IdentOpphørtMessage

internal class IdentOpphørtRiver (
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "ident_opphørt"
    override val riverName = "Ident opphørt"


    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer", "nye_identer.fødselsnummer", "nye_identer.aktørId")
        message.requireArray("gamle_identer") {
            requireAny("type", listOf("FØDSELSNUMMER", "AKTØRID", "NPID"))
            requireKey("ident")
        }
    }

    override fun createMessage(packet: JsonMessage) = IdentOpphørtMessage(packet)
}