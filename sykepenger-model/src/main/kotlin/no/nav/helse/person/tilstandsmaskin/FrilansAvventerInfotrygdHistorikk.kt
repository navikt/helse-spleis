package no.nav.helse.person.tilstandsmaskin

internal data object FrilansAvventerInfotrygdHistorikk : Vedtaksperiodetilstand by(AvventerInfotrygdHistorikk) {
    override val type = TilstandType.FRILANS_AVVENTER_INFOTRYGDHISTORIKK
}
