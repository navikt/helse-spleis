package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spleis.IHendelseMediator

internal class MinimumSykdomsgradVurdertMessage(packet: JsonMessage) : HendelseMessage(packet) {
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val aktørId: String = packet["aktørId"].asText()
    private val perioderMedMinimumSykdomsgradVurdertOK = packet["perioder_med_minimum_sykdomsgrad_vurdert_ok"].map(::asPeriode)
    private val perioderMedMinimumSykdomsgradVurdertIkkeOK = packet["perioder_med_minimum_sykdomsgrad_vurdert_ikke_ok"].map(::asPeriode)

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(
            this,
            MinimumSykdomsgradsvurderingMelding(
                perioderMedMinimumSykdomsgradVurdertOK = perioderMedMinimumSykdomsgradVurdertOK.toSet(),
                perioderMedMinimumSykdomsgradVurdertIkkeOK = perioderMedMinimumSykdomsgradVurdertIkkeOK.toSet(),
                meldingsreferanseId = this.id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId
            ),
            context
        )
    }

}