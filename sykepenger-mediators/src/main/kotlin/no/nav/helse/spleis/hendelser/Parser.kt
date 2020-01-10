package no.nav.helse.spleis.hendelser

import no.nav.helse.spleis.HendelseStream

// Understands a stream of valid JSON packets meeting certain criteria
// Implements GoF Mediator pattern to notify unrecognized messages
// Implements GoF Command pattern for parsing
internal class Parser(private val director: ParserDirector) : HendelseStream.MessageListener {
    private val parsers = mutableListOf<MessageRecognizer<*>>()

    fun register(recognizer: MessageRecognizer<*>) {
        parsers.add(recognizer)
    }

    override fun onMessage(message: String) {
        val accumulatedProblems = MessageProblems(message)
        for (parser in parsers)
            if (parser.recognize(message, accumulatedProblems)) return
        director.onUnrecognizedMessage(accumulatedProblems)
    }

    // GoF Mediator
    internal interface ParserDirector {
        fun onUnrecognizedMessage(problems: MessageProblems)
    }
}
