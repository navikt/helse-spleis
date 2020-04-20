package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val antallGangerPåminnet = packet["antallGangerPåminnet"].asInt()
    private val tilstand = TilstandType.valueOf(packet["tilstand"].asText())
    private val tilstandsendringstidspunkt = packet["tilstandsendringstidspunkt"].asLocalDateTime()
    private val påminnelsestidspunkt = packet["påminnelsestidspunkt"].asLocalDateTime()
    private val nestePåminnelsestidspunkt = packet["nestePåminnelsestidspunkt"].asLocalDateTime()

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asPåminnelse(): Påminnelse {
        return Påminnelse(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            antallGangerPåminnet = antallGangerPåminnet,
            tilstand = tilstand,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            påminnelsestidspunkt = påminnelsestidspunkt,
            nestePåminnelsestidspunkt = nestePåminnelsestidspunkt
        )
    }
}
