package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.util.*
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.InntekterForBeregning
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt

internal data class UberegnetVedtaksperiode(
    val vedtaksperiodeId: UUID,
    val yrkesaktivitet: Arbeidsgiverberegning.Yrkesaktivitet,
    val periode: Periode,
    val sykdomstidslinje: Sykdomstidslinje,
    val utbetalingstidslinjeBuilder: UtbetalingstidslinjeBuilder
)

internal fun lagUtbetalingstidslinjePerArbeidsgiver(vedtaksperioder: List<UberegnetVedtaksperiode>, inntekterForBeregning: InntekterForBeregning): List<Arbeidsgiverberegning> {
    val utbetalingstidslinjer = vedtaksperioder
        .groupBy({ it.yrkesaktivitet }) { vedtaksperiode ->
            val (fastsattÅrsinntekt, inntektjusteringer) = inntekterForBeregning.tilBeregning(vedtaksperiode.yrkesaktivitet)
            lagUtbetalingstidslinjeForVedtaksperiode(vedtaksperiode, fastsattÅrsinntekt, inntektjusteringer)
        }
        .map { (yrkesaktivitet, vedtaksperioder) ->
            Arbeidsgiverberegning(
                yrkesaktivitet = yrkesaktivitet,
                vedtaksperioder = vedtaksperioder,
                ghostOgAndreInntektskilder = emptyList()
            )
        }
    // nå vi må lage en ghost-tidslinje per arbeidsgiver for de som eksisterer i sykepengegrunnlaget.
    // i tillegg må vi lage én tidslinje per inntektskilde som ikke er en del av sykepengegrunnlaget
    // resultatet er én utbetalingstidslinje per arbeidsgiver som garantert dekker perioden ${vedtaksperiode.periode}, dog kan
    // andre arbeidsgivere dekke litt før/litt etter, avhengig av perioden til vedtaksperiodene som overlapper
    return inntekterForBeregning.hensyntattAlleInntektskilder(utbetalingstidslinjer)
}

internal fun filtrerUtbetalingstidslinjer(
    uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
    sykepengegrunnlag: Inntekt,
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus?,
    harOpptjening: Boolean,
    sekstisyvårsdagen: LocalDate,
    syttiårsdagen: LocalDate,
    dødsdato: LocalDate?,
    erUnderMinsteinntektskravTilFylte67: Boolean,
    erUnderMinsteinntektEtterFylte67: Boolean,
    historisktidslinje: Utbetalingstidslinje,
    perioderMedMinimumSykdomsgradVurdertOK: Set<Periode>,
    regler: MaksimumSykepengedagerregler
): List<BeregnetPeriode> {
    val maksdatoberegning = Maksdatoberegning(
        sekstisyvårsdagen = sekstisyvårsdagen,
        syttiårsdagen = syttiårsdagen,
        dødsdato = dødsdato,
        regler =regler,
        infotrygdtidslinje = historisktidslinje
    )
    val filtere = listOf(
        Sykdomsgradfilter(
            perioderMedMinimumSykdomsgradVurdertOK = perioderMedMinimumSykdomsgradVurdertOK
        ),
        Minsteinntektfilter(
            sekstisyvårsdagen = sekstisyvårsdagen,
            erUnderMinsteinntektskravTilFylte67 = erUnderMinsteinntektskravTilFylte67,
            erUnderMinsteinntektEtterFylte67 = erUnderMinsteinntektEtterFylte67,
        ),
        Medlemskapsfilter(
            medlemskapstatus = medlemskapstatus,
        ),
        Opptjeningfilter(
            harOpptjening = harOpptjening
        ),
        MaksimumSykepengedagerfilter(
            maksdatoberegning = maksdatoberegning
        ),
        MaksimumUtbetalingFilter(
            sykepengegrunnlagBegrenset6G = sykepengegrunnlag
        )
    )

    val beregnetTidslinjePerArbeidsgiver = filtere.fold(uberegnetTidslinjePerArbeidsgiver) { tidslinjer, filter ->
        filter.filter(tidslinjer)
    }

    return beregnetTidslinjePerArbeidsgiver
        .flatMap {
            it.vedtaksperioder.map { vedtaksperiodeberegning ->
                val maksdatoresultat = maksdatoberegning.beregnMaksdatoBegrensetTilPeriode(vedtaksperiodeberegning.periode)
                BeregnetPeriode(
                    vedtaksperiodeId = vedtaksperiodeberegning.vedtaksperiodeId,
                    utbetalingstidslinje = vedtaksperiodeberegning.utbetalingstidslinje,
                    maksdatoresultat = maksdatoresultat
                )
            }
        }
}

private fun lagUtbetalingstidslinjeForVedtaksperiode(vedtaksperiode: UberegnetVedtaksperiode, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje): Vedtaksperiodeberegning {
    return Vedtaksperiodeberegning(
        vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
        utbetalingstidslinje = vedtaksperiode.utbetalingstidslinjeBuilder.result(vedtaksperiode.sykdomstidslinje, inntekt, inntektjusteringer)
    )
}
