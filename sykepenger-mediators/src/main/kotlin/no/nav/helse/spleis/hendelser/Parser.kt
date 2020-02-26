package no.nav.helse.spleis.hendelser

import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.hendelser.model.HendelseMessage

// Understands a stream of valid JSON packets meeting certain criteria
internal class Parser(private val director: ParserDirector, rapidsConnection: RapidsConnection) : RapidsConnection.MessageListener {

    private val factories = mutableListOf<MessageFactory<HendelseMessage>>()

    init {
        rapidsConnection.register(this)
    }

    fun register(factory: MessageFactory<HendelseMessage>) {
        factories.add(factory)
    }

    override fun onMessage(message: String, context: RapidsConnection.MessageContext) {
        try {
            val accumulatedProblems = mutableListOf<Pair<String, MessageProblems>>()
            for (factory in factories) {
                val problems = MessageProblems(message)
                val packet = factory.createMessage(message, problems)
                if (!problems.hasErrors()) return director.onRecognizedMessage(packet, context)
                accumulatedProblems.add(packet::class.simpleName!! to problems)
            }
            director.onUnrecognizedMessage(accumulatedProblems)
        } catch (err: MessageProblems.MessageException) {
            director.onMessageException(err)
        }
    }

    // GoF Mediator
    internal interface ParserDirector {
        fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext)
        fun onUnrecognizedMessage(problems: List<Pair<String, MessageProblems>>)
        fun onMessageException(exception: MessageProblems.MessageException)
    }
}
