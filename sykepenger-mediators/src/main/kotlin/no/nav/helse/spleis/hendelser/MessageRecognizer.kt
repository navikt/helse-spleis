package no.nav.helse.spleis.hendelser

// Uses Collecting parameter to collect errors/messages
// Uses GoF mediator pattern to notify on recognized messages
// Uses GoF template method factory for creating messages
internal abstract class MessageRecognizer<MessageType: JsonMessage>(
    private val director: MessageDirector<MessageType>
) {
    protected abstract fun createMessage(message: String, problems: MessageProblems): MessageType

    fun recognize(message: String, accumulatedProblems: MessageProblems): Boolean {
        val problems = MessageProblems(message)
        val msg = createMessage(message, problems)
        return (!problems.hasErrors()).also { ok ->
            if (ok) {
                director.onMessage(msg, problems)
            } else {
                accumulatedProblems.addAll(problems, msg::class.java.simpleName)
            }
        }
    }
}
