package no.nav.helse.person

import no.nav.helse.serde.reflection.AktivitetsloggReflect
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
class Aktivitetslogg(private var forelder: Aktivitetslogg? = null) : IAktivitetslogg {
    private val aktiviteter = mutableListOf<Aktivitet>()
    private val kontekster = mutableListOf<Aktivitetskontekst>()  // Doesn't need serialization

    internal fun accept(visitor: AktivitetsloggVisitor) {
        visitor.preVisitAktivitetslogg(this)
        aktiviteter.forEach { it.accept(visitor) }
        visitor.postVisitAktivitetslogg(this)
    }

    override fun info(melding: String, vararg params: Any?) {
        add(Aktivitet.Info(kontekster.toSpesifikk(), String.format(melding, *params)))
    }

    override fun warn(melding: String, vararg params: Any?) {
        add(Aktivitet.Warn(kontekster.toSpesifikk(), String.format(melding, *params)))
    }

    override fun behov(type: Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any>) {
        add(Aktivitet.Behov(type, kontekster.toSpesifikk(), melding, detaljer))
    }

    override fun error(melding: String, vararg params: Any?) {
        add(Aktivitet.Error(kontekster.toSpesifikk(), String.format(melding, *params)))
    }

    override fun severe(melding: String, vararg params: Any?): Nothing {
        add(Aktivitet.Severe(kontekster.toSpesifikk(), String.format(melding, *params)))

        throw AktivitetException(this)
    }

    private fun add(aktivitet: Aktivitet) {
        this.aktiviteter.add(aktivitet)
        forelder?.let { forelder?.add(aktivitet) }
    }

    private fun MutableList<Aktivitetskontekst>.toSpesifikk() = this.map { it.toSpesifikkKontekst() }

    override fun hasMessages() = info().isNotEmpty() || hasWarnings()

    override fun hasWarnings() = warn().isNotEmpty() || hasBehov()

    override fun hasBehov() = behov().isNotEmpty() || hasErrors()

    override fun hasErrors() = error().isNotEmpty() || severe().isNotEmpty()

    override fun barn() = Aktivitetslogg(this)

    override fun toString() = this.aktiviteter.map { it.inOrder() }.fold("") { acc, s -> acc + "\n" + s }

    override fun aktivitetsteller() = aktiviteter.size

    override fun kontekst(kontekst: Aktivitetskontekst) { kontekster.add(kontekst) }

    override fun kontekst(person: Person) {
        forelder = person.aktivitetslogg
        kontekst(person as Aktivitetskontekst)
    }

    internal fun logg(kontekst: Aktivitetskontekst): Aktivitetslogg {
        return Aktivitetslogg(this).also {
            it.aktiviteter.addAll(this.aktiviteter.filter { aktivitet -> kontekst in aktivitet })
        }
    }
    private fun info() = Aktivitet.Info.filter(aktiviteter)
    private fun warn() = Aktivitet.Warn.filter(aktiviteter)
    fun behov() = Aktivitet.Behov.filter(aktiviteter)
    private fun error() = Aktivitet.Error.filter(aktiviteter)
    private fun severe() = Aktivitet.Severe.filter(aktiviteter)

    class AktivitetException internal constructor(private val aktivitetslogg: Aktivitetslogg) :
        RuntimeException(aktivitetslogg.toString()) {
        fun aktivitetslogg() = aktivitetslogg
    }

    sealed class Aktivitet(
        private val alvorlighetsgrad: Int,
        internal val label: Char,
        private var melding: String,
        private val tidsstempel: String,
        internal val kontekster: List<SpesifikkKontekst>
    ) : Comparable<Aktivitet> {
        companion object {
            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        open fun kontekst(): Map<String, Any> =
            kontekster.fold(mutableMapOf()) { result, kontekst -> result.apply { putAll(kontekst.kontekstMap) } }

        override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
            .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }

        internal fun inOrder() = label + "\t" + this.toString()

        override fun toString() = tidsstempel + "\t" + melding + meldingerString()

        private fun meldingerString(): String {
            return kontekster.map { "(${it.melding()})" }.fold("") { acc, s -> acc + " " + s }
        }

        internal abstract fun accept(visitor: AktivitetsloggVisitor)

        operator fun contains(kontekst: Aktivitetskontekst) = kontekst.toSpesifikkKontekst() in kontekster
        internal class Info(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(0, 'I', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Info> {
                    return aktiviteter.filterIsInstance<Info>()
                }

            }
            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitInfo(kontekster, this, melding, tidsstempel)
            }

        }
        internal class Warn(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(25, 'W', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Warn> {
                    return aktiviteter.filterIsInstance<Warn>()
                }

            }
            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitWarn(kontekster, this, melding, tidsstempel)
            }

        }

        class Behov(
            private val type: Behovtype,
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val detaljer: Map<String, Any> = emptyMap(),
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(50, 'N', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Behov> {
                    return aktiviteter.filterIsInstance<Behov>()
                }
            }

            override fun kontekst() = super.kontekst().toMutableMap() + detaljer

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitBehov(kontekster, this, type, melding, tidsstempel)
            }

            enum class Behovtype {
                Sykepengehistorikk,
                Foreldrepenger,
                EgenAnsatt,
                Godkjenning,
                Utbetaling,
                Inntektsberegning,
                Opptjening
            }
        }

        internal class Error(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(75, 'E', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Error> {
                    return aktiviteter.filterIsInstance<Error>()
                }
            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitError(kontekster, this, melding, tidsstempel)
            }
        }

        internal class Severe(
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(100, 'S', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Severe> {
                    return aktiviteter.filterIsInstance<Severe>()
                }
            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitSevere(kontekster, this, melding, tidsstempel)
            }
        }
    }
}
 interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any?)
    fun warn(melding: String, vararg params: Any?)
    fun behov(type: Aktivitetslogg.Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any> = emptyMap())
    fun error(melding: String, vararg params: Any?)
    fun severe(melding: String, vararg params: Any?): Nothing

    fun hasMessages(): Boolean
    fun hasWarnings(): Boolean
    fun hasBehov(): Boolean
    fun hasErrors(): Boolean
    fun aktivitetsteller(): Int

    fun barn(): Aktivitetslogg
    fun kontekst(kontekst: Aktivitetskontekst)
    fun kontekst(person: Person)
}

internal interface AktivitetsloggVisitor {
    fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun visitInfo(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Info,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitWarn(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Warn,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitBehov(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Behov,
        type: Aktivitetslogg.Aktivitet.Behov.Behovtype,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitError(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Error,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitSevere(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Severe,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun postVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
}

interface Aktivitetskontekst {
    fun toSpesifikkKontekst(): SpesifikkKontekst
}

class SpesifikkKontekst(internal val kontekstType: String, internal val kontekstMap: Map<String, String> = mapOf()) {
    internal fun melding() = kontekstType + kontekstMap.entries.fold(""){ acc, entry -> acc + " ${entry.key}: ${entry.value}"}
    override fun equals(other: Any?) =
        this === other || other is SpesifikkKontekst && this.kontekstMap == other.kontekstMap
    override fun hashCode() = kontekstMap.hashCode()
}

internal open class Personkontekst(private val aktørId: String, private val fødselsnummer: String) : Aktivitetskontekst {
    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Person", mapOf("fødselsnummer" to fødselsnummer, "aktørId" to aktørId))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Personkontekst) return false
        return this.toMap() == other.toMap()
    }

    override fun hashCode() = toMap().hashCode()

    open fun toMap() = mapOf<String, Any>(
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer
    )
}

internal open class Arbeidsgiverkontekst(aktørId: String, fødselsnummer: String, private val organisasjonsnummer: String) : Personkontekst(aktørId, fødselsnummer) {
    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Arbeidsgiver", mapOf("organisasjonsnummer" to organisasjonsnummer))
    }

    override fun toMap() = super.toMap() + ("organisasjonsnummer" to organisasjonsnummer)
}

internal class Vedtaksperiodekontekst(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, private val vedtaksperiodeId: UUID) : Arbeidsgiverkontekst(aktørId, fødselsnummer, organisasjonsnummer) {
    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to vedtaksperiodeId.toString()))
    }

    override fun toMap(): Map<String, Any> = super.toMap() + ("vedtaksperiodeId" to vedtaksperiodeId)
}

fun Aktivitetslogg.toMap() = AktivitetsloggReflect(this).toMap()
