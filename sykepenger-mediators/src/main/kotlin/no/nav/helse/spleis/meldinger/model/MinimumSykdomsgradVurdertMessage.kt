package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spleis.IHendelseMediator

internal class MinimumSykdomsgradVurdertMessage(packet: JsonMessage) : HendelseMessage(packet) {
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val harTaptTilstrekkeligArbeidstid = packet["har_tapt_tilstrekkelig_arbeidstid"].map(::asPeriode)
    private val harIkkeTaptTilstrekkeligArbeidstid = packet["har_ikke_tapt_tilstrekkelig_arbeidstid"].map(::asPeriode)

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(
            this,
            MinimumSykdomsgradsvurderingMelding(
                harTaptTilstrekkeligArbeidstid.toSet(),
                harIkkeTaptTilstrekkeligArbeidstid.toSet()
            ),
            context
        )
    }

}