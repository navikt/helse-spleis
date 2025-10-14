package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Inntekt

data class Vedtaksperiodeberegning(
    val vedtaksperiodeId: UUID,
    val utbetalingstidslinje: Utbetalingstidslinje
) {
    val periode = utbetalingstidslinje.periode()
}

fun filtrerUtbetalingstidslinjer(
    uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
    sykepengegrunnlagBegrenset6G: Inntekt,
    erMedlemAvFolketrygden: Boolean,
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
            erMedlemAvFolketrygden = erMedlemAvFolketrygden,
        ),
        Opptjeningfilter(
            harOpptjening = harOpptjening
        ),
        MaksimumSykepengedagerfilter(
            maksdatoberegning = maksdatoberegning
        ),
        MaksimumUtbetalingFilter(
            sykepengegrunnlagBegrenset6G = sykepengegrunnlagBegrenset6G
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
