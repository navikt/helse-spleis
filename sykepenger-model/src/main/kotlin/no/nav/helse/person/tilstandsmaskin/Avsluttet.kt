package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.etterlevelse.`fvl § 35 ledd 1`
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.behandlingkilde

internal data object Avsluttet : Vedtaksperiodetilstand {
    override val type = TilstandType.AVSLUTTET

    override val erFerdigBehandlet = true
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftAvsluttetBehandlingMedVedtak(vedtaksperiode.yrkesaktivitet)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun håndterKorrigerendeInntektsmelding(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterKorrigerendeInntektsmelding(dager, FunksjonelleFeilTilVarsler(aktivitetslogg))
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.yrkesaktivitet)
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        vedtaksperiode.skalHåndtereDagerRevurdering(dager, aktivitetslogg)

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
        vedtaksperiode.subsumsjonslogg.logg(`fvl § 35 ledd 1`())
        revurdering.inngåSomRevurdering(vedtaksperiode, aktivitetslogg)
        vedtaksperiode.tilstand(aktivitetslogg, AvventerRevurdering)
    }
}
