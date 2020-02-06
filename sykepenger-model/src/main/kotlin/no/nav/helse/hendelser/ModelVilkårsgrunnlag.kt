package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.VedtaksperiodeHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.math.absoluteValue
import kotlin.streams.toList

class ModelVilkårsgrunnlag(
    hendelseId: UUID,
    private val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val rapportertDato: LocalDateTime,
    private val inntektsmåneder: List<Måned>,
    private val arbeidsforhold: List<Arbeidsforhold>,
    private val erEgenAnsatt: Boolean,
    aktivitetslogger: Aktivitetslogger
) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Vilkårsgrunnlag, aktivitetslogger), VedtaksperiodeHendelse {
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun rapportertdato() = rapportertDato
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    private fun antallOpptjeningsdager(førsteFraværsdag: LocalDate) = arbeidsforhold
            .filter { it.orgnummer == orgnummer }
            .filter { it.tom == null || it.tom.isAfter(førsteFraværsdag) }
            .map { it.fom }
            .min()?.datesUntil(førsteFraværsdag)?.toList()?.size ?: 0

    private fun beregnetÅrsInntekt(): Double {
        assert(inntektsmåneder.size <= 12)
        return inntektsmåneder
            .flatMap { it.inntektsliste }
            .sumByDouble { it.beløp }
    }

    private fun avviksprosentInntekt(månedsinntektFraInntektsmelding: Double) =
        ((månedsinntektFraInntektsmelding * 12) - beregnetÅrsInntekt()).absoluteValue / beregnetÅrsInntekt()

    internal fun harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding: Double) =
        avviksprosentInntekt(månedsinntektFraInntektsmelding) > 0.25

    internal fun måHåndteresManuelt(
        månedsinntektFraInntektsmelding: Double,
        førsteFraværsdag: LocalDate
    ): Resultat {
        val grunnlag = Grunnlagsdata(
            erEgenAnsatt,
            beregnetÅrsInntekt(),
            avviksprosentInntekt(månedsinntektFraInntektsmelding),
            antallOpptjeningsdager(førsteFraværsdag) >= 28
        )

        val harAvvikIOppgittInntekt = harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding)

        if (erEgenAnsatt) aktivitetslogger.warn("Er egen ansatt")
        else aktivitetslogger.info("er ikke egen ansatt")

        if (harAvvikIOppgittInntekt) aktivitetslogger.warn("Har ${grunnlag.avviksprosent*100} %% avvik i inntekt")
        else aktivitetslogger.info("har ${grunnlag.avviksprosent*100} %% avvik i inntekt")

        return Resultat(erEgenAnsatt || harAvvikIOppgittInntekt, grunnlag)
    }

    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Vilkårsgrunnlag")
    }

    override fun accept(visitor: PersonVisitor) {
        visitor.visitVilkårsgrunnlagHendelse(this)
    }

    data class Måned(
        internal val årMåned: YearMonth,
        internal val inntektsliste: List<Inntekt>
    )

    data class Inntekt(
        internal val beløp: Double
    )

    data class Arbeidsforhold (
        internal val orgnummer: String,
        internal val fom: LocalDate,
        internal val tom: LocalDate? = null
    )

    data class Resultat(
        internal val måBehandlesManuelt: Boolean,
        internal val grunnlagsdata: Grunnlagsdata
    )

    data class Grunnlagsdata(
        internal val erEgenAnsatt: Boolean,
        internal val beregnetÅrsinntektFraInntektskomponenten: Double,
        internal val avviksprosent: Double,
        internal val harOpptjening: Boolean
    )
}

