package no.nav.helse.person

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
class Problemer(private val originalMessage: String? = null) : RuntimeException() {
    private val info = mutableListOf<String>()
    private val warn = mutableListOf<String>()
    private val error = mutableListOf<String>()
    private val severe = mutableListOf<String>()

    fun info(melding: String, vararg params: Any) = info.add(String.format(melding, *params))
    fun warn(melding: String, vararg params: Any) = warn.add(String.format(melding, *params))
    fun error(melding: String, vararg params: Any) = error.add(String.format(melding, *params))
    fun severe(melding: String, vararg params: Any): Nothing = severe.add(String.format(melding, *params)).let { throw this }

    fun hasMessages() = info.isNotEmpty() || warn.isNotEmpty() || hasErrors()
    fun hasErrors() = error.isNotEmpty() || severe.isNotEmpty()

    override val message get() = toString()

    override fun toString(): String {
        if (!hasMessages()) return "Ingen meldinger eller problemer\n"
        val results = StringBuffer()
        results.append("Meldinger eller problemer finnes. ${ originalMessage?.let { "Original melding: $it" } } \n\t")
        append("Severe errors", severe, results)
        append("Errors", error, results)
        append("Warnings", warn, results)
        append("Information", info, results)
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
}
