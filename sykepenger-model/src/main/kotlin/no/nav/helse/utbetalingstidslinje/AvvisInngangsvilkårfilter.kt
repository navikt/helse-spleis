package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-3 ledd 2 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-51 ledd 2`
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.økonomi.Inntekt

data class Minsteinntektsvurdering(
    val minsteinntektkravTilFylte67: Inntekt,
    val minsteinntektkravEtterFylte67: Inntekt,
    val erUnderMinsteinntektskravTilFylte67: Boolean,
    val erUnderMinsteinntektEtterFylte67: Boolean,
    val periodeTilFylte67: Periode?,
    val periodeEtterFylte67: Periode?
) {
    val periodeTilFylte67UnderMinsteinntekt = periodeTilFylte67.takeIf { erUnderMinsteinntektskravTilFylte67 }
    val periodeEtterFylte67UnderMinsteinntekt = periodeEtterFylte67.takeIf { erUnderMinsteinntektEtterFylte67 }
    val erUnderMinsteinntektskrav = periodeTilFylte67UnderMinsteinntekt != null || periodeEtterFylte67UnderMinsteinntekt != null

    fun subsummere(subsumsjonslogg: Subsumsjonslogg, skjæringstidspunkt: LocalDate, beregningsgrunnlag: Inntekt, redusertYtelseAlder: LocalDate, vedtaksperiode: Periode) {
        if (periodeTilFylte67 != null) {
            subsumsjonslogg.logg(
                `§ 8-3 ledd 2 punktum 1`(
                    oppfylt = !erUnderMinsteinntektskravTilFylte67,
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                    minimumInntektÅrlig = minsteinntektkravTilFylte67.årlig
                )
            )
        }

        if (periodeEtterFylte67 != null) {
            subsumsjonslogg
                .logg(
                    `§ 8-51 ledd 2`(
                        oppfylt = !erUnderMinsteinntektEtterFylte67,
                        utfallFom = periodeEtterFylte67.start,
                        utfallTom = periodeEtterFylte67.endInclusive,
                        sekstisyvårsdag = redusertYtelseAlder,
                        periodeFom = vedtaksperiode.start,
                        periodeTom = vedtaksperiode.endInclusive,
                        beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                        minimumInntektÅrlig = minsteinntektkravEtterFylte67.årlig
                    )
                )
        }
    }

    companion object {
        fun lagMinsteinntektsvurdering(skjæringstidspunkt: LocalDate, vedtaksperiode: Periode, sykepengegrunnlag: Inntekt, redusertYtelseAlder: LocalDate): Minsteinntektsvurdering {
            val minsteinntektkravTilFylte67 = halvG.minsteinntekt(skjæringstidspunkt)
            val minsteinntektkravEtterFylte67 = `2G`.minsteinntekt(skjæringstidspunkt)
            val erUnderMinsteinntektskravTilFylte67 = sykepengegrunnlag < minsteinntektkravTilFylte67
            val erUnderMinsteinntektEtterFylte67 = sykepengegrunnlag < minsteinntektkravEtterFylte67
            val periodeTilFylte67 = vedtaksperiode.beholdDagerTil(redusertYtelseAlder)
            val periodeEtterFylte67 = vedtaksperiode.beholdDagerEtter(redusertYtelseAlder)

            return Minsteinntektsvurdering(
                minsteinntektkravTilFylte67 = minsteinntektkravTilFylte67,
                minsteinntektkravEtterFylte67 = minsteinntektkravEtterFylte67,
                erUnderMinsteinntektskravTilFylte67 = erUnderMinsteinntektskravTilFylte67,
                erUnderMinsteinntektEtterFylte67 = erUnderMinsteinntektEtterFylte67,
                periodeTilFylte67 = periodeTilFylte67,
                periodeEtterFylte67 = periodeEtterFylte67
            )
        }
    }
}

internal class AvvisInngangsvilkårfilter(
    private val periodeTilFylte67UnderMinsteinntekt: Periode?,
    private val periodeEtterFylte67UnderMinsteinntekt: Periode?,
    private val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus?,
    private val harOpptjening: Boolean
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        return arbeidsgivere
            .minsteinntekt()
            .avvisMedlemskap()
            .avvisOpptjening()
    }

    private fun List<Arbeidsgiverberegning>.minsteinntekt(): List<Arbeidsgiverberegning> {
        val avviste = this
            .avvis(listOfNotNull(periodeTilFylte67UnderMinsteinntekt), Begrunnelse.MinimumInntekt)
            .avvis(listOfNotNull(periodeEtterFylte67UnderMinsteinntekt), Begrunnelse.MinimumInntektOver67)

        return avviste
    }

    private fun List<Arbeidsgiverberegning>.avvisMedlemskap(): List<Arbeidsgiverberegning> {
        if (medlemskapstatus != Medlemskapsvurdering.Medlemskapstatus.Nei) return this
        return avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerMedlemskap)
    }

    private fun List<Arbeidsgiverberegning>.avvisOpptjening(): List<Arbeidsgiverberegning> {
        if (harOpptjening) return this
        return avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerOpptjening)
    }
}
