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
    internal fun medInntekt(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, økonomi: Økonomi = Økonomi.ikkeBetalt(arbeidsgiverperiode)) =
        vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi, arbeidsgiverperiode, regler, subsumsjonObserver) ?: økonomi

    internal fun utenInntekt(dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?) =
        vilkårsgrunnlagHistorikk.utenInntekt(dato, økonomi, arbeidsgiverperiode)
}
