package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import kotlin.streams.toList

@Deprecated("inntektsmåneder, arbeidsforhold og erEgenAnsatt sendes som tre parametre til modellen")
class Vilkårsgrunnlag(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val inntektsmåneder: List<Måned>,
    private val arbeidsforhold: List<Arbeidsforhold>,
    private val erEgenAnsatt: Boolean
) : ArbeidstakerHendelse() {
    private companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28
        private const val MAKSIMALT_TILLATT_AVVIK = .25
    }

    private val sammenligningsgrunnlag: BigDecimal

    init {
        sammenligningsgrunnlag = inntektsmåneder
            .flatMap { it.inntektsliste }
            .sumByDouble { it }
            .toBigDecimal()
    }

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    internal fun valider(): Aktivitetslogg {
        if (inntektsmåneder.size > 12) error("Forventer 12 eller færre inntektsmåneder")
        if (sammenligningsgrunnlag <= BigDecimal.ZERO) error("sammenligningsgrunnlaget er <= 0")
        return aktivitetslogg
    }

    private fun avviksprosentInntekt(beregnetInntekt: BigDecimal) =
        (beregnetInntekt * 12.toBigDecimal())
            .minus(sammenligningsgrunnlag)
            .abs()
            .divide(sammenligningsgrunnlag, MathContext.DECIMAL128)

    internal fun harAvvikIOppgittInntekt(beregnetInntekt: BigDecimal) =
        avviksprosentInntekt(beregnetInntekt) > MAKSIMALT_TILLATT_AVVIK.toBigDecimal()

    internal fun måHåndteresManuelt(
        beregnetInntekt: BigDecimal,
        førsteFraværsdag: LocalDate
    ): Resultat {
        val antallOpptjeningsdager = Arbeidsforhold.antallOptjeningsdager(arbeidsforhold, førsteFraværsdag, orgnummer)
        val grunnlag = Grunnlagsdata(
            erEgenAnsatt = erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = sammenligningsgrunnlag.setScale(
                2,
                RoundingMode.HALF_UP
            ).toDouble(),
            avviksprosent = avviksprosentInntekt(beregnetInntekt).toDouble(),
            antallOpptjeningsdagerErMinst = antallOpptjeningsdager,
            harOpptjening = antallOpptjeningsdager >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER
        )

        val harAvvikIOppgittInntekt = harAvvikIOppgittInntekt(beregnetInntekt)

        if (erEgenAnsatt) error("Støtter ikke behandling av NAV-ansatte eller familiemedlemmer av NAV-ansatte")
        else info("er ikke egen ansatt")

        if (harAvvikIOppgittInntekt) error("Har mer enn %.0f %% avvik", MAKSIMALT_TILLATT_AVVIK * 100)
        else info("Har %.0f %% eller mindre avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK * 100, grunnlag.avviksprosent * 100)

        if (grunnlag.harOpptjening) info("Har minst %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
        else error("Har mindre enn %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)

        return Resultat(erEgenAnsatt || harAvvikIOppgittInntekt || !grunnlag.harOpptjening, grunnlag)
    }

    class Måned(
        internal val årMåned: YearMonth,
        internal val inntektsliste: List<Double>
    )

    class Arbeidsforhold(
        private val orgnummer: String,
        private val fom: LocalDate,
        private val tom: LocalDate? = null
    ) {
        companion object {
            fun antallOptjeningsdager(liste: List<Arbeidsforhold>, førsteFraværsdag: LocalDate, orgnummer: String) = liste
                .filter { it.orgnummer == orgnummer }
                .filter { it.fom <= førsteFraværsdag }
                .filter { it.tom == null || it.tom.isAfter(førsteFraværsdag) }
                .map { it.fom }
                .min()
                ?.datesUntil(førsteFraværsdag)
                ?.toList()
                ?.size ?: 0
        }
    }

    class Resultat(
        internal val måBehandlesManuelt: Boolean,
        internal val grunnlagsdata: Grunnlagsdata
    )

    class Grunnlagsdata(
        internal val erEgenAnsatt: Boolean,
        internal val beregnetÅrsinntektFraInntektskomponenten: Double,
        internal val avviksprosent: Double,
        internal val antallOpptjeningsdagerErMinst: Int,
        internal val harOpptjening: Boolean
    )
}

