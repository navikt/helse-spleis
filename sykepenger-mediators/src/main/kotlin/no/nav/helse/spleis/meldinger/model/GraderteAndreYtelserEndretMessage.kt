package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.hendelser.GraderteAndreYtelserEndret
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class GraderteAndreYtelserEndretMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val graderteAndreYtelserEndretFom = packet["graderteAndreYtelserEndretFom"].asLocalDate()

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(
            message = this,
            graderteAndreYtelserEndret = GraderteAndreYtelserEndret(
                meldingsreferanseId = meldingsporing.id,
                graderteAndreYtelserEndretFom = graderteAndreYtelserEndretFom
            ),
            context = context
        )
    }
}
