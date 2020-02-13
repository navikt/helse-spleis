package no.nav.helse.spleis.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.KafkaRapid

// Understands a stream of valid JSON packets meeting certain criteria
// Implements GoF Mediator pattern to notify unrecognized messages
// Implements GoF Command pattern for parsing
internal class Parser(private val director: ParserDirector) : KafkaRapid.MessageListener {
    private val factories = mutableListOf<MessageFactory>()

    fun register(factory: MessageFactory) {
        factories.add(factory)
    }

    override fun onMessage(message: String) {
        val accumulatedProblems = Aktivitetslogger(message)
        val summaryAktivitetslogg = Aktivitetslogg()

        try {
            for (factory in factories) {
                val problems = Aktivitetslogger(message)
                val aktivitetslogg = Aktivitetslogg(summaryAktivitetslogg)
                val newMessage = factory.createMessage(message, problems, aktivitetslogg)
                if (!problems.hasErrorsOld()) return director.onRecognizedMessage(newMessage, problems, aktivitetslogg)
                accumulatedProblems.addAll(problems, newMessage::class.java.simpleName)
            }

            director.onUnrecognizedMessage(accumulatedProblems, summaryAktivitetslogg)
        } catch (err: Aktivitetslogger.AktivitetException) {
            director.onMessageError(err)
        } catch (err: Aktivitetslogg.AktivitetException) {
            director.onMessageError(err)
        }
    }

    // GoF Mediator
    internal interface ParserDirector {
        fun onRecognizedMessage(message: JsonMessage, aktivitetslogger: Aktivitetslogger, aktivitetslogg: Aktivitetslogg)
        fun onUnrecognizedMessage(aktivitetslogger: Aktivitetslogger, aktivitetslogg: Aktivitetslogg)
        fun onMessageError(aktivitetException: Aktivitetslogger.AktivitetException)
        fun onMessageError(aktivitetException: Aktivitetslogg.AktivitetException)
    }
}
