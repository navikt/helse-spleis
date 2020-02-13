package no.nav.helse.person

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
class Aktivitetslogg internal constructor(private val forelder: Aktivitetslogg? = null) : IAktivitetslogg {
    private val aktiviteter = mutableListOf<Aktivitet>()
    private val kontekster = mutableListOf<Aktivitetskontekst>()

    internal fun accept(visitor: AktivitetsloggVisitor) {
        visitor.preVisitAktivitetslogg(this)
        aktiviteter.forEach { it.accept(visitor) }
        visitor.postVisitAktivitetslogg(this)
    }

    override fun info(melding: String, vararg params: Any) {
        add(Aktivitet.Info(kontekster.toList(), String.format(melding, *params)))
    }

    override fun warn(melding: String, vararg params: Any) {
        add(Aktivitet.Warn(kontekster.toList(), String.format(melding, *params)))
    }

    override fun need(type: NeedType, melding: String, vararg params: Any) {
        add(Aktivitet.Need(kontekster.toList(), type, String.format(melding, *params)))
    }

    override fun error(melding: String, vararg params: Any) {
        add(Aktivitet.Error(kontekster.toList(), String.format(melding, *params)))
    }

    override fun severe(melding: String, vararg params: Any): Nothing {
        add(Aktivitet.Severe(kontekster.toList(), String.format(melding, *params)))
        throw AktivitetException(this)
    }

    private fun add(aktivitet: Aktivitet) {
        this.aktiviteter.add(aktivitet)
        forelder?.let { forelder.add(aktivitet) }
    }

    override fun hasMessages() = info().isNotEmpty() || hasWarnings()

    override fun hasWarnings() = warn().isNotEmpty() || hasNeeds()

    override fun hasNeeds() = need().isNotEmpty() || hasErrors()

    override fun hasErrors() = error().isNotEmpty() || severe().isNotEmpty()

    override fun barn() = Aktivitetslogg(this)

    override fun toString() = this.aktiviteter.map { it.inOrder() }.fold("") { acc, s -> acc + "\n" + s }

    override fun aktivitetsteller() = aktiviteter.size

    internal fun kontekst(kontekst: Aktivitetskontekst) { kontekster.add(kontekst) }

    internal fun logg(kontekst: Aktivitetskontekst): Aktivitetslogg {
        return Aktivitetslogg(this).also {
            it.aktiviteter.addAll(this.aktiviteter.filter { aktivitet -> kontekst in aktivitet })
        }
    }

    private fun info() = Aktivitet.Info.filter(aktiviteter)
    private fun warn() = Aktivitet.Warn.filter(aktiviteter)
    private fun need() = Aktivitet.Need.filter(aktiviteter)
    private fun error() = Aktivitet.Error.filter(aktiviteter)
    private fun severe() = Aktivitet.Severe.filter(aktiviteter)

    class AktivitetException internal constructor(private val aktivitetslogg: Aktivitetslogg) :
        RuntimeException(aktivitetslogg.toString()) {
        fun aktivitetslogg() = aktivitetslogg
    }

    internal sealed class Aktivitet(
        private val alvorlighetsgrad: Int,
        private var melding: String,
        private val tidsstempel: String,
        private val kontekster: List<Aktivitetskontekst>
    ) : Comparable<Aktivitet> {
        companion object {
            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        internal abstract val label: Char

        override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
            .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }

        internal fun inOrder() = label + "\t" + this.toString()

        override fun toString() = tidsstempel + "\t" + melding + meldingerString()

        private fun meldingerString(): String {
            return kontekster.map { "(${it.melding()})" }.fold("") { acc, s -> acc + " " + s }
        }

        abstract fun accept(visitor: AktivitetsloggVisitor)

        operator fun contains(kontekst: Aktivitetskontekst) = kontekst in kontekster

        internal class Info(
            kontekster: List<Aktivitetskontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(0, melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Info> {
                    return aktiviteter.filterIsInstance<Info>()
                }
            }

            override val label = 'I'

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitInfo(this, melding, tidsstempel)
            }
        }

        internal class Warn(
            kontekster: List<Aktivitetskontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(25, melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Warn> {
                    return aktiviteter.filterIsInstance<Warn>()
                }
            }

            override val label = 'W'

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitWarn(this, melding, tidsstempel)
            }
        }

        internal class Need(
            kontekster: List<Aktivitetskontekst>,
            private val type: NeedType,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(50, melding, tidsstempel, kontekster) {

            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Need> {
                    return aktiviteter.filterIsInstance<Need>()
                }
            }

            override val label = 'N'

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitNeed(this, type, melding, tidsstempel)
            }

        }

        internal class Error(
            kontekster: List<Aktivitetskontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(75, melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Error> {
                    return aktiviteter.filterIsInstance<Error>()
                }
            }

            override val label = 'E'

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitError(this, melding, tidsstempel)
            }
        }

        internal class Severe(
            kontekster: List<Aktivitetskontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(100, melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Severe> {
                    return aktiviteter.filterIsInstance<Severe>()
                }
            }

            override val label = 'S'

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitSevere(this, melding, tidsstempel)
            }
        }
    }
}

internal interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any)
    fun warn(melding: String, vararg params: Any)
    fun need(type: NeedType, melding: String, vararg params: Any)
    fun error(melding: String, vararg params: Any)
    fun severe(melding: String, vararg params: Any): Nothing

    fun hasMessages(): Boolean
    fun hasWarnings(): Boolean
    fun hasNeeds(): Boolean
    fun hasErrors(): Boolean
    fun aktivitetsteller(): Int

    fun barn(): Aktivitetslogg
}

internal interface AktivitetsloggVisitor {
    fun preVisitAktivitetslogg(aktivitetslogger: Aktivitetslogg) {}
    fun visitInfo(aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {}
    fun visitWarn(aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {}
    fun visitNeed(
        aktivitet: Aktivitetslogg.Aktivitet.Need,
        type: NeedType,
        tidsstempel: String,
        melding: String
    ) {}
    fun visitError(aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {}
    fun visitSevere(aktivitet: Aktivitetslogg.Aktivitet.Severe, melding: String, tidsstempel: String) {}
    fun postVisitAktivitetslogg(aktivitetslogger: Aktivitetslogg) {}
}

internal interface Aktivitetskontekst {
    fun melding(): String
}

enum class NeedType {
    GjennomgåTidslinje
}
