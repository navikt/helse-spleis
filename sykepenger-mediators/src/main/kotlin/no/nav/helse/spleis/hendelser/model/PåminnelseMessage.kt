package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.ModelPåminnelse
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.asLocalDateTime
import java.util.*

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(originalMessage: String, private val problems: Aktivitetslogger) :
    JsonMessage(originalMessage, problems) {

    private val oldTilstander = mapOf(
        "NY_SØKNAD_MOTTATT" to TilstandType.MOTTATT_NY_SØKNAD,
        "SENDT_SØKNAD_MOTTATT" to TilstandType.AVVENTER_INNTEKTSMELDING,
        "INNTEKTSMELDING_MOTTATT" to TilstandType.AVVENTER_SENDT_SØKNAD,
        "KOMPLETT_SYKDOMSTIDSLINJE" to TilstandType.AVVENTER_HISTORIKK
    )

    init {
        requiredKey(
            "antallGangerPåminnet",
            "tilstandsendringstidspunkt", "påminnelsestidspunkt",
            "nestePåminnelsestidspunkt", "vedtaksperiodeId",
            "organisasjonsnummer", "fødselsnummer", "aktørId"
        )

        requiredValueOneOf("tilstand", TilstandType.values().map(Enum<*>::name) + oldTilstander.keys)
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    internal fun asModelPåminnelse(): ModelPåminnelse {
        return ModelPåminnelse(
            UUID.randomUUID(),
            this["aktørId"].asText(),
            this["fødselsnummer"].asText(),
            this["organisasjonsnummer"].asText(),
            this["vedtaksperiodeId"].asText(),
            this["antallGangerPåminnet"].asInt(),
            this["tilstand"].asText().let { oldTilstander[it] ?: TilstandType.valueOf(it) },
            this["tilstandsendringstidspunkt"].asLocalDateTime(),
            this["påminnelsestidspunkt"].asLocalDateTime(),
            this["nestePåminnelsestidspunkt"].asLocalDateTime(),
            problems
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger) =
            PåminnelseMessage(message, problems)
    }
}
