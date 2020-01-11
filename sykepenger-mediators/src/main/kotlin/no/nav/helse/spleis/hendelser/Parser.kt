package no.nav.helse.spleis.hendelser

import no.nav.helse.spleis.HendelseStream

// Understands a stream of valid JSON packets meeting certain criteria
// Implements GoF Mediator pattern to notify unrecognized messages
// Implements GoF Command pattern for parsing
internal class Parser(private val director: ParserDirector) : HendelseStream.MessageListener {
    private val factories = mutableListOf<MessageFactory<*>>()

    fun register(factory: MessageFactory<*>) {
        factories.add(factory)
    }

    override fun onMessage(message: String) {
        val accumulatedProblems = MessageProblems(message)
        for (factory in factories) {
            val problems = MessageProblems(message)
            val newMessage = factory.createMessage(message, problems)
            if (!problems.hasErrors()) return director.onRecognizedMessage(newMessage, problems)
            accumulatedProblems.addAll(problems, "label")
        }
        director.onUnrecognizedMessage(accumulatedProblems)
    }

    // GoF Mediator
    internal interface ParserDirector {
        fun onRecognizedMessage(message: JsonMessage, warnings: MessageProblems)
        fun onUnrecognizedMessage(problems: MessageProblems)
    }
}
