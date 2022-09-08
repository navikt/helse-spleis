package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov
import no.nav.helse.serde.reflection.AktivitetsloggMap
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype


interface AktivitetsloggObserver {
    fun aktivitet(label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime)
}

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
class Aktivitetslogg(
    private var forelder: Aktivitetslogg? = null
) : IAktivitetslogg {
    internal val aktiviteter = mutableListOf<Aktivitet>()
    private val kontekster = mutableListOf<Aktivitetskontekst>()  // Doesn't need serialization
    private val observers = mutableListOf<AktivitetsloggObserver>()

    internal fun accept(visitor: AktivitetsloggVisitor) {
        visitor.preVisitAktivitetslogg(this)
        aktiviteter.forEach { it.accept(visitor) }
        visitor.postVisitAktivitetslogg(this)
    }

    override fun register(observer: AktivitetsloggObserver) {
        observers.add(observer)
    }

    override fun info(melding: String, vararg params: Any?) {
        add(Aktivitet.Info.opprett(kontekster.toSpesifikk(), String.format(melding, *params)))
    }

    override fun varsel(melding: String) {
        add(Aktivitet.Varsel.opprett(kontekster.toSpesifikk(), melding))
    }

    override fun behov(type: Behov.Behovtype, melding: String, detaljer: Map<String, Any?>) {
        add(Behov.opprett(type, kontekster.toSpesifikk(), melding, detaljer))
    }

    override fun funksjonellFeil(melding: String) {
        add(Aktivitet.FunksjonellFeil.opprett(kontekster.toSpesifikk(), melding))
    }

    override fun logiskFeil(melding: String, vararg params: Any?): Nothing {
        add(Aktivitet.LogiskFeil.opprett(kontekster.toSpesifikk(), String.format(melding, *params)))

        throw AktivitetException(this)
    }

    private fun add(aktivitet: Aktivitet) {
        observers.forEach { aktivitet.notify(it) }
        this.aktiviteter.add(aktivitet)
        forelder?.add(aktivitet)
    }

    private fun MutableList<Aktivitetskontekst>.toSpesifikk() = this.map { it.toSpesifikkKontekst() }

    override fun harAktiviteter() = info().isNotEmpty() || harVarslerEllerVerre() || behov().isNotEmpty()

    override fun harVarslerEllerVerre() = varsel().isNotEmpty() || harFunksjonelleFeilEllerVerre()

    override fun harFunksjonelleFeilEllerVerre() = funksjonellFeil().isNotEmpty() || logiskFeil().isNotEmpty()

    override fun barn() = Aktivitetslogg(this)

    override fun toString() = this.aktiviteter.map { it.inOrder() }.joinToString(separator = "\n") { it }

    override fun aktivitetsteller() = aktiviteter.size

    override fun kontekst(kontekst: Aktivitetskontekst) {
        val spesifikkKontekst = kontekst.toSpesifikkKontekst()
        val index = kontekster.indexOfFirst { spesifikkKontekst.sammeType(it) }
        if (index >= 0) fjernKonteksterFraOgMed(index)
        kontekster.add(kontekst)
    }

    private fun fjernKonteksterFraOgMed(indeks: Int) {
        val antall = kontekster.size - indeks
        repeat(antall) { kontekster.removeLast() }
    }

    override fun kontekst(person: Person) {
        forelder = person.aktivitetslogg
        kontekst(person as Aktivitetskontekst)
    }

    override fun toMap(): Map<String, List<Map<String, Any>>> = AktivitetsloggMap(this).toMap()

    internal fun logg(vararg kontekst: Aktivitetskontekst): Aktivitetslogg {
        return Aktivitetslogg(this).also {
            it.aktiviteter.addAll(this.aktiviteter.filter { aktivitet ->
                kontekst.any { it in aktivitet }
            })
        }
    }

    internal fun logg(vararg kontekst: String): Aktivitetslogg {
        return Aktivitetslogg(this).also { aktivitetslogg ->
            aktivitetslogg.aktiviteter.addAll(this.aktiviteter.filter { aktivitet ->
                kontekst.any { kontekst -> kontekst in aktivitet.kontekster.map { it.kontekstType } }
            })
        }
    }

    override fun kontekster() =
        aktiviteter
            .groupBy { it.kontekst(null) }
            .map { Aktivitetslogg(this).apply { aktiviteter.addAll(it.value) } }

    override fun hendelseskontekster(): Map<String, String> {
        return kontekster
            .map(Aktivitetskontekst::toSpesifikkKontekst)
            .filter { it.kontekstType in MODELL_KONTEKSTER }
            .map(SpesifikkKontekst::kontekstMap)
            .fold(mapOf()) { result, kontekst -> result + kontekst }
    }

    override fun hendelseskontekst(): Hendelseskontekst = Hendelseskontekst(hendelseskontekster())

    private fun info() = Aktivitet.Info.filter(aktiviteter)
    internal fun varsel() = Aktivitet.Varsel.filter(aktiviteter)
    override fun behov() = Behov.filter(aktiviteter)
    private fun funksjonellFeil() = Aktivitet.FunksjonellFeil.filter(aktiviteter)
    private fun logiskFeil() = Aktivitet.LogiskFeil.filter(aktiviteter)

    companion object {
        private val MODELL_KONTEKSTER: Array<String> = arrayOf("Person", "Arbeidsgiver", "Vedtaksperiode")
    }

    class AktivitetException internal constructor(private val aktivitetslogg: Aktivitetslogg) :
        RuntimeException(aktivitetslogg.toString()) {

        fun kontekst() = aktivitetslogg.kontekster.fold(mutableMapOf<String, String>()) { result, kontekst ->
            result.apply { putAll(kontekst.toSpesifikkKontekst().kontekstMap) }
        }

        fun aktivitetslogg() = aktivitetslogg
    }

    sealed class Aktivitet(
        protected val id: UUID,
        private val alvorlighetsgrad: Int,
        private val label: Char,
        private var melding: String,
        private val tidsstempel: String,
        internal val kontekster: List<SpesifikkKontekst>
    ) : Comparable<Aktivitet> {
        private companion object {
            private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        }

        fun kontekst(): Map<String, String> = kontekst(null)

        internal fun kontekst(typer: Array<String>?): Map<String, String> = kontekster
            .filter { typer == null || it.kontekstType in typer }
            .fold(mapOf()) { result, kontekst -> result + kontekst.kontekstMap }

        override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
            .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }

        internal fun inOrder() = label + "\t" + this.toString()

        override fun toString() = label + "  \t" + tidsstempel + "  \t" + melding + meldingerString()

        private fun meldingerString(): String {
            return kontekster.joinToString(separator = "") { " (${it.melding()})" }
        }

        internal abstract fun accept(visitor: AktivitetsloggVisitor)

        internal open fun notify(observer: AktivitetsloggObserver) {
            observer.aktivitet(label, melding, kontekster, LocalDateTime.parse(tidsstempel, tidsstempelformat))
        }

        operator fun contains(kontekst: Aktivitetskontekst) = kontekst.toSpesifikkKontekst() in kontekster
        internal class Info private constructor(
            id: UUID,
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(id, 0, 'I', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Info> {
                    return aktiviteter.filterIsInstance<Info>()
                }

                internal fun gjennopprett(id: UUID, kontekster: List<SpesifikkKontekst>, melding: String, tidsstempel: String) = Info(id, kontekster, melding, tidsstempel)
                internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) = Info(UUID.randomUUID(), kontekster, melding)
            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitInfo(id, kontekster, this, melding, tidsstempel)
            }
        }

        internal class Varsel private constructor(
            id: UUID,
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(id, 25, 'W', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Varsel> {
                    return aktiviteter.filterIsInstance<Varsel>()
                }
                internal fun gjennopprett(id: UUID, kontekster: List<SpesifikkKontekst>, melding: String, tidsstempel: String) = Varsel(id, kontekster, melding, tidsstempel)
                internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) = Varsel(UUID.randomUUID(), kontekster, melding)
            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitVarsel(id, kontekster, this, melding, tidsstempel)
            }
        }

        class Behov private constructor(
            id: UUID,
            val type: Behovtype,
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val detaljer: Map<String, Any?> = emptyMap(),
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(id, 50, 'N', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<Behov> {
                    return aktiviteter.filterIsInstance<Behov>()
                }

                internal fun gjennopprett(id: UUID, type: Behovtype, kontekster: List<SpesifikkKontekst>, melding: String, detaljer: Map<String, Any?>, tidsstempel: String) =
                    Behov(id, type, kontekster, melding, detaljer, tidsstempel)
                internal fun opprett(type: Behovtype, kontekster: List<SpesifikkKontekst>, melding: String, detaljer: Map<String, Any?>) = Behov(UUID.randomUUID(), type, kontekster, melding, detaljer)

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

                internal fun dødsinformasjon(aktivitetslogg: IAktivitetslogg) {
                    aktivitetslogg.behov(
                        Behovtype.Dødsinfo,
                        "Trenger informasjon om dødsdato fra PDL"
                    )
                }

                internal fun inntekterForSammenligningsgrunnlag(
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

                internal fun inntekterForSykepengegrunnlag(
                    aktivitetslogg: IAktivitetslogg,
                    beregningStart: YearMonth,
                    beregningSlutt: YearMonth
                ) {
                    aktivitetslogg.behov(
                        Behovtype.InntekterForSykepengegrunnlag,
                        "Trenger inntekter for sykepengegrunnlag",
                        mapOf(
                            "beregningStart" to beregningStart.toString(),
                            "beregningSlutt" to beregningSlutt.toString()
                        )
                    )
                }

                internal fun arbeidsforhold(aktivitetslogg: IAktivitetslogg) {
                    aktivitetslogg.behov(Behovtype.ArbeidsforholdV2, "Trenger informasjon om arbeidsforhold")
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

                internal fun godkjenning(
                    aktivitetslogg: IAktivitetslogg,
                    periodeFom: LocalDate,
                    periodeTom: LocalDate,
                    skjæringstidspunkt: LocalDate,
                    vedtaksperiodeaktivitetslogg: Aktivitetslogg,
                    periodetype: Periodetype,
                    førstegangsbehandling: Boolean,
                    utbetalingtype: Utbetalingtype,
                    inntektskilde: Inntektskilde,
                    orgnummereMedRelevanteArbeidsforhold: List<String>,
                    arbeidsforholdId: String?,
                ) {
                    aktivitetslogg.behov(
                        Behovtype.Godkjenning, "Forespør godkjenning fra saksbehandler", mapOf(
                            "periodeFom" to periodeFom.toString(),
                            "periodeTom" to periodeTom.toString(),
                            "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                            "periodetype" to periodetype.name,
                            "førstegangsbehandling" to førstegangsbehandling,
                            "utbetalingtype" to utbetalingtype.name,
                            "inntektskilde" to inntektskilde.name,
                            "warnings" to Aktivitetslogg().apply {
                                aktiviteter.addAll(vedtaksperiodeaktivitetslogg.varsel())

                            }.toMap(),
                            "orgnummereMedRelevanteArbeidsforhold" to orgnummereMedRelevanteArbeidsforhold,
                            "arbeidsforholdId" to arbeidsforholdId
                        )
                    )
                }
            }

            fun detaljer() = detaljer

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitBehov(id, kontekster, this, type, melding, detaljer, tidsstempel)
            }

            override fun notify(observer: AktivitetsloggObserver) {}

            enum class Behovtype {
                Sykepengehistorikk,
                SykepengehistorikkForFeriepenger,
                Foreldrepenger,
                Pleiepenger,
                Omsorgspenger,
                Opplæringspenger,
                Institusjonsopphold,

                @Deprecated("Behovet er ikke i bruk, men beholdes for derserialisering av aktivitetsloggen")
                EgenAnsatt,
                Godkjenning,
                Simulering,
                Utbetaling,
                InntekterForSammenligningsgrunnlag,
                InntekterForSykepengegrunnlag,

                @Deprecated("Behovet er ikke i bruk, men beholdes for derserialisering av aktivitetsloggen")
                Opptjening,
                Dagpenger,
                Arbeidsavklaringspenger,
                Medlemskap,
                Dødsinfo,
                ArbeidsforholdV2
            }
        }

        internal class FunksjonellFeil private constructor(
            id: UUID,
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(id, 75, 'E', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<FunksjonellFeil> {
                    return aktiviteter.filterIsInstance<FunksjonellFeil>()
                }
                internal fun gjennopprett(id: UUID, kontekster: List<SpesifikkKontekst>, melding: String, tidsstempel: String) = FunksjonellFeil(id, kontekster, melding, tidsstempel)
                internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) = FunksjonellFeil(UUID.randomUUID(), kontekster, melding)
            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitFunksjonellFeil(id, kontekster, this, melding, tidsstempel)
            }
        }

        internal class LogiskFeil private constructor(
            id: UUID,
            kontekster: List<SpesifikkKontekst>,
            private val melding: String,
            private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
        ) : Aktivitet(id, 100, 'S', melding, tidsstempel, kontekster) {
            companion object {
                internal fun filter(aktiviteter: List<Aktivitet>): List<LogiskFeil> {
                    return aktiviteter.filterIsInstance<LogiskFeil>()
                }

                internal fun gjennopprett(id: UUID, kontekster: List<SpesifikkKontekst>, melding: String, tidsstempel: String) = LogiskFeil(id, kontekster, melding, tidsstempel)
                internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) = LogiskFeil(UUID.randomUUID(), kontekster, melding)
            }

            override fun accept(visitor: AktivitetsloggVisitor) {
                visitor.visitLogiskFeil(id, kontekster, this, melding, tidsstempel)
            }
        }
    }
}

interface IAktivitetslogg {
    fun info(melding: String, vararg params: Any?)
    fun behov(type: Behov.Behovtype, melding: String, detaljer: Map<String, Any?> = emptyMap())
    fun varsel(melding: String)
    fun funksjonellFeil(melding: String)
    fun logiskFeil(melding: String, vararg params: Any?): Nothing

    fun harAktiviteter(): Boolean
    fun harVarslerEllerVerre(): Boolean
    fun harFunksjonelleFeilEllerVerre(): Boolean

    fun aktivitetsteller(): Int
    fun behov(): List<Behov>
    fun barn(): Aktivitetslogg
    fun kontekst(kontekst: Aktivitetskontekst)
    fun kontekst(person: Person)
    fun kontekster(): List<IAktivitetslogg>
    fun hendelseskontekster(): Map<String, String>
    fun hendelseskontekst(): Hendelseskontekst
    fun toMap(): Map<String, List<Map<String, Any>>>

    fun register(observer: AktivitetsloggObserver)
}

internal interface AktivitetsloggVisitor {
    fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun visitInfo(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Info,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitVarsel(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Varsel,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitBehov(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Behov,
        type: Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
    }

    fun visitFunksjonellFeil(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.FunksjonellFeil,
        melding: String,
        tidsstempel: String
    ) {
    }

    fun visitLogiskFeil(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.LogiskFeil,
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

    internal fun sammeType(other: Aktivitetskontekst) = this.kontekstType == other.toSpesifikkKontekst().kontekstType

    fun toMap(): Map<String, Any> {
        return mapOf(
            "konteksttype" to kontekstType,
            "kontekstmap" to kontekstMap
        )
    }

    override fun equals(other: Any?) =
        this === other ||
                (other is SpesifikkKontekst
                        && this.kontekstMap == other.kontekstMap
                        && this.kontekstType == other.kontekstType)

    override fun hashCode(): Int {
        var result = kontekstType.hashCode()
        result = 31 * result + kontekstMap.hashCode()
        return result
    }
}
