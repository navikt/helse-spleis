package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.behandlingkilde
import no.nav.helse.person.lagArbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning

internal data object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.AVSLUTTET_UTEN_UTBETALING
    override val erFerdigBehandlet = true

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        avsluttUtenVedtak(vedtaksperiode, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    private fun avsluttUtenVedtak(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        val utbetalingstidslinje = lagArbeidsgiverberegning(listOf(vedtaksperiode))
            .single {
                val vedtaksperiodeYrkesaktivitet = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype
                when (val ya = it.yrkesaktivitet) {
                    Arbeidsgiverberegning.Yrkesaktivitet.Arbeidsledig -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Arbeidsledig
                    is Arbeidsgiverberegning.Yrkesaktivitet.Arbeidstaker -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Arbeidstaker && ya.organisasjonsnummer == vedtaksperiode.yrkesaktivitet.organisasjonsnummer
                    Arbeidsgiverberegning.Yrkesaktivitet.Frilans -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Frilans
                    Arbeidsgiverberegning.Yrkesaktivitet.Selvstendig -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Selvstendig
                }
            }
            .vedtaksperioder
            .single()

        vedtaksperiode.behandlinger.avsluttUtenVedtak(vedtaksperiode.yrkesaktivitet, aktivitetslogg, utbetalingstidslinje.utbetalingstidslinje, emptyMap())
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
            vedtaksperiode.person.skjæringstidspunkter,
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
