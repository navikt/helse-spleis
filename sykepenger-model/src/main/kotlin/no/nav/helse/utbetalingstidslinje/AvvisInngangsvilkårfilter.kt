package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-3 ledd 2 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-51 ledd 2`
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.ArbeidstakerOpptjening
import no.nav.helse.person.Opptjening
import no.nav.helse.person.SelvstendigNæringsdrivendeOpptjening
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager

internal class AvvisInngangsvilkårfilter(
    private val skjæringstidspunkt: LocalDate,
    private val alder: Alder,
    private val subsumsjonslogg: Subsumsjonslogg,
    private val aktivitetslogg: IAktivitetslogg,
    private val inntektsgrunnlag: Inntektsgrunnlag,
    private val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus?,
    private val opptjening: Opptjening?
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        return arbeidsgivere
            .minsteinntekt(vedtaksperiode)
            .avvisMedlemskap()
            .avvisOpptjening()
    }

    private fun List<Arbeidsgiverberegning>.minsteinntekt(periode: Periode): List<Arbeidsgiverberegning> {
        // dager frem til og med alder.redusertYtelseAlder avvises hvis sykepengegrunnlaget er under halvG
        val minsteinntektkravTilFylte67 = halvG.minsteinntekt(skjæringstidspunkt)
        val erUnderMinsteinntektskravTilFylte67 = inntektsgrunnlag.sykepengegrunnlag < minsteinntektkravTilFylte67
        val aktuellPeriodeTilFylte67 = LocalDate.MIN til alder.redusertYtelseAlder

        // dager etter alder.redusertYtelseAlder avvises hvis sykepengegrunnlaget er under 2g
        val minsteinntektkravEtterFylte67 = `2G`.minsteinntekt(skjæringstidspunkt)
        val erUnderMinsteinntektEtterFylte67 = inntektsgrunnlag.sykepengegrunnlag < minsteinntektkravEtterFylte67
        val aktuellPeriodeEtterFylte67 = alder.redusertYtelseAlder.nesteDag til LocalDate.MAX

        val avviste = this
            .avvis(if (erUnderMinsteinntektskravTilFylte67) listOf(aktuellPeriodeTilFylte67) else emptyList(), Begrunnelse.MinimumInntekt)
            .avvis(if (erUnderMinsteinntektEtterFylte67) listOf(aktuellPeriodeEtterFylte67) else emptyList(), Begrunnelse.MinimumInntektOver67)


        val avvisteDagerFremTil67 = avvisteDager(avviste.map { it.samletVedtaksperiodetidslinje }, periode, Begrunnelse.MinimumInntekt).map { it.dato }
        val avvisteDagerOver67 = avvisteDager(avviste.map { it.samletVedtaksperiodetidslinje }, periode, Begrunnelse.MinimumInntektOver67).map { it.dato }

        if (avvisteDagerFremTil67.isNotEmpty() || avvisteDagerOver67.isNotEmpty()) {
            aktivitetslogg.varsel(RV_SV_1)
        } else {
            aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
        }

        val periodeTilFylte67 = aktuellPeriodeTilFylte67.overlappendePeriode(periode)
        if (periodeTilFylte67 != null) {
            subsumsjonslogg.logg(
                `§ 8-3 ledd 2 punktum 1`(
                    oppfylt = !erUnderMinsteinntektskravTilFylte67,
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlagÅrlig = inntektsgrunnlag.beregningsgrunnlag.årlig,
                    minimumInntektÅrlig = minsteinntektkravTilFylte67.årlig
                )
            )
        }

        val periodeEtterFylte67 = aktuellPeriodeEtterFylte67.overlappendePeriode(periode)
        if (periodeEtterFylte67 != null) {
            subsumsjonslogg
                .logg(
                    `§ 8-51 ledd 2`(
                        oppfylt = !erUnderMinsteinntektEtterFylte67,
                        utfallFom = periodeEtterFylte67.start,
                        utfallTom = periodeEtterFylte67.endInclusive,
                        sekstisyvårsdag = alder.redusertYtelseAlder,
                        periodeFom = periode.start,
                        periodeTom = periode.endInclusive,
                        beregningsgrunnlagÅrlig = inntektsgrunnlag.beregningsgrunnlag.årlig,
                        minimumInntektÅrlig = minsteinntektkravEtterFylte67.årlig
                    )
                )
        }

        return avviste
    }

    private fun List<Arbeidsgiverberegning>.avvisMedlemskap(): List<Arbeidsgiverberegning> {
        if (medlemskapstatus != Medlemskapsvurdering.Medlemskapstatus.Nei) return this
        return avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerMedlemskap)
    }

    private fun List<Arbeidsgiverberegning>.avvisOpptjening(): List<Arbeidsgiverberegning> {
        if (opptjening == null) return this
        when (opptjening) {
            is ArbeidstakerOpptjening -> if (opptjening.harTilstrekkeligAntallOpptjeningsdager()) return this
            is SelvstendigNæringsdrivendeOpptjening -> TODO()
        }
        return avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerOpptjening)
    }
}
