package no.nav.helse.spleis.meldinger.model

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Ny Søknad
internal class NySøknadMessage(packet: JsonMessage, private val builder: NySøknadBuilder = NySøknadBuilder()) : SøknadMessage(packet, builder) {

    override fun _behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, builder.build())
    }
}
