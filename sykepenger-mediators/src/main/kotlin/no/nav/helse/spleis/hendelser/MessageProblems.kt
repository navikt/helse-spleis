package no.nav.helse.spleis.hendelser

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
class MessageProblems(private val originalMessage: String) : RuntimeException() {
    private val fatalErrors = mutableListOf<String>()
    private val errors = mutableListOf<String>()
    private val warnings = mutableListOf<String>()

    fun hasFatalErrors() = fatalErrors.isNotEmpty()
    fun hasErrors() = hasFatalErrors() || errors.isNotEmpty()
    fun hasMessages() = hasErrors() || warnings.isNotEmpty()

    fun fatalError(error: String) {
        fatalErrors.add(error)
    }

    fun error(error: String) {
        errors.add(error)
    }

    fun warning(warning: String) {
        warnings.add(warning)
    }

    fun addAll(other: MessageProblems, label: String) {
        other.fatalErrors.forEach { this.addMessage(it, label, this::fatalError) }
        other.errors.forEach { this.addMessage(it, label, this::error) }
        other.warnings.forEach { this.addMessage(it, label, this::warning) }
    }

    private fun addMessage(message: String, label: String, adder: (String) -> Unit) {
        adder("$message ($label)")
    }

    override val message: String? get() {
        if (!hasMessages()) return "No errors detected in message:\n\t$originalMessage"
        return "Errors exist. Original JSON string is:\n\t$originalMessage\n" +
            formatMessages("Fatal errors", fatalErrors) +
            formatMessages("Errors", errors) +
            formatMessages("Warnings", warnings)
    }

    private fun formatMessages(label: String, messages: List<String>) =
        "$label: ${messages.size}${messages.joinToString { "\n\t$it" }}\n"

    override fun toString(): String {
        return message ?: ""
    }
}
