package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-3 ledd 2 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-51 ledd 2`
import no.nav.helse.hendelser.Periode
import no.nav.helse.nesteDag
import no.nav.helse.økonomi.Inntekt

data class Minsteinntektsvurdering(
    val minsteinntektkravTilFylte67: Inntekt,
    val minsteinntektkravEtterFylte67: Inntekt,
    val erUnderMinsteinntektskravTilFylte67: Boolean,
    val erUnderMinsteinntektEtterFylte67: Boolean
) {

    fun erUnderMinsteinntektskrav(sekstisyvårsdagen: LocalDate, vedtaksperiode: Periode): Boolean {
        if (!erUnderMinsteinntektskravTilFylte67 && !erUnderMinsteinntektEtterFylte67) return false
        if (erUnderMinsteinntektskravTilFylte67 && vedtaksperiode.erInnenforMinsteinntektskravTilFylte67(sekstisyvårsdagen)) return true
        return erUnderMinsteinntektEtterFylte67 && vedtaksperiode.erInnenforMinsteinntektskravEtterFylte67(sekstisyvårsdagen)
    }

    private fun Periode.erInnenforMinsteinntektskravTilFylte67(sekstisyvårsdagen: LocalDate) = this.start <= sekstisyvårsdagen
    private fun Periode.erInnenforMinsteinntektskravEtterFylte67(sekstisyvårsdagen: LocalDate) = this.endInclusive > sekstisyvårsdagen

    fun subsummere(subsumsjonslogg: Subsumsjonslogg, skjæringstidspunkt: LocalDate, beregningsgrunnlag: Inntekt, sekstisyvårsdagen: LocalDate, vedtaksperiode: Periode) {
        if (vedtaksperiode.erInnenforMinsteinntektskravTilFylte67(sekstisyvårsdagen)) {
            subsumsjonslogg.logg(
                `§ 8-3 ledd 2 punktum 1`(
                    oppfylt = !erUnderMinsteinntektskravTilFylte67,
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                    minimumInntektÅrlig = minsteinntektkravTilFylte67.årlig
                )
            )
        }

        if (vedtaksperiode.erInnenforMinsteinntektskravEtterFylte67(sekstisyvårsdagen)) {
            subsumsjonslogg
                .logg(
                    `§ 8-51 ledd 2`(
                        oppfylt = !erUnderMinsteinntektEtterFylte67,
                        utfallFom = maxOf(sekstisyvårsdagen.nesteDag, vedtaksperiode.start),
                        utfallTom = vedtaksperiode.endInclusive,
                        sekstisyvårsdag = sekstisyvårsdagen,
                        periodeFom = vedtaksperiode.start,
                        periodeTom = vedtaksperiode.endInclusive,
                        beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                        minimumInntektÅrlig = minsteinntektkravEtterFylte67.årlig
                    )
                )
        }
    }

    companion object {
        fun lagMinsteinntektsvurdering(skjæringstidspunkt: LocalDate, sykepengegrunnlag: Inntekt): Minsteinntektsvurdering {
            val minsteinntektkravTilFylte67 = halvG.minsteinntekt(skjæringstidspunkt)
            val minsteinntektkravEtterFylte67 = `2G`.minsteinntekt(skjæringstidspunkt)
            val erUnderMinsteinntektskravTilFylte67 = sykepengegrunnlag < minsteinntektkravTilFylte67
            val erUnderMinsteinntektEtterFylte67 = sykepengegrunnlag < minsteinntektkravEtterFylte67

            return Minsteinntektsvurdering(
                minsteinntektkravTilFylte67 = minsteinntektkravTilFylte67,
                minsteinntektkravEtterFylte67 = minsteinntektkravEtterFylte67,
                erUnderMinsteinntektskravTilFylte67 = erUnderMinsteinntektskravTilFylte67,
                erUnderMinsteinntektEtterFylte67 = erUnderMinsteinntektEtterFylte67
            )
        }
    }
}
