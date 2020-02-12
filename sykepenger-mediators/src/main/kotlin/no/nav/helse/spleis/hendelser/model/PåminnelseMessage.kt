package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.asLocalDateTime

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(originalMessage: String, private val problems: Aktivitetslogger) :
    JsonMessage(originalMessage, problems) {

    private val oldTilstander = mapOf(
        "NY_SØKNAD_MOTTATT" to TilstandType.MOTTATT_NY_SØKNAD,
        "MOTTATT_SENDT_SØKNAD" to TilstandType.AVVENTER_INNTEKTSMELDING,
        "MOTTATT_INNTEKTSMELDING" to TilstandType.AVVENTER_SENDT_SØKNAD,
        "BEREGN_UTBETALING" to TilstandType.AVVENTER_HISTORIKK,
        "VILKÅRSPRØVING" to TilstandType.AVVENTER_VILKÅRSPRØVING,
        "TIL_GODKJENNING" to TilstandType.AVVENTER_GODKJENNING
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

    internal fun asPåminnelse(): Påminnelse {
        return Påminnelse(
            hendelseId = this.id,
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            antallGangerPåminnet = this["antallGangerPåminnet"].asInt(),
            tilstand = this["tilstand"].asText().let { oldTilstander[it] ?: TilstandType.valueOf(it) },
            tilstandsendringstidspunkt = this["tilstandsendringstidspunkt"].asLocalDateTime(),
            påminnelsestidspunkt = this["påminnelsestidspunkt"].asLocalDateTime(),
            nestePåminnelsestidspunkt = this["nestePåminnelsestidspunkt"].asLocalDateTime(),
            aktivitetslogger = problems
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger) =
            PåminnelseMessage(message, problems)
    }
}
