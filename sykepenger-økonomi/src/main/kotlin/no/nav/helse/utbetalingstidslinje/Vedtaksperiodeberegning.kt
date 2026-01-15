package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel

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
    regler: MaksimumSykepengedagerregler,
    andreYtelser: (dato: LocalDate) -> Prosentdel
): List<BeregnetPeriode> {
    val maksdatoberegning = Maksdatoberegning(
        sekstisyvårsdagen = sekstisyvårsdagen,
        syttiårsdagen = syttiårsdagen,
        dødsdato = dødsdato,
        regler = regler,
        historisktidslinje = historisktidslinje
    )

    val beregnetTidslinjePerArbeidsgiver = uberegnetTidslinjePerArbeidsgiver
        .sykdomsgradsberegning(perioderMedMinimumSykdomsgradVurdertOK)
        .avvisMinsteinntekt(
            sekstisyvårsdagen = sekstisyvårsdagen,
            erUnderMinsteinntektskravTilFylte67 = erUnderMinsteinntektskravTilFylte67,
            erUnderMinsteinntektEtterFylte67 = erUnderMinsteinntektEtterFylte67,
        )
        .avvisMedlemskap(erMedlemAvFolketrygden)
        .avvisOpptjening(harOpptjening)
        .avvisMaksimumSykepengerdager(maksdatoberegning)
        .maksimumUtbetalingsberegning(sykepengegrunnlagBegrenset6G, andreYtelser)

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
