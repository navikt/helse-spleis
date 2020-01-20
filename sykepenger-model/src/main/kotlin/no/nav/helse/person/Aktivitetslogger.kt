package no.nav.helse.person

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
class Aktivitetslogger(private val originalMessage: String? = null) : RuntimeException(), IAktivitetslogger {
    private val info = mutableListOf<Aktivitet>()
    private val warn = mutableListOf<Aktivitet>()
    private val error = mutableListOf<Aktivitet>()
    private val severe = mutableListOf<Aktivitet>()

    override fun info(melding: String, vararg params: Any) {
        info.add(Aktivitet(String.format(melding, *params)))
    }

    override fun warn(melding: String, vararg params: Any) {
        warn.add(Aktivitet(String.format(melding, *params)))
    }

    override fun error(melding: String, vararg params: Any) {
        error.add(Aktivitet(String.format(melding, *params)))
    }

    override fun severe(melding: String, vararg params: Any): Nothing {
        severe.add(Aktivitet(String.format(melding, *params)))
        throw this
    }

    override fun hasMessages() = info.isNotEmpty() || warn.isNotEmpty() || hasErrors()

    override fun hasErrors() = error.isNotEmpty() || severe.isNotEmpty()

    override fun addAll(other: Aktivitetslogger, label: String) {
        other.severe.forEach { this.severe("$it ($label)") }
        other.error.forEach { this.error("$it ($label)") }
        other.warn.forEach { this.warn("$it ($label)") }
    }

    override val message get() = toString()

    override fun expectNoErrors(): Boolean {
        if (hasErrors()) throw this
        return true
    }

    override fun toString(): String {
        if (!hasMessages()) return "Ingen meldinger eller problemer\n"
        val results = StringBuffer()
        results.append("Meldinger eller problemer finnes. ${originalMessage?.let { "Original melding: $it" }} \n\t")
        append("Severe errors", severe, results)
        append("Errors", error, results)
        append("Warnings", warn, results)
        append("Information", info, results)
        results.append("\n")
        return results.toString()
    }

    private fun append(label: String, messages: List<Aktivitet>, results: StringBuffer) {
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

    private class Aktivitet(private val melding: String) {
        companion object {
            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        private val tidsstempel = LocalDateTime.now().format(tidsstempelformat)
        override fun toString(): String {
            return tidsstempel + "\t" + melding
        }
    }
}

interface IAktivitetslogger {
    fun info(melding: String, vararg params: Any)
    fun warn(melding: String, vararg params: Any)
    fun error(melding: String, vararg params: Any)
    fun severe(melding: String, vararg params: Any): Nothing

    fun hasMessages(): Boolean

    fun hasErrors(): Boolean

    fun addAll(other: Aktivitetslogger, label: String)

    fun expectNoErrors(): Boolean
}
