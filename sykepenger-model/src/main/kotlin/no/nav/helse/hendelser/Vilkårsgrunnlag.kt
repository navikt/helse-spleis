package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.absoluteValue
import kotlin.streams.toList

@Deprecated("inntektsmåneder, arbeidsforhold og erEgenAnsatt sendes som tre parametre til modellen")
class Vilkårsgrunnlag(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val inntektsmåneder: List<Måned>,
    private val arbeidsforhold: MangeArbeidsforhold,
    private val erEgenAnsatt: Boolean
) : ArbeidstakerHendelse() {
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    private fun beregnetÅrsInntekt(): Double {
        if (inntektsmåneder.size > 12) severe("Forventer 12 eller færre inntektsmåneder")
        return inntektsmåneder
            .flatMap { it.inntektsliste }
            .sumByDouble { it }
    }

    private fun avviksprosentInntekt(månedsinntektFraInntektsmelding: Double) =
        ((månedsinntektFraInntektsmelding * 12) - beregnetÅrsInntekt()).absoluteValue / beregnetÅrsInntekt()

    internal fun harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding: Double) =
        avviksprosentInntekt(månedsinntektFraInntektsmelding) > 0.25

    internal fun måHåndteresManuelt(
        månedsinntektFraInntektsmelding: Double,
        førsteFraværsdag: LocalDate
    ): Resultat {
        val antallOpptjeningsdager =  arbeidsforhold.antallOpptjeningsdager(førsteFraværsdag, orgnummer)
        val grunnlag = Grunnlagsdata(
            erEgenAnsatt = erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = beregnetÅrsInntekt(),
            avviksprosent = avviksprosentInntekt(månedsinntektFraInntektsmelding),
            antallOpptjeningsdagerErMinst = antallOpptjeningsdager,
            harOpptjening = antallOpptjeningsdager >= 28
        )

        val harAvvikIOppgittInntekt = harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding)

        if (erEgenAnsatt) error("Støtter ikke behandling av NAV-ansatte eller familiemedlemmer av NAV-ansatte")
        else info("er ikke egen ansatt")

        if (harAvvikIOppgittInntekt) error("Har mer enn 25 %% avvik")
        else info("Har 25 %% eller mindre avvik i inntekt (${grunnlag.avviksprosent*100} %%)")

        if(grunnlag.harOpptjening) info("Har minst 28 dager opptjening")
        else error("Har mindre enn 28 dager opptjening")

        return Resultat(erEgenAnsatt || harAvvikIOppgittInntekt || !grunnlag.harOpptjening, grunnlag)
    }

    class Måned(
        internal val årMåned: YearMonth,
        internal val inntektsliste: List<Double>
    )

    class Arbeidsforhold (
        internal val orgnummer: String,
        internal val fom: LocalDate,
        internal val tom: LocalDate? = null
    )

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

    class MangeArbeidsforhold(
        private val arbeidsforhold: List<Arbeidsforhold>
    ) {
        internal fun antallOpptjeningsdager(førsteFraværsdag: LocalDate, orgnummer: String) = arbeidsforhold
            .filter { it.orgnummer == orgnummer }
            .filter { it.tom == null || it.tom.isAfter(førsteFraværsdag) }
            .map { it.fom }
            .min()?.datesUntil(førsteFraværsdag)?.toList()?.size ?: 0
    }
}

