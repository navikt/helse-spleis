package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.hendelser.Migrate
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class MigrateMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val migrate
        get() = Migrate(
            meldingsreferanseId = meldingsporing.id
        )

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(this, migrate, context)
    }
}
