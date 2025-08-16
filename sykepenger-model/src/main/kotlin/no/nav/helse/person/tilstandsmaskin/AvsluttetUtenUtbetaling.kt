package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.Venteårsak.Companion.fordi
import no.nav.helse.person.Venteårsak.Companion.utenBegrunnelse
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.behandlingkilde
import no.nav.helse.person.inntekt.InntekterForBeregning

internal data object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.AVSLUTTET_UTEN_UTBETALING
    override val erFerdigBehandlet = true

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        val arbeidsgiverperiode = vedtaksperiode.arbeidsgiver.arbeidsgiverperiode(vedtaksperiode.periode)
        check(arbeidsgiverperiode?.forventerInntekt(vedtaksperiode.periode) != true) {
            "i granskauen! skal jo ikke skje dette ?!"
        }
        avsluttUtenVedtak(vedtaksperiode, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    private fun avsluttUtenVedtak(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        val inntekterForBeregning = with(InntekterForBeregning.Builder(vedtaksperiode.periode)) {
            vedtaksperiode.vilkårsgrunnlag?.inntektsgrunnlag?.beverte(this)
            build()
        }
        val (fastsattÅrsinntekt, inntektjusteringer) = inntekterForBeregning.tilBeregning(vedtaksperiode.arbeidsgiver.organisasjonsnummer)

        val utbetalingstidslinje = vedtaksperiode.behandlinger.lagUtbetalingstidslinje(fastsattÅrsinntekt, inntektjusteringer, vedtaksperiode.arbeidsgiver.yrkesaktivitetssporing)

        vedtaksperiode.behandlinger.avsluttUtenVedtak(vedtaksperiode.arbeidsgiver, aktivitetslogg, utbetalingstidslinje, inntekterForBeregning)
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.arbeidsgiver)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak {
        if (!vedtaksperiode.skalOmgjøres()) return Venteårsak.Hva.HJELP.utenBegrunnelse
        return Venteårsak.Hva.HJELP fordi Venteårsak.Hvorfor.VIL_OMGJØRES
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        if (!vedtaksperiode.skalOmgjøres()) null
        else vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.behandlinger.sikreNyBehandling(
            vedtaksperiode.arbeidsgiver,
            revurdering.hendelse.metadata.behandlingkilde,
            vedtaksperiode.person.beregnSkjæringstidspunkt(),
            vedtaksperiode.arbeidsgiver.beregnArbeidsgiverperiode()
        )
        if (vedtaksperiode.skalOmgjøres()) {
            revurdering.inngåSomEndring(vedtaksperiode, aktivitetslogg)
            revurdering.loggDersomKorrigerendeSøknad(
                aktivitetslogg,
                "Startet omgjøring grunnet korrigerende søknad"
            )
            vedtaksperiode.videreførEksisterendeRefusjonsopplysninger(
                behandlingkilde = revurdering.hendelse.metadata.behandlingkilde,
                dokumentsporing = null,
                aktivitetslogg = aktivitetslogg
            )
            aktivitetslogg.info("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden")
            if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) {
                aktivitetslogg.info("mangler nødvendige opplysninger fra arbeidsgiver")
                return vedtaksperiode.tilstand(aktivitetslogg, AvventerInntektsmelding)
            }
        }
        vedtaksperiode.tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
    }

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterDager(dager, aktivitetslogg)

        if (!aktivitetslogg.harFunksjonelleFeilEllerVerre()) return
        if (!vedtaksperiode.kanForkastes()) return
        vedtaksperiode.forkast(dager.hendelse, aktivitetslogg)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        if (!vedtaksperiode.skalOmgjøres() && vedtaksperiode.behandlinger.erAvsluttet()) return aktivitetslogg.info("Forventer ikke inntekt. Vil forbli i AvsluttetUtenUtbetaling")
    }
}
