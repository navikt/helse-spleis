package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.Personopplysninger

// Understands a JSON message representing a Søknad that is only sent to the employer
internal class SendtSøknadArbeidsgiverMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
    private val builder: SendtSøknadBuilder = SendtSøknadBuilder(),
) : SøknadMessage(packet, builder) {

    override fun _behandle(
        mediator: IHendelseMediator,
        personopplysninger: Personopplysninger,
        packet: JsonMessage,
        context: MessageContext,
    ) {
        builder.sendt(packet["sendtArbeidsgiver"].asLocalDateTime())
        SendtSøknadNavMessage.byggSendtSøknad(builder, packet)
        mediator.behandle(
            personopplysninger,
            this,
            builder.build(meldingsporing),
            context,
            packet["historiskeFolkeregisteridenter"]
                .map(JsonNode::asText)
                .map { Personidentifikator(it) }
                .toSet(),
        )
    }
}
