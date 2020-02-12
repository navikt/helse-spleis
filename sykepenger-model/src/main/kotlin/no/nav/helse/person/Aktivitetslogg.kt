package no.nav.helse.person

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
class Aktivitetslogg(private var forelder: Aktivitetslogg? = null) : IAktivitetslogg {
    private val aktiviteter = mutableListOf<Aktivitet>()
    private lateinit var aktivitetsmelding: Aktivitetsmelding

    fun accept(visitor: AktivitetsloggVisitor) {
        visitor.preVisitAktivitetslogg(this)
        aktiviteter.forEach { it.accept(visitor) }
        visitor.postVisitAktivitetslogg(this)
    }

    override fun info(melding: String, vararg params: Any) {
        add(Aktivitet.Info(String.format(melding, *params)))
    }

    override fun warn(melding: String, vararg params: Any) {
        add(Aktivitet.Warn(String.format(melding, *params)))
    }

    override fun need(type: Aktivitet.Need.NeedType, melding: String, vararg params: Any) {
        add(Aktivitet.Need(type, String.format(melding, *params)))
    }

    override fun error(melding: String, vararg params: Any) {
        add(Aktivitet.Error(String.format(melding, *params)))
    }

    override fun severe(melding: String, vararg params: Any): Nothing {
        add(Aktivitet.Severe(String.format(melding, *params)))
        throw AktivitetException(this)
    }

    private fun add(aktivitet: Aktivitet, aktivitetsmelding: Aktivitetsmelding? = null) {
        aktivitetsmelding?.let { aktivitet.add(it) }
        this.aktiviteter.add(aktivitet)
        forelder?.add(aktivitet, this.aktivitetsmelding)
    }

    override fun hasMessages() = info().isNotEmpty() || hasWarnings()

    override fun hasWarnings() = warn().isNotEmpty() || hasNeeds()

    override fun hasNeeds() = need().isNotEmpty() || hasErrors()

    override fun hasErrors() = error().isNotEmpty() || severe().isNotEmpty()

    override fun barn() = Aktivitetslogg(this)

    internal fun aktivitetsmelding(aktivitetsmelding: Aktivitetsmelding) {
        this.aktivitetsmelding = aktivitetsmelding
    }

    internal fun forelder(nyForelder: Aktivitetslogg) {
        this.forelder = nyForelder
    }

    fun toReport(): String {
        if (!hasMessages()) return "Ingen meldinger eller problemer\n"
        val results = StringBuffer()
        results.append("Meldinger eller problemer finnes.\n\t")
        append("Severe errors", severe(), results)
        append("Errors", error(), results)
        append("Needs", need(), results)
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

    override fun toString() = this.aktiviteter.map { it.inOrder() }.fold("") { acc, s -> acc + "\n" + s }

    private fun info() = Aktivitet.Info.filter(aktiviteter)
    private fun warn() = Aktivitet.Warn.filter(aktiviteter)
    private fun need() = Aktivitet.Need.filter(aktiviteter)
    private fun error() = Aktivitet.Error.filter(aktiviteter)
    private fun severe() = Aktivitet.Severe.filter(aktiviteter)

    class AktivitetException internal constructor(private val aktivitetslogger: Aktivitetslogg) :
        RuntimeException(aktivitetslogger.toString()) {
        fun accept(visitor: AktivitetsloggVisitor) {
            aktivitetslogger.accept(visitor)
        }
    }

    sealed class Aktivitet(
        private val alvorlighetsgrad: Int,
        private var melding: String,
        private val tidsstempel: String
    ) : Comparable<Aktivitet> {
        companion object {
            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        private val meldinger = mutableListOf<Aktivitetsmelding>()

        internal fun add(aktivitetsmelding: Aktivitetsmelding) {
            meldinger.add(aktivitetsmelding)
        }

        protected abstract fun label(): Char
        internal abstract fun cloneWith(label: String): Aktivitet

        override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
            .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }


        internal fun inOrder() = label() + "\t" + this.toString()

        override fun toString() = tidsstempel + "\t" + melding + meldingerString()

        private fun meldingerString(): String {
            return meldinger.reversed().map { "(${it.melding()})" }.fold("") { acc, s -> acc + " " + s }
        }

        abstract fun accept(visitor: AktivitetsloggVisitor)

        class Info(
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(0, melding, tidsstempel) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Info> {
                    return aktiviteter.filterIsInstance<Info>()
                }
            }

            override fun label() = 'I'

            override fun cloneWith(label: String) = Info("$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitInfo(this, melding, tidsstempel)
            }
        }

        class Warn(
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(25, melding, tidsstempel) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Warn> {
                    return aktiviteter.filterIsInstance<Warn>()
                }
            }

            override fun label() = 'W'

            override fun cloneWith(label: String) = Warn("$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitWarn(this, melding, tidsstempel)
            }
        }

        class Need(
            private val type: NeedType,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(50, melding, tidsstempel) {
            enum class NeedType {
                GjennomgåTidslinje
            }

            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Need> {
                    return aktiviteter.filterIsInstance<Need>()
                }
            }

            override fun label() = 'N'

            override fun cloneWith(label: String) = Need(type, "$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitNeed(this, type, melding, tidsstempel)
            }

        }

        class Error(
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(75, melding, tidsstempel) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Error> {
                    return aktiviteter.filterIsInstance<Error>()
                }
            }

            override fun label() = 'E'

            override fun cloneWith(label: String) = Error("$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitError(this, melding, tidsstempel)
            }
        }

        class Severe(
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(100, melding, tidsstempel) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Severe> {
                    return aktiviteter.filterIsInstance<Severe>()
                }
            }

            override fun label() = 'S'

            override fun cloneWith(label: String) = Severe("$melding ($label)", tidsstempel)

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitSevere(this, melding, tidsstempel)
            }
        }
    }
}

interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any)
    fun warn(melding: String, vararg params: Any)
    fun need(type: Aktivitetslogg.Aktivitet.Need.NeedType, melding: String, vararg params: Any)
    fun error(melding: String, vararg params: Any)
    fun severe(melding: String, vararg params: Any): Nothing

    fun hasMessages(): Boolean
    fun hasWarnings(): Boolean
    fun hasNeeds(): Boolean
    fun hasErrors(): Boolean
    fun barn(): Aktivitetslogg
}

interface AktivitetsloggVisitor {
    fun preVisitAktivitetslogg(aktivitetslogger: Aktivitetslogg) {}
    fun visitInfo(aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {}
    fun visitWarn(aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {}
    fun visitNeed(
        aktivitet: Aktivitetslogg.Aktivitet.Need,
        type: Aktivitetslogg.Aktivitet.Need.NeedType,
        tidsstempel: String,
        melding: String
    ) {}
    fun visitError(aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {}
    fun visitSevere(aktivitet: Aktivitetslogg.Aktivitet.Severe, melding: String, tidsstempel: String) {}
    fun postVisitAktivitetslogg(aktivitetslogger: Aktivitetslogg) {}
}

interface Aktivitetsmelding {
    fun melding(): String
}
