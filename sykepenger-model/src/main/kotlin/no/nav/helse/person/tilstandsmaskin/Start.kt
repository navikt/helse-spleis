package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.Venteårsak.Companion.utenBegrunnelse
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object Start : Vedtaksperiodetilstand {
    override val type = TilstandType.START
    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = Venteårsak.Hva.HJELP.utenBegrunnelse

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.tilstand(
            aktivitetslogg,
            when {
                !vedtaksperiode.person.infotrygdhistorikk.harHistorikk() -> AvventerInfotrygdHistorikk
                else -> when (vedtaksperiode.arbeidsgiver.yrkesaktivitetssporing) {
                    is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> AvventerInntektsmelding
                    Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                    Behandlingsporing.Yrkesaktivitet.Frilans,
                    Behandlingsporing.Yrkesaktivitet.Selvstendig,
                    Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker,
                    Behandlingsporing.Yrkesaktivitet.SelvstendigFisker,
                    Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma -> AvventerBlokkerendePeriode
                }
            }
        )
    }
}
