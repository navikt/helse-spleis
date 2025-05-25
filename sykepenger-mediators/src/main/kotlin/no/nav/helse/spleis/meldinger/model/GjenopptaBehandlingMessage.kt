package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.GjenopptaBehandling
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

// Understands a JSON message representing a Påminnelse
internal class GjenopptaBehandlingMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, GjenopptaBehandling(meldingsreferanseId = meldingsporing.id), context)
    }
}
