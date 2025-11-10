package no.nav.helse.person.tilstandsmaskin

internal data object ArbeidsledigAvventerInfotrygdHistorikk : Vedtaksperiodetilstand by(AvventerInfotrygdHistorikk) {
    override val type = TilstandType.ARBEIDSLEDIG_AVVENTER_INFOTRYGDHISTORIKK
}
