package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import java.util.*

data class Vedtaksperiodeberegning(
    val vedtaksperiodeId: UUID,
    val utbetalingstidslinje: Utbetalingstidslinje,
) {
    val periode = utbetalingstidslinje.periode()
}

/**
 * Gradering av andre ytelser for en gitt inntektskilde (yrkesaktivitet) på en gitt dato
 */
typealias GraderteAndreYtelser = (inntektskilde: Arbeidsgiverberegning.Inntektskilde, dato: LocalDate) -> Prosentdel

val IngenGraderteAndreYtelser: GraderteAndreYtelser = { _, _ -> Prosentdel.NullProsent }

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
    graderteAndreYtelser: GraderteAndreYtelser,
    avslåttDag: (dato: LocalDate, begrunnelse: Begrunnelse) -> Unit = { _, _ -> },
): List<BeregnetPeriode> {
    val maksdatoberegning =
        Maksdatoberegning(
            sekstisyvårsdagen = sekstisyvårsdagen,
            syttiårsdagen = syttiårsdagen,
            dødsdato = dødsdato,
            regler = regler,
            historisktidslinje = historisktidslinje,
            avslåttDag = avslåttDag,
        )

    val beregnetTidslinjePerArbeidsgiver =
        uberegnetTidslinjePerArbeidsgiver
            .sykdomsgradsberegning(perioderMedMinimumSykdomsgradVurdertOK, graderteAndreYtelser)
            .avvisMinsteinntekt(
                sekstisyvårsdagen = sekstisyvårsdagen,
                erUnderMinsteinntektskravTilFylte67 = erUnderMinsteinntektskravTilFylte67,
                erUnderMinsteinntektEtterFylte67 = erUnderMinsteinntektEtterFylte67,
            ).avvisMedlemskap(erMedlemAvFolketrygden)
            .avvisOpptjening(harOpptjening)
            .avvisMaksimumSykepengerdager(maksdatoberegning)
            .maksimumUtbetalingsberegning(sykepengegrunnlagBegrenset6G, graderteAndreYtelser)

    return beregnetTidslinjePerArbeidsgiver
        .flatMap {
            it.vedtaksperioder.map { vedtaksperiodeberegning ->
                val maksdatoresultat = maksdatoberegning.beregnMaksdatoBegrensetTilPeriode(vedtaksperiodeberegning.periode)
                BeregnetPeriode(
                    vedtaksperiodeId = vedtaksperiodeberegning.vedtaksperiodeId,
                    utbetalingstidslinje = vedtaksperiodeberegning.utbetalingstidslinje,
                    maksdatoresultat = maksdatoresultat,
                )
            }
        }
}
