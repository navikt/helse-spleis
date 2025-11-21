package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.til
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class AvbruttSøknadMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing, val behandlingsporing: Behandlingsporing.Yrkesaktivitet) : HendelseMessage(packet) {

    private val periode = packet["fom"].asLocalDate() til packet["tom"].asLocalDate()

    private val avbruttSøknad
        get() = AvbruttSøknad(
            meldingsreferanseId = meldingsporing.id,
            behandlingsporing = behandlingsporing,
            periode = periode
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, avbruttSøknad, context)
    }
}
