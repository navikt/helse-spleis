package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import java.util.*

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(
    originalMessage: String,
    problems: MessageProblems) :
    HendelseMessage(originalMessage, problems) {

    init {
        requireValue("@event_name", "påminnelse")
        requireKey(
            "antallGangerPåminnet",
            "tilstandsendringstidspunkt", "påminnelsestidspunkt",
            "nestePåminnelsestidspunkt", "vedtaksperiodeId",
            "organisasjonsnummer", "fødselsnummer", "aktørId"
        )

        requireAny("tilstand", TilstandType.values().map(Enum<*>::name))
    }

    override val id: UUID = UUID.randomUUID()

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asPåminnelse(aktivitetslogger: Aktivitetslogger, aktivitetslogg: Aktivitetslogg): Påminnelse {
        return Påminnelse(
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            antallGangerPåminnet = this["antallGangerPåminnet"].asInt(),
            tilstand = TilstandType.valueOf(this["tilstand"].asText()),
            tilstandsendringstidspunkt = this["tilstandsendringstidspunkt"].asLocalDateTime(),
            påminnelsestidspunkt = this["påminnelsestidspunkt"].asLocalDateTime(),
            nestePåminnelsestidspunkt = this["nestePåminnelsestidspunkt"].asLocalDateTime(),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )
    }

    object Factory : MessageFactory<PåminnelseMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = PåminnelseMessage(message, problems)
    }
}
