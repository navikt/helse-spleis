package no.nav.helse.rapids_rivers

class MessageProblems(private val originalMessage: String) {
    private val errors = mutableListOf<String>()
    private val severe = mutableListOf<String>()

    fun error(melding: String, vararg params: Any) {
        errors.add(String.format(melding, *params))
    }

    fun severe(melding: String, vararg params: Any): Nothing {
        severe.add(String.format(melding, *params))
        throw MessageException(this)
    }

    fun hasErrors() = severe.isNotEmpty() || errors.isNotEmpty()

    override fun toString(): String {
        if (!hasErrors()) return "No errors\n"
        val results = StringBuffer()
        results.append("Problems exist. Original message: $originalMessage\n\t")
        append("Severe errors", severe, results)
        append("Errors", errors, results)
        results.append("\n")
        return results.toString()
    }

    private fun append(label: String, messages: List<String>, results: StringBuffer) {
        if (messages.isEmpty()) return
        results.append("\n")
        results.append(label)
        results.append(": ")
        results.append(messages.size)
        for (message in messages) {
            results.append("\n\t")
            results.append(message)
        }
    }

    class MessageException(problems: MessageProblems) : RuntimeException(problems.toString())
}
