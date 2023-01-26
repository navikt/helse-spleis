package no.nav.helse.person.aktivitetslogg

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.hendelser.Periode

sealed class Aktivitet(
    protected val id: UUID,
    private val alvorlighetsgrad: Int,
    protected val label: Char,
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
        observer.aktivitet(id, label, melding, kontekster, LocalDateTime.parse(tidsstempel, tidsstempelformat))
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

            internal fun gjennopprett(id: UUID, kontekster: List<SpesifikkKontekst>, melding: String, tidsstempel: String) =
                Info(id, kontekster, melding, tidsstempel)
            internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) =
                Info(UUID.randomUUID(), kontekster, melding)
        }

        override fun accept(visitor: AktivitetsloggVisitor) {
            visitor.visitInfo(id, kontekster, this, melding, tidsstempel)
        }
    }

    internal class Varsel private constructor(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        private val kode: Varselkode? = null,
        private val melding: String,
        private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
    ) : Aktivitet(id, 25, 'W', melding, tidsstempel, kontekster) {
        companion object {
            internal fun filter(aktiviteter: List<Aktivitet>): List<Varsel> {
                return aktiviteter.filterIsInstance<Varsel>()
            }
            internal fun gjennopprett(id: UUID, kontekster: List<SpesifikkKontekst>, kode: Varselkode?, melding: String, tidsstempel: String) =
                Varsel(id, kontekster, kode, melding, tidsstempel)

            internal fun opprett(kontekster: List<SpesifikkKontekst>, kode: Varselkode? = null, melding: String) =
                Varsel(UUID.randomUUID(), kontekster, kode, melding = melding)
        }

        override fun accept(visitor: AktivitetsloggVisitor) {
            visitor.visitVarsel(id, kontekster, this, kode, melding, tidsstempel)
        }

        override fun notify(observer: AktivitetsloggObserver) {
            observer.varsel(id, label, kode, melding, kontekster, LocalDateTime.parse(tidsstempel, tidsstempelformat))
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
            internal fun opprett(type: Behovtype, kontekster: List<SpesifikkKontekst>, melding: String, detaljer: Map<String, Any?>) = Behov(
                UUID.randomUUID(), type, kontekster, melding, detaljer)

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
                periodetype: String,
                førstegangsbehandling: Boolean,
                utbetalingtype: String,
                inntektskilde: String,
                orgnummereMedRelevanteArbeidsforhold: List<String>,
                arbeidsforholdId: String?,
            ) {
                aktivitetslogg.behov(
                    Behovtype.Godkjenning, "Forespør godkjenning fra saksbehandler", mapOf(
                        "periodeFom" to periodeFom.toString(),
                        "periodeTom" to periodeTom.toString(),
                        "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                        "periodetype" to periodetype,
                        "førstegangsbehandling" to førstegangsbehandling,
                        "utbetalingtype" to utbetalingtype,
                        "inntektskilde" to inntektskilde,
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
            internal fun gjennopprett(id: UUID, kontekster: List<SpesifikkKontekst>, melding: String, tidsstempel: String) =
                FunksjonellFeil(id, kontekster, melding, tidsstempel)
            internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) =
                FunksjonellFeil(UUID.randomUUID(), kontekster, melding)
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

            internal fun gjennopprett(id: UUID, kontekster: List<SpesifikkKontekst>, melding: String, tidsstempel: String) =
                LogiskFeil(id, kontekster, melding, tidsstempel)
            internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) =
                LogiskFeil(UUID.randomUUID(), kontekster, melding)
        }

        override fun accept(visitor: AktivitetsloggVisitor) {
            visitor.visitLogiskFeil(id, kontekster, this, melding, tidsstempel)
        }
    }
}