package no.nav.helse.spleis.hendelser.model

import no.nav.helse.behov.Behovstype
import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Behov
internal abstract class BehovMessage(originalMessage: String, private val problems: MessageProblems) :
    JsonMessage(originalMessage, problems) {
    init {
        requiredKey(
            "@behov", "@id", "@opprettet",
            "@final", "@løsning", "@besvart",
            "hendelse", "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId"
        )
        requiredValue("@final", true)
    }
}

internal class YtelserMessage(originalMessage: String, private val problems: MessageProblems) :
    BehovMessage(originalMessage, problems) {
    init {
        requiredValues("@behov", Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<YtelserMessage> {

        override fun createMessage(message: String, problems: MessageProblems): YtelserMessage {
            return YtelserMessage(message, problems)
        }
    }
}

internal class VilkårsgrunnlagMessage(originalMessage: String, private val problems: MessageProblems) :
    BehovMessage(originalMessage, problems) {
    init {
        requiredValues("@behov", Behovstype.Inntektsberegning, Behovstype.EgenAnsatt)
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<VilkårsgrunnlagMessage> {

        override fun createMessage(message: String, problems: MessageProblems): VilkårsgrunnlagMessage {
            return VilkårsgrunnlagMessage(message, problems)
        }
    }
}

internal class ManuellSaksbehandlingMessage(originalMessage: String, private val problems: MessageProblems) :
    BehovMessage(originalMessage, problems) {
    init {
        requiredValues("@behov", Behovstype.GodkjenningFraSaksbehandler)
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<ManuellSaksbehandlingMessage> {

        override fun createMessage(message: String, problems: MessageProblems): ManuellSaksbehandlingMessage {
            return ManuellSaksbehandlingMessage(message, problems)
        }
    }
}
