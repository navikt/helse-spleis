package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.VedtaksperiodeHendelse
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.math.absoluteValue

class ModelVilkårsgrunnlag(
    hendelseId: UUID,
    private val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val rapportertDato: LocalDateTime,
    private val inntektsmåneder: List<Måned>,
    private val erEgenAnsatt: Boolean,
    private val originalJson: String,
    aktivitetslogger: Aktivitetslogger
) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Vilkårsgrunnlag, aktivitetslogger), VedtaksperiodeHendelse {
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun rapportertdato() = rapportertDato
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    private fun beregnetÅrsInntekt(): Double {
        return inntektsmåneder
            .flatMap { it.inntektsliste }
            .sumByDouble { it.beløp }
    }

    private fun avviksprosentInntekt(månedsinntektFraInntektsmelding: Double) =
        ((månedsinntektFraInntektsmelding * 12) - beregnetÅrsInntekt()).absoluteValue / beregnetÅrsInntekt()

    internal fun harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding: Double) =
        avviksprosentInntekt(månedsinntektFraInntektsmelding) > 0.25

    internal fun måHåndteresManuelt(månedsinntektFraInntektsmelding: Double): Resultat {
        val grunnlag = Grunnlagsdata(
            erEgenAnsatt,
            beregnetÅrsInntekt(),
            avviksprosentInntekt(månedsinntektFraInntektsmelding)
        )

        return Resultat(erEgenAnsatt || harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding), grunnlag)
    }

    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Vilkårsgrunnlag")
    }

    data class Måned(
        val årMåned: YearMonth,
        val inntektsliste: List<Inntekt>
    )

    data class Inntekt(
        val beløp: Double
    )

    data class Resultat(
        val resultat: Boolean,
        val grunnlagsdata: Grunnlagsdata
    )

    data class Grunnlagsdata(
        val erEgenAnsatt: Boolean,
        val beregnetÅrsinntektFraInntektskomponenten: Double,
        val avviksprosent: Double
    )
}

