package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class DødsmeldingMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val dødsdato = packet["dødsdato"].asLocalDate()

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(
            this, Dødsmelding(
            meldingsreferanseId = meldingsporing.id,
            dødsdato = dødsdato
        ), context
        )
    }

}
