package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.inngårIkkeISykepengegrunnlaget
import no.nav.helse.person.Vedtaksperiode.Companion.manglerRefusjonsopplysninger
import no.nav.helse.person.Vedtaksperiode.Companion.manglerVilkårsgrunnlag
import no.nav.helse.person.Vedtaksperiode.Companion.ugyldigUtbetalingstidslinje
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.inntekt.ManglerRefusjonsopplysning
import no.nav.helse.økonomi.Økonomi

internal class Inntekter(
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val organisasjonsnummer: String,
    private val regler: ArbeidsgiverRegler,
    private val subsumsjonObserver: SubsumsjonObserver,
    private val vedtaksperioder: List<Vedtaksperiode> = emptyList()
) {
    internal fun medInntekt(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, økonomi: Økonomi = Økonomi.ikkeBetalt(arbeidsgiverperiode)) =
        vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi, arbeidsgiverperiode, regler, subsumsjonObserver)

    internal fun medUtbetalingsopplysninger(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, økonomi: Økonomi = Økonomi.ikkeBetalt(arbeidsgiverperiode), manglerRefusjonsopplysning: ManglerRefusjonsopplysning) = try {
        vilkårsgrunnlagHistorikk.medUtbetalingsopplysninger(organisasjonsnummer, dato, økonomi, arbeidsgiverperiode, regler, subsumsjonObserver, manglerRefusjonsopplysning)
    } catch (exception: IllegalStateException) {
        exception.håndter(dato, vedtaksperioder)
        throw exception
    }

    internal fun utenInntekt(dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?) =
        vilkårsgrunnlagHistorikk.utenInntekt(dato, økonomi, arbeidsgiverperiode)

    internal fun ugyldigUtbetalingstidslinje(dager: Set<LocalDate>) =
        vedtaksperioder.ugyldigUtbetalingstidslinje(dager)

    private companion object {
        fun IllegalStateException.håndter(dag: LocalDate, vedtaksperioder: List<Vedtaksperiode>) {
            if (manglerVilkårsgrunnlag) vedtaksperioder.manglerVilkårsgrunnlag(dag)
            if (inngårIkkeISykepengegrunnlaget) vedtaksperioder.inngårIkkeISykepengegrunnlaget(dag)
            if (manglerRefusjonsopplysninger) vedtaksperioder.manglerRefusjonsopplysninger(dag)
        }
        val IllegalStateException.manglerVilkårsgrunnlag get() = message?.startsWith("Fant ikke vilkårsgrunnlag") == true
        val IllegalStateException.inngårIkkeISykepengegrunnlaget get() = message?.startsWith("Fant ikke arbeidsgiver") == true
        val IllegalStateException.manglerRefusjonsopplysninger get() = message?.startsWith("Har ingen refusjonsopplysninger") == true
    }
}