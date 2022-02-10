package no.nav.helse.utbetalingstidslinje.ny

import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException.ManglerInntektException
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException.NegativDekningsgrunnlagException
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class Inntekter(
    private val skjæringstidspunkter: List<LocalDate>,
    private val inntektPerSkjæringstidspunkt: Map<LocalDate, Inntektshistorikk.Inntektsopplysning?>?,
    private val regler: ArbeidsgiverRegler,
    private val subsumsjonObserver: SubsumsjonObserver
) {

    internal fun medInntekt(dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?): Økonomi {
        val (skjæringstidspunkt, inntekt) = inntektForDato(dato)
        return økonomi.inntekt(
            aktuellDagsinntekt = inntekt,
            dekningsgrunnlag = dekningsgrunnlag(inntekt, dato, skjæringstidspunkt),
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverperiode = arbeidsgiverperiode
        )
    }

    internal fun medFrivilligInntekt(dato: LocalDate, økonomi: Økonomi): Økonomi {
        val (skjæringstidspunkt, inntekt) = inntektForDatoOrNull(dato) ?: return økonomi
        return økonomi.inntekt(
            aktuellDagsinntekt = inntekt,
            dekningsgrunnlag = dekningsgrunnlag(inntekt, dato, skjæringstidspunkt),
            skjæringstidspunkt = skjæringstidspunkt
        )
    }

    internal fun medSkjæringstidspunkt(dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?): Økonomi {
        val skjæringstidspunkt = inntektForDatoOrNull(dato)?.first ?: dato
        return økonomi.inntekt(
            aktuellDagsinntekt = INGEN,
            dekningsgrunnlag = INGEN,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverperiode = arbeidsgiverperiode
        )
    }

    private fun inntektForDatoOrNull(dato: LocalDate) =
        skjæringstidspunkter
            .sorted()
            .lastOrNull { it <= dato }
            ?.let { skjæringstidspunkt ->
                finnInntekt(skjæringstidspunkt, dato)?.let { inntektsopplysning ->
                    skjæringstidspunkt to inntektsopplysning.grunnlagForSykepengegrunnlag()
                }
            }

    private fun finnInntekt(skjæringstidspunkt: LocalDate, dato: LocalDate) = inntektPerSkjæringstidspunkt?.get(skjæringstidspunkt)
        ?: inntektPerSkjæringstidspunkt?.entries?.firstOrNull { (key) -> key in skjæringstidspunkt..dato }?.value

    private fun inntektForDato(dato: LocalDate) = inntektForDatoOrNull(dato) ?: throw ManglerInntektException(dato, skjæringstidspunkter)
    private fun dekningsgrunnlag(inntekt: Inntekt, dagen: LocalDate, skjæringstidspunkt: LocalDate): Inntekt {
        val dekningsgrunnlag = inntekt.dekningsgrunnlag(dagen, regler, subsumsjonObserver)
        if (dekningsgrunnlag < INGEN) throw NegativDekningsgrunnlagException(dekningsgrunnlag, dagen, skjæringstidspunkt)
        return dekningsgrunnlag
    }
}
