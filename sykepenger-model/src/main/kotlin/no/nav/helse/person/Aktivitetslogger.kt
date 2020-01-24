package no.nav.helse.person

import no.nav.helse.person.Aktivitetslogger.Alvorlighetsgrad.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
class Aktivitetslogger(private val originalMessage: String? = null) : IAktivitetslogger {
    private val aktiviteter = mutableListOf<Aktivitet>()

    override fun info(melding: String, vararg params: Any) {
        aktiviteter.add(Aktivitet(INFO, String.format(melding, *params)))
    }

    override fun warn(melding: String, vararg params: Any) {
        aktiviteter.add(Aktivitet(WARN, String.format(melding, *params)))
    }

    override fun error(melding: String, vararg params: Any) {
        aktiviteter.add(Aktivitet(ERROR, String.format(melding, *params)))
    }

    override fun severe(melding: String, vararg params: Any): Nothing {
        aktiviteter.add(Aktivitet(SEVERE, String.format(melding, *params)))
        throw AktivitetException(this)
    }

    override fun hasMessages() = info().isNotEmpty() || hasWarnings() || hasErrors()

    override fun hasWarnings() = warn().isNotEmpty()

    override fun hasErrors() = error().isNotEmpty() || severe().isNotEmpty()

    override fun addAll(other: Aktivitetslogger, label: String) {
        this.aktiviteter.addAll(other.aktiviteter.map { it.cloneWith(label) })
        this.aktiviteter.sort()
    }

    override fun expectNoErrors(): Boolean {
        if (hasErrors()) throw AktivitetException(this)
        return true
    }

    fun toReport(): String {
        if (!hasMessages()) return "Ingen meldinger eller problemer\n"
        val results = StringBuffer()
        results.append("Meldinger eller problemer finnes. ${originalMessage?.let { "Original melding: $it" }?: ""} \n\t")
        append("Severe errors", severe(), results)
        append("Errors", error(), results)
        append("Warnings", warn(), results)
        append("Information", info(), results)
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

    override fun toString() = aktiviteter.map { it.inOrder() }.fold("") { acc, s -> acc + "\n" + s}


    private fun info() = Aktivitet.filter(INFO, aktiviteter)
    private fun warn() = Aktivitet.filter(WARN, aktiviteter)
    private fun error() = Aktivitet.filter(ERROR, aktiviteter)
    private fun severe() = Aktivitet.filter(SEVERE, aktiviteter)

    class AktivitetException internal constructor(aktivitetslogger: Aktivitetslogger) : RuntimeException(aktivitetslogger.toString())

    private class Aktivitet(
        private val alvorlighetsgrad: Alvorlighetsgrad,
        private var melding: String,
        private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
    ): Comparable<Aktivitet> {
        companion object {
            internal fun filter(
                alvorlighetsgrad: Alvorlighetsgrad,
                aktiviteter: MutableList<Aktivitet>
            ): List<Aktivitet> {
                return aktiviteter.filter { it.alvorlighetsgrad == alvorlighetsgrad }
            }

            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        internal fun cloneWith(label: String) = Aktivitet(
                alvorlighetsgrad,
                melding + " ($label)",
                tidsstempel
            )

        override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
            .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }

        override fun toString() = tidsstempel + "\t" + melding
        internal fun inOrder() = alvorlighetsgrad
            .toString() + "\t" + tidsstempel + "\t" + melding
    }

    private enum class Alvorlighetsgrad(private val label: String): Comparable<Alvorlighetsgrad> {
        INFO("I"),
        WARN("W"),
        ERROR("E"),
        SEVERE("S");

        override fun toString() = label
    }
}

interface IAktivitetslogger {
    fun info(melding: String, vararg params: Any)
    fun warn(melding: String, vararg params: Any)
    fun error(melding: String, vararg params: Any)
    fun severe(melding: String, vararg params: Any): Nothing

    fun hasMessages(): Boolean
    fun hasWarnings(): Boolean

    fun hasErrors(): Boolean

    fun addAll(other: Aktivitetslogger, label: String)

    fun expectNoErrors(): Boolean
}
