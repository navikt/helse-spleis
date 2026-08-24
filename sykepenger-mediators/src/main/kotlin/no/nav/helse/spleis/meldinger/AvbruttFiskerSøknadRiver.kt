package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.AvbruttSøknadMessage

internal class AvbruttFiskerSøknadRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventNames = setOf("avbrutt_fisker_søknad")
    override val riverName = "Avbrutt fisker søknad"

    override fun validate(message: JsonMessage) {
        message.requireKey("@id", "fnr")
        message.require("fom", JsonNode::asLocalDate)
        message.require("tom", JsonNode::asLocalDate)
    }

    override fun createMessage(packet: JsonMessage) = AvbruttSøknadMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fnr"].asText()
        ),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig
    )
}
