package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.Personopplysninger

internal class SendtSøknadFrilansMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing, private val builder: SendtSøknadBuilder = SendtSøknadBuilder(packet["arbeidssituasjon"].asText())) : SøknadMessage(packet, builder.frilans()) {
    override fun _behandle(mediator: IHendelseMediator, personopplysninger: Personopplysninger, packet: JsonMessage, context: BehandlingContext) {
        builder.sendt(packet["sendtNav"].asLocalDateTime())
        SendtSøknadNavMessage.byggSendtSøknad(builder, packet)
        mediator.behandle(personopplysninger, this, builder.build(meldingsporing), context, packet["historiskeFolkeregisteridenter"].map(JsonNode::asText).map { Personidentifikator(it) }.toSet())
    }
}
