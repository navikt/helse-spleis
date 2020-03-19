package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.ManuellSaksbehandling
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Manuell saksbehandling-behov
internal class ManuellSaksbehandlingMessage(originalMessage: String, problems: MessageProblems) : BehovMessage(originalMessage, problems) {
    init {
        requireAll("@behov", Godkjenning)
        requireKey("@løsning.${Godkjenning.name}.godkjent")
        requireKey("saksbehandlerIdent")
        interestedIn("godkjenttidspunkt")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asManuellSaksbehandling() =
        ManuellSaksbehandling(
            aktørId = this["aktørId"].asText(),
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            saksbehandler = this["saksbehandlerIdent"].asText(),
            godkjenttidspunkt = this["godkjenttidspunkt"].asLocalDateTime(),
            utbetalingGodkjent = this["@løsning.${Godkjenning.name}.godkjent"].asBoolean()
        )

    object Factory : MessageFactory<ManuellSaksbehandlingMessage> {
        override fun createMessage(message: String, problems: MessageProblems) =
            ManuellSaksbehandlingMessage(message, problems)
    }
}
