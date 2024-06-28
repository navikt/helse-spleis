package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.DumpVedtaksperioder
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator

internal class DumpVedtaksperioderMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()

    private val dump
        get() = DumpVedtaksperioder(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, dump, context)
    }
}
