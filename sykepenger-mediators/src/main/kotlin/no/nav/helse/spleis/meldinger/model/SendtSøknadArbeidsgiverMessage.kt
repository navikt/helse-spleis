package no.nav.helse.spleis.meldinger.model

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Søknad that is only sent to the employer
internal class SendtSøknadArbeidsgiverMessage(private val packet: JsonMessage, private val builder: SendtSøknadBuilder = SendtSøknadBuilder()) : SøknadMessage(packet, builder) {

    override fun _behandle(mediator: IHendelseMediator, packet: JsonMessage) {
        builder.sendt(packet["sendtArbeidsgiver"].asLocalDateTime())
        SendtSøknadNavMessage.byggSendtSøknad(builder, packet)
        mediator.behandle(this, builder.build())
    }
}
