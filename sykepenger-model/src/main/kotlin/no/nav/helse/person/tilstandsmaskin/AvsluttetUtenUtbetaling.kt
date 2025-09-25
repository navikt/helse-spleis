package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.behandlingkilde
import no.nav.helse.utbetalingstidslinje.beregning.BeregningRequest
import no.nav.helse.utbetalingstidslinje.beregning.beregnUtbetalinger
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

internal data object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.AVSLUTTET_UTEN_UTBETALING
    override val erFerdigBehandlet = true

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        avsluttUtenVedtak(vedtaksperiode, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    private fun avsluttUtenVedtak(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        val dataForBeregning = vedtaksperiode.dataForBeregning()
        val request = BeregningRequest(
            perioderSomMåHensyntasVedBeregning = listOf(
                BeregningRequest.VedtaksperiodeForBeregning(
                    vedtaksperiodeId = vedtaksperiode.id,
                    sykdomstidslinje = vedtaksperiode.sykdomstidslinje,
                    dataForBeregning = dataForBeregning
                )
            ),
            fastsatteÅrsinntekter = with (vedtaksperiode) {
                vilkårsgrunnlag?.fastsatteÅrsinntekter()
            } ?: emptyMap(),
            selvstendigNæringsdrivende = null,
            inntektjusteringer = emptyMap()
        )
        val response = beregnUtbetalinger(request)
        val utbetalingstidslinje = response.yrkesaktiviteter.single { it.yrkesaktivitet == dataForBeregning.yrkesaktivitet }.vedtaksperioder.single().utbetalingstidslinje
        vedtaksperiode.behandlinger.avsluttUtenVedtak(vedtaksperiode.yrkesaktivitet, aktivitetslogg, utbetalingstidslinje, emptyMap())
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.yrkesaktivitet)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.behandlinger.sikreNyBehandling(
            vedtaksperiode.yrkesaktivitet,
            revurdering.hendelse.metadata.behandlingkilde,
            vedtaksperiode.person.beregnSkjæringstidspunkt(),
            vedtaksperiode.yrkesaktivitet.beregnArbeidsgiverperiode()
        )
        if (vedtaksperiode.skalBehandlesISpeil()) {
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

    override fun håndterKorrigerendeInntektsmelding(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterDager(dager, aktivitetslogg)

        if (!aktivitetslogg.harFunksjonelleFeilEllerVerre()) return
        if (!vedtaksperiode.kanForkastes()) return
        vedtaksperiode.forkast(dager.hendelse, aktivitetslogg)
    }
}
