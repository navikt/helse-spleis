package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class MinimumSykdomsgradVurdertMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
) : HendelseMessage(packet) {
    private val perioderMedMinimumSykdomsgradVurdertOK = packet["perioderMedMinimumSykdomsgradVurdertOk"].map(::asPeriode)
    private val perioderMedMinimumSykdomsgradVurdertIkkeOK = packet["perioderMedMinimumSykdomsgradVurdertIkkeOk"].map(::asPeriode)

    override fun behandle(
        mediator: IHendelseMediator,
        context: MessageContext,
    ) {
        mediator.behandle(
            this,
            MinimumSykdomsgradsvurderingMelding(
                perioderMedMinimumSykdomsgradVurdertOK = perioderMedMinimumSykdomsgradVurdertOK.toSet(),
                perioderMedMinimumSykdomsgradVurdertIkkeOK = perioderMedMinimumSykdomsgradVurdertIkkeOK.toSet(),
                meldingsreferanseId = meldingsporing.id,
            ),
            context,
        )
    }
}
