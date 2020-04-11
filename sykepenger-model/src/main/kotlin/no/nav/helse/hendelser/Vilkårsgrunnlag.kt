package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlin.streams.toList

@Deprecated("inntektsmåneder, arbeidsforhold og erEgenAnsatt sendes som tre parametre til modellen")
class Vilkårsgrunnlag(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val arbeidsforhold: List<Arbeidsforhold>,
    private val erEgenAnsatt: Boolean
) : ArbeidstakerHendelse() {
    private companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28
    }

    private var grunnlagsdata: Grunnlagsdata? = null

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider(beregnetInntekt: BigDecimal, førsteFraværsdag: LocalDate): Aktivitetslogg {
        inntektsvurdering.valider(aktivitetslogg, beregnetInntekt)
        val antallOpptjeningsdager = Arbeidsforhold.antallOptjeningsdager(arbeidsforhold, førsteFraværsdag, orgnummer)
        grunnlagsdata = Grunnlagsdata(
            erEgenAnsatt = erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = inntektsvurdering.sammenligningsgrunnlag(),
            avviksprosent = inntektsvurdering.avviksprosent(),
            antallOpptjeningsdagerErMinst = antallOpptjeningsdager,
            harOpptjening = antallOpptjeningsdager >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER
        ).also {
            if (erEgenAnsatt) error("Støtter ikke behandling av NAV-ansatte eller familiemedlemmer av NAV-ansatte")
            else info("er ikke egen ansatt")

            if (it.harOpptjening) info("Har minst %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
            else error("Har mindre enn %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
        }
        return aktivitetslogg
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    class Inntektsvurdering(
        private val perioder: Map<YearMonth, List<Double>>
    ) {
        private companion object {
            private const val MAKSIMALT_TILLATT_AVVIK = .25
        }

        private val sammenligningsgrunnlag = perioder.flatMap { it.value }.sum()
        private var avviksprosent = Double.POSITIVE_INFINITY

        internal fun sammenligningsgrunnlag(): Double =
            (sammenligningsgrunnlag * 100).roundToInt() / 100.0 // behold to desimaler
        internal fun avviksprosent() = avviksprosent

        fun valider(aktivitetslogg: Aktivitetslogg, beregnetInntekt: BigDecimal): Aktivitetslogg {
            if (antallPerioder() > 12) aktivitetslogg.error("Forventer 12 eller færre inntektsmåneder")
            if (sammenligningsgrunnlag <= 0.0) return aktivitetslogg.apply { error("sammenligningsgrunnlaget er <= 0") }
            avviksprosent = avviksprosent(beregnetInntekt)
            if (avviksprosent > MAKSIMALT_TILLATT_AVVIK) aktivitetslogg.error("Har mer enn %.0f %% avvik", MAKSIMALT_TILLATT_AVVIK * 100)
            else aktivitetslogg.info("Har %.0f %% eller mindre avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK * 100, avviksprosent * 100)
            return aktivitetslogg
        }

        private fun antallPerioder() = perioder.keys.size
        private fun avviksprosent(beregnetInntekt: BigDecimal) =
            sammenligningsgrunnlag.toBigDecimal().let { sammenligningsgrunnlag ->
                beregnetInntekt.omregnetÅrsinntekt()
                    .minus(sammenligningsgrunnlag)
                    .abs()
                    .divide(sammenligningsgrunnlag, MathContext.DECIMAL128)
            }.toDouble()

        private fun BigDecimal.omregnetÅrsinntekt() = this * 12.toBigDecimal()
    }

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

    class Grunnlagsdata(
        internal val erEgenAnsatt: Boolean,
        internal val beregnetÅrsinntektFraInntektskomponenten: Double,
        internal val avviksprosent: Double,
        internal val antallOpptjeningsdagerErMinst: Int,
        internal val harOpptjening: Boolean
    )
}

