package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.hendelser.UtbetalingHendelse as ModelUtbetaling

internal class UtbetalingMessage(originalMessage: String, problems: MessageProblems) : BehovMessage(originalMessage, problems) {
    init {
        requireAll("@behov", Utbetaling)
        requireKey("@løsning.${Utbetaling.name}")
        requireAny("@løsning.${Utbetaling.name}.status", Oppdragstatus.values().filterNot { it == Oppdragstatus.OVERFØRT }.map(Enum<*>::name))
        requireKey("@løsning.${Utbetaling.name}.beskrivelse")
        requireKey("utbetalingsreferanse")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asUtbetaling(): ModelUtbetaling {
        return ModelUtbetaling(
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = fødselsnummer,
            orgnummer = this["organisasjonsnummer"].asText(),
            utbetalingsreferanse = this["utbetalingsreferanse"].asText(),
            status = enumValueOf(this["@løsning.${Utbetaling.name}.status"].asText()),
            melding = this["@løsning.${Utbetaling.name}.beskrivelse"].asText()
        )
    }

    object Factory : MessageFactory<UtbetalingMessage> {
        override fun createMessage(message: String, problems: MessageProblems) =
            UtbetalingMessage(message, problems)
    }
}
