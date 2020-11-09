package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov
import no.nav.helse.serde.reflection.AktivitetsloggReflect
import no.nav.helse.serde.reflection.OppdragReflect
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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

    override fun behov(type: Behov.Behovtype, melding: String, detaljer: Map<String, Any>) {
        add(Behov(type, kontekster.toSpesifikk(), melding, detaljer))
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

    override fun hasActivities() = info().isNotEmpty() || hasWarningsOrWorse() || behov().isNotEmpty()

    override fun hasWarningsOrWorse() = warn().isNotEmpty() || hasErrorsOrWorse()

    override fun hasErrorsOrWorse() = error().isNotEmpty() || severe().isNotEmpty()

    override fun barn() = Aktivitetslogg(this)

    override fun toString() = this.aktiviteter.map { it.inOrder() }.joinToString(separator = "\n") { it }

    override fun aktivitetsteller() = aktiviteter.size

    override fun kontekst(kontekst: Aktivitetskontekst) {
        kontekster.add(kontekst)
    }

    override fun kontekst(person: Person) {
        forelder = person.aktivitetslogg
        kontekst(person as Aktivitetskontekst)
    }

    internal fun logg(vararg kontekst: Aktivitetskontekst): Aktivitetslogg {
        return Aktivitetslogg(this).also {
            it.aktiviteter.addAll(this.aktiviteter.filter { aktivitet ->
                kontekst.any { it in aktivitet }
            })
        }
    }

    override fun kontekster() =
        aktiviteter
            .groupBy { it.kontekst(listOf("Person", "Arbeidsgiver", "Vedtaksperiode")) }
            .map { Aktivitetslogg(this).apply { aktiviteter.addAll(it.value) } }

    private fun info() = Aktivitet.Info.filter(aktiviteter)
    internal fun warn() = Aktivitet.Warn.filter(aktiviteter)
    override fun behov() = Behov.filter(aktiviteter)
    private fun error() = Aktivitet.Error.filter(aktiviteter)
    private fun severe() = Aktivitet.Severe.filter(aktiviteter)

    class AktivitetException internal constructor(private val aktivitetslogg: Aktivitetslogg) :
        RuntimeException(aktivitetslogg.toString()) {

        fun kontekst() = aktivitetslogg.kontekster.fold(mutableMapOf<String, String>()) { result, kontekst ->
            result.apply { putAll(kontekst.toSpesifikkKontekst().kontekstMap) }
        }

        fun aktivitetslogg() = aktivitetslogg
    }

    sealed class Aktivitet(
        private val alvorlighetsgrad: Int,
        private val label: Char,
        private var melding: String,
        private val tidsstempel: String,
        internal val kontekster: List<SpesifikkKontekst>
    ) : Comparable<Aktivitet> {
        private companion object {
            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        fun kontekst(): Map<String, String> = kontekst(emptyList())

        internal fun kontekst(typer: List<String>): Map<String, String> =
            kontekster
                .let { if (typer.isEmpty()) it else it.filter { kontekst -> kontekst.kontekstType in typer } }
                .fold(mutableMapOf()) { result, kontekst -> result.apply { putAll(kontekst.kontekstMap) } }

        override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
            .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }

        internal fun inOrder() = label + "\t" + this.toString()

        override fun toString() = label + "  \t" + tidsstempel + "  \t" + melding + meldingerString()

        private fun meldingerString(): String {
            return kontekster.joinToString(separator = "") { " (${it.melding()})" }
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
            val type: Behovtype,
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val detaljer: Map<String, Any> = emptyMap(),
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(50, 'N', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Behov> {
                    return aktiviteter.filterIsInstance<Behov>()
                }

                internal fun utbetalingshistorikk(
                    aktivitetslogg: IAktivitetslogg,
                    periode: Periode
                ) {
                    aktivitetslogg.behov(
                        Behovtype.Sykepengehistorikk, "Trenger sykepengehistorikk fra Infotrygd", mapOf(
                            "historikkFom" to periode.start.toString(),
                            "historikkTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun foreldrepenger(aktivitetslogg: IAktivitetslogg) {
                    aktivitetslogg.behov(
                        Behovtype.Foreldrepenger,
                        "Trenger informasjon om foreldrepengeytelser fra FPSAK"
                    )
                }

                internal fun pleiepenger(aktivitetslogg: IAktivitetslogg, periode: Periode) {
                    aktivitetslogg.behov(
                        Behovtype.Pleiepenger,
                        "Trenger informasjon om pleiepengeytelser fra Infotrygd", mapOf(
                            "pleiepengerFom" to periode.start.toString(),
                            "pleiepengerTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun omsorgspenger(aktivitetslogg: IAktivitetslogg, periode: Periode) {
                    aktivitetslogg.behov(
                        Behovtype.Omsorgspenger,
                        "Trenger informasjon om omsorgspengerytelser fra Infotrygd", mapOf(
                            "omsorgspengerFom" to periode.start.toString(),
                            "omsorgspengerTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun opplæringspenger(aktivitetslogg: IAktivitetslogg, periode: Periode) {
                    aktivitetslogg.behov(
                        Behovtype.Opplæringspenger,
                        "Trenger informasjon om opplæringspengerytelser fra Infotrygd", mapOf(
                            "opplæringspengerFom" to periode.start.toString(),
                            "opplæringspengerTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun institusjonsopphold(aktivitetslogg: IAktivitetslogg, periode: Periode) {
                    aktivitetslogg.behov(
                        Behovtype.Institusjonsopphold,
                        "Trenger informasjon om institusjonsopphold fra Inst2", mapOf(
                            "institusjonsoppholdFom" to periode.start.toString(),
                            "institusjonsoppholdTom" to periode.endInclusive.toString()
                        )
                    )
                }

                internal fun inntektsberegning(
                    aktivitetslogg: IAktivitetslogg,
                    beregningStart: YearMonth,
                    beregningSlutt: YearMonth
                ) {
                    aktivitetslogg.behov(
                        Behovtype.InntekterForSammenligningsgrunnlag,
                        "Trenger inntekter for sammenligningsgrunnlag",
                        mapOf(
                            "beregningStart" to beregningStart.toString(),
                            "beregningSlutt" to beregningSlutt.toString()
                        )
                    )
                }

                internal fun opptjening(aktivitetslogg: IAktivitetslogg) {
                    aktivitetslogg.behov(Behovtype.Opptjening, "Trenger informasjon om sykepengeopptjening")
                }

                internal fun dagpenger(aktivitetslogg: IAktivitetslogg, fom: LocalDate, tom: LocalDate) {
                    aktivitetslogg.behov(
                        Behovtype.Dagpenger, "Trenger informasjon om dagpenger", mapOf(
                            "periodeFom" to fom.toString(),
                            "periodeTom" to tom.toString()
                        )
                    )
                }

                internal fun arbeidsavklaringspenger(aktivitetslogg: IAktivitetslogg, fom: LocalDate, tom: LocalDate) {
                    aktivitetslogg.behov(
                        Behovtype.Arbeidsavklaringspenger, "Trenger informasjon om arbeidsavklaringspenger", mapOf(
                            "periodeFom" to fom.toString(),
                            "periodeTom" to tom.toString()
                        )
                    )
                }

                internal fun medlemskap(aktivitetslogg: IAktivitetslogg, fom: LocalDate, tom: LocalDate) {
                    aktivitetslogg.behov(
                        Behovtype.Medlemskap, "Trenger informasjon om medlemskap", mapOf(
                            "medlemskapPeriodeFom" to fom.toString(),
                            "medlemskapPeriodeTom" to tom.toString()
                        )
                    )
                }

                internal fun simulering(
                    aktivitetslogg: IAktivitetslogg,
                    oppdrag: Oppdrag,
                    maksdato: LocalDate,
                    saksbehandler: String
                ) {
                    aktivitetslogg.behov(
                        Behovtype.Simulering,
                        "Trenger simulering fra Oppdragssystemet",
                        mutableMapOf(
                            "maksdato" to maksdato.toString(),
                            "saksbehandler" to saksbehandler
                        ) + OppdragReflect(oppdrag).toMap()
                    )
                }

                internal fun godkjenning(
                    aktivitetslogg: IAktivitetslogg,
                    periodeFom: LocalDate,
                    periodeTom: LocalDate,
                    sykepengegrunnlag: Inntekt,
                    vedtaksperiodeaktivitetslogg: Aktivitetslogg,
                    periodetype: Periodetype
                ) {
                    aktivitetslogg.behov(
                        Behovtype.Godkjenning, "Forespør godkjenning fra saksbehandler", mapOf(
                            "periodeFom" to periodeFom.toString(),
                            "periodeTom" to periodeTom.toString(),
                            "sykepengegrunnlag" to sykepengegrunnlag.reflection { _, månedlig, _, _ -> månedlig },
                            "periodetype" to periodetype.name,
                            "warnings" to Aktivitetslogg().apply {
                                aktiviteter.addAll(vedtaksperiodeaktivitetslogg.warn())
                            }.toMap()
                        )
                    )
                }

                internal fun utbetaling(
                    aktivitetslogg: IAktivitetslogg,
                    oppdrag: Oppdrag,
                    maksdato: LocalDate? = null,
                    saksbehandler: String,
                    saksbehandlerEpost: String,
                    godkjenttidspunkt: LocalDateTime,
                    annullering: Boolean
                ) {
                    aktivitetslogg.behov(
                        Behovtype.Utbetaling,
                        "Trenger å sende utbetaling til Oppdrag",
                        OppdragReflect(oppdrag).toMap().apply {
                            put("saksbehandler", saksbehandler)
                            put("saksbehandlerEpost", saksbehandlerEpost)
                            put("godkjenttidspunkt", godkjenttidspunkt.toString())
                            put("annullering", annullering)
                            maksdato?.let { put("maksdato", maksdato.toString()) }
                        })
                }
            }

            fun detaljer() = detaljer

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitBehov(kontekster, this, type, melding, detaljer, tidsstempel)
            }

            enum class Behovtype {
                Sykepengehistorikk,
                Foreldrepenger,
                Pleiepenger,
                Omsorgspenger,
                Opplæringspenger,
                Institusjonsopphold,
                EgenAnsatt,
                Godkjenning,
                Simulering,
                Utbetaling,
                InntekterForSammenligningsgrunnlag,
                Opptjening,
                Dagpenger,
                Arbeidsavklaringspenger,
                Medlemskap,
                Dødsinfo
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
    fun behov(type: Behov.Behovtype, melding: String, detaljer: Map<String, Any> = emptyMap())
    fun error(melding: String, vararg params: Any?)
    fun severe(melding: String, vararg params: Any?): Nothing

    fun hasActivities(): Boolean
    fun hasWarningsOrWorse(): Boolean
    fun hasErrorsOrWorse(): Boolean

    fun aktivitetsteller(): Int
    fun behov(): List<Behov>
    fun barn(): Aktivitetslogg
    fun kontekst(kontekst: Aktivitetskontekst)
    fun kontekst(person: Person)
    fun kontekster(): List<IAktivitetslogg>
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
        aktivitet: Behov,
        type: Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any>,
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
    internal fun melding() =
        kontekstType + kontekstMap.entries.joinToString(separator = "") { " ${it.key}: ${it.value}" }

    override fun equals(other: Any?) =
        this === other || other is SpesifikkKontekst && this.kontekstMap == other.kontekstMap

    override fun hashCode() = kontekstMap.hashCode()
}

fun Aktivitetslogg.toMap() = AktivitetsloggReflect(this).toMap()
