package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.KansellerUtbetaling
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

internal class KansellerUtbetalingMessage(originalMessage: String, private val problems: MessageProblems) :
    HendelseMessage(originalMessage, problems) {
    init {
        requireValue("@event_name", "kanseller_utbetaling")
        requireKey("aktørId", "fødselsnummer", "organisasjonsnummer", "fagsystemId", "saksbehandler")
    }

    override val fødselsnummer: String get() = this["fødselsnummer"].asText()

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asKansellerUtbetaling() = KansellerUtbetaling(
        this["aktørId"].asText(),
        fødselsnummer,
        this["organisasjonsnummer"].asText(),
        this["fagsystemId"].asText(),
        this["saksbehandler"].asText()
    )

    object Factory : MessageFactory<KansellerUtbetalingMessage> {
        override fun createMessage(message: String, problems: MessageProblems) =
            KansellerUtbetalingMessage(message, problems)
    }
}
