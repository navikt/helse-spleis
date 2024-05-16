package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.inngårIkkeISykepengegrunnlaget
import no.nav.helse.person.Vedtaksperiode.Companion.manglerRefusjonsopplysninger
import no.nav.helse.person.Vedtaksperiode.Companion.manglerVilkårsgrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.økonomi.Økonomi

internal class Inntekter(
    private val hendelse: IAktivitetslogg,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val organisasjonsnummer: String,
    private val regler: ArbeidsgiverRegler,
    private val subsumsjonslogg: Subsumsjonslogg,
    private val vedtaksperioder: List<Vedtaksperiode> = emptyList()
) {
    internal fun medInntekt(dato: LocalDate, økonomi: Økonomi) =
        vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)

    internal fun medUtbetalingsopplysninger(dato: LocalDate, økonomi: Økonomi) = try {
        vilkårsgrunnlagHistorikk.medUtbetalingsopplysninger(hendelse, organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
    } catch (exception: IllegalStateException) {
        exception.håndter(hendelse, dato, vedtaksperioder)
        throw exception
    }

    private companion object {
        fun IllegalStateException.håndter(hendelse: IAktivitetslogg, dag: LocalDate, vedtaksperioder: List<Vedtaksperiode>) {
            if (manglerVilkårsgrunnlag) vedtaksperioder.manglerVilkårsgrunnlag(hendelse, dag)
            if (inngårIkkeISykepengegrunnlaget) vedtaksperioder.inngårIkkeISykepengegrunnlaget(hendelse, dag)
            if (manglerRefusjonsopplysninger) vedtaksperioder.manglerRefusjonsopplysninger(hendelse, dag)
        }
        val IllegalStateException.manglerVilkårsgrunnlag get() = message?.startsWith("Fant ikke vilkårsgrunnlag") == true
        val IllegalStateException.inngårIkkeISykepengegrunnlaget get() = message?.startsWith("Fant ikke arbeidsgiver") == true
        val IllegalStateException.manglerRefusjonsopplysninger get() = message?.startsWith("Har ingen refusjonsopplysninger") == true
    }
}