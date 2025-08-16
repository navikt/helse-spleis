package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.etterlevelse.`fvl § 35 ledd 1`
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.Venteårsak.Companion.utenBegrunnelse
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.behandlingkilde

internal data object Avsluttet : Vedtaksperiodetilstand {
    override val type = TilstandType.AVSLUTTET

    override val erFerdigBehandlet = true
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftAvsluttetBehandlingMedVedtak(vedtaksperiode.arbeidsgiver)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) =
        Venteårsak.Hva.HJELP.utenBegrunnelse

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterKorrigerendeInntektsmelding(dager, FunksjonelleFeilTilVarsler(aktivitetslogg))
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.arbeidsgiver)
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
            vedtaksperiode.arbeidsgiver,
            revurdering.hendelse.metadata.behandlingkilde,
            vedtaksperiode.person.beregnSkjæringstidspunkt(),
            vedtaksperiode.arbeidsgiver.beregnArbeidsgiverperiode()
        )
        vedtaksperiode.subsumsjonslogg.logg(`fvl § 35 ledd 1`())
        revurdering.inngåSomRevurdering(vedtaksperiode, aktivitetslogg)
        vedtaksperiode.tilstand(aktivitetslogg, AvventerRevurdering)
    }
}
