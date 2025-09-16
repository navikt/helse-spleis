package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object Start : Vedtaksperiodetilstand {
    override val type = TilstandType.START

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.tilstand(
            aktivitetslogg,
            when {
                !vedtaksperiode.person.infotrygdhistorikk.harHistorikk() -> AvventerInfotrygdHistorikk
                else -> when (vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype) {
                    is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> AvventerInntektsmelding
                    Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                    Behandlingsporing.Yrkesaktivitet.Frilans -> AvventerBlokkerendePeriode

                    Behandlingsporing.Yrkesaktivitet.Selvstendig -> error("Selvstendig skal ikke være her")
                }
            }
        )
    }
}
