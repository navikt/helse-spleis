package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

// Understands a JSON message representing a Påminnelse
internal class PersonPåminnelseMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val påminnelse
        get() = PersonPåminnelse(meldingsreferanseId = meldingsporing.id)

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(this, påminnelse, context)
    }
}
