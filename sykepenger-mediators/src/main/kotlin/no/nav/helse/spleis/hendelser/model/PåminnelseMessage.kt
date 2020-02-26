package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.asLocalDateTime
import java.util.*

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(
    originalMessage: String,
    private val problems: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) :
    JsonMessage(originalMessage, problems, aktivitetslogg) {

    init {
        requiredValue("@event_name", "påminnelse")
        requiredKey(
            "antallGangerPåminnet",
            "tilstandsendringstidspunkt", "påminnelsestidspunkt",
            "nestePåminnelsestidspunkt", "vedtaksperiodeId",
            "organisasjonsnummer", "fødselsnummer", "aktørId"
        )

        requiredValueOneOf("tilstand", TilstandType.values().map(Enum<*>::name))
    }

    override val id: UUID = UUID.randomUUID()

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asPåminnelse(): Påminnelse {
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
            aktivitetslogg = aktivitetslogg
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger, aktivitetslogg: Aktivitetslogg) =
            PåminnelseMessage(message, problems, aktivitetslogg)
    }
}
