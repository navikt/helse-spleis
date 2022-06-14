package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Økonomi

internal class Inntekter(
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val organisasjonsnummer: String,
    private val regler: ArbeidsgiverRegler,
    private val subsumsjonObserver: SubsumsjonObserver
) {
    internal fun medInntekt(dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?) =
        vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi, arbeidsgiverperiode, regler, subsumsjonObserver)

    internal fun medFrivilligInntekt(dato: LocalDate, økonomi: Økonomi) =
        vilkårsgrunnlagHistorikk.medFrivilligInntekt(organisasjonsnummer, dato, økonomi, null, regler, subsumsjonObserver)

    internal fun medSkjæringstidspunkt(dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?) =
        vilkårsgrunnlagHistorikk.medIngenInntekt(dato, økonomi, arbeidsgiverperiode)
}
