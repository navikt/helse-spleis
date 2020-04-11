package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@Deprecated("inntektsvurdering, opptjeningvurdering og erEgenAnsatt sendes som tre parametre til modellen")
class Vilkårsgrunnlag(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val opptjeningvurdering: Opptjeningvurdering,
    private val erEgenAnsatt: Boolean
) : ArbeidstakerHendelse() {
    private var grunnlagsdata: Grunnlagsdata? = null

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider(beregnetInntekt: BigDecimal, førsteFraværsdag: LocalDate): Aktivitetslogg {
        inntektsvurdering.valider(aktivitetslogg, beregnetInntekt)
        opptjeningvurdering.valider(aktivitetslogg, orgnummer, førsteFraværsdag)
        if (erEgenAnsatt) error("Støtter ikke behandling av NAV-ansatte eller familiemedlemmer av NAV-ansatte")
        else info("er ikke egen ansatt")
        grunnlagsdata = Grunnlagsdata(
            erEgenAnsatt = erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = inntektsvurdering.sammenligningsgrunnlag(),
            avviksprosent = inntektsvurdering.avviksprosent(),
            antallOpptjeningsdagerErMinst = opptjeningvurdering.opptjeningsdager(orgnummer),
            harOpptjening = opptjeningvurdering.harOpptjening(orgnummer)
        )
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

    class Opptjeningvurdering(
        private val arbeidsforhold: List<Arbeidsforhold>
    ) {
        private companion object {
            private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28
        }

        private val antallOpptjeningsdager = mutableMapOf<String, Int>()

        internal fun opptjeningsdager(orgnummer: String) = antallOpptjeningsdager[orgnummer] ?: 0
        internal fun harOpptjening(orgnummer: String) = opptjeningsdager(orgnummer) >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

        fun valider(aktivitetslogg: Aktivitetslogg, orgnummer: String, førsteFraværsdag: LocalDate): Aktivitetslogg {
            Arbeidsforhold.opptjeningsdager(arbeidsforhold, antallOpptjeningsdager, førsteFraværsdag)
            if (harOpptjening(orgnummer)) aktivitetslogg.info("Har minst %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
            else aktivitetslogg.error("Har mindre enn %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
            return aktivitetslogg
        }

        class Arbeidsforhold(
            private val orgnummer: String,
            private val fom: LocalDate,
            private val tom: LocalDate? = null
        ) {
            private fun opptjeningsdager(førsteFraværsdag: LocalDate): Int {
                if (fom > førsteFraværsdag) return 0
                if (tom != null && tom < førsteFraværsdag) return 0
                return fom.datesUntil(førsteFraværsdag).count().toInt()
            }

            companion object {
                fun opptjeningsdager(liste: List<Arbeidsforhold>, map: MutableMap<String, Int>, førsteFraværsdag: LocalDate) {
                    liste.forEach { map[it.orgnummer] = it.opptjeningsdager(førsteFraværsdag) }
                }
            }
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

