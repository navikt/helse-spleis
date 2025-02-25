package no.nav.helse.person.aktivitetslogg

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

sealed class Aktivitet(
    val id: UUID,
    private val alvorlighetsgrad: Int,
    val label: Char,
    val melding: String,
    val tidsstempel: LocalDateTime,
    val kontekster: List<SpesifikkKontekst>
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

    override fun toString() = label + "  \t" + tidsstempel.format(tidsstempelformat) + "  \t" + melding + meldingerString()

    private fun meldingerString(): String {
        return kontekster.joinToString(separator = "") { " (${it.melding()})" }
    }

    operator fun contains(kontekst: Aktivitetskontekst) = kontekst.toSpesifikkKontekst() in kontekster
    class Info private constructor(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        melding: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Aktivitet(id, 0, 'I', melding, tidsstempel, kontekster) {
        companion object {
            internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) =
                Info(UUID.randomUUID(), kontekster, melding)
        }
    }

    class Varsel private constructor(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        val kode: Varselkode,
        melding: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Aktivitet(id, 25, 'W', melding, tidsstempel, kontekster) {
        companion object {
            internal fun opprett(kontekster: List<SpesifikkKontekst>, kode: Varselkode, melding: String) =
                Varsel(UUID.randomUUID(), kontekster, kode, melding = melding)
        }
    }

    class Behov private constructor(
        id: UUID,
        val type: Behovtype,
        kontekster: List<SpesifikkKontekst>,
        melding: String,
        val detaljer: Map<String, Any?> = emptyMap(),
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Aktivitet(id, 50, 'N', melding, tidsstempel, kontekster) {
        companion object {
            internal fun opprett(type: Behovtype, kontekster: List<SpesifikkKontekst>, melding: String, detaljer: Map<String, Any?>) = Behov(
                UUID.randomUUID(), type, kontekster, melding, detaljer
            )

            fun utbetalingshistorikk(
                aktivitetslogg: IAktivitetslogg,
                periode: ClosedRange<LocalDate>
            ) {
                aktivitetslogg.behov(
                    Behovtype.Sykepengehistorikk, "Trenger sykepengehistorikk fra Infotrygd", mapOf(
                    "historikkFom" to periode.start.toString(),
                    "historikkTom" to periode.endInclusive.toString()
                )
                )
            }

            fun foreldrepenger(aktivitetslogg: IAktivitetslogg, periode: ClosedRange<LocalDate>) {
                aktivitetslogg.behov(
                    Behovtype.Foreldrepenger,
                    "Trenger informasjon om foreldrepengeytelser fra FPSAK", mapOf(
                    "foreldrepengerFom" to periode.start.toString(),
                    "foreldrepengerTom" to periode.endInclusive.toString()
                )
                )
            }

            fun pleiepenger(aktivitetslogg: IAktivitetslogg, periode: ClosedRange<LocalDate>) {
                aktivitetslogg.behov(
                    Behovtype.Pleiepenger,
                    "Trenger informasjon om pleiepengeytelser fra Infotrygd", mapOf(
                    "pleiepengerFom" to periode.start.toString(),
                    "pleiepengerTom" to periode.endInclusive.toString()
                )
                )
            }

            fun omsorgspenger(aktivitetslogg: IAktivitetslogg, periode: ClosedRange<LocalDate>) {
                aktivitetslogg.behov(
                    Behovtype.Omsorgspenger,
                    "Trenger informasjon om omsorgspengerytelser fra Infotrygd", mapOf(
                    "omsorgspengerFom" to periode.start.toString(),
                    "omsorgspengerTom" to periode.endInclusive.toString()
                )
                )
            }

            fun opplæringspenger(aktivitetslogg: IAktivitetslogg, periode: ClosedRange<LocalDate>) {
                aktivitetslogg.behov(
                    Behovtype.Opplæringspenger,
                    "Trenger informasjon om opplæringspengerytelser fra Infotrygd", mapOf(
                    "opplæringspengerFom" to periode.start.toString(),
                    "opplæringspengerTom" to periode.endInclusive.toString()
                )
                )
            }

            fun institusjonsopphold(aktivitetslogg: IAktivitetslogg, periode: ClosedRange<LocalDate>) {
                aktivitetslogg.behov(
                    Behovtype.Institusjonsopphold,
                    "Trenger informasjon om institusjonsopphold fra Inst2", mapOf(
                    "institusjonsoppholdFom" to periode.start.toString(),
                    "institusjonsoppholdTom" to periode.endInclusive.toString()
                )
                )
            }

            fun inntekterForSykepengegrunnlag(
                aktivitetslogg: IAktivitetslogg,
                skjæringstidspunkt: LocalDate,
                beregningStart: YearMonth,
                beregningSlutt: YearMonth
            ) {
                aktivitetslogg.behov(
                    Behovtype.InntekterForSykepengegrunnlag,
                    "Trenger inntekter for sykepengegrunnlag",
                    mapOf(
                        "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                        "beregningStart" to beregningStart.toString(),
                        "beregningSlutt" to beregningSlutt.toString()
                    )
                )
            }

            fun inntekterForSykepengegrunnlagForArbeidsgiver(
                aktivitetslogg: IAktivitetslogg,
                skjæringstidspunkt: LocalDate,
                organisasjonsnummer: String,
                beregningStart: YearMonth,
                beregningSlutt: YearMonth
            ) {
                aktivitetslogg.behov(
                    Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver,
                    "Trenger inntekter for sykepengegrunnlag for arbeidsgiver",
                    mapOf(
                        "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                        "organisasjonsnummer" to organisasjonsnummer,
                        "beregningStart" to beregningStart.toString(),
                        "beregningSlutt" to beregningSlutt.toString()
                    )
                )
            }

            fun inntekterForOpptjeningsvurdering(
                aktivitetslogg: IAktivitetslogg,
                skjæringstidspunkt: LocalDate,
                beregningStart: YearMonth,
                beregningSlutt: YearMonth
            ) {
                aktivitetslogg.behov(
                    Behovtype.InntekterForOpptjeningsvurdering,
                    "Trenger inntekter for opptjeningsvurdering",
                    mapOf(
                        "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                        "beregningStart" to beregningStart.toString(),
                        "beregningSlutt" to beregningSlutt.toString()
                    )
                )
            }

            fun inntekterForBeregning(aktivitetslogg: IAktivitetslogg, periode: ClosedRange<LocalDate>) {
                aktivitetslogg.behov(
                    Behovtype.InntekterForBeregning,
                    "Trenger informasjon om inntekter for beregning",
                    mapOf(
                        "fom" to periode.start.toString(),
                        "tom" to periode.endInclusive.toString()
                    )
                )
            }

            fun arbeidsforhold(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate) {
                aktivitetslogg.behov(
                    Behovtype.ArbeidsforholdV2, "Trenger informasjon om arbeidsforhold", mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt.toString()
                )
                )
            }

            fun dagpenger(aktivitetslogg: IAktivitetslogg, fom: LocalDate, tom: LocalDate) {
                aktivitetslogg.behov(
                    Behovtype.Dagpenger, "Trenger informasjon om dagpenger", mapOf(
                    "periodeFom" to fom.toString(),
                    "periodeTom" to tom.toString()
                )
                )
            }

            fun arbeidsavklaringspenger(aktivitetslogg: IAktivitetslogg, fom: LocalDate, tom: LocalDate) {
                aktivitetslogg.behov(
                    Behovtype.Arbeidsavklaringspenger, "Trenger informasjon om arbeidsavklaringspenger", mapOf(
                    "periodeFom" to fom.toString(),
                    "periodeTom" to tom.toString()
                )
                )
            }

            fun medlemskap(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate, fom: LocalDate, tom: LocalDate) {
                aktivitetslogg.behov(
                    Behovtype.Medlemskap, "Trenger informasjon om medlemskap", mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                    "medlemskapPeriodeFom" to fom.toString(),
                    "medlemskapPeriodeTom" to tom.toString()
                )
                )
            }

            fun godkjenning(aktivitetslogg: IAktivitetslogg, godkjenningsbehov: Map<String, Any>) {
                aktivitetslogg.behov(Behovtype.Godkjenning, "Forespør godkjenning fra saksbehandler", godkjenningsbehov)
            }
        }

        fun detaljer() = detaljer

        enum class Behovtype {
            Sykepengehistorikk,
            SykepengehistorikkForFeriepenger,
            Foreldrepenger,
            Pleiepenger,
            Omsorgspenger,
            Opplæringspenger,
            Institusjonsopphold,

            Godkjenning,
            Simulering,
            Utbetaling,
            InntekterForSykepengegrunnlag,
            InntekterForSykepengegrunnlagForArbeidsgiver,
            InntekterForOpptjeningsvurdering,
            InntekterForBeregning,

            Dagpenger,
            Arbeidsavklaringspenger,
            Medlemskap,
            Dødsinfo,
            ArbeidsforholdV2
        }
    }

    class FunksjonellFeil private constructor(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        val kode: Varselkode,
        melding: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Aktivitet(id, 75, 'E', melding, tidsstempel, kontekster) {
        companion object {
            internal fun opprett(kontekster: List<SpesifikkKontekst>, kode: Varselkode, melding: String) =
                FunksjonellFeil(UUID.randomUUID(), kontekster, kode, melding)
        }
    }

    class LogiskFeil private constructor(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        melding: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Aktivitet(id, 100, 'S', melding, tidsstempel, kontekster) {
        companion object {
            internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) =
                LogiskFeil(UUID.randomUUID(), kontekster, melding)
        }
    }
}
