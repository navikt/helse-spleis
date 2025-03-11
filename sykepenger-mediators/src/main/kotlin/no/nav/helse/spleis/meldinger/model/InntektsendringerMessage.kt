package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Inntektsendringer
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class InntektsendringerMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val inntektsendringFom = packet["inntektsendringFom"].asLocalDate()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(
            message = this,
            inntektsendringer = Inntektsendringer(
                meldingsreferanseId = meldingsporing.id,
                inntektsendringFom = inntektsendringFom
            ),
            context = context
        )
    }
}
