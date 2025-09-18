package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object TilInfotrygd : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_INFOTRYGD
    override val erFerdigBehandlet = true
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Vedtaksperioden kan ikke behandles i Spleis.")
    }

    override fun skalHåndtereDager(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) = false
    override fun håndterKorrigerendeInntektsmelding(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {}

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        throw IllegalStateException("Revurdering håndteres av en periode i til_infotrygd")
    }
}
