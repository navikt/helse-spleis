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
    val halvG: Inntekt,
    val `2G`: Inntekt,
    val erSykepengegrunnlagetUnderHalvG: Boolean,
    val erSykepengegrunnlagetUnder2G: Boolean
) {

    fun erUnderMinsteinntektskrav(sekstisyvårsdagen: LocalDate, vedtaksperiode: Periode): Boolean {
        if (!erSykepengegrunnlagetUnderHalvG && !erSykepengegrunnlagetUnder2G) return false
        if (erSykepengegrunnlagetUnderHalvG && vedtaksperiode.erFørFylte67(sekstisyvårsdagen)) return true
        return erSykepengegrunnlagetUnder2G && vedtaksperiode.erEtterFylte67(sekstisyvårsdagen)
    }

    private fun Periode.erFørFylte67(sekstisyvårsdagen: LocalDate) = this.start <= sekstisyvårsdagen
    private fun Periode.erEtterFylte67(sekstisyvårsdagen: LocalDate) = this.endInclusive > sekstisyvårsdagen

    fun subsummere(subsumsjonslogg: Subsumsjonslogg, skjæringstidspunkt: LocalDate, beregningsgrunnlag: Inntekt, sekstisyvårsdagen: LocalDate, vedtaksperiode: Periode) {
        if (vedtaksperiode.erFørFylte67(sekstisyvårsdagen)) {
            subsumsjonslogg.logg(
                `§ 8-3 ledd 2 punktum 1`(
                    oppfylt = !erSykepengegrunnlagetUnderHalvG,
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                    minimumInntektÅrlig = halvG.årlig
                )
            )
        }

        if (vedtaksperiode.erEtterFylte67(sekstisyvårsdagen)) {
            subsumsjonslogg
                .logg(
                    `§ 8-51 ledd 2`(
                        oppfylt = !erSykepengegrunnlagetUnder2G,
                        utfallFom = maxOf(sekstisyvårsdagen.nesteDag, vedtaksperiode.start),
                        utfallTom = vedtaksperiode.endInclusive,
                        sekstisyvårsdag = sekstisyvårsdagen,
                        periodeFom = vedtaksperiode.start,
                        periodeTom = vedtaksperiode.endInclusive,
                        beregningsgrunnlagÅrlig = beregningsgrunnlag.årlig,
                        minimumInntektÅrlig = `2G`.årlig
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
                halvG = minsteinntektkravTilFylte67,
                `2G` = minsteinntektkravEtterFylte67,
                erSykepengegrunnlagetUnderHalvG = erUnderMinsteinntektskravTilFylte67,
                erSykepengegrunnlagetUnder2G = erUnderMinsteinntektEtterFylte67
            )
        }
    }
}
