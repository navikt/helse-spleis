package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.hendelser.GraderteAndreYtelserEndringer
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class GraderteAndreYtelserEndringerMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val graderteAndreYtelserEndringerFom = packet["graderteAndreYtelserEndringerFom"].asLocalDate()

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(
            message = this,
            graderteAndreYtelserEndringer = GraderteAndreYtelserEndringer(
                meldingsreferanseId = meldingsporing.id,
                graderteAndreYtelserEndringFom = graderteAndreYtelserEndringerFom
            ),
            context = context
        )
    }
}
